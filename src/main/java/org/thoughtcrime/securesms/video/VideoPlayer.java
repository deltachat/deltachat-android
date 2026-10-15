/*
 * Copyright (C) 2017 Whisper Systems
 * Copyright (C) 2026 DeltaChat Android Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.video;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.mms.VideoSlide;
import org.thoughtcrime.securesms.util.ViewUtil;

@OptIn(markerClass = UnstableApi.class)
public class VideoPlayer extends FrameLayout {

  public interface ControlsVisibilityListener {
    void onControlsVisibilityChanged(boolean visible);
  }

  private static final String TAG = "VideoPlayer";

  private final PlayerView exoView;

  @Nullable private ExoPlayer exoPlayer;
  @Nullable private Window window;
  @Nullable private ControlsVisibilityListener controlsVisibilityListener;

  public VideoPlayer(Context context) {
    this(context, null);
  }

  public VideoPlayer(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public VideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    inflate(context, R.layout.video_player, this);

    this.exoView = ViewUtil.findById(this, R.id.video_view);

    exoView.setControllerVisibilityListener(
        (PlayerView.ControllerVisibilityListener)
            visibility -> {
              if (controlsVisibilityListener != null) {
                controlsVisibilityListener.onControlsVisibilityChanged(visibility == View.VISIBLE);
              }
            });

    exoView.setControllerAutoShow(false);
  }

  public void setVideoSource(@NonNull VideoSlide videoSource, boolean autoplay) {
    ExoPlayer player = ensurePlayer();
    Uri uri = videoSource.getUri();
    if (uri == null) {
      Log.w(TAG, "setVideoSource: Slide has no uri, ignoring");
      return;
    }
    player.setMediaItem(MediaItem.fromUri(uri));
    player.prepare();
    player.setPlayWhenReady(autoplay);
  }

  public void setControlsVisibilityListener(@Nullable ControlsVisibilityListener listener) {
    this.controlsVisibilityListener = listener;
  }

  public void setControlsVisible(boolean visible) {
    if (visible) {
      exoView.showController();
    } else {
      exoView.hideController();
    }
  }

  public void pause() {
    if (this.exoPlayer != null) {
      this.exoPlayer.setPlayWhenReady(false);
    }
  }

  public void cleanup() {
    if (this.exoPlayer != null) {
      exoView.setPlayer(null);
      this.exoPlayer.release();
      this.exoPlayer = null;
    }
  }

  public void setWindow(@Nullable Window window) {
    this.window = window;
  }

  // reused across rebinds
  private @NonNull ExoPlayer ensurePlayer() {
    if (exoPlayer == null) {
      AudioAttributes audioAttributes =
          new AudioAttributes.Builder()
              .setUsage(C.USAGE_MEDIA)
              .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
              .build();

      exoPlayer =
          new ExoPlayer.Builder(getContext())
              .setAudioAttributes(audioAttributes, true)
              .setHandleAudioBecomingNoisy(true)
              .build();
      exoPlayer.addListener(new ExoPlayerListener());
      exoView.setPlayer(exoPlayer);
    }
    return exoPlayer;
  }

  private class ExoPlayerListener implements Player.Listener {
    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
      Window w = window;
      if (w == null) {
        return;
      }
      if (isPlaying) {
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      } else {
        w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      }
    }

    @Override
    public void onPlayerError(@NonNull PlaybackException error) {
      Log.w(TAG, "Video playback failed", error);
    }
  }
}
