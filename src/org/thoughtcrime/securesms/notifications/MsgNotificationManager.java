package org.thoughtcrime.securesms.notifications;

import android.util.Log;

import androidx.annotation.NonNull;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;

public class MsgNotificationManager {
    private static final String TAG = InChatSounds.class.getSimpleName();
    @NonNull private ApplicationDcContext dcContext;
    private volatile int visibleChatId = 0;

    public MsgNotificationManager(ApplicationDcContext dcContext) {
        this.dcContext = dcContext;
    }

    public void addNotification(int chatId, int msgId) {
        Util.runOnAnyBackgroundThread(() -> {

            if (Prefs.isChatMuted(dcContext.context, chatId)) {
                return;
            }

            if (chatId == visibleChatId) {
                InChatSounds.getInstance(dcContext.context).playIncomingSound();
                return;
            }

        });
    }

    public void removeNotifications(int chatId) {
        Util.runOnAnyBackgroundThread(() -> {

        });
    }

    public void updateVisibleChat(int chatId) {
        Util.runOnAnyBackgroundThread(() -> {

            visibleChatId = chatId;
            removeNotifications(chatId);

        });
    }
}
