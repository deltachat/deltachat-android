package org.thoughtcrime.securesms.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Prefs.VibrateState;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import static org.thoughtcrime.securesms.notifications.MessageNotifierCompat.SUMMARY_NOTIFICATION_ID;

public class NotificationState {

  private final LinkedList<NotificationItem> notifications = new LinkedList<>();
  private final LinkedHashSet<Integer>       chats         = new LinkedHashSet<>();

  private int notificationCount = 0;

  NotificationState() {}

  NotificationState(@NonNull List<NotificationItem> items) {
    for (NotificationItem item : items) {
      addNotification(item);
    }
  }

  public void reset() {
    notificationCount = 0;
    notifications.clear();
    chats.clear();
  }

  void addNotification(NotificationItem item) {
    notifications.addFirst(item);

    if (chats.contains(item.getChatId())) {
      chats.remove(item.getChatId());
    }

    chats.add(item.getChatId());
    notificationCount++;
  }

  @Nullable Uri getRingtone(Context context) {
    if (!notifications.isEmpty()) {
      Recipient recipient = notifications.iterator().next().getRecipient();
      if (recipient.getAddress().isDcChat()) {
        return Prefs.getChatRingtone(context, recipient.getAddress().getDcChatId());
      }
    }

    return null;
  }

  VibrateState getVibrate(Context context) {
    if (!notifications.isEmpty()) {
      Recipient recipient = notifications.iterator().next().getRecipient();

      if (recipient.getAddress().isDcChat()) {
        return Prefs.getChatVibrate(context, recipient.getAddress().getDcChatId());
      }
    }

    return VibrateState.DEFAULT;
  }

  boolean hasMultipleChats() {
    return chats.size() > 1;
  }

  public LinkedHashSet<Integer> getChats() {
    return chats;
  }

  int getChatCount() {
    return chats.size();
  }

  int getMessageCount() {
    return notificationCount;
  }

  public List<NotificationItem> getNotifications() {
    return notifications;
  }

  List<NotificationItem> getNotificationsForChat(int chatId) {
    LinkedList<NotificationItem> list = new LinkedList<>();

    for (NotificationItem item : notifications) {
      if (item.getChatId() == chatId) list.addFirst(item);
    }

    return list;
  }

   List<NotificationItem> removeNotificationsForChat(int chatId) {
    LinkedList<NotificationItem> removedItems = new LinkedList<>();
    chats.remove(chatId);
    for (Iterator<NotificationItem> it = notifications.iterator(); it.hasNext();) {
      NotificationItem item = it.next();
      if (item.getChatId() == chatId) {
        removedItems.add(item);
        it.remove();
      }
    }
    notificationCount -= removedItems.size();
    return removedItems;
  }

  PendingIntent getMarkAsReadIntent(Context context, int chatId,  int notificationId) {
    int    index       = 0;
    int[] chatArray;
    if (notificationId == SUMMARY_NOTIFICATION_ID) {
      chatArray = new int[chats.size()];
      for (int chat : chats) {
        Log.w("NotificationState", "Added chat: " + chat);
        chatArray[index++] = chat;
      }
    } else {
      chatArray = new int[]{chatId};
    }

    Intent intent = new Intent(MarkReadReceiver.CLEAR_ACTION);
    intent.setClass(context, MarkReadReceiver.class);
    intent.setData((Uri.parse("custom://"+System.currentTimeMillis())));
    intent.putExtra(MarkReadReceiver.CHAT_IDS_EXTRA, chatArray);
    intent.putExtra(MarkReadReceiver.NOTIFICATION_ID_EXTRA, notificationId);

    return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }
}
