package org.thoughtcrime.securesms.audio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.AudioSlide;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.guava.Optional;
import org.thoughtcrime.securesms.video.exo.AttachmentDataSourceFactory;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class AudioSlidePlayer {

  private static final String TAG = AudioSlidePlayer.class.getSimpleName();

  private static @NonNull Optional<AudioSlidePlayer> playing = Optional.absent();

  private final @NonNull  Context           context;
  private final @NonNull  AudioSlide        slide;
  private final @NonNull  Handler           progressEventHandler;

  private @NonNull  WeakReference<Listener> listener;
  private @Nullable SimpleExoPlayer         mediaPlayer;
  private @Nullable SimpleExoPlayer         durationCalculator;

  public synchronized static AudioSlidePlayer createFor(@NonNull Context context,
                                                        @NonNull AudioSlide slide,
                                                        @NonNull Listener listener)
  {
    if (playing.isPresent() && playing.get().getAudioSlide().equals(slide)) {
      playing.get().setListener(listener);
      return playing.get();
    } else {
      return new AudioSlidePlayer(context, slide, listener);
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

  public void requestDuration() {
    try {
      LoadControl loadControl = new DefaultLoadControl.Builder().setBufferDurationsMs(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE).build();
      durationCalculator = new SimpleExoPlayer.Builder(context, new DefaultRenderersFactory(context))
        .setTrackSelector(new DefaultTrackSelector(context))
        .setLoadControl(loadControl)
        .build();
      durationCalculator.setPlayWhenReady(false);
      durationCalculator.addListener(new Player.Listener() {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
          if (playbackState == Player.STATE_READY) {
              Util.runOnMain(() -> {
                synchronized (AudioSlidePlayer.this) {
                  if (durationCalculator == null) return;
                  Log.d(TAG, "request duration " + durationCalculator.getDuration());
                  getListener().onReceivedDuration(Long.valueOf(durationCalculator.getDuration()).intValue());
                  durationCalculator.release();
                  durationCalculator.removeListener(this);
                  durationCalculator = null;
                }
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
    play(progress, false);
  }

  private void play(final double progress, boolean earpiece) throws IOException {
    if (this.mediaPlayer != null) {
      return;
    }

    if (slide.getUri() == null) {
      throw new IOException("Slide has no URI!");
    }

    LoadControl loadControl = new DefaultLoadControl.Builder().setBufferDurationsMs(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE).build();
    this.mediaPlayer           = new SimpleExoPlayer.Builder(context, new DefaultRenderersFactory(context))
      .setTrackSelector(new DefaultTrackSelector(context))
      .setLoadControl(loadControl)
      .build();

    mediaPlayer.prepare(createMediaSource(slide.getUri()));
    mediaPlayer.setPlayWhenReady(true);
    mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
            .setContentType(earpiece ? C.AUDIO_CONTENT_TYPE_SPEECH : C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(earpiece ? C.USAGE_VOICE_COMMUNICATION : C.USAGE_MEDIA)
            .build(), false);
    mediaPlayer.addListener(new Player.Listener() {

      boolean started = false;

      @Override
      public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.d(TAG, "onPlayerStateChanged(" + playWhenReady + ", " + playbackState + ")");
        switch (playbackState) {
          case Player.STATE_READY:

            Log.i(TAG, "onPrepared() " + mediaPlayer.getBufferedPercentage() + "% buffered");
            synchronized (AudioSlidePlayer.this) {
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

            keepScreenOn(true);
            notifyOnStart();
            progressEventHandler.sendEmptyMessage(0);
            break;

          case Player.STATE_ENDED:
            Log.i(TAG, "onComplete");
            synchronized (AudioSlidePlayer.this) {
              getListener().onReceivedDuration(Long.valueOf(mediaPlayer.getDuration()).intValue());
              mediaPlayer.release();
              mediaPlayer = null;
            }

            keepScreenOn(false);
            notifyOnStop();
            progressEventHandler.removeMessages(0);
        }
      }

      @Override
      public void onPlayerError(PlaybackException error) {
        Log.w(TAG, "MediaPlayer Error: " + error);

        synchronized (AudioSlidePlayer.this) {
          mediaPlayer.release();
          mediaPlayer = null;
        }

        notifyOnStop();
        progressEventHandler.removeMessages(0);

        // Failed to play media file, maybe another app can handle it
        int msgId = getAudioSlide().getDcMsgId();
        DcHelper.openForViewOrShare(context, msgId, Intent.ACTION_VIEW);
      }
    });
  }

  private MediaSource createMediaSource(@NonNull Uri uri) {
    DefaultDataSourceFactory    defaultDataSourceFactory    = new DefaultDataSourceFactory(context, "GenericUserAgent", null);
    AttachmentDataSourceFactory attachmentDataSourceFactory = new AttachmentDataSourceFactory(defaultDataSourceFactory);
    ExtractorsFactory           extractorsFactory           = new DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true);

    return new ProgressiveMediaSource.Factory(attachmentDataSourceFactory, extractorsFactory)
            .createMediaSource(MediaItem.fromUri(uri));
  }

  public synchronized void stop() {
    Log.i(TAG, "Stop called!");

    keepScreenOn(false);
    removePlaying(this);

    if (this.mediaPlayer != null) {
      this.mediaPlayer.stop();
      this.mediaPlayer.release();
    }

    this.mediaPlayer = null;
  }

  public static void stopAll() {
    if (playing.isPresent()) {
      synchronized (AudioSlidePlayer.class) {
        if (playing.isPresent()) {
          playing.get().stop();
        }
      }
    }
  }

  public void setListener(@NonNull Listener listener) {
    this.listener = new WeakReference<>(listener);

    if (this.mediaPlayer != null && this.mediaPlayer.getPlaybackState() == Player.STATE_READY) {
      notifyOnStart();
    }
  }

  public @NonNull AudioSlide getAudioSlide() {
    return slide;
  }


  private Pair<Double, Integer> getProgress() {
    if (mediaPlayer == null || mediaPlayer.getCurrentPosition() <= 0 || mediaPlayer.getDuration() <= 0) {
      return new Pair<>(0D, 0);
    } else {
      return new Pair<>((double) mediaPlayer.getCurrentPosition() / (double) mediaPlayer.getDuration(),
              (int) mediaPlayer.getCurrentPosition());
    }
  }

  private void notifyOnStart() {
    Util.runOnMain(new Runnable() {
      @Override
      public void run() {
        getListener().onStart();
      }
    });
  }

  private void notifyOnStop() {
    Util.runOnMain(new Runnable() {
      @Override
      public void run() {
        getListener().onStop();
      }
    });
  }

  private void notifyOnProgress(final double progress, final long millis) {
    Util.runOnMain(new Runnable() {
      @Override
      public void run() {
        getListener().onProgress(slide, progress, millis);
      }
    });
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
      public void onProgress(AudioSlide slide, double progress, long millis) {}
      @Override
      public void onReceivedDuration(int millis) {}
    };
  }

  public void keepScreenOn(boolean keepOn) {
    if (context instanceof Activity) {
      if (keepOn) {
        ((Activity) context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      } else {
        ((Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      }
    }
  }

  private synchronized static void setPlaying(@NonNull AudioSlidePlayer player) {
    if (playing.isPresent() && playing.get() != player) {
      playing.get().notifyOnStop();
      playing.get().stop();
    }

    playing = Optional.of(player);
  }

  private synchronized static void removePlaying(@NonNull AudioSlidePlayer player) {
    if (playing.isPresent() && playing.get() == player) {
      playing = Optional.absent();
    }
  }

  public interface Listener {
    void onStart();
    void onStop();
    void onProgress(AudioSlide slide, double progress, long millis);
    void onReceivedDuration(int millis);
  }

  private static class ProgressEventHandler extends Handler {

    private final WeakReference<AudioSlidePlayer> playerReference;

    private ProgressEventHandler(@NonNull AudioSlidePlayer player) {
      this.playerReference = new WeakReference<>(player);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
      AudioSlidePlayer player = playerReference.get();

      if (player == null || player.mediaPlayer == null || !isPlayerActive(player.mediaPlayer)) {
        return;
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
