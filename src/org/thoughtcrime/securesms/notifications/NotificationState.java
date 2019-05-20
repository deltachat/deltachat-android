package org.thoughtcrime.securesms.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.thoughtcrime.securesms.ConversationActivity;
import org.thoughtcrime.securesms.ConversationPopupActivity;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Prefs.VibrateState;
import org.thoughtcrime.securesms.recipients.Recipient;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

public class NotificationState {

  private final LinkedList<NotificationItem> notifications = new LinkedList<>();
  private final LinkedHashSet<Integer>       chats         = new LinkedHashSet<>();

  private int notificationCount = 0;

  public NotificationState() {}

  public NotificationState(@NonNull List<NotificationItem> items) {
    for (NotificationItem item : items) {
      addNotification(item);
    }
  }

  public void addNotification(NotificationItem item) {
    notifications.add(item);

    if (chats.contains(item.getChatId())) {
      chats.remove(item.getChatId());
    }

    chats.add(item.getChatId());
    notificationCount++;
  }

  public @Nullable Uri getRingtone(Context context) {
    if (!notifications.isEmpty()) {
      Recipient recipient = notifications.getFirst().getRecipient();

      if (recipient != null && recipient.getAddress().isDcChat()) {
        return Prefs.getChatRingtone(context, recipient.getAddress().getDcChatId());
      }
    }

    return null;
  }

  public VibrateState getVibrate(Context context) {
    if (!notifications.isEmpty()) {
      Recipient recipient = notifications.getFirst().getRecipient();

      if (recipient != null && recipient.getAddress().isDcChat()) {
        return Prefs.getChatVibrate(context, recipient.getAddress().getDcChatId());
      }
    }

    return VibrateState.DEFAULT;
  }

  public boolean hasMultipleChats() {
    return chats.size() > 1;
  }

  public LinkedHashSet<Integer> getChats() {
    return chats;
  }

  public int getChatCount() {
    return chats.size();
  }

  public int getMessageCount() {
    return notificationCount;
  }

  public List<NotificationItem> getNotifications() {
    return notifications;
  }

  public List<NotificationItem> getNotificationsForChat(int chatId) {
    LinkedList<NotificationItem> list = new LinkedList<>();

    for (NotificationItem item : notifications) {
      if (item.getChatId() == chatId) list.addFirst(item);
    }

    return list;
  }

  public PendingIntent getMarkAsReadIntent(Context context, int notificationId) {
    int[] chatArray = new int[chats.size()];
    int    index       = 0;

    for (int chat : chats) {
      Log.w("NotificationState", "Added chat: " + chat);
      chatArray[index++] = chat;
    }

    Intent intent = new Intent(MarkReadReceiver.CLEAR_ACTION);
    intent.setClass(context, MarkReadReceiver.class);
    intent.setData((Uri.parse("custom://"+System.currentTimeMillis())));
    intent.putExtra(MarkReadReceiver.CHAT_IDS_EXTRA, chatArray);
    intent.putExtra(MarkReadReceiver.NOTIFICATION_ID_EXTRA, notificationId);

    return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  public PendingIntent getRemoteReplyIntent(Context context, Recipient recipient) {
    if (chats.size() != 1) throw new AssertionError("We only support replies to single chat notifications!");

    Intent intent = new Intent(RemoteReplyReceiver.REPLY_ACTION);
    intent.setClass(context, RemoteReplyReceiver.class);
    intent.setData((Uri.parse("custom://"+System.currentTimeMillis())));
    intent.putExtra(RemoteReplyReceiver.ADDRESS_EXTRA, recipient.getAddress());
    intent.setPackage(context.getPackageName());

    return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  public PendingIntent getAndroidAutoReplyIntent(Context context, Recipient recipient) {
    if (chats.size() != 1) throw new AssertionError("We only support replies to single chat notifications!");

    Intent intent = new Intent(AndroidAutoReplyReceiver.REPLY_ACTION);
    intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
    intent.setClass(context, AndroidAutoReplyReceiver.class);
    intent.setData((Uri.parse("custom://"+System.currentTimeMillis())));
    intent.putExtra(AndroidAutoReplyReceiver.ADDRESS_EXTRA, recipient.getAddress());
    intent.putExtra(AndroidAutoReplyReceiver.CHAT_ID_EXTRA, (int) chats.toArray()[0]);
    intent.setPackage(context.getPackageName());

    return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  public PendingIntent getAndroidAutoHeardIntent(Context context, int notificationId) {
    int[] chatArray = new int[chats.size()];
    int    index       = 0;
    for (int chat : chats) {
      Log.w("NotificationState", "getAndroidAutoHeardIntent Added chat: " + chat);
      chatArray[index++] = chat;
    }

    Intent intent = new Intent(AndroidAutoHeardReceiver.HEARD_ACTION);
    intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
    intent.setClass(context, AndroidAutoHeardReceiver.class);
    intent.setData((Uri.parse("custom://"+System.currentTimeMillis())));
    intent.putExtra(AndroidAutoHeardReceiver.CHAT_IDS_EXTRA, chatArray);
    intent.putExtra(AndroidAutoHeardReceiver.NOTIFICATION_ID_EXTRA, notificationId);
    intent.setPackage(context.getPackageName());

    return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  public PendingIntent getQuickReplyIntent(Context context, Recipient recipient) {
    if (chats.size() != 1) throw new AssertionError("We only support replies to single chat notifications! " + chats.size());

    Intent     intent           = new Intent(context, ConversationPopupActivity.class);
    intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, (chats.toArray(new Integer[chats.size()]))[0]);
    intent.setData((Uri.parse("custom://"+System.currentTimeMillis())));

    return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  public PendingIntent getDeleteIntent(Context context) {
    int       index = 0;
    long[]    ids   = new long[notifications.size()];

    for (NotificationItem notificationItem : notifications) {
      ids[index] = notificationItem.getId();
    }

    Intent intent = new Intent(context, DeleteNotificationReceiver.class);
    intent.setAction(DeleteNotificationReceiver.DELETE_NOTIFICATION_ACTION);
    intent.putExtra(DeleteNotificationReceiver.EXTRA_IDS, ids);
    intent.setData((Uri.parse("custom://"+System.currentTimeMillis())));

    return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }


}
