package org.thoughtcrime.securesms.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.ResolvingDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionCommands;
import androidx.media3.session.SessionResult;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.AudioSlide;

public class AudioPlaybackService extends MediaSessionService {

  private static final String TAG = "AudioPlaybackService";

  private ExoPlayer player;
  private MediaSession session;

  @OptIn(markerClass = UnstableApi.class)
  @Override
  public void onCreate() {
    super.onCreate();

    AudioAttributes audioAttributes =
        new AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA) // USAGE_VOICE_COMMUNICATION is for VoIP calls
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build();

    ResolvingDataSource.Factory dataSourceFactory =
        new ResolvingDataSource.Factory(
            new DefaultDataSource.Factory(this),
            dataSpec -> {
              Uri uri = dataSpec.uri;
              if (!"dcmsg".equals(uri.getScheme())) {
                return dataSpec;
              }

              String host = uri.getHost();
              String segment = uri.getLastPathSegment();
              int accountId = 0;
              int msgId = 0;
              if (host != null && segment != null) {
                try {
                  accountId = Integer.parseInt(host);
                  msgId = Integer.parseInt(segment);
                } catch (NumberFormatException ignored) {
                }
              }
              if (accountId <= 0 || msgId <= 0) {
                throw new IOException("Invalid dcmsg uri: " + uri);
              }

              DcContext dcContext = DcHelper.getAccounts(this).getAccount(accountId);
              DcMsg msg = dcContext.getMsg(msgId);
              Uri resolved = new AudioSlide(this, msg).getUri();
              if (resolved == null) {
                throw new IOException("No file for msgId " + msgId + " in account " + accountId);
              }
              return dataSpec.withUri(resolved);
            });

    MediaSource.Factory mediaSourceFactory =
        new DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory);

    player =
        new ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(mediaSourceFactory)
            .build();

    // This is for click on the notification to go back to app
    Intent intent = new Intent(this, ConversationListActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent initialIntent =
        PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    session =
        new MediaSession.Builder(this, player)
            .setSessionActivity(initialIntent)
            .setCallback(
                new MediaSession.Callback() {

                  @OptIn(markerClass = UnstableApi.class)
                  @Override
                  public MediaSession.ConnectionResult onConnect(
                      MediaSession session, MediaSession.ControllerInfo controller) {
                    SessionCommands sessionCommands =
                        MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                            .buildUpon()
                            .add(new SessionCommand("UPDATE_ACTIVITY_CONTEXT", new Bundle()))
                            .build();

                    return new MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .build();
                  }

                  @NonNull
                  @Override
                  public ListenableFuture<SessionResult> onCustomCommand(
                      MediaSession session,
                      MediaSession.ControllerInfo controller,
                      SessionCommand customCommand,
                      Bundle args) {
                    if ("UPDATE_ACTIVITY_CONTEXT".equals(customCommand.customAction)) {
                      updateSessionActivity(args);
                    }
                    return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
                  }
                })
            .build();
  }

  @OptIn(markerClass = UnstableApi.class)
  private void updateSessionActivity(Bundle args) {
    try {
      // Put all the original extras back into the intent
      if (args != null && !args.isEmpty()) {
        String activityClassName = args.getString("activity_class");
        args.remove("activity_class");

        if (activityClassName != null) {
          Class<?> activityClass = Class.forName(activityClassName);
          Intent intent = new Intent(this, activityClass);
          intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
          intent.putExtras(args);

          PendingIntent pendingIntent =
              PendingIntent.getActivity(
                  this,
                  0,
                  intent,
                  PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

          session.setSessionActivity(pendingIntent);
        }
      }
    } catch (ClassNotFoundException e) {
      Log.e(TAG, "Activity class not found", e);
    }
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
