package org.thoughtcrime.securesms.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AudioFocusHolder {

  private static final String TAG = "AudioFocusHolder";

  private final @Nullable AudioManager audioManager;
  private final Runnable onFocusLost;

  private @Nullable AudioManager.OnAudioFocusChangeListener focusChangeListener;
  private @Nullable Object focusRequest; // API 26+ only
  private boolean held;

  public AudioFocusHolder(@NonNull Context context, @NonNull Runnable onFocusLost) {
    this.audioManager =
        (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
    this.onFocusLost = onFocusLost;
  }

  @SuppressWarnings("deprecation")
  public synchronized boolean acquire() {
    if (held) {
      return true;
    }
    if (audioManager == null) {
      return false;
    }

    focusChangeListener = this::onAudioFocusChange;

    int result;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      AudioAttributes attributes =
          new AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_MEDIA)
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .build();
      AudioFocusRequest request =
          new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
              .setAudioAttributes(attributes)
              .setOnAudioFocusChangeListener(focusChangeListener)
              .build();
      focusRequest = request;
      result = audioManager.requestAudioFocus(request);
    } else {
      result =
          audioManager.requestAudioFocus(
              focusChangeListener,
              AudioManager.STREAM_MUSIC,
              AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
    }

    held = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    if (!held) {
      Log.w(TAG, "Audio focus request was not granted: " + result);
      focusRequest = null;
      focusChangeListener = null;
    }
    return held;
  }

  @SuppressWarnings("deprecation")
  public synchronized void release() {
    if (!held || audioManager == null) {
      return;
    }
    held = false;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      if (focusRequest != null) {
        audioManager.abandonAudioFocusRequest((AudioFocusRequest) focusRequest);
      }
    } else if (focusChangeListener != null) {
      audioManager.abandonAudioFocus(focusChangeListener);
    }

    focusRequest = null;
    focusChangeListener = null;
  }

  private void onAudioFocusChange(int focusChange) {
    if (focusChange != AudioManager.AUDIOFOCUS_LOSS
        && focusChange != AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
      return;
    }

    synchronized (this) {
      if (!held) return;
    }

    Log.w(TAG, "Audio focus lost during recording");
    onFocusLost.run();
  }
}
