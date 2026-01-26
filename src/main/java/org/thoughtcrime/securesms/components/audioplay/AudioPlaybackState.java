package org.thoughtcrime.securesms.components.audioplay;

import android.net.Uri;

import androidx.annotation.Nullable;

public class AudioPlaybackState {
  private final @Nullable Uri audioUri;
  private final PlaybackStatus status;
  private final long currentPosition;
  private final long duration;

  public enum PlaybackStatus {
    IDLE,
    LOADING,
    PLAYING,
    PAUSED,
    ERROR
  }

  public AudioPlaybackState(@Nullable Uri audioUri,
                            PlaybackStatus status,
                            long currentPosition,
                            long duration) {
    this.audioUri = audioUri;
    this.status = status;
    this.currentPosition = currentPosition;
    this.duration = duration;
  }

  public static AudioPlaybackState idle() {
    return new AudioPlaybackState(null, PlaybackStatus.IDLE, 0, 0);
  }

  @Nullable
  public Uri getAudioUri() {
    return audioUri;
  }

  public PlaybackStatus getStatus() {
    return status;
  }

  public long getCurrentPosition() {
    return currentPosition;
  }

  public long getDuration() {
    return duration;
  }
}
