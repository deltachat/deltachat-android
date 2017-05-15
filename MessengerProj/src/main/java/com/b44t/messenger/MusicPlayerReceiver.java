/*******************************************************************************
 *
 *                              Delta Chat Android
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class MusicPlayerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
            if (intent.getExtras() == null) {
                return;
            }
            KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
            if (keyEvent == null) {
                return;
            }
            if (keyEvent.getAction() != KeyEvent.ACTION_DOWN)
                return;

            switch (keyEvent.getKeyCode()) {
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    if (MediaController.getInstance().isAudioPaused()) {
                        MediaController.getInstance().playAudio(MediaController.getInstance().getPlayingMessageObject());
                    } else {
                        MediaController.getInstance().pauseAudio(MediaController.getInstance().getPlayingMessageObject());
                    }
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    MediaController.getInstance().playAudio(MediaController.getInstance().getPlayingMessageObject());
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    MediaController.getInstance().pauseAudio(MediaController.getInstance().getPlayingMessageObject());
                    break;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    MediaController.getInstance().playNextMessage(+1, false);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    MediaController.getInstance().playNextMessage(-1, false);
                    break;
            }
        } else {
            if (intent.getAction().equals(MusicPlayerService.NOTIFY_PLAY)) {
                MediaController.getInstance().playAudio(MediaController.getInstance().getPlayingMessageObject());
            } else if (intent.getAction().equals(MusicPlayerService.NOTIFY_PAUSE) || intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                MediaController.getInstance().pauseAudio(MediaController.getInstance().getPlayingMessageObject());
            } else if (intent.getAction().equals(MusicPlayerService.NOTIFY_NEXT)) {
                MediaController.getInstance().playNextMessage(+1, false);
            } else if (intent.getAction().equals(MusicPlayerService.NOTIFY_CLOSE)) {
                MediaController.getInstance().cleanupPlayer(true, true);
            } else if (intent.getAction().equals(MusicPlayerService.NOTIFY_PREVIOUS)) {
                MediaController.getInstance().playNextMessage(-1, false);
            }
        }
    }
}
