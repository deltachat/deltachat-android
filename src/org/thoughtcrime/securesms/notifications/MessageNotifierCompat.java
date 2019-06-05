package org.thoughtcrime.securesms.notifications;

import android.content.Context;
import android.os.Build;

import org.thoughtcrime.securesms.util.Util;

public class MessageNotifierCompat {

    public static final int     NO_VISIBLE_CHAT_ID        = -1;
    static final  int           SUMMARY_NOTIFICATION_ID   = 1339;
    static final  String        EXTRA_REMOTE_REPLY        = "extra_remote_reply";


    private static MessageNotifier instance;

    public static void init(Context context) {
        if (instance != null) {
            return;
        }

        if (Build.VERSION.SDK_INT < 23) {
            instance = new MessageNotifierPreApi23(context);
        } else {
            instance = new MessageNotifierApi23(context);
        }
    }

    public static void playSendSound() {
        instance.playSendSound();
    }

    public static void updateNotification(int chatId, int messageId) {
        Util.runOnAnyBackgroundThread(() -> instance.updateNotification(chatId, messageId));
    }

    public static void updateVisibleChat(int chatId) {
        Util.runOnAnyBackgroundThread(() -> instance.updateVisibleChat(chatId));
    }

    public static void onNotificationPrivacyChanged() {
        Util.runOnAnyBackgroundThread(() -> instance.onNotificationPrivacyChanged());
    }

    static void removeNotifications(int[] chatIds) {
        Util.runOnAnyBackgroundThread(() -> instance.removeNotifications(chatIds));
    }

    public static void removeNotifications(int chatId) {
        Util.runOnAnyBackgroundThread(() -> instance.removeNotifications(chatId));
    }


}
