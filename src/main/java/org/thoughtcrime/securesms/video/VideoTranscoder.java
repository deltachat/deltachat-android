package org.thoughtcrime.securesms.video;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.effect.Presentation;
import androidx.media3.transformer.AudioEncoderSettings;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.DefaultEncoderFactory;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.EditedMediaItemSequence;
import androidx.media3.transformer.Effects;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.ProgressHolder;
import androidx.media3.transformer.TransformationRequest;
import androidx.media3.transformer.Transformer;
import androidx.media3.transformer.VideoEncoderSettings;
import com.google.common.collect.ImmutableList;
import java.io.File;

/**
 * One video re-encode by media3 Transformer: decode -> GL (scale) -> H.264/AAC encode -> MP4. All
 * methods must be called on the main thread. Transformer is on a looper thread and offloads the
 * actual work internally.
 */
@OptIn(markerClass = UnstableApi.class)
public class VideoTranscoder {

  private static final String TAG = "VideoTranscoder";

  // HDR->SDR mapping attempts, best first.
  // GL tone mapping needs API 29+; the second entry is a fallback to SDR on older devices.
  private static final int[] HDR_MODES = {
    Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL,
    Composition.HDR_MODE_EXPERIMENTAL_FORCE_INTERPRET_HDR_AS_SDR,
  };

  public interface Callback {
    void onProgress(int progress);

    void onCompleted();

    void onFailed(@Nullable ExportException error);

    void onCancelled();
  }

  private final Context context;
  private final Callback callback;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final ProgressHolder progressHolder = new ProgressHolder();

  private String inputPath;
  private String outputPath;
  private int width;
  private int height;
  private int videoBitrate;
  private boolean hasAudio;

  private Transformer transformer;
  private int hdrAttempt;
  private boolean cancelled;
  private boolean terminated;
  private boolean polling;

  public VideoTranscoder(Context context, Callback callback) {
    this.context = context.getApplicationContext();
    this.callback = callback;
  }

  public void start(
      String inputPath,
      String outputPath,
      int width,
      int height,
      int videoBitrate,
      boolean hasAudio) {
    this.inputPath = inputPath;
    this.outputPath = outputPath;
    this.width = width;
    this.height = height;
    this.videoBitrate = videoBitrate;
    this.hasAudio = hasAudio;
    hdrAttempt = 0;
    cancelled = false;
    terminated = false;
    startAttempt();
  }

  /**
   * In media3 1.8.0, cancel() blocks the calling thread until the export is torn down and delivers
   * no listener callback. It may also rethrow release failures, so we do our own onCancelled()
   * here.
   */
  public void cancel() {
    if (terminated) {
      return;
    }
    terminated = true;
    cancelled = true;
    stopPolling();
    Transformer t = transformer;
    transformer = null;
    if (t != null) {
      try {
        t.cancel();
      } catch (RuntimeException e) {
        Log.w(TAG, "Cancel: release failed", e);
      }
    }
    callback.onCancelled();
  }

  private void startAttempt() {
    transformer = buildTransformer();
    transformer.start(buildComposition(), outputPath);
    startPolling();
  }

  private Composition buildComposition() {
    MediaItem mediaItem = MediaItem.fromUri(Uri.fromFile(new File(inputPath)));
    EditedMediaItem editedMediaItem =
        new EditedMediaItem.Builder(mediaItem)
            .setEffects(
                new Effects(
                    ImmutableList.of(),
                    ImmutableList.of(
                        Presentation.createForWidthAndHeight(
                            width, height, Presentation.LAYOUT_SCALE_TO_FIT))))
            .setRemoveAudio(!hasAudio)
            .setFlattenForSlowMotion(true)
            .build();
    // Input rotation metadata is applied by the decode part; output frames are upright,
    // so the pre-computed display dimensions above are the right size.
    return new Composition.Builder(new EditedMediaItemSequence.Builder(editedMediaItem).build())
        .setHdrMode(HDR_MODES[hdrAttempt])
        .build();
  }

  private Transformer buildTransformer() {
    VideoEncoderSettings videoSettings =
        new VideoEncoderSettings.Builder()
            .setBitrate(videoBitrate)
            .setiFrameIntervalSeconds(2.0f)
            .build();
    AudioEncoderSettings audioSettings =
        new AudioEncoderSettings.Builder().setBitrate(VideoRecodeManager.AUDIO_BPS).build();
    return new Transformer.Builder(context)
        .setVideoMimeType(MimeTypes.VIDEO_H264)
        .setAudioMimeType(MimeTypes.AUDIO_AAC)
        .setEncoderFactory(
            new DefaultEncoderFactory.Builder(context)
                .setRequestedVideoEncoderSettings(videoSettings)
                .setRequestedAudioEncoderSettings(audioSettings)
                .setEnableFallback(true)
                .build())
        // encode portrait videos upright, with no rotation metadata
        .setPortraitEncodingEnabled(true)
        // give very slow devices more time than default
        .setMaxDelayBetweenMuxerSamplesMs(30_000)
        .addListener(transformerListener)
        .build();
  }

  private final Transformer.Listener transformerListener =
      new Transformer.Listener() {
        @Override
        public void onCompleted(
            @NonNull Composition composition, @NonNull ExportResult exportResult) {
          if (terminated) {
            return;
          }
          terminated = true;
          stopPolling();
          transformer = null;
          callback.onCompleted();
        }

        @Override
        public void onError(
            @NonNull Composition composition,
            @NonNull ExportResult exportResult,
            @NonNull ExportException exception) {
          if (terminated) {
            return;
          }
          stopPolling();
          transformer = null;
          if (hdrAttempt + 1 < HDR_MODES.length) {
            Log.w(
                TAG,
                "Export failed with HDR mode " + HDR_MODES[hdrAttempt] + ", retrying",
                exception);
            hdrAttempt++;
            startAttempt();
            return;
          }
          terminated = true;
          callback.onFailed(exception);
        }

        @Override
        public void onFallbackApplied(
            @NonNull Composition composition,
            @NonNull TransformationRequest originalRequest,
            @NonNull TransformationRequest fallbackRequest) {
          Log.i(TAG, "Fallback applied: " + originalRequest + " -> " + fallbackRequest);
        }
      };

  private final Runnable progressPoller =
      new Runnable() {
        @Override
        public void run() {
          if (!polling) {
            return;
          }
          Transformer t = transformer;
          if (t != null && t.getProgress(progressHolder) == Transformer.PROGRESS_STATE_AVAILABLE) {
            callback.onProgress(progressHolder.progress);
          }
          handler.postDelayed(this, 500);
        }
      };

  private void startPolling() {
    if (!polling) {
      polling = true;
      handler.post(progressPoller);
    }
  }

  private void stopPolling() {
    polling = false;
    handler.removeCallbacks(progressPoller);
  }
}
