/*
 * Copyright (C) 2017 Whisper Systems
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
import android.util.AttributeSet;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.mms.VideoSlide;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.video.exo.AttachmentDataSourceFactory;

public class VideoPlayer extends FrameLayout {

  @Nullable private final PlayerView exoView;

  @Nullable private       SimpleExoPlayer     exoPlayer;
  @Nullable private       Window              window;

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
  }

  public void setVideoSource(@NonNull VideoSlide videoSource, boolean autoplay)
  {
    setExoViewSource(videoSource, autoplay);
  }

  public void pause() {
    if (this.exoPlayer != null) {
      this.exoPlayer.setPlayWhenReady(false);
    }
  }

  public void cleanup() {
    if (this.exoPlayer != null) {
      this.exoPlayer.release();
    }
  }

  public void setWindow(@Nullable Window window) {
    this.window = window;
  }

  private void setExoViewSource(@NonNull VideoSlide videoSource, boolean autoplay)
  {
    BandwidthMeter         bandwidthMeter             = new DefaultBandwidthMeter.Builder(getContext()).build();
    TrackSelector          trackSelector              = new DefaultTrackSelector(getContext());
    LoadControl            loadControl                = new DefaultLoadControl();

    exoPlayer = new SimpleExoPlayer.Builder(getContext())
      .setTrackSelector(trackSelector)
      .setBandwidthMeter(bandwidthMeter)
      .setLoadControl(loadControl)
      .build();
    exoPlayer.addListener(new ExoPlayerListener(window));
    //noinspection ConstantConditions
    exoView.setPlayer(exoPlayer);

    DefaultDataSourceFactory    defaultDataSourceFactory    = new DefaultDataSourceFactory(getContext(), "GenericUserAgent", null);
    AttachmentDataSourceFactory attachmentDataSourceFactory = new AttachmentDataSourceFactory(defaultDataSourceFactory);
    ExtractorsFactory           extractorsFactory           = new DefaultExtractorsFactory();

    MediaSource mediaSource = new ProgressiveMediaSource.Factory(attachmentDataSourceFactory, extractorsFactory)
      .createMediaSource(MediaItem.fromUri(videoSource.getUri()));

    exoPlayer.prepare(mediaSource);
    exoPlayer.setPlayWhenReady(autoplay);
  }

  private static class ExoPlayerListener implements Player.Listener {
    private final Window window;

    ExoPlayerListener(Window window) {
      this.window = window;
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
      switch(playbackState) {
        case Player.STATE_IDLE:
        case Player.STATE_BUFFERING:
        case Player.STATE_ENDED:
          window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
          break;
        case Player.STATE_READY:
          if (playWhenReady) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
          } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
          }
          break;
        default:
          break;
      }
    }
  }


}
