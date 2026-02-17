package org.thoughtcrime.securesms.components.audioplay;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.lifecycle.Observer;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.mms.AudioSlide;
import org.thoughtcrime.securesms.util.DateUtils;

import java.util.Map;


public class AudioView extends FrameLayout {

  private static final String TAG = AudioView.class.getSimpleName();

  private final @NonNull ImageView       playPauseButton;
  private final AnimatedVectorDrawableCompat playToPauseDrawable;
  private final AnimatedVectorDrawableCompat pauseToPlayDrawable;
  private final Drawable                 playDrawable;
  private final Drawable                 pauseDrawable;
  private final Animatable2Compat.AnimationCallback animationCallback;
  private final @NonNull SeekBar         seekBar;
  private final @NonNull TextView        timestamp;
  private final @NonNull TextView        title;
  private final @NonNull View            mask;
  private OnActionListener               listener;

  private int                            msgId = -1;
  private Uri                            audioUri;
  private int                            progress;
  private int                            duration;
  private AudioPlaybackViewModel         viewModel;
  private final Observer<AudioPlaybackState> stateObserver = this::onPlaybackStateChanged;
  private final Observer<Map<Integer, Long>> durationObserver = this::onDurationsChanged;
  private boolean                        isPlaying;

  public AudioView(Context context) {
    this(context, null);
  }

  public AudioView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public AudioView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    inflate(context, R.layout.audio_view, this);

    this.playPauseButton  = findViewById(R.id.play_pause);
    this.seekBar          = findViewById(R.id.seek);
    this.timestamp        = findViewById(R.id.timestamp);
    this.title            = findViewById(R.id.title);
    this.mask             = findViewById(R.id.interception_mask);

    updateTimestampsAndSeekBar();

    // Load drawables once
    this.playToPauseDrawable = AnimatedVectorDrawableCompat.create(
      getContext(), R.drawable.play_to_pause_animation);
    this.pauseToPlayDrawable = AnimatedVectorDrawableCompat.create(
      getContext(), R.drawable.pause_to_play_animation);
    this.playDrawable = AppCompatResources.getDrawable(getContext(), R.drawable.play_icon);
    this.pauseDrawable = AppCompatResources.getDrawable(getContext(), R.drawable.pause_icon);

    this.animationCallback = new Animatable2Compat.AnimationCallback() {
      @Override
      public void onAnimationEnd(Drawable drawable) {
        Drawable endState = isPlaying ? pauseDrawable : playDrawable;
        playPauseButton.setImageDrawable(endState);
      }
    };
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    setupControls();
  }

  private void setupControls() {
    // Set up observer in a very specific case when the view is detached and then re-attached,
    // but binding from adapter has not happened yet
    if (viewModel != null) {
      viewModel.getPlaybackState().removeObserver(stateObserver);
      viewModel.getPlaybackState().observeForever(stateObserver);

      viewModel.getDurations().removeObserver(durationObserver);
      viewModel.getDurations().observeForever(durationObserver);
    }

    playPauseButton.setOnClickListener(v -> {
      Log.w(TAG, "playPauseButton onClick");

      if (viewModel == null || audioUri == null) return;

      AudioPlaybackState state = viewModel.getPlaybackState().getValue();

      if (state != null && msgId == state.getMsgId() && audioUri.equals(state.getAudioUri())) {
        // Same audio
        if (state.getStatus() == AudioPlaybackState.PlaybackStatus.PLAYING) {
          viewModel.pause(msgId, audioUri);
        } else {
          viewModel.play(msgId, audioUri);
        }
      } else {
        // Different audio
        // Note: they can be the same *physical* file, but in different messages
        viewModel.loadAudioAndPlay(msgId, audioUri);
      }

      if (listener != null) {
        listener.onPlayPauseButtonClicked(v);
      }
    });

    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
          AudioView.this.progress = progress;
          updateTimestampsAndSeekBar();
        }
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
        viewModel.setUserSeeking(true);
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        viewModel.setUserSeeking(false);
        viewModel.seekTo(seekBar.getProgress(), msgId, audioUri);
      }
    });

    if (playToPauseDrawable != null) {
      playToPauseDrawable.registerAnimationCallback(animationCallback);
    }
    if (pauseToPlayDrawable != null) {
      pauseToPlayDrawable.registerAnimationCallback(animationCallback);
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    if (viewModel != null) {
      viewModel.getPlaybackState().removeObserver(stateObserver);
      viewModel.getDurations().removeObserver(durationObserver);
    }
    if (playToPauseDrawable != null) {
      playToPauseDrawable.clearAnimationCallbacks();
    }
    if (pauseToPlayDrawable != null) {
      pauseToPlayDrawable.clearAnimationCallbacks();
    }
    super.onDetachedFromWindow();
  }

  public void setPlaybackViewModel(AudioPlaybackViewModel viewModel) {
    if (this.viewModel != null) {
      this.viewModel.getPlaybackState().removeObserver(stateObserver);
      this.viewModel.getDurations().removeObserver(durationObserver);
    }

    // ViewModel is used directly for simplicity, since there is no reuse yet
    this.viewModel = viewModel;

    if (viewModel != null) {
      viewModel.getPlaybackState().observeForever(stateObserver);
      viewModel.getDurations().observeForever(durationObserver);
    }
  }

  public void setAudio(final @NonNull AudioSlide audio)
  {
    msgId = audio.getDcMsgId();
    audioUri = audio.getUri();
    playPauseButton.setImageDrawable(playDrawable);

    seekBar.setEnabled(true);

    // Get duration
    Map<Integer, Long> durations = viewModel.getDurations().getValue();
    if (durations != null && durations.containsKey(msgId)) {
      this.duration = Math.toIntExact(durations.get(msgId));
      updateTimestampsAndSeekBar();
    } else {
      viewModel.ensureDurationLoaded(getContext(), msgId, audioUri);
    }

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
    this.playPauseButton.setOnLongClickListener(listener);
  }

  public int getMsgId() {
    return msgId;
  }

  public Uri getAudioUri() {
    return audioUri;
  }

  public interface OnActionListener {
    void onPlayPauseButtonClicked(View view);
  }

  public void setOnActionListener(OnActionListener listener) {
    this.listener = listener;
  }

  public void togglePlay() {
    playPauseButton.performClick();
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

  private void updateProgress(AudioPlaybackState state) {
    int duration = Math.toIntExact(state.getDuration());
    int position = Math.toIntExact(state.getCurrentPosition());

    if (duration > 0) {
      this.progress = position;
      this.duration = duration;
      updateTimestampsAndSeekBar();
    }
  }

  public void disablePlayer(boolean disable) {
    this.mask.setVisibility(disable? View.VISIBLE : View.GONE);
  }

  public void getSeekBarGlobalVisibleRect(@NonNull Rect rect) {
    seekBar.getGlobalVisibleRect(rect);
  }

  private void togglePlayPause(boolean expectedPlaying) {
    isPlaying = expectedPlaying;
    Drawable expectedDrawable = expectedPlaying ? pauseDrawable : playDrawable;

    boolean isAnimating = false;
    Drawable currentDrawable = playPauseButton.getDrawable();
    if (currentDrawable instanceof AnimatedVectorDrawableCompat) {
      isAnimating = ((AnimatedVectorDrawableCompat) currentDrawable).isRunning();
    }
    if (!isAnimating && playPauseButton.getDrawable() != expectedDrawable) {
      AnimatedVectorDrawableCompat animDrawable = expectedPlaying ? playToPauseDrawable : pauseToPlayDrawable;
      String contentDescription = getContext().getString(
        expectedPlaying ? R.string.menu_pause : R.string.menu_play);

      if (animDrawable != null) {
        playPauseButton.setImageDrawable(animDrawable);
        playPauseButton.setContentDescription(contentDescription);

        animDrawable.start();
      }
    }
  }

  private void onPlaybackStateChanged(AudioPlaybackState state) {
    if (audioUri == null || state == null) return;

    // Check if this state is about this message
    boolean isThisMessage = msgId == state.getMsgId() && audioUri.equals(state.getAudioUri());

    if (isThisMessage) {
      updateUIForPlaybackState(state);
    } else {
      togglePlayPause(false);

      // Also clear progress to avoid confusion
      this.progress = 0;
      updateTimestampsAndSeekBar();
    }
  }

  private void updateUIForPlaybackState(AudioPlaybackState state) {
    switch (state.getStatus()) {
      case PLAYING:
        togglePlayPause(true);
        updateProgress(state);
        break;

      case PAUSED:
        togglePlayPause(false);
        updateProgress(state);
        break;

      case LOADING:
      case ERROR:
        // No special handling yet
        break;
    }
  }

  private void onDurationsChanged(Map<Integer, Long> durations) {
    AudioPlaybackState state = viewModel.getPlaybackState().getValue();

    // When there is no playback happening, msgId can be -1 and audioUri is null
    if (state != null &&
      msgId >= 0 && msgId == state.getMsgId() &&
      audioUri != null && audioUri.equals(state.getAudioUri())) {
      return;   // Is playing this message
    }

    Long duration = durations.get(msgId);
    if (duration != null && seekBar.getMax() <= 100) {
      this.duration = Math.toIntExact(duration);
      updateTimestampsAndSeekBar();
      seekBar.setMax(this.duration);
    }
  }

  private void updateTimestampsAndSeekBar() {
    String progressText = DateUtils.getFormatedDuration(progress);
    String durationText = DateUtils.getFormatedDuration(duration);
    timestamp.setText(String.format("%s / %s", progressText, durationText));
    seekBar.setProgress(progress);
    seekBar.setMax(duration);
  }
}
