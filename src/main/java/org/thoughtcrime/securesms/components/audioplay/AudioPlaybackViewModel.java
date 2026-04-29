package org.thoughtcrime.securesms.components.audioplay;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioPlaybackViewModel extends ViewModel {
  private static final String TAG = "AudioPlaybackViewModel";

  private static final int NON_MESSAGE_AUDIO_MSG_ID =
      0; // Audios not attached to a message doesn't have message id.

  private final MutableLiveData<AudioPlaybackState> playbackState;

  private final MutableLiveData<Map<Integer, Long>> durations =
      new MutableLiveData<>(new HashMap<>());
  private final Set<Integer> extractionInProgress = new HashSet<>();
  private final ExecutorService extractionExecutor = Executors.newFixedThreadPool(2);

  private @Nullable MediaController mediaController;
  private @Nullable ChatAudioQueueProvider queueProvider;
  private @Nullable Player.Listener playerListener;
  private final Handler handler;
  private boolean isUserSeeking = false;

  public AudioPlaybackViewModel() {
    playbackState = new MutableLiveData<>(AudioPlaybackState.idle());
    handler = new Handler(Looper.getMainLooper());
  }

  public LiveData<AudioPlaybackState> getPlaybackState() {
    return playbackState;
  }

  public void setMediaController(@Nullable MediaController controller) {
    if (this.mediaController != null && playerListener != null) {
      this.mediaController.removeListener(playerListener);
    }
    playerListener = null;

    this.mediaController = controller;
    if (mediaController != null && mediaController.isPlaying()) {
      startUpdateProgress();
    }
    updateCurrentState(true);
    setupPlayerListener();
  }

  // Public methods
  public void loadAudioAndPlay(int msgId, Uri audioUri) {
    if (mediaController == null) return;

    String mediaId = String.valueOf(msgId);

    MediaItem current = mediaController.getCurrentMediaItem();
    if (current != null && mediaId.equals(current.mediaId)) {
      mediaController.play();
      return;
    }

    updateState(msgId, audioUri, AudioPlaybackState.PlaybackStatus.LOADING, 0, 0);

    List<MediaItem> items = null;
    int startIndex = -1;

    if (queueProvider != null) {
      items = queueProvider.buildAudioQueue();
      startIndex = indexOfMediaId(items, mediaId);
    }

    if (startIndex < 0) {
      items =
          Collections.singletonList(
              new MediaItem.Builder().setMediaId(mediaId).setUri(audioUri).build());
      startIndex = 0;
    }

    mediaController.setMediaItems(items, startIndex, 0);
    mediaController.prepare();
    mediaController.play();
  }

  private static int indexOfMediaId(List<MediaItem> items, String mediaId) {
    for (int i = 0; i < items.size(); i++) {
      if (mediaId.equals(items.get(i).mediaId)) return i;
    }
    return -1;
  }

  public LiveData<Map<Integer, Long>> getDurations() {
    return durations;
  }

  public void ensureDurationLoaded(Context context, int msgId, Uri audioUri) {
    // Check cache
    Map<Integer, Long> currentDurations = durations.getValue();
    if (currentDurations != null && currentDurations.containsKey(msgId)) {
      return;
    }

    // Check extracting
    synchronized (extractionInProgress) {
      if (extractionInProgress.contains(msgId)) {
        return;
      }
      extractionInProgress.add(msgId);
    }

    // Extract in background
    extractionExecutor.execute(
        () -> {
          long duration = extractDurationFromAudio(context, audioUri);

          handler.post(
              () -> {
                Map<Integer, Long> updatedDurations = new HashMap<>(durations.getValue());
                updatedDurations.put(msgId, duration);
                durations.setValue(updatedDurations);
              });

          synchronized (extractionInProgress) {
            extractionInProgress.remove(msgId);
          }
        });
  }

  private long extractDurationFromAudio(Context context, Uri audioUri) {
    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    try {
      retriever.setDataSource(context, audioUri);
      String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
      return durationStr != null ? Long.parseLong(durationStr) : 0;
    } catch (Exception e) {
      return 0;
    } finally {
      try {
        retriever.release();
      } catch (Exception ignored) {
      }
    }
  }

  public void pause(int msgId) {
    if (isCurrentItem(msgId)) {
      mediaController.pause();
    }
  }

  public void play(int msgId) {
    if (isCurrentItem(msgId)) {
      mediaController.play();
    }
  }

  public void seekTo(long position, int msgId) {
    if (isCurrentItem(msgId)) {
      mediaController.seekTo(position);
    }
  }

  public void stop(int msgId) {
    if (isCurrentItem(msgId)) {
      mediaController.stop();
      mediaController.clearMediaItems();
      stopUpdateProgress();
      playbackState.setValue(AudioPlaybackState.idle());
    }
  }

  private boolean isCurrentItem(int msgId) {
    if (mediaController == null) return false;
    MediaItem current = mediaController.getCurrentMediaItem();
    return current != null && String.valueOf(msgId).equals(current.mediaId);
  }

  public void stopNonMessageAudioPlayback() {
    stopByIds(NON_MESSAGE_AUDIO_MSG_ID);
  }

  // A special method for deleting message, where we only use message Ids
  public void stopByIds(int... msgIds) {
    if (mediaController == null) return;

    AudioPlaybackState currentState = playbackState.getValue();
    boolean stoppedCurrent = false;

    if (currentState != null) {
      for (int msgId : msgIds) {
        if (msgId == currentState.getMsgId()) {
          mediaController.stop();
          mediaController.clearMediaItems();
          stopUpdateProgress();
          playbackState.setValue(AudioPlaybackState.idle());
          stoppedCurrent = true;
          break;
        }
      }
    }

    if (!stoppedCurrent) {
      Set<String> deletedMediaIds = new HashSet<>();
      for (int msgId : msgIds) {
        deletedMediaIds.add(String.valueOf(msgId));
      }
      for (int i = mediaController.getMediaItemCount() - 1; i >= 0; i--) {
        MediaItem item = mediaController.getMediaItemAt(i);
        if (deletedMediaIds.contains(item.mediaId)) {
          mediaController.removeMediaItem(i);
        }
      }
    }
  }

  public void setUserSeeking(boolean isUserSeeking) {
    this.isUserSeeking = isUserSeeking;
  }

  // Private methods
  private void setupPlayerListener() {
    if (mediaController == null) return;

    playerListener =
        new Player.Listener() {
          @Override
          public void onEvents(@NonNull Player player, @NonNull Player.Events events) {
            if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED)) {
              if (player.isPlaying()) {
                startUpdateProgress();
              } else {
                stopUpdateProgress();
              }
              updateCurrentState(false);
            }
            if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
              updateCurrentState(true);
            }
            if (events.containsAny(Player.EVENT_PLAYBACK_STATE_CHANGED)) {
              if (player.getPlaybackState() == Player.STATE_READY) {
                updateCurrentState(false);
              } else if (player.getPlaybackState() == Player.STATE_ENDED
                  && !player.hasNextMediaItem()) {
                mediaController.setPlayWhenReady(false);
              }
            }
          }

          @Override
          public void onPlayerError(@NonNull PlaybackException error) {
            Log.w(
                TAG,
                "Playback error on msgId="
                    + (mediaController.getCurrentMediaItem() != null
                        ? mediaController.getCurrentMediaItem().mediaId
                        : "null"),
                error);

            if (mediaController.hasNextMediaItem()) {
              mediaController.seekToNextMediaItem();
              mediaController.prepare();
              mediaController.play();
            } else {
              updateCurrentAudioState(AudioPlaybackState.PlaybackStatus.ERROR, 0, 0);
              mediaController.clearMediaItems();
            }
          }
        };
    mediaController.addListener(playerListener);
  }

  private void updateCurrentState(boolean queryPlaying) {
    if (mediaController == null) return;

    AudioPlaybackState.PlaybackStatus status;
    if (mediaController.isPlaying()) {
      status = AudioPlaybackState.PlaybackStatus.PLAYING;
    } else if (mediaController.getPlaybackState() == Player.STATE_READY
        || mediaController.getPlaybackState() == Player.STATE_ENDED) {
      status = AudioPlaybackState.PlaybackStatus.PAUSED;
    } else {
      status = AudioPlaybackState.PlaybackStatus.IDLE;
    }

    Uri currentUri = null;
    int currentMsgId = 0;
    if (playbackState.getValue() != null) {
      currentMsgId = playbackState.getValue().getMsgId();
      currentUri = playbackState.getValue().getAudioUri();
    }
    if (queryPlaying || playbackState.getValue() == null) {
      MediaItem item = mediaController.getCurrentMediaItem();
      if (item != null) {
        try {
          currentMsgId = Integer.parseInt(item.mediaId);
        } catch (NumberFormatException e) {
          Log.w(TAG, "Invalid integer", e);
        }
        if (item.localConfiguration != null) {
          currentUri = item.localConfiguration.uri;
        }
      }
    }
    updateState(
        currentMsgId,
        currentUri,
        status,
        mediaController.getCurrentPosition(),
        mediaController.getDuration());
  }

  private void updateState(
      int msgId,
      Uri audioUri,
      AudioPlaybackState.PlaybackStatus status,
      long position,
      long duration) {
    // Sanitize longs
    if (position < 0 || position > Integer.MAX_VALUE) {
      position = 0;
    }
    if (duration < 0 || duration > Integer.MAX_VALUE) {
      duration = 0;
    }

    playbackState.setValue(new AudioPlaybackState(msgId, audioUri, status, position, duration));
  }

  private void updateCurrentAudioState(
      AudioPlaybackState.PlaybackStatus status, long position, long duration) {
    AudioPlaybackState current = playbackState.getValue();

    if (current != null) {
      updateState(current.getMsgId(), current.getAudioUri(), status, position, duration);
    }
  }

  // Playing Queue
  public void setQueueProvider(@Nullable ChatAudioQueueProvider provider) {
    this.queueProvider = provider;
  }

  // Progress tracking
  private final Runnable progressRunnable =
      new Runnable() {
        @Override
        public void run() {
          if (mediaController != null && mediaController.isPlaying() && !isUserSeeking) {
            updateCurrentAudioState(
                AudioPlaybackState.PlaybackStatus.PLAYING,
                mediaController.getCurrentPosition(),
                mediaController.getDuration());
            handler.postDelayed(this, 100);
          }
        }
      };

  private void startUpdateProgress() {
    stopUpdateProgress();
    handler.post(progressRunnable);
  }

  private void stopUpdateProgress() {
    handler.removeCallbacks(progressRunnable);
  }

  @Override
  protected void onCleared() {
    stopUpdateProgress();
    extractionExecutor.shutdown();
    super.onCleared();
  }
}
