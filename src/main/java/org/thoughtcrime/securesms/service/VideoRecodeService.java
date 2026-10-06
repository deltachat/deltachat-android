package org.thoughtcrime.securesms.service;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.transformer.ExportException;
import com.b44t.messenger.DcContext;
import java.io.File;
import org.thoughtcrime.securesms.DummyActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.notifications.NotificationCenter;
import org.thoughtcrime.securesms.util.IntentUtils;
import org.thoughtcrime.securesms.video.RecodeJob;
import org.thoughtcrime.securesms.video.VideoRecodeManager;
import org.thoughtcrime.securesms.video.VideoTranscoder;

/**
 * Serially recodes queued videos and sends them when done. Started through {@link #enqueueWork};
 * stops itself when the queue is empty.
 */
@OptIn(markerClass = UnstableApi.class)
public class VideoRecodeService extends Service {

  private static final String TAG = "VideoRecodeService";
  private static final String ACTION_PROCESS = "video_recode_process";
  private static final String ACTION_CANCEL = "video_recode_cancel";
  private static final String EXTRA_JOB_ID = "job_id";
  private static final int NOTIFICATION_ID = 3524;
  private static final int ONESHOT_NOTIFICATION_ID = 3525;

  public static void enqueueWork(Context context) {
    try {
      ContextCompat.startForegroundService(
          context, new Intent(context, VideoRecodeService.class).setAction(ACTION_PROCESS));
    } catch (Exception e) {
      // if the app was backgrounded between tapping send and here, the job stays queued
      // in VideoRecodeManager; it will be processed on the next enqueue
      Log.w(TAG, "Could not start recode service", e);
    }
  }

  private VideoRecodeManager manager;
  private RecodeJob job;
  private VideoTranscoder transcoder;
  private boolean lowTier;
  private boolean stopping;
  private boolean foreground;
  private int curW;
  private int curH;
  private int jobsDone;

  @Override
  public void onCreate() {
    super.onCreate();
    try {
      android.content.pm.ServiceInfo info =
          getPackageManager()
              .getServiceInfo(new android.content.ComponentName(this, VideoRecodeService.class), 0);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Log.d(TAG, "installed FGS type = " + info.getForegroundServiceType());
      }
    } catch (PackageManager.NameNotFoundException e) {
      Log.w(TAG, "service info not found", e);
    }
    manager = VideoRecodeManager.getInstance(this);
    GenericForegroundService.createFgNotificationChannel(this);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent == null) {
      return START_NOT_STICKY;
    }
    if (ACTION_CANCEL.equals(intent.getAction())) {
      manager.cancel(intent.getIntExtra(EXTRA_JOB_ID, 0));
      return START_NOT_STICKY;
    }
    if (!foreground) {
      foreground = true;
      startForegroundWithNotification(0);
    }
    processNext();
    return START_NOT_STICKY;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private void processNext() {
    if (stopping || job != null) {
      return;
    }
    job = manager.nextJob();
    if (job == null) {
      shutdown();
      return;
    }
    Log.i(TAG, "Processing recode job " + job.id);
    lowTier = false;
    startTier();
  }

  private void startTier() {
    if (job.cancelRequested) {
      handleCancelled();
      return;
    }
    int[] wh = job.sizeForTier(lowTier);
    curW = wh[0];
    curH = wh[1];
    transcoder = new VideoTranscoder(this, transcoderCallback);
    manager.onJobStarted(job, transcoder);
    updateNotification(job.lastProgress);
    transcoder.start(
        job.inputPath, job.outputPath, curW, curH, job.videoBitrateForTier(lowTier), job.hasAudio);
  }

  private final VideoTranscoder.Callback transcoderCallback =
      new VideoTranscoder.Callback() {
        @Override
        public void onProgress(int progress) {
          // pass 1 0-60%, pass 2 (if needed) 60-100%
          int mapped = lowTier ? 60 + progress * 2 / 5 : progress * 3 / 5;
          manager.notifyProgress(job, mapped);
          updateNotification(mapped);
        }

        @Override
        public void onCompleted() {
          File out = new File(job.outputPath);
          long size = out.length();
          if (size <= 0) {
            handleFailed(null);
          } else if (size > job.maxBytes) {
            //noinspection ResultOfMethodCallIgnored
            out.delete();
            if (!lowTier) {
              lowTier = true;
              startTier();
            } else {
              finishJob(VideoRecodeManager.Result.FAILED_TOO_BIG);
            }
          } else {
            finishJob(
                sendRecoded()
                    ? VideoRecodeManager.Result.SENT
                    : VideoRecodeManager.Result.FAILED_ERROR);
          }
        }

        @Override
        public void onFailed(@Nullable ExportException error) {
          handleFailed(error);
        }

        @Override
        public void onCancelled() {
          handleCancelled();
        }
      };

  private void handleFailed(@Nullable ExportException error) {
    Log.w(TAG, "recode failed", error);
    //noinspection ResultOfMethodCallIgnored
    new File(job.outputPath).delete();
    if (job.originalBytes <= job.maxBytes) {
      DcContext dcContext = manager.getDcContextByAcc(job.accountId);
      job.msg.setDimension(job.dispW, job.dispH);
      job.msg.setDuration((int) job.durationMs);
      if (dcContext.sendMsg(job.chatId, job.msg) != 0) {
        finishJob(VideoRecodeManager.Result.SENT_ORIGINAL);
        return;
      }
      finishJob(VideoRecodeManager.Result.FAILED_ERROR);
      return;
    }
    finishJob(VideoRecodeManager.Result.FAILED_TOO_BIG);
  }

  private void handleCancelled() {
    //noinspection ResultOfMethodCallIgnored
    new File(job.outputPath).delete();
    finishJob(VideoRecodeManager.Result.CANCELLED);
  }

  private boolean sendRecoded() {
    DcContext dc = manager.getDcContextByAcc(job.accountId);
    // output is always MP4/H.264 now, fix file name and mime type.
    // Also fixes https://github.com/deltachat/deltachat-android/issues/3928#issuecomment-4422383476
    String name = job.msg.getFilename();
    String base = TextUtils.isEmpty(name) ? "video" : name;
    int dot = base.lastIndexOf('.');
    if (dot > 0) {
      base = base.substring(0, dot);
    }
    job.msg.setFileAndDeduplicate(job.outputPath, base + ".mp4", "video/mp4");
    job.msg.setDimension(curW, curH);
    job.msg.setDuration((int) job.durationMs);
    int msgId = dc.sendMsg(job.chatId, job.msg);
    if (msgId == 0) {
      Log.w(TAG, "sendMsg failed: " + dc.getLastError());
      //noinspection ResultOfMethodCallIgnored
      new File(job.outputPath).delete();
      return false;
    }
    return true;
  }

  private void finishJob(VideoRecodeManager.Result result) {
    RecodeJob done = job;
    job = null;
    transcoder = null;
    manager.onJobFinished();
    jobsDone++;
    maybePostOneShotNotification(result);
    manager.notifyFinished(done, result);
    processNext();
  }

  private void maybePostOneShotNotification(VideoRecodeManager.Result result) {
    String text;
    if (result == VideoRecodeManager.Result.FAILED_TOO_BIG) {
      text = VideoRecodeManager.TOO_BIG_MSG;
    } else if (result == VideoRecodeManager.Result.SENT_ORIGINAL) {
      text = VideoRecodeManager.SENT_ORIGINAL_MSG;
    } else {
      return;
    }
    if (manager.hasListeners()) {
      return;
    }
    Notification notification =
        new NotificationCompat.Builder(this, NotificationCenter.CH_GENERIC)
            .setSmallIcon(R.drawable.notification_permanent)
            .setContentTitle(getString(R.string.video_compressing))
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, new Intent(this, DummyActivity.class), IntentUtils.FLAG_MUTABLE()))
            .build();
    NotificationManagerCompat.from(this).notify(ONESHOT_NOTIFICATION_ID, notification);
  }

  @Override
  public void onTimeout(int startId, int fgsType) {
    Log.w(TAG, "onTimeout: stopping");
    stopping = true;
    if (transcoder != null) {
      transcoder.cancel();
    }
    shutdown();
  }

  @Override
  public void onDestroy() {
    stopping = true;
    if (transcoder != null) {
      transcoder.cancel();
    }
    super.onDestroy();
  }

  private void shutdown() {
    if (foreground) {
      foreground = false;
      ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
    }
    stopSelf();
  }

  @SuppressLint("InlinedApi")
  private void startForegroundWithNotification(int progress) {
    try {
      int type = FOREGROUND_SERVICE_TYPE_MANIFEST;
      Log.d(
          TAG,
          "startForeground: sdk=" + Build.VERSION.SDK_INT + " type=0x" + Integer.toHexString(type));
      ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(progress), type);
    } catch (Exception e) {
      Log.w(TAG, "startForeground failed", e);
    }
  }

  private void updateNotification(int progress) {
    if (foreground) {
      NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification(progress));
    }
  }

  private Notification buildNotification(int progress) {
    NotificationCompat.Builder builder =
        new NotificationCompat.Builder(this, NotificationCenter.CH_GENERIC)
            .setSmallIcon(R.drawable.notification_permanent)
            .setContentTitle(getString(R.string.video_compressing))
            .setProgress(100, progress, progress <= 0)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, new Intent(this, DummyActivity.class), IntentUtils.FLAG_MUTABLE()));
    int total = jobsDone + (job != null ? 1 : 0) + manager.queuedCount();
    if (total > 1 && job != null) {
      builder.setContentText((jobsDone + 1) + "/" + total);
    }
    if (job != null) {
      Intent cancel =
          new Intent(this, VideoRecodeService.class)
              .setAction(ACTION_CANCEL)
              .putExtra(EXTRA_JOB_ID, job.id);
      builder.addAction(
          0,
          getString(android.R.string.cancel),
          PendingIntent.getService(
              this,
              job.id,
              cancel,
              PendingIntent.FLAG_UPDATE_CURRENT | IntentUtils.FLAG_MUTABLE()));
    }
    return builder.build();
  }
}
