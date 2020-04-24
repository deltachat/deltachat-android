package org.thoughtcrime.securesms.notifications;

import android.content.Context;

import androidx.core.app.NotificationManagerCompat;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;

import java.util.List;

import static org.thoughtcrime.securesms.notifications.MessageNotifierCompat.SUMMARY_NOTIFICATION_ID;

class MessageNotifierPreApi23 extends MessageNotifier {

    MessageNotifierPreApi23(Context context) {
        super(context);
    }

    @Override
    public void removeNotifications(int chatId) {
        synchronized (lock) {
            notificationState.removeNotificationsForChat(chatId);
        }
        recreateSummaryNotification();
    }

    @Override
    public void removeNotifications(int[] chatIds) {
        synchronized (lock) {
            for (int id : chatIds) {
                notificationState.removeNotificationsForChat(id);
            }
        }
        recreateSummaryNotification();
    }

    @Override
    void cancelNotifications(List<NotificationItem> removedItems) {
        cancelNotifications();
    }

    @Override
    void sendNotifications(int chatId, int messageId, boolean signal) {
        ApplicationDcContext dcContext = DcHelper.getContext(appContext);
        if (signal = isSignalAllowed(signal)) {
            lastAudibleNotification = System.currentTimeMillis();
        }

        if (dcContext.getChat(chatId).isDeviceTalk()) {
            // currently, we just never notify on device chat.
            // esp. on first start, this is annoying.
            return;
        }

        addMessageToNotificationState(dcContext, chatId, messageId);
        synchronized (lock) {
            if (notificationState.hasMultipleChats()) {
                sendMultipleChatNotification(appContext, notificationState, signal);
            } else {
                sendSingleChatNotification(appContext, notificationState, signal, false);
            }
        }
    }

    private void cancelNotifications() {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(appContext);
        notificationManagerCompat.cancel(SUMMARY_NOTIFICATION_ID);
    }

    private void recreateSummaryNotification() {
        cancelNotifications();
        synchronized (lock) {
            if (notificationState.hasMultipleChats()) {
                sendMultipleChatNotification(appContext, notificationState, false);
            } else {
                sendSingleChatNotification(appContext, notificationState, false, false);
            }
        }
    }
}
