package org.thoughtcrime.securesms.service;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.util.UnstableApi;
import org.thoughtcrime.securesms.DummyActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.notifications.NotificationCenter;
import org.thoughtcrime.securesms.util.IntentUtils;
import org.thoughtcrime.securesms.video.RecodeState;
import org.thoughtcrime.securesms.video.VideoRecodeManager;

/**
 * FGS for video recoding, keeps the process alive and renders progress notifications from {@link
 * RecodeState}. All logic lives in {@link VideoRecodeManager}.
 */
@OptIn(markerClass = UnstableApi.class)
public class VideoRecodeService extends Service {

  private static final String TAG = "VideoRecodeService";
  private static final String ACTION_CANCEL = "video_recode_cancel";
  private static final int NOTIFICATION_ID = 3524;
  private static final int ONESHOT_NOTIFICATION_ID = 3525;
  private static VideoRecodeService sThis;

  public static void onStateChanged(Context context, RecodeState state) {
    if (sThis != null) {
      sThis.render(state);
      return;
    }
    if (state.isIdle()) {
      return;
    }
    try {
      ContextCompat.startForegroundService(context, new Intent(context, VideoRecodeService.class));
    } catch (Exception e) {
      Log.w(TAG, "Could not start recode service", e);
    }
  }

  public static void postOnce(Context context, String text) {
    GenericForegroundService.createFgNotificationChannel(context);
    Notification notification =
        new NotificationCompat.Builder(context, NotificationCenter.CH_GENERIC)
            .setSmallIcon(R.drawable.notification_permanent)
            .setContentTitle(context.getString(R.string.video_compressing))
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    new Intent(context, DummyActivity.class),
                    IntentUtils.FLAG_MUTABLE()))
            .build();
    try {
      NotificationManagerCompat.from(context).notify(ONESHOT_NOTIFICATION_ID, notification);
    } catch (Exception e) {
      Log.w(TAG, "One-time notification failed", e);
    }
  }

  private boolean foreground;

  @Override
  public void onCreate() {
    super.onCreate();
    sThis = this;
    GenericForegroundService.createFgNotificationChannel(this);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null && ACTION_CANCEL.equals(intent.getAction())) {
      VideoRecodeManager.getInstance(this).cancelOperation();
      return START_NOT_STICKY;
    }
    RecodeState state = VideoRecodeManager.getInstance(this).getState();
    foreground = startForegroundNow(buildNotification(state)) || foreground;
    render(state);
    return START_NOT_STICKY;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private void render(RecodeState state) {
    if (state.isIdle()) {
      shutdown();
      return;
    }
    if (!foreground) {
      return;
    }
    try {
      NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification(state));
    } catch (Exception e) {
      Log.w(TAG, "Progress notification failed", e);
    }
  }

  @SuppressLint("InlinedApi")
  private boolean startForegroundNow(Notification notification) {
    try {
      // raw Service.startForeground on purpose: ServiceCompat masks the type and turns
      // mediaProcessing into 0, which crashes on targetSdk 34+.
      if (Build.VERSION.SDK_INT >= 35) {
        startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING);
      } else if (Build.VERSION.SDK_INT >= 29) {
        startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC);
      } else {
        startForeground(NOTIFICATION_ID, notification);
      }
      return true;
    } catch (Exception e) {
      Log.w(TAG, "startForeground failed", e);
      return false;
    }
  }

  private Notification buildNotification(RecodeState state) {
    String title =
        getString(
            state.phase == RecodeState.Phase.INGEST
                ? R.string.video_preparing
                : R.string.video_compressing);
    NotificationCompat.Builder builder =
        new NotificationCompat.Builder(this, NotificationCenter.CH_GENERIC)
            .setSmallIcon(R.drawable.notification_permanent)
            .setContentTitle(title)
            .setProgress(100, state.progress, state.progress <= 0)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, new Intent(this, DummyActivity.class), IntentUtils.FLAG_MUTABLE()));
    if (state.total > 1) {
      builder.setContentText(state.position() + "/" + state.total);
    }
    if (!state.isIdle()) {
      Intent cancel = new Intent(this, VideoRecodeService.class).setAction(ACTION_CANCEL);
      builder.addAction(
          0,
          getString(android.R.string.cancel),
          PendingIntent.getService(
              this, 0, cancel, PendingIntent.FLAG_UPDATE_CURRENT | IntentUtils.FLAG_MUTABLE()));
    }
    return builder.build();
  }

  private void shutdown() {
    if (foreground) {
      foreground = false;
      ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
    }
    NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
    stopSelf();
  }

  @Override
  public void onTimeout(int startId, int fgsType) {
    Log.w(TAG, "onTimeout: cancelling video recode operation");
    VideoRecodeManager.getInstance(this).cancelOperation();
    shutdown();
  }

  @Override
  public void onDestroy() {
    if (sThis == this) {
      sThis = null;
    }
    super.onDestroy();
  }
}
