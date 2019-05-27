package org.thoughtcrime.securesms.notifications;

import android.content.Context;
import android.support.v4.app.NotificationManagerCompat;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;

import java.util.List;

import static org.thoughtcrime.securesms.notifications.MessageNotifierCompat.SUMMARY_NOTIFICATION_ID;

public class MessageNotifierPreApi23 extends MessageNotifier {

    MessageNotifierPreApi23(Context context) {
        super(context);
    }

    @Override
    public void removeNotifications(int chatId) {
        notificationState.removeNotificationsForChat(chatId);
        recreateSummaryNotification();
    }

    @Override
    public void removeNotifications(int[] chatIds) {
        for (int id : chatIds) {
            notificationState.removeNotificationsForChat(id);
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
            lastAudibleNotification = System.currentTimeMillis();;
        }

        addMessageToNotificationState(dcContext, chatId, messageId);
        if (notificationState.hasMultipleChats()) {
            sendMultipleChatNotification(appContext, notificationState, signal);
        } else {
            sendSingleChatNotification(appContext, notificationState, signal, false);
        }
    }

    private void cancelNotifications() {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(appContext);
        notificationManagerCompat.cancel(SUMMARY_NOTIFICATION_ID);
    }

    private void recreateSummaryNotification() {
        cancelNotifications();

        if (notificationState.hasMultipleChats()) {
            sendMultipleChatNotification(appContext, notificationState, false);
        } else {
            sendSingleChatNotification(appContext, notificationState, false, false);
        }
    }
}
