package org.thoughtcrime.securesms.audio;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.mms.AudioSlide;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.guava.Optional;
import org.thoughtcrime.securesms.video.exo.AttachmentDataSourceFactory;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class AudioSlidePlayer {
  // this is used as a single element all aspects of this class can synchronize on.
  // compare: (search for static synchronized)
  // https://docs.oracle.com/javase/tutorial/essential/concurrency/locksync.html
  private static final Object MONITOR = new Object();

  private static final String TAG = AudioSlidePlayer.class.getSimpleName();

  private static @NonNull Optional<AudioSlidePlayer> playing = Optional.absent();

  private final @NonNull  Context           context;
  private final @NonNull  AudioSlide        slide;
  private final @NonNull  Handler           progressEventHandler;

  private @NonNull  WeakReference<Listener> listener;
  private @Nullable SimpleExoPlayer         mediaPlayer;
  private @Nullable SimpleExoPlayer         durationCalculator;

  public static AudioSlidePlayer createFor(@NonNull Context context,
                                           @NonNull AudioSlide slide,
                                           @NonNull Listener listener)
  {
    synchronized (MONITOR) {
      if (playing.isPresent() && playing.get().getAudioSlide().equals(slide)) {
        playing.get().setListener(listener);
        return playing.get();
      } else {
        return new AudioSlidePlayer(context, slide, listener);
      }
    }
  }

  private AudioSlidePlayer(@NonNull Context context,
                           @NonNull AudioSlide slide,
                           @NonNull Listener listener)
  {
    this.context              = context;
    this.slide                = slide;
    this.listener             = new WeakReference<>(listener);
    this.progressEventHandler = new ProgressEventHandler(this);
  }

  /**
   * creates a non-playing player and requests the duration from it.
   * The value is then sent to the AudioSlidePlayer.Listener.onReceivedDuration() function.
   */
  public void requestDuration() {
    if (slide.getUri() == null) {
      getListener().onReceivedDuration(0);
      return; // we can't handle this here, but in the worst case the duration is not displayed
      // no need to throw IOException here.
    }

    try {
      LoadControl loadControl = new DefaultLoadControl.Builder().setBufferDurationsMs(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE).createDefaultLoadControl();
      durationCalculator = ExoPlayerFactory.newSimpleInstance(context, new DefaultRenderersFactory(context), new DefaultTrackSelector(), loadControl);
      durationCalculator.setPlayWhenReady(false);
      durationCalculator.addListener(new Player.EventListener() {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
          if (playbackState == Player.STATE_READY) {
              Util.runOnMain(() -> {
                Log.d(TAG, "request duration " + durationCalculator.getDuration());
                getListener().onReceivedDuration(Long.valueOf(durationCalculator.getDuration()).intValue());
                durationCalculator.release();
                durationCalculator.removeListener(this);
                durationCalculator = null;
              });
          }
        }
      });
      durationCalculator.prepare(createMediaSource(slide.getUri()));
    } catch (Exception e) {
        Log.w(TAG, e);
        getListener().onReceivedDuration(0);
    }
  }

  public void play(final double progress) throws IOException {
    // TODO: synchronized (MONITOR) {
    if (this.mediaPlayer != null) {
      return;
    }

    if (slide.getUri() == null) {
      throw new IOException("Slide has no URI!");
    }

    LoadControl loadControl = new DefaultLoadControl.Builder().setBufferDurationsMs(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE).createDefaultLoadControl();
    this.mediaPlayer        = ExoPlayerFactory.newSimpleInstance(context, new DefaultRenderersFactory(context), new DefaultTrackSelector(), loadControl);

    mediaPlayer.prepare(createMediaSource(slide.getUri()));
    mediaPlayer.setPlayWhenReady(true);
    mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build());

    startKeepingScreenOn();

    mediaPlayer.addListener(new MediaPlayerListener(progress));
  }

  private class MediaPlayerListener implements Player.EventListener {

    final double progress;
    MediaPlayerListener(double progress) {
      this.progress = progress;
    }

    boolean started = false;

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
      Log.d(TAG, "onPlayerStateChanged(" + playWhenReady + ", " + playbackState + ")");
      switch (playbackState) {
        case Player.STATE_READY:

          synchronized (MONITOR) {
            if(mediaPlayer == null) return;
            Log.i(TAG, "onPrepared() " + mediaPlayer.getBufferedPercentage() + "% buffered");
            if (mediaPlayer == null) return;
            Log.d(TAG, "DURATION: " + mediaPlayer.getDuration());

            if (started) {
              Log.d(TAG, "Already started. Ignoring.");
              return;
            }

            started = true;

            if (progress > 0) {
              mediaPlayer.seekTo((long) (mediaPlayer.getDuration() * progress));
            }

            setPlaying(AudioSlidePlayer.this);
          }

          notifyOnStart();
          progressEventHandler.sendEmptyMessage(0);
          break;

        case Player.STATE_ENDED:
          Log.i(TAG, "onComplete");
          stopKeepingScreenOn();
          synchronized (MONITOR) {
            if(mediaPlayer == null) return;
            getListener().onReceivedDuration(Long.valueOf(mediaPlayer.getDuration()).intValue());
            mediaPlayer = null;
          }

          notifyOnStop();
          progressEventHandler.removeMessages(0);
      }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
      Log.w(TAG, "MediaPlayer Error: " + error);

      Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show();

      synchronized (MONITOR) {
        mediaPlayer = null;
      }
      stopKeepingScreenOn();
      notifyOnStop();
      progressEventHandler.removeMessages(0);
    }
  }

  private void startKeepingScreenOn() {
    Log.d(TAG, "startKeepingScreenOn");
    if(context instanceof Activity) { // should always be true
      Activity activity = ((Activity)context);
      activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    } else {
      Log.i(TAG, "currently in non-activity context, can't keep the screen on");
    }
  }

  private void stopKeepingScreenOn() {
    Log.d(TAG, "stopKeepingScreenOn");
    if(context instanceof Activity) { // should always be true
      Activity activity = ((Activity)context);
      activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    } // else we already handled when setting the screen on
  }

  private MediaSource createMediaSource(@NonNull Uri uri) {
    DefaultDataSourceFactory    defaultDataSourceFactory    = new DefaultDataSourceFactory(context, "GenericUserAgent", null);
    AttachmentDataSourceFactory attachmentDataSourceFactory = new AttachmentDataSourceFactory(defaultDataSourceFactory);
    ExtractorsFactory           extractorsFactory           = new DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true);

    return new ExtractorMediaSource.Factory(attachmentDataSourceFactory)
            .setExtractorsFactory(extractorsFactory)
            .createMediaSource(uri);
  }

  public void stop() {
    synchronized (MONITOR) {
      Log.i(TAG, "Stop called!");

      removePlaying(this);
      stopKeepingScreenOn();
      if (this.mediaPlayer != null) {
        this.mediaPlayer.stop();
        this.mediaPlayer.release();
      }

      this.mediaPlayer = null;
    }
  }

  public static void stopAll() {
    synchronized (MONITOR) {
      if (playing.isPresent()) {
        playing.get().stop();
      }
    }
  }

  public void setListener(@NonNull Listener listener) {
    this.listener = new WeakReference<>(listener);
    synchronized (MONITOR) {
      if (this.mediaPlayer != null && this.mediaPlayer.getPlaybackState() == Player.STATE_READY) {
        notifyOnStart();
      }
    }
  }

  private @NonNull AudioSlide getAudioSlide() {
    return slide;
  }


  private Pair<Double, Integer> getProgress() {
    synchronized (MONITOR) {
      if (mediaPlayer == null || mediaPlayer.getCurrentPosition() <= 0 || mediaPlayer.getDuration() <= 0) {
        return new Pair<>(0D, 0);
      } else {
        return new Pair<>((double) mediaPlayer.getCurrentPosition() / (double) mediaPlayer.getDuration(),
            (int) mediaPlayer.getCurrentPosition());
      }
    }
  }

  private void notifyOnStart() {
    Util.runOnMain(() -> getListener().onStart());
  }

  private void notifyOnStop() {
    Util.runOnMain(() -> getListener().onStop());
  }

  private void notifyOnProgress(final double progress, final long millis) {
    Util.runOnMain(() -> getListener().onProgress(progress, millis));
  }

  private @NonNull Listener getListener() {
    Listener listener = this.listener.get();

    if (listener != null) return listener;
    else                  return new Listener() {
      @Override
      public void onStart() {}
      @Override
      public void onStop() {}
      @Override
      public void onProgress(double progress, long millis) {}
      @Override
      public void onReceivedDuration(int millis) {}
    };
  }

  private static void setPlaying(@NonNull AudioSlidePlayer player) {
    synchronized (MONITOR) {
      if (playing.isPresent() && playing.get() != player) {
        playing.get().notifyOnStop();
        playing.get().stop();
      }

      playing = Optional.of(player);
    }
  }

  private static void removePlaying(@NonNull AudioSlidePlayer player) {
    synchronized (MONITOR) {
      if (playing.isPresent() && playing.get() == player) {
        playing = Optional.absent();
      }
    }
  }

  public interface Listener {
    void onStart();
    void onStop();
    void onProgress(double progress, long millis);
    void onReceivedDuration(int millis);
  }

  private static class ProgressEventHandler extends Handler {

    private final WeakReference<AudioSlidePlayer> playerReference;

    private ProgressEventHandler(@NonNull AudioSlidePlayer player) {
      this.playerReference = new WeakReference<>(player);
    }

    @Override
    public void handleMessage(Message msg) {
      AudioSlidePlayer player = playerReference.get();
      if (player == null) return;
      synchronized (MONITOR) {
        if (player.mediaPlayer == null || !isPlayerActive(player.mediaPlayer)) {
          return;
        }
      }

      Pair<Double, Integer> progress = player.getProgress();
      player.notifyOnProgress(progress.first, progress.second);
      sendEmptyMessageDelayed(0, 50);
    }

    private boolean isPlayerActive(@NonNull SimpleExoPlayer player) {
      return player.getPlaybackState() == Player.STATE_READY || player.getPlaybackState() == Player.STATE_BUFFERING;
    }
  }
}
