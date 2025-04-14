package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.audio.AudioSlidePlayer;
import org.thoughtcrime.securesms.mms.AudioSlide;
import org.thoughtcrime.securesms.util.DateUtils;

import java.io.IOException;


public class AudioView extends FrameLayout implements AudioSlidePlayer.Listener {

  private static final String TAG = AudioView.class.getSimpleName();

  private final @NonNull AnimatingToggle controlToggle;
  private final @NonNull ImageView       playButton;
  private final @NonNull ImageView       pauseButton;
  private final @NonNull SeekBar         seekBar;
  private final @NonNull TextView        timestamp;
  private final @NonNull TextView        title;
  private final @NonNull View            mask;

  private @Nullable AudioSlidePlayer   audioSlidePlayer;
  private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
  private int backwardsCounter;

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

    this.playButton.setOnClickListener(new PlayClickedListener());
    this.pauseButton.setOnClickListener(new PauseClickedListener());
    this.seekBar.setOnSeekBarChangeListener(new SeekBarModifiedListener());

    this.playButton.setImageDrawable(context.getDrawable(R.drawable.play_icon));
    this.pauseButton.setImageDrawable(context.getDrawable(R.drawable.pause_icon));
    this.playButton.setBackground(context.getDrawable(R.drawable.ic_circle_fill_white_48dp));
    this.pauseButton.setBackground(context.getDrawable(R.drawable.ic_circle_fill_white_48dp));

    setTint(getContext().getResources().getColor(R.color.audio_icon));
  }

  public void setAudio(final @NonNull AudioSlide audio, int duration)
  {
    controlToggle.displayQuick(playButton);
    seekBar.setEnabled(true);
    seekBar.setProgress(0);
    audioSlidePlayer = AudioSlidePlayer.createFor(getContext(), audio, this);
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

  public void setDuration(int duration) {
    if (getProgress()==0)
      this.timestamp.setText(DateUtils.getFormatedDuration(duration));
  }

  public void cleanup() {
    if (this.audioSlidePlayer != null && pauseButton.getVisibility() == View.VISIBLE) {
      this.audioSlidePlayer.stop();
    }
  }

  @Override
  public void onReceivedDuration(int millis) {
    this.timestamp.setText(DateUtils.getFormatedDuration(millis));
  }

  @Override
  public void onStart() {
    if (this.pauseButton.getVisibility() != View.VISIBLE) {
      togglePlayToPause();
    }
  }

  @Override
  public void onStop() {
    if (this.playButton.getVisibility() != View.VISIBLE) {
      togglePauseToPlay();
    }

    if (seekBar.getProgress() + 5 >= seekBar.getMax()) {
      backwardsCounter = 4;
      onProgress(audioSlidePlayer.getAudioSlide(), 0.0, -1);
    }
  }

  public void disablePlayer(boolean disable) {
    this.mask.setVisibility(disable? View.VISIBLE : View.GONE);
  }

  @Override
  public void onProgress(AudioSlide slide, double progress, long millis) {
    if (!audioSlidePlayer.getAudioSlide().equals(slide)) {
      return;
    }
    int seekProgress = (int) Math.floor(progress * this.seekBar.getMax());

    if (seekProgress > seekBar.getProgress() || backwardsCounter > 3) {
      backwardsCounter = 0;
      this.seekBar.setProgress(seekProgress);
      if (millis != -1) {
        this.timestamp.setText(DateUtils.getFormatedDuration(millis));
      }
    } else {
      backwardsCounter++;
    }
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

  private class PlayClickedListener implements View.OnClickListener {
    @Override
    public void onClick(View v) {
      try {
        Log.w(TAG, "playbutton onClick");
        if (audioSlidePlayer != null) {
          if (Build.VERSION.SDK_INT >= 26) {
            if (audioFocusChangeListener == null) {
              audioFocusChangeListener = focusChange -> {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                  pauseButton.performClick();
                }
              };
            }

            AudioAttributes playbackAttributes = new AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .build();

            AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
              .setAudioAttributes(playbackAttributes)
              .setAcceptsDelayedFocusGain(false)
              .setWillPauseWhenDucked(false)
              .setOnAudioFocusChangeListener(audioFocusChangeListener)
              .build();

            AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            audioManager.requestAudioFocus(focusRequest);
          }

          togglePlayToPause();
          audioSlidePlayer.play(getProgress());
        }
      } catch (IOException e) {
        Log.w(TAG, e);
      }
    }
  }

  private class PauseClickedListener implements View.OnClickListener {
    @Override
    public void onClick(View v) {
      Log.w(TAG, "pausebutton onClick");
      if (audioSlidePlayer != null) {
        togglePauseToPlay();
        audioSlidePlayer.stop();
      }
    }
  }

  private class SeekBarModifiedListener implements SeekBar.OnSeekBarChangeListener {
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

    @Override
    public synchronized void onStartTrackingTouch(SeekBar seekBar) {
      if (audioSlidePlayer != null && pauseButton.getVisibility() == View.VISIBLE) {
        audioSlidePlayer.stop();
      }
    }

    @Override
    public synchronized void onStopTrackingTouch(SeekBar seekBar) {
      try {
        if (audioSlidePlayer != null && pauseButton.getVisibility() == View.VISIBLE) {
          audioSlidePlayer.play(getProgress());
        }
      } catch (IOException e) {
        Log.w(TAG, e);
      }
    }
  }
}
