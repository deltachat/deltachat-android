package org.thoughtcrime.securesms.notifications;

public interface IMessageNotifier {

    void playSendSound();
    void updateVisibleChat(int chatId);
    void updateNotification(int chatId, int messageId);
    void onNotificationPrivacyChanged();
    void removeNotifications(int[] chatIds);
    void removeNotifications(int chatId);

}
