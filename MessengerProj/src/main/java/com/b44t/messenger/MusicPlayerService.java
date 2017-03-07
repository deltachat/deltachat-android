/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *                        (C) 2013-2016 Nikolai Kudashov
 *                           (C) 2017 Björn Petersen
 *                    Contact: r10s@b44t.com, http://b44t.com
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see http://www.gnu.org/licenses/ .
 *
 ******************************************************************************/


package com.b44t.messenger;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;

import com.b44t.messenger.audioinfo.AudioInfo;
import com.b44t.ui.LaunchActivity;

public class MusicPlayerService extends Service implements NotificationCenter.NotificationCenterDelegate {

    public static final String NOTIFY_PREVIOUS = "com.b44t.android.musicplayer.previous";
    public static final String NOTIFY_CLOSE = "com.b44t.android.musicplayer.close";
    public static final String NOTIFY_PAUSE = "com.b44t.android.musicplayer.pause";
    public static final String NOTIFY_PLAY = "com.b44t.android.musicplayer.play";
    public static final String NOTIFY_NEXT = "com.b44t.android.musicplayer.next";

    private RemoteControlClient remoteControlClient;
    private AudioManager audioManager;

    private static boolean supportLockScreenControls = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioProgressDidChanged);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioPlayStateChanged);
        super.onCreate();
    }

    @SuppressLint("NewApi")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            MessageObject messageObject = MediaController.getInstance().getPlayingMessageObject();
            if (messageObject == null) {
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        stopSelf();
                    }
                });
                return START_STICKY;
            }
            if (supportLockScreenControls) {
                ComponentName remoteComponentName = new ComponentName(getApplicationContext(), MusicPlayerReceiver.class.getName());
                try {
                    if (remoteControlClient == null) {
                        audioManager.registerMediaButtonEventReceiver(remoteComponentName);
                        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                        mediaButtonIntent.setComponent(remoteComponentName);
                        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
                        remoteControlClient = new RemoteControlClient(mediaPendingIntent);
                        audioManager.registerRemoteControlClient(remoteControlClient);
                    }
                    remoteControlClient.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE |
                            RemoteControlClient.FLAG_KEY_MEDIA_STOP | RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS | RemoteControlClient.FLAG_KEY_MEDIA_NEXT);
                } catch (Exception e) {
                    FileLog.e("messenger", e);
                }
            }
            createNotification(messageObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return START_STICKY;
    }

    @SuppressLint("NewApi")
    private void createNotification(MessageObject messageObject)
    {
        AudioInfo audioInfo = MediaController.getInstance().getAudioInfo();

        MrPoortext pt = MrMailbox.getMsg(messageObject.getId()).getMediainfo();
        String authorName = pt.getText1();
        String songName = pt.getText2();
        if( songName == null || songName.length()==0 ) {
            TLRPC.Document document = messageObject.messageOwner.media.document;
            for (int i = 0; i < document.attributes.size(); i++) {
                TLRPC.DocumentAttribute attr = document.attributes.get(i);
                if (attr instanceof TLRPC.TL_documentAttributeAudio) {
                    authorName = attr.performer;
                    songName = attr.title;
                    break;
                }
            }
        }

        if( songName == null || songName.length()==0 ) {
            // preview of just recorded and not send voice messages
            authorName = ApplicationLoader.applicationContext.getString(R.string.FromSelf);
            songName = ApplicationLoader.applicationContext.getString(R.string.AttachVoiceMessage);
        }

        RemoteViews simpleContentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.player_small_notification);

        //Intent intent = new Intent(ApplicationLoader.applicationContext, LaunchActivity.class);
        //intent.setAction("com.b44t.messenger.openchat"+messageObject.getDialogId());
        //intent.setFlags(32768);
        //PendingIntent contentIntent = PendingIntent.getActivity(ApplicationLoader.applicationContext, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.notification_player)
                //.setContentIntent(contentIntent)
                .setContentTitle(songName).build();

        notification.contentView = simpleContentView;

        setListeners(simpleContentView);

        Bitmap albumArt = audioInfo != null ? audioInfo.getSmallCover() : null;
        if (albumArt != null) {
            notification.contentView.setImageViewBitmap(R.id.player_album_art, albumArt);
            notification.contentView.setViewVisibility(R.id.player_album_art, View.VISIBLE);
        } else {
            notification.contentView.setViewVisibility(R.id.player_album_art, View.GONE);
        }

        {
            notification.contentView.setViewVisibility(R.id.player_next, View.VISIBLE);
            notification.contentView.setViewVisibility(R.id.player_previous, View.VISIBLE);

            if (MediaController.getInstance().isAudioPaused()) {
                notification.contentView.setViewVisibility(R.id.player_pause, View.GONE);
                notification.contentView.setViewVisibility(R.id.player_play, View.VISIBLE);
            } else {
                notification.contentView.setViewVisibility(R.id.player_pause, View.VISIBLE);
                notification.contentView.setViewVisibility(R.id.player_play, View.GONE);
            }
        }

        notification.contentView.setTextViewText(R.id.player_song_name, songName);
        notification.contentView.setTextViewText(R.id.player_author_name, authorName);
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        startForeground(5, notification);

        if (remoteControlClient != null) {
            RemoteControlClient.MetadataEditor metadataEditor = remoteControlClient.editMetadata(true);
            metadataEditor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, authorName);
            metadataEditor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, songName);
            if (audioInfo != null && audioInfo.getCover() != null) {
                try {
                    metadataEditor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, audioInfo.getCover());
                } catch (Throwable e) {
                    FileLog.e("messenger", e);
                }
            }
            metadataEditor.apply();
        }
    }

    public void setListeners(RemoteViews view) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(NOTIFY_PREVIOUS), PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.player_previous, pendingIntent);

        pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(NOTIFY_CLOSE), PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.player_close, pendingIntent);

        pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(NOTIFY_PAUSE), PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.player_pause, pendingIntent);

        pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(NOTIFY_NEXT), PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.player_next, pendingIntent);

        pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(NOTIFY_PLAY), PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.player_play, pendingIntent);
    }

    @SuppressLint("NewApi")
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (remoteControlClient != null) {
            RemoteControlClient.MetadataEditor metadataEditor = remoteControlClient.editMetadata(true);
            metadataEditor.clear();
            metadataEditor.apply();
            audioManager.unregisterRemoteControlClient(remoteControlClient);
        }
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioProgressDidChanged);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioPlayStateChanged);
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.audioPlayStateChanged) {
            MessageObject messageObject = MediaController.getInstance().getPlayingMessageObject();
            if (messageObject != null) {
                createNotification(messageObject);
            } else {
                stopSelf();
            }
        }
    }
}
