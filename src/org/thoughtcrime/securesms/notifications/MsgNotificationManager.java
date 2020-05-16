package org.thoughtcrime.securesms.notifications;

import androidx.annotation.NonNull;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.util.Util;

public class MsgNotificationManager {
    @NonNull private DcContext dcContext;

    public MsgNotificationManager(DcContext dcContext) {
        this.dcContext = dcContext;
    }

    public void addNotification(int chatId, int msgId) {
        Util.runOnAnyBackgroundThread(() -> {

        });
    }

    public void removeNotifications(int chatId) {
        Util.runOnAnyBackgroundThread(() -> {

        });
    }

    public void updateVisibleChat(int chatId) {
        Util.runOnAnyBackgroundThread(() -> {
            removeNotifications(chatId);
        });
    }

    public void playSendSound() {

    }
}
