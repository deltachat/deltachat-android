package org.thoughtcrime.securesms.components;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.ListenableFuture;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.mms.AudioSlide;
import org.thoughtcrime.securesms.service.AudioPlaybackService;
import org.thoughtcrime.securesms.util.DateUtils;


public class AudioView extends FrameLayout {

  private static final String TAG = AudioView.class.getSimpleName();

  private final @NonNull AnimatingToggle controlToggle;
  private final @NonNull ImageView       playButton;
  private final @NonNull ImageView       pauseButton;
  private final @NonNull SeekBar         seekBar;
  private final @NonNull TextView        timestamp;
  private final @NonNull TextView        title;
  private final @NonNull View            mask;

  private @Nullable MediaController      mediaController;
  private Handler                        progressHandler;
  private Runnable                       progressUpdater;
  private boolean                        isUserSeeking = false;
  private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
  private int backwardsCounter;
  private Uri                            audioUri;
  private ListenableFuture<MediaController> mediaControllerFuture;

  public AudioView(Context context) {
    this(context, null);
  }

  public AudioView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public AudioView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    inflate(context, R.layout.audio_view, this);

    this.controlToggle    = (AnimatingToggle) findViewById(R.id.control_toggle);
    this.playButton       = (ImageView) findViewById(R.id.play);
    this.pauseButton      = (ImageView) findViewById(R.id.pause);
    this.seekBar          = (SeekBar) findViewById(R.id.seek);
    this.timestamp        = (TextView) findViewById(R.id.timestamp);
    this.title            = (TextView) findViewById(R.id.title);
    this.mask             = findViewById(R.id.interception_mask);

    this.timestamp.setText("00:00");

    this.playButton.setImageDrawable(context.getDrawable(R.drawable.play_icon));
    this.pauseButton.setImageDrawable(context.getDrawable(R.drawable.pause_icon));
    this.playButton.setBackground(context.getDrawable(R.drawable.ic_circle_fill_white_48dp));
    this.pauseButton.setBackground(context.getDrawable(R.drawable.ic_circle_fill_white_48dp));

    progressHandler = new Handler(Looper.getMainLooper());

    setTint(getContext().getResources().getColor(R.color.audio_icon));
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    initializeController();
  }

  private void initializeController() {
    Context context = getContext();
    SessionToken sessionToken = new SessionToken(context,
      new ComponentName(context, AudioPlaybackService.class));
    mediaControllerFuture = new MediaController.Builder(context, sessionToken)
      .buildAsync();
    mediaControllerFuture.addListener(() -> {
      try {
        mediaController = mediaControllerFuture.get();
        setupControls();
        updateUIFromController();
      } catch (Exception e) {
        Log.e(TAG, "Error connecting to audio playback service", e);
      }
    }, ContextCompat.getMainExecutor(context));
  }

  private void updateUIFromController() {
    if (mediaController == null) return;

    if (mediaController.isPlaying()) {
      updateUIForPlay();
    } else if (!mediaController.isPlaying()) {
      updateUIForPause();
    }
  }

  private void setupControls() {
    if (mediaController == null) return;
    if (audioUri == null) return;

    mediaController.addListener(new Player.Listener() {
      @Override
      public void onEvents(Player player, Player.Events events) {
        if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED)) {
          if (player.isPlaying()) {
            updateUIForPlay();
          } else if (!player.isPlaying()) {
            updateUIForPause();
          }
        }
        if (events.containsAny(Player.EVENT_PLAYBACK_STATE_CHANGED)) {
          if (player.getPlaybackState() == Player.STATE_ENDED
            && player.getAvailableCommands().contains(Player.COMMAND_PLAY_PAUSE)) {
            mediaController.setPlayWhenReady(false);
          }
        }
      }
    });

    playButton.setOnClickListener(v -> {
      Log.w(TAG, "playButton onClick");
      MediaItem currentItem = mediaController.getCurrentMediaItem();
      if (currentItem == null ||
        (currentItem.localConfiguration != null && !audioUri.equals(currentItem.localConfiguration.uri))) {
        // Different media
        MediaItem mediaItem = MediaItem.fromUri(audioUri);
        mediaController.setMediaItem(mediaItem);
        mediaController.prepare();
        mediaController.play();
        updateUIForPlay();
      } else {
        // Same media, just resume
        if (!mediaController.isPlaying()) {
          mediaController.play();
          updateUIForPlay();
        }
      }
    });
    pauseButton.setOnClickListener(v -> {
      Log.w(TAG, "pauseButton onClick");
      if (mediaController.isPlaying()) {
        mediaController.pause();
        updateUIForPause();
      }
    });
    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
          timestamp.setText(DateUtils.getFormatedDuration(progress));
        }
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
        isUserSeeking = true;
        stopUpdateProgress();
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        isUserSeeking = false;
        if (mediaController != null) {
          mediaController.seekTo(seekBar.getProgress());
        }
        startUpdateProgress();
      }
    });
  }

  private void updateUIForPlay() {
    if (pauseButton.getVisibility() != View.VISIBLE) {
      togglePlayToPause();
    }
    startUpdateProgress();
  }

  private void updateUIForPause() {
    if (playButton.getVisibility() != View.VISIBLE) {
      togglePauseToPlay();
    }
    stopUpdateProgress();
  }

  @Override
  protected void onDetachedFromWindow() {
    releaseController();
    super.onDetachedFromWindow();
  }

  public void setAudio(final @NonNull AudioSlide audio, int duration)
  {
    controlToggle.displayQuick(playButton);
    seekBar.setEnabled(true);
    seekBar.setProgress(0);
    audioUri = audio.getUri();
    timestamp.setText(DateUtils.getFormatedDuration(duration));

    if(audio.asAttachment().isVoiceNote() || !audio.getFileName().isPresent()) {
      title.setVisibility(View.GONE);
    }
    else {
      title.setText(audio.getFileName().get());
      title.setVisibility(View.VISIBLE);
    }
  }

  @Override
  public void setOnClickListener(OnClickListener listener) {
    super.setOnClickListener(listener);
    this.mask.setOnClickListener(listener);
  }

  @Override
  public void setOnLongClickListener(OnLongClickListener listener) {
    super.setOnLongClickListener(listener);
    this.mask.setOnLongClickListener(listener);
    this.playButton.setOnLongClickListener(listener);
    this.pauseButton.setOnLongClickListener(listener);
  }

  public void togglePlay() {
    if (this.playButton.getVisibility() == View.VISIBLE) {
        playButton.performClick();
    } else {
        pauseButton.performClick();
    }
  }

  public String getDescription() {
    String desc;
    if (this.title.getVisibility() == View.VISIBLE) {
      desc = getContext().getString(R.string.audio);
    } else {
      desc = getContext().getString(R.string.voice_message);
    }
    desc += "\n" + this.timestamp.getText();
    if (title.getVisibility() == View.VISIBLE) {
        desc += "\n" + this.title.getText();
    }
    return desc;
  }

  @Deprecated
  public void setDuration(int duration) {
    if (getProgress()==0)
      this.timestamp.setText(DateUtils.getFormatedDuration(duration));
  }

  public void releaseController() {
    if (mediaController != null && mediaControllerFuture != null) {
      MediaController.releaseFuture(mediaControllerFuture);
    }
  }

  // Poll progress and update UI
  private void startUpdateProgress() {
    if (progressUpdater == null) {
      progressUpdater = new Runnable() {
        @Override
        public void run() {
          if (mediaController != null && !isUserSeeking) {
            updateProgress();
            // Update every 100ms for smooth progress
            progressHandler.postDelayed(this, 100);
          }
        }
      };
    }
    progressHandler.removeCallbacks(progressUpdater);
    progressHandler.post(progressUpdater);
  }

  private void stopUpdateProgress() {
    if (progressUpdater != null) {
      progressHandler.removeCallbacks(progressUpdater);
    }
    updateProgress();  // Make sure the UI is aligned even when update has stopped
  }

  private void updateProgress() {
    if (mediaController == null) return;

    long currentPosition = mediaController.getCurrentPosition();
    long duration = mediaController.getDuration();

    if (duration > 0) {
      seekBar.setMax((int) duration);
      seekBar.setProgress((int) currentPosition);
      timestamp.setText(DateUtils.getFormatedDuration(currentPosition));
    }
  }

  public void disablePlayer(boolean disable) {
    this.mask.setVisibility(disable? View.VISIBLE : View.GONE);
  }

  public void setTint(int foregroundTint) {
    this.playButton.setBackgroundTintList(ColorStateList.valueOf(foregroundTint));
    this.pauseButton.setBackgroundTintList(ColorStateList.valueOf(foregroundTint));

    this.seekBar.getProgressDrawable().setColorFilter(foregroundTint, PorterDuff.Mode.SRC_IN);

    this.seekBar.getThumb().setColorFilter(foregroundTint, PorterDuff.Mode.SRC_IN);
  }

  public void getSeekBarGlobalVisibleRect(@NonNull Rect rect) {
    seekBar.getGlobalVisibleRect(rect);
  }

  private double getProgress() {
    if (this.seekBar.getProgress() <= 0 || this.seekBar.getMax() <= 0) {
      return 0;
    } else {
      return (double)this.seekBar.getProgress() / (double)this.seekBar.getMax();
    }
  }

  private void togglePlayToPause() {
    controlToggle.displayQuick(pauseButton);

    AnimatedVectorDrawable playToPauseDrawable = (AnimatedVectorDrawable) getContext().getDrawable(R.drawable.play_to_pause_animation);
    pauseButton.setImageDrawable(playToPauseDrawable);
    playToPauseDrawable.start();
  }

  private void togglePauseToPlay() {
    controlToggle.displayQuick(playButton);

    AnimatedVectorDrawable pauseToPlayDrawable = (AnimatedVectorDrawable) getContext().getDrawable(R.drawable.pause_to_play_animation);
    playButton.setImageDrawable(pauseToPlayDrawable);
    pauseToPlayDrawable.start();
  }
}
