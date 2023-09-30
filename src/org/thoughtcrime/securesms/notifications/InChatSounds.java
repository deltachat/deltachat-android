package org.thoughtcrime.securesms.notifications;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

import org.thoughtcrime.securesms.R;

public class InChatSounds {
    private static final String TAG = InChatSounds.class.getSimpleName();
    private static volatile InChatSounds instance;

    private SoundPool soundPool = null;
    private int soundIn = 0;
    private int soundOut = 0;

    static public InChatSounds getInstance(Context context) {
        if (instance == null) {
            synchronized (InChatSounds.class) {
                if (instance == null) {
                    instance = new InChatSounds(context);
                }
            }
        }
        return instance;
    }

    private InChatSounds(Context context) {
        try {
            soundPool = new SoundPool(3, AudioManager.STREAM_SYSTEM, 0);
            soundIn = soundPool.load(context, R.raw.sound_in, 1);
            soundOut = soundPool.load(context, R.raw.sound_out, 1);
        } catch(Exception e) {
            Log.e(TAG, "cannot initialize sounds", e);
        }
    }

    public void playSendSound() {
        try {
            soundPool.play(soundOut, 1.0f, 1.0f, 1, 0, 1.0f);
        } catch(Exception e) {
            Log.e(TAG, "cannot play send sound", e);
        }
    }

    public void playIncomingSound() {
        try {
            soundPool.play(soundIn, 1.0f, 1.0f, 1, 0, 1.0f);
        } catch(Exception e) {
            Log.e(TAG, "cannot play incoming sound", e);
        }
    }
}
