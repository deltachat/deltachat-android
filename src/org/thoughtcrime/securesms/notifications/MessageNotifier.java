/*
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.notifications;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;

import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.connect.ForegroundDetector;
import org.thoughtcrime.securesms.connect.KeepAliveService;
import org.thoughtcrime.securesms.mms.SlideDeck;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.Pair;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ServiceUtil;
import org.thoughtcrime.securesms.util.SpanUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

import me.leolin.shortcutbadger.ShortcutBadger;


/**
 * Handles posting system notifications for new messages.
 *
 *
 * @author Moxie Marlinspike
 */

public class MessageNotifier {

  private static final String TAG = MessageNotifier.class.getSimpleName();

  static final  String EXTRA_REMOTE_REPLY = "extra_remote_reply";

  public  static final long   NO_VISIBLE_CHAT_ID        = -1L;
  private static final  int   SUMMARY_NOTIFICATION_ID   = 1338;
  private static final int    PENDING_MESSAGES_ID       = 1111;
  private static final String NOTIFICATION_GROUP        = "messages";
  private static final long   MIN_AUDIBLE_PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(20);
  private static final long   STARTUP_SILENCE_DELTA     = TimeUnit.MINUTES.toMillis(1);
  private static final long   INITIAL_STARTUP           = System.currentTimeMillis();

  private volatile static long               visibleChatId                = NO_VISIBLE_CHAT_ID;
  private volatile static long               lastAudibleNotification      = -1;

  private final           SoundPool          soundPool;
  private final           int                soundIn;
  private final           int                soundOut;
  private                 boolean            soundInLoaded;
  private                 boolean            soundOutLoaded;
  private                 Context            context;

  private static MessageNotifier instance;

  private MessageNotifier(Context context) {
    this.context = context.getApplicationContext();
    soundPool = new SoundPool(3, AudioManager.STREAM_SYSTEM, 0);
    soundIn = soundPool.load(context, R.raw.sound_in, 1);
    soundOut = soundPool.load(context, R.raw.sound_out, 1);

    soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
      if (status == 0) {
        if (sampleId == soundIn) {
          soundInLoaded = true;
        } else if (sampleId == soundOut) {
          soundOutLoaded = true;
        }
      }
    });
  }

  public static MessageNotifier init(Context context) {
    if (instance == null) {
      instance = new MessageNotifier(context);
    }
    return instance;
  }

  public static void playSendSound() {
    if (instance == null) {
      Log.w(TAG, "Message notifier not initialized. Cannot play sounds");
      return;
    }

    if (Prefs.isInChatNotifications(instance.context) && instance.soundOutLoaded) {
      instance.soundPool.play(instance.soundIn, 1.0f, 1.0f, 1, 0, 1.0f);
    }
  }

  private static LinkedList<Pair<Integer, Boolean>> pendingNotifications = new LinkedList<>();

  public static void updateVisibleChat(Context context, long chatId) {
    visibleChatId = chatId;
    if (visibleChatId == NO_VISIBLE_CHAT_ID && pendingNotifications.size() > 0) {
      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          updatePendingNotifications(context);
          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  private static void cancelActiveNotifications(@NonNull Context context) {
    NotificationManager notifications = ServiceUtil.getNotificationManager(context);
    notifications.cancel(SUMMARY_NOTIFICATION_ID);

    if (Build.VERSION.SDK_INT >= 23) {
      try {
        StatusBarNotification[] activeNotifications = notifications.getActiveNotifications();

        for (StatusBarNotification activeNotification : activeNotifications) {
          notifications.cancel(activeNotification.getId());
        }
      } catch (Throwable e) {
        // XXX Appears to be a ROM bug, see #6043
        Log.w(TAG, e);
        notifications.cancelAll();
      }
    }
  }

  private static void cancelOrphanedNotifications(@NonNull Context context, NotificationState notificationState) {
    if (Build.VERSION.SDK_INT >= 23) {
      try {
        NotificationManager     notifications       = ServiceUtil.getNotificationManager(context);
        StatusBarNotification[] activeNotifications = notifications.getActiveNotifications();

        for (StatusBarNotification notification : activeNotifications) {
          boolean validNotification = false;

          if (notification.getId() != SUMMARY_NOTIFICATION_ID &&
              notification.getId() != KeepAliveService.FG_NOTIFICATION_ID           &&
              notification.getId() != PENDING_MESSAGES_ID)
          {
            for (NotificationItem item : notificationState.getNotifications()) {
              if (notification.getId() == (SUMMARY_NOTIFICATION_ID + item.getChatId())) {
                validNotification = true;
                break;
              }
            }

            if (!validNotification) {
              notifications.cancel(notification.getId());
            }
          }
        }
      } catch (Throwable e) {
        // XXX Android ROM Bug, see #6043
        Log.w(TAG, e);
      }
    }
  }

  public static void updateNotification(@NonNull Context context) {
    if (!Prefs.isNotificationsEnabled(context)) {
      return;
    }

    updateNotification(context, true, 0);
  }

  public static void updateNotification(@NonNull Context context, int chatId)
  {
    updateNotification(context, chatId, true);
  }

  public static void updateNotification(@NonNull  Context context,
                                        int       chatId,
                                        boolean   signal)
  {
    boolean    isVisible  = visibleChatId == chatId;
    ApplicationDcContext dcContext = DcHelper.getContext(context);

    if (isVisible) {
      dcContext.marknoticedChat(chatId);
    }

    if (!Prefs.isNotificationsEnabled(context) ||
        Prefs.isChatMuted(context, chatId))
    {
      return;
    }

    removePendingNotifications(chatId);
    if (isVisible && signal) {
      sendInChatNotification(context, chatId);
    } else if (ForegroundDetector.getInstance().isBackground()) {
      updateNotification(context, signal, 0);
    } else if (isVisible || visibleChatId == NO_VISIBLE_CHAT_ID) {
      updateNotification(context, false, 0);
    } else {
      //different chat is visible
      pendingNotifications.push(new Pair<>(chatId, signal));
    }
  }

  // @param signal: true to beep, false to stay silent.
  private static void updateNotification(@NonNull Context context,
                                         boolean signal,
                                         int     reminderCount)
  {
    ApplicationDcContext dcContext = DcHelper.getContext(context);
    int[] freshMessages = dcContext.getFreshMsgs();

    if (freshMessages.length == 0)
    {
      cancelActiveNotifications(context);
      updateBadge(context, 0);
      clearReminder(context);
      return;
    }

    NotificationState notificationState = constructNotificationState(dcContext, freshMessages);

    long now = System.currentTimeMillis();
    if (signal && (
            (now - INITIAL_STARTUP) < STARTUP_SILENCE_DELTA ||
                    (now - lastAudibleNotification) < MIN_AUDIBLE_PERIOD_MILLIS)) {
      signal = false;
    } else if (signal) {
      lastAudibleNotification = now;
    }

    if (notificationState.hasMultipleChats()) {
      if (Build.VERSION.SDK_INT >= 23) {
        for (int chatId : notificationState.getChats()) {
          sendSingleChatNotification(context, new NotificationState(notificationState.getNotificationsForChat(chatId)), false, true);
        }
      }

      sendMultipleChatNotification(context, notificationState, signal);
    } else {
      sendSingleChatNotification(context, notificationState, signal, false);
    }

    cancelOrphanedNotifications(context, notificationState);
    updateBadge(context, notificationState.getMessageCount());

    if (signal) {
      scheduleReminder(context, reminderCount);
    }
  }

  private static void sendSingleChatNotification(@NonNull  Context context,
                                                 @NonNull  NotificationState notificationState,
                                                 boolean signal,
                                                 boolean bundled)
  {
    if (notificationState.getNotifications().isEmpty()) {
      if (!bundled) cancelActiveNotifications(context);
      return;
    }

    SingleRecipientNotificationBuilder builder        = new SingleRecipientNotificationBuilder(context, Prefs.getNotificationPrivacy(context));
    List<NotificationItem>             notifications  = notificationState.getNotifications();
    Recipient                          recipient      = notifications.get(0).getRecipient();
    int                                notificationId = (SUMMARY_NOTIFICATION_ID + (bundled ? notifications.get(0).getChatId() : 0));

    builder.setChat(notifications.get(0).getRecipient());
    builder.setMessageCount(notificationState.getMessageCount());
    builder.setPrimaryMessageBody(recipient, notifications.get(0).getIndividualRecipient(),
                                  notifications.get(0).getText(""), notifications.get(0).getSlideDeck());
    builder.setContentIntent(notifications.get(0).getPendingIntent(context));
    builder.setGroup(NOTIFICATION_GROUP);
    builder.setDeleteIntent(notificationState.getDeleteIntent(context));

    long timestamp = notifications.get(0).getTimestamp();
    if (timestamp != 0) builder.setWhen(timestamp);

    builder.addActions(notificationState.getMarkAsReadIntent(context, notificationId),
                       notificationState.getQuickReplyIntent(context, notifications.get(0).getRecipient()),
                       notificationState.getRemoteReplyIntent(context, notifications.get(0).getRecipient()));

    builder.addAndroidAutoAction(notificationState.getAndroidAutoReplyIntent(context, notifications.get(0).getRecipient()),
                                 notificationState.getAndroidAutoHeardIntent(context, notificationId), notifications.get(0).getTimestamp());

    ListIterator<NotificationItem> iterator = notifications.listIterator();

    while(iterator.hasNext()) {
      NotificationItem item = iterator.next();
      builder.addMessageBody(item.getRecipient(), item.getIndividualRecipient(), item.getText());
    }

    if (signal) {
      builder.setAlarms(notificationState.getRingtone(context), notificationState.getVibrate(context));
      builder.setTicker(notifications.get(0).getIndividualRecipient(),
                        notifications.get(0).getText());
    }

    if (!bundled) {
      builder.setGroupSummary(true);
    }

    NotificationManagerCompat.from(context).notify(notificationId, builder.build());
  }

  private static void updatePendingNotifications(Context context) {
    while (pendingNotifications.size() > 0) {
      Pair<Integer, Boolean> chatSignalPair = pendingNotifications.pop();
      updateNotification(context, chatSignalPair.first(), chatSignalPair.second());
    }
  }

  private static void sendMultipleChatNotification(@NonNull  Context context,
                                                   @NonNull  NotificationState notificationState,
                                                   boolean signal)
  {
    MultipleRecipientNotificationBuilder builder       = new MultipleRecipientNotificationBuilder(context, Prefs.getNotificationPrivacy(context));
    List<NotificationItem>               notifications = notificationState.getNotifications();

    builder.setMessageCount(notificationState.getMessageCount(), notificationState.getChatCount());
    builder.setMostRecentSender(notifications.get(0).getIndividualRecipient());
    builder.setGroup(NOTIFICATION_GROUP);
    builder.setDeleteIntent(notificationState.getDeleteIntent(context));

    long timestamp = notifications.get(0).getTimestamp();
    if (timestamp != 0) builder.setWhen(timestamp);

    builder.addActions(notificationState.getMarkAsReadIntent(context, SUMMARY_NOTIFICATION_ID));

    ListIterator<NotificationItem> iterator = notifications.listIterator(notifications.size());

    while(iterator.hasPrevious()) {
      NotificationItem item = iterator.previous();
      builder.addMessageBody(item.getRecipient(), item.getIndividualRecipient(), item.getText());
    }

    if (signal) {
      builder.setAlarms(notificationState.getRingtone(context), notificationState.getVibrate(context));
      builder.setTicker(notifications.get(0).getIndividualRecipient(),
                        notifications.get(0).getText());
    }

    NotificationManagerCompat.from(context).notify(SUMMARY_NOTIFICATION_ID, builder.build());
  }

  private static void removePendingNotifications(int chatId) {
    for (Pair<Integer, Boolean> pendingNotification : pendingNotifications) {
      if (pendingNotification.first() == chatId) {
        pendingNotifications.remove(pendingNotification);
        break;
      }
    }
  }

  private static void sendInChatNotification(Context context, int chatId) {
    if (instance == null) {
      Log.w(TAG, "Message notifier not initialized. Cannot play sounds");
      return;
    }

    if (!Prefs.isInChatNotifications(context) ||
        ServiceUtil.getAudioManager(context).getRingerMode() != AudioManager.RINGER_MODE_NORMAL)
    {
      return;
    }

    if(Prefs.isChatMuted(context, chatId)) {
      Log.d(TAG, "chat muted");
      return;
    }

    if (instance.soundInLoaded) {
      instance.soundPool.play(instance.soundIn, 1.0f, 1.0f, 1, 0, 1.0f);
    }
  }

  private static NotificationState constructNotificationState(@NonNull ApplicationDcContext dcContext,
                                                              @NonNull  int[] freshMessages)
  {
    NotificationState     notificationState = new NotificationState();
    Context context = dcContext.context;

    for(int msgId : freshMessages) {
      DcMsg record = dcContext.getMsg(msgId);
      if (record.isInfo()) {
        continue;
      }
      int          id                    = record.getId();
      boolean      mms                   = record.isMms() || record.isMediaPending();
      int          chatId                = record.getChatId();
      CharSequence body                  = record.getDisplayBody();
      DcMsg        dcMsg                 = dcContext.getMsg(msgId);
      Recipient    chatRecipient         = new Recipient(context, dcContext.getChat(dcMsg.getChatId()), null);
      Recipient    individualRecipient   = new Recipient(context, null, dcContext.getContact(dcMsg.getFromId()));
      SlideDeck    slideDeck             = new SlideDeck(dcContext.context, record);
      long         timestamp             = record.getTimestamp();

      if(slideDeck.getSlides().isEmpty())
        slideDeck = null;

      // TODO: if message content should be hidden on screen lock, do it here.
      if (record.hasFile() && TextUtils.isEmpty(body)) {
        String summaryText = record.getSummarytext(100);
        if (summaryText.isEmpty()) {
          body = SpanUtil.italic(context.getString(R.string.notify_media_message));
        } else {
          body = SpanUtil.italic(summaryText);
        }
      } else if (record.hasFile() && !record.isMediaPending()) {
        String message      = context.getString(R.string.notify_media_message_with_text, body);
        int    italicLength = message.length() - body.length();
        body = SpanUtil.italic(message, italicLength);
      }

      if (!Prefs.isChatMuted(context, chatId)) {
        notificationState.addNotification(new NotificationItem(id, mms, chatRecipient, individualRecipient, chatId, body, timestamp, slideDeck));
      }
    }

    return notificationState;
  }

  private static void updateBadge(Context context, int count) {
    try {
      if (count == 0) ShortcutBadger.removeCount(context);
      else            ShortcutBadger.applyCount(context, count);
    } catch (RuntimeException t) {
      // NOTE :: I don't totally trust this thing, so I'm catching
      // everything.
      Log.w("MessageNotifier", t);
    }
  }

  private static void scheduleReminder(Context context, int count) {
    if (count >= Prefs.getRepeatAlertsCount(context)) {
      return;
    }

    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    if(alarmManager == null) return;
    Intent       alarmIntent  = new Intent(ReminderReceiver.REMINDER_ACTION);
    alarmIntent.putExtra("reminder_count", count);

    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    long          timeout       = TimeUnit.MINUTES.toMillis(2);

    alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeout, pendingIntent);
  }

  static void clearReminder(Context context) {
    Intent        alarmIntent   = new Intent(ReminderReceiver.REMINDER_ACTION);
    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    AlarmManager  alarmManager  = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    if(alarmManager == null) return;
    alarmManager.cancel(pendingIntent);
  }

  public static class ReminderReceiver extends BroadcastReceiver {

    public static final String REMINDER_ACTION = "org.thoughtcrime.securesms.MessageNotifier.REMINDER_ACTION";

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onReceive(final Context context, final Intent intent) {
      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          int reminderCount = intent.getIntExtra("reminder_count", 0);
          MessageNotifier.updateNotification(context, true, reminderCount + 1);

          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }
}
