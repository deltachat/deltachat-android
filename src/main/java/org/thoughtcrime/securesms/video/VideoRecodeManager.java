package org.thoughtcrime.securesms.video;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.service.VideoRecodeService;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;

/**
 * Decides whether a video needs recoding and queues recode jobs; {@link VideoRecodeService} does
 * the actual work and sends the message when done.
 *
 * <p>Policy: recode when the file exceeds sys.msgsize_max_recommended, or when the bitrate exceeds
 * ~3 Mbps, or always when "hard compression" is enabled. Recoding runs a medium pass first (~6
 * MB/min) and a low pass (~1.5 MB/min) only if the result does not fit size limit.
 */
@SuppressLint("StaticFieldLeak") // holds only application context
public class VideoRecodeManager {

  private static final String TAG = "VideoRecodeManager";

  static final int MEDIUM_BPS = 800_000; // ~6 MB/min
  static final int LOW_BPS = 200_000; // ~1.5 MB/min
  static final int AUDIO_BPS = 96_000;
  static final int MEDIUM_MAX_SIDE = 640;
  static final int LOW_MAX_SIDE = 480;
  private static final long BITRATE_RECODE_THRESHOLD = 3_000_000L;
  private static final long FALLBACK_MAX_BYTES = 25L * 1024 * 1024; // 25 min

  public static final String TOO_BIG_MSG =
      "Video cannot be compressed to a reasonable size. Try a shorter video or a lower quality.";
  public static final String SENT_ORIGINAL_MSG =
      "Could not recode video; sending it at its original size.";

  public enum Result {
    SENT,
    SENT_ORIGINAL,
    FAILED_TOO_BIG,
    FAILED_ERROR,
    CANCELLED
  }

  public interface Listener {
    void onProgress(int jobId, int progress);

    void onFinished(int jobId, boolean fromComposer, Result result);
  }

  private static VideoRecodeManager instance;

  public static synchronized VideoRecodeManager getInstance(Context context) {
    if (instance == null) {
      instance = new VideoRecodeManager(context.getApplicationContext());
    }
    return instance;
  }

  private final Context context;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private final ArrayDeque<RecodeJob> queue = new ArrayDeque<>();
  private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
  private final AtomicInteger jobIds = new AtomicInteger(1);
  private RecodeJob activeJob;
  private VideoTranscoder activeTranscoder;

  private VideoRecodeManager(Context context) {
    this.context = context;
  }

  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  public boolean hasListeners() {
    return !listeners.isEmpty();
  }

  /**
   * Must be called on a background thread.
   *
   * @param uiContext used for user-visible alerts
   * @return jobId &gt; 0: enqueued, the service will recode and then send the message; 0: no recode
   *     needed, the caller should send the message directly; -1: aborted, the user alerted
   */
  @WorkerThread
  public synchronized int submitForSend(
      Context uiContext, int accountId, int chatId, DcMsg msg, boolean fromComposer) {
    String inPath = msg.getFile();
    if (inPath == null || inPath.isEmpty() || !new File(inPath).canRead()) {
      return 0;
    }
    DcContext dc = getDcContextByAcc(accountId);
    long maxBytes = maxBytesFor(dc);
    long inBytes = new File(inPath).length();

    VideoMeta meta = probe(inPath);
    if (meta == null || meta.durationMs <= 0) {
      // Cannot parse. Send small files as-is, refuse large ones.
      if (inBytes > maxBytes) {
        showTooBig(uiContext);
        return -1;
      }
      return 0;
    }

    msg.setDimension(meta.dispW, meta.dispH);
    msg.setDuration((int) meta.durationMs);

    long bitrate =
        meta.overallBitrate > 0
            ? meta.overallBitrate
            : (inBytes * 8 / Math.max(1, meta.durationMs / 1000));
    boolean hard = Prefs.isHardCompressionEnabled(context);
    if (!hard && inBytes <= maxBytes && bitrate <= BITRATE_RECODE_THRESHOLD) {
      return 0;
    }

    long lowEstimate = (long) LOW_BPS / 8 * (meta.durationMs / 1000);
    if (lowEstimate > maxBytes + maxBytes / 4) {
      showTooBig(uiContext);
      return -1;
    }

    RecodeJob job = new RecodeJob();
    job.id = jobIds.getAndIncrement();
    job.accountId = accountId;
    job.chatId = chatId;
    job.msg = msg;
    job.inputPath = inPath;
    job.outputPath = DcHelper.getBlobdirFile(dc, inPath);
    job.maxBytes = maxBytes;
    job.originalBytes = inBytes;
    job.durationMs = meta.durationMs;
    job.hasAudio = meta.hasAudio;
    job.dispW = meta.dispW;
    job.dispH = meta.dispH;
    job.fromComposer = fromComposer;
    queue.add(job);
    VideoRecodeService.enqueueWork(context);
    Log.i(TAG, "Enqueued recode job " + job.id + " for chat " + chatId);
    return job.id;
  }

  public void cancelAllForChat(int accountId, int chatId) {
    mainHandler.post(
        () -> {
          synchronized (this) {
            Iterator<RecodeJob> it = queue.iterator();
            while (it.hasNext()) {
              RecodeJob j = it.next();
              if (j.accountId == accountId && j.chatId == chatId) {
                it.remove();
                notifyFinished(j, Result.CANCELLED);
              }
            }
            if (activeJob != null
                && activeJob.accountId == accountId
                && activeJob.chatId == chatId) {
              if (activeTranscoder != null) {
                activeTranscoder.cancel();
              } else {
                activeJob.cancelRequested = true;
              }
            }
          }
        });
  }

  public void cancelAllForJob(int jobId) {
    mainHandler.post(
        () -> {
          int accountId;
          int chatId;
          synchronized (this) {
            RecodeJob target = null;
            if (activeJob != null && activeJob.id == jobId) {
              target = activeJob;
            } else {
              for (RecodeJob j : queue) {
                if (j.id == jobId) {
                  target = j;
                  break;
                }
              }
            }
            if (target == null) {
              return;
            }
            accountId = target.accountId;
            chatId = target.chatId;
          }
          cancelAllForChat(accountId, chatId);
        });
  }

  public synchronized int findJob(int accountId, int chatId) {
    if (activeJob != null && activeJob.accountId == accountId && activeJob.chatId == chatId) {
      return activeJob.id;
    }
    for (RecodeJob j : queue) {
      if (j.accountId == accountId && j.chatId == chatId) {
        return j.id;
      }
    }
    return 0;
  }

  public synchronized int progressFor(int jobId) {
    if (activeJob != null && activeJob.id == jobId) {
      return activeJob.lastProgress;
    }
    for (RecodeJob j : queue) {
      if (j.id == jobId) {
        return j.lastProgress;
      }
    }
    return 0;
  }

  public synchronized int queuedCount() {
    return queue.size();
  }

  public synchronized RecodeJob nextJob() {
    activeJob = queue.poll();
    activeTranscoder = null;
    return activeJob;
  }

  public synchronized void onJobStarted(RecodeJob job, VideoTranscoder transcoder) {
    activeJob = job;
    activeTranscoder = transcoder;
  }

  public synchronized void onJobFinished() {
    activeJob = null;
    activeTranscoder = null;
  }

  public DcContext getDcContextByAcc(int accountId) {
    try {
      DcContext dc = DcHelper.getAccounts(context).getAccount(accountId);
      if (dc != null) {
        return dc;
      }
    } catch (Exception e) {
      Log.w(TAG, "Cannot get account " + accountId, e);
    }
    return DcHelper.getContext(context);
  }

  public void notifyProgress(RecodeJob job, int progress) {
    job.lastProgress = progress;
    mainHandler.post(
        () -> {
          for (Listener l : listeners) {
            l.onProgress(job.id, progress);
          }
        });
  }

  public void notifyFinished(RecodeJob job, Result result) {
    boolean fromComposer = job.fromComposer;
    mainHandler.post(
        () -> {
          for (Listener l : listeners) {
            l.onFinished(job.id, fromComposer, result);
          }
        });
  }

  private long maxBytesFor(DcContext dc) {
    try {
      String v = dc.getConfig("sys.msgsize_max_recommended");
      if (v != null && !v.isEmpty()) {
        return Long.parseLong(v);
      }
    } catch (Exception e) {
      Log.w(TAG, "sys.msgsize_max_recommended unreadable", e);
    }
    return FALLBACK_MAX_BYTES;
  }

  private void showTooBig(Context uiContext) {
    Util.runOnMain(
        () -> {
          if (uiContext instanceof Activity && !((Activity) uiContext).isFinishing()) {
            new AlertDialog.Builder(uiContext)
                .setCancelable(false)
                .setMessage(TOO_BIG_MSG)
                .setPositiveButton(android.R.string.ok, null)
                .show();
          }
        });
  }

  private static class VideoMeta {
    int width;
    int height;
    int rotation;
    int dispW;
    int dispH;
    long durationMs;
    long overallBitrate;
    boolean hasAudio;
  }

  @SuppressLint("InlinedApi")
  @Nullable
  private static VideoMeta probe(String path) {
    VideoMeta meta = new VideoMeta();
    boolean foundVideo = false;
    MediaExtractor extractor = new MediaExtractor();
    try {
      extractor.setDataSource(path);
      for (int i = 0; i < extractor.getTrackCount(); i++) {
        MediaFormat format = extractor.getTrackFormat(i);
        String mime = format.getString(MediaFormat.KEY_MIME);
        if (mime == null) {
          continue;
        }
        if (mime.startsWith("video/")) {
          foundVideo = true;
          meta.width = format.getInteger(MediaFormat.KEY_WIDTH);
          meta.height = format.getInteger(MediaFormat.KEY_HEIGHT);
          if (format.containsKey(MediaFormat.KEY_DURATION)) {
            meta.durationMs = format.getLong(MediaFormat.KEY_DURATION) / 1000;
          }
          if (format.containsKey(MediaFormat.KEY_ROTATION)) {
            meta.rotation = ((format.getInteger(MediaFormat.KEY_ROTATION) % 360) + 360) % 360;
          }
        } else if (mime.startsWith("audio/")) {
          meta.hasAudio = true;
          if (meta.durationMs == 0 && format.containsKey(MediaFormat.KEY_DURATION)) {
            meta.durationMs = format.getLong(MediaFormat.KEY_DURATION) / 1000;
          }
        }
      }
    } catch (Exception e) {
      Log.w(TAG, "probe: cannot read " + path, e);
      return null;
    } finally {
      extractor.release();
    }
    if (!foundVideo || meta.width <= 0 || meta.height <= 0) {
      return null;
    }

    // noinspection resource
    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    try {
      retriever.setDataSource(path);
      String bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
      if (bitrate != null) {
        meta.overallBitrate = Long.parseLong(bitrate);
      }
      if (meta.durationMs == 0) {
        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if (duration != null) {
          meta.durationMs = Long.parseLong(duration);
        }
      }
    } catch (Exception e) {
      Log.w(TAG, "probe: retriever failed for " + path, e);
    } finally {
      try {
        retriever.release();
      } catch (Exception e) {
        Log.w(TAG, "probe: retriever release failed", e);
      }
    }

    if (meta.rotation == 90 || meta.rotation == 270) {
      meta.dispW = meta.height;
      meta.dispH = meta.width;
    } else {
      meta.dispW = meta.width;
      meta.dispH = meta.height;
    }
    return meta;
  }
}
