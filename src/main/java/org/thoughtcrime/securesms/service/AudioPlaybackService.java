package org.thoughtcrime.securesms.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.provider.MediaStore;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import org.thoughtcrime.securesms.ConversationListActivity;

public class AudioPlaybackService extends MediaSessionService {

  private ExoPlayer player;
  private MediaSession session;

  @Override
  public void onCreate() {
    super.onCreate();

    AudioAttributes audioAttributes = new AudioAttributes.Builder()
      .setUsage(C.USAGE_MEDIA)  // USAGE_VOICE_COMMUNICATION is for VoIP calls
      .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
      .build();

    player = new ExoPlayer.Builder(this)
      .setAudioAttributes(audioAttributes, true)
      .setHandleAudioBecomingNoisy(true)
      .build();

    // This is for click on the notification to go back to app
    // TODO: Go to the right conversation
    Intent intent = new Intent(this, ConversationListActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(
      this, 0, intent,
      PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );

    session = new MediaSession.Builder(this, player)
      .setSessionActivity(pendingIntent)
      .build();
  }

  @Nullable
  @Override
  public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
    return session;
  }

  @Override
  public void onDestroy() {
    if (session != null) {
      session.release();
      session = null;
    }
    if (player != null) {
      player.release();
      player = null;
    }
    super.onDestroy();
  }
}
