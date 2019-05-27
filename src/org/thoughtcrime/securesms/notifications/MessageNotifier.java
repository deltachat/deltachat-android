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

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;

import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.SlideDeck;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ServiceUtil;
import org.thoughtcrime.securesms.util.SpanUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

import static org.thoughtcrime.securesms.notifications.MessageNotifierCompat.NO_VISIBLE_CHAT_ID;
import static org.thoughtcrime.securesms.notifications.MessageNotifierCompat.SUMMARY_NOTIFICATION_ID;


/**
 * Handles posting system notifications for new messages.
 *
 *
 * @author Moxie Marlinspike
 */

@TargetApi(23)
class MessageNotifier implements IMessageNotifier {

  private static final String TAG = MessageNotifier.class.getSimpleName();

  private static final String NOTIFICATION_GROUP        = "messages";
  private static final long   MIN_AUDIBLE_PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(20);
  private static final long   STARTUP_SILENCE_DELTA     = TimeUnit.MINUTES.toMillis(1);
  private static final long   INITIAL_STARTUP           = System.currentTimeMillis();

          static volatile int                visibleChatId                = NO_VISIBLE_CHAT_ID;
          static volatile long               lastAudibleNotification      = -1;
          final           NotificationState  notificationState;
          final           Context            appContext;
  private final           SoundPool          soundPool;
  private final           int                soundIn;
  private final           int                soundOut;
  private                 boolean            soundInLoaded;
  private                 boolean            soundOutLoaded;

  protected MessageNotifier(Context context) {
    appContext = context.getApplicationContext();
    soundPool = new SoundPool(3, AudioManager.STREAM_SYSTEM, 0);
    soundIn = soundPool.load(context, R.raw.sound_in, 1);
    soundOut = soundPool.load(context, R.raw.sound_out, 1);
    notificationState = new NotificationState();

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

  @Override
  public void playSendSound() {
    if (Prefs.isInChatNotifications(appContext) && soundOutLoaded) {
      soundPool.play(soundOut, 1.0f, 1.0f, 1, 0, 1.0f);
    }
  }

  @Override
  public void updateVisibleChat(int chatId) {
    visibleChatId = chatId;
    if (visibleChatId != NO_VISIBLE_CHAT_ID) {
      removeNotifications(visibleChatId);
    }
  }

  @Override
  public void updateNotification(int chatId, int messageId) {
    boolean isVisible = visibleChatId == chatId;
    ApplicationDcContext dcContext = DcHelper.getContext(appContext);

    if (isVisible) {
      dcContext.marknoticedChat(chatId);
    }

    if (!Prefs.isNotificationsEnabled(appContext) ||
            Prefs.isChatMuted(appContext, chatId))
    {
      return;
    }

    if (isVisible) {
      sendInChatNotification(chatId);
    } else if (visibleChatId != NO_VISIBLE_CHAT_ID) {
      //different chat
      sendNotifications(chatId, messageId, false);
    } else {
      //app is in background or different Activity is on top
      sendNotifications(chatId, messageId, true);
    }
  }

  /**
   * On notification privacy preference changed,
   * the notification state needs to be updated.
   */
  @Override
  public void onNotificationPrivacyChanged() {
    if (!Prefs.isNotificationsEnabled(appContext)) {
      return;
    }

    clearNotifications();
    ApplicationDcContext dcContext = DcHelper.getContext(appContext);
    int[] freshMessages = dcContext.getFreshMsgs();
    for (int message : freshMessages) {
      DcMsg record = dcContext.getMsg(message);
      updateNotification(record.getChatId(), record.getId());
    }
  }

  @Override
   public void removeNotifications(int[] chatIds) {
    List<NotificationItem> removedItems = new LinkedList<>();
      for (int id : chatIds) {
        removedItems.addAll(notificationState.removeNotificationsForChat(id));
      }
    cancelNotifications(removedItems);
    recreateSummaryNotification();
  }

  @Override
  public void removeNotifications(int chatId) {
    List<NotificationItem> removedItems = notificationState.removeNotificationsForChat(chatId);
    cancelNotifications(removedItems);
    recreateSummaryNotification();
  }

  void cancelNotifications(List<NotificationItem> removedItems) {
    NotificationManager notifications = ServiceUtil.getNotificationManager(appContext);
    for (NotificationItem item : removedItems) {
      notifications.cancel(item.getId());
    }
  }

  private void recreateSummaryNotification() {
    NotificationManager notifications = ServiceUtil.getNotificationManager(appContext);
    notifications.cancel(SUMMARY_NOTIFICATION_ID);

    if (notificationState.hasMultipleChats()) {
      for (Integer id : notificationState.getChats()) {
        sendSingleChatNotification(appContext, new NotificationState(notificationState.getNotificationsForChat(id)), false, true);
      }
      sendMultipleChatNotification(appContext, notificationState, false);
    } else {
      sendSingleChatNotification(appContext, notificationState, false, false);
    }
  }

  private void cancelActiveNotifications() {
    NotificationManager notifications = ServiceUtil.getNotificationManager(appContext);
    notifications.cancel(SUMMARY_NOTIFICATION_ID);
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

  void sendNotifications(int chatId, int messageId, boolean signal) {
    ApplicationDcContext dcContext = DcHelper.getContext(appContext);
    if (signal = isSignalAllowed(signal)) {
      lastAudibleNotification = System.currentTimeMillis();;
    }

    addMessageToNotificationState(dcContext, chatId, messageId);
    if (notificationState.hasMultipleChats()) {
      for (int id : notificationState.getChats()) {
        sendSingleChatNotification(appContext, new NotificationState(notificationState.getNotificationsForChat(id)), false, true);
      }
      sendMultipleChatNotification(appContext, notificationState, signal);
    } else {
      sendSingleChatNotification(appContext, notificationState, signal, false);
    }
  }

  boolean isSignalAllowed(boolean signalRequested) {
    long now = System.currentTimeMillis();
    return signalRequested && (
            now - INITIAL_STARTUP) > STARTUP_SILENCE_DELTA &&
                    (now - lastAudibleNotification) > MIN_AUDIBLE_PERIOD_MILLIS;
  }

  private void clearNotifications() {
    notificationState.reset();
    cancelActiveNotifications();
  }

  void sendSingleChatNotification(@NonNull  Context context,
                                                 @NonNull  NotificationState notificationState,
                                                 boolean signal,
                                                 boolean bundled)
  {
    if (notificationState.getNotifications().isEmpty()) {
      if (!bundled) cancelActiveNotifications();
      return;
    }

    SingleRecipientNotificationBuilder builder               = new SingleRecipientNotificationBuilder(context, Prefs.getNotificationPrivacy(context));
    List<NotificationItem>             notifications         = notificationState.getNotifications();
    NotificationItem                   firstItem             = notifications.get(0);
    Recipient                          recipient             = firstItem.getRecipient();
    int                                chatId                = firstItem.getChatId();
    int                                notificationId        = (SUMMARY_NOTIFICATION_ID + (bundled ? chatId : 0));

    builder.setChat(firstItem.getRecipient());
    builder.setMessageCount(notificationState.getMessageCount());
    builder.setPrimaryMessageBody(recipient, firstItem.getIndividualRecipient(),
                                  firstItem.getText(""), firstItem.getSlideDeck());
    builder.setContentIntent(firstItem.getPendingIntent(context));
    builder.setGroup(NOTIFICATION_GROUP);
    builder.setDeleteIntent(notificationState.getMarkAsReadIntent(context, chatId, notificationId));

    long timestamp = firstItem.getTimestamp();
    if (timestamp != 0) builder.setWhen(timestamp);

    builder.addActions(notificationState.getMarkAsReadIntent(context, chatId, notificationId),
                       notificationState.getQuickReplyIntent(context, recipient),
                       notificationState.getRemoteReplyIntent(context, recipient));

    ListIterator<NotificationItem> iterator = notifications.listIterator(notifications.size());

    while(iterator.hasPrevious()) {
      NotificationItem item = iterator.previous();
      builder.addMessageBody(item.getRecipient(), item.getIndividualRecipient(), item.getText());
    }

    if (signal) {
      builder.setAlarms(notificationState.getRingtone(context), notificationState.getVibrate(context));
      builder.setTicker(firstItem.getIndividualRecipient(),
                        firstItem.getText());
    }

    if (!bundled) {
      builder.setGroupSummary(true);
    }

    NotificationManagerCompat.from(context).notify(notificationId, builder.build());
  }

  void sendMultipleChatNotification(@NonNull  Context context,
                                                   @NonNull  NotificationState notificationState,
                                                   boolean signal)
  {
    MultipleRecipientNotificationBuilder builder               = new MultipleRecipientNotificationBuilder(context, Prefs.getNotificationPrivacy(context));
    List<NotificationItem>               notifications         = notificationState.getNotifications();
    NotificationItem                     firstItem             = notifications.get(0);

    builder.setMessageCount(notificationState.getMessageCount(), notificationState.getChatCount());
    builder.setMostRecentSender(firstItem.getIndividualRecipient());
    builder.setGroup(NOTIFICATION_GROUP);
    builder.setDeleteIntent(notificationState.getMarkAsReadIntent(context, 0, SUMMARY_NOTIFICATION_ID));

    long timestamp = firstItem.getTimestamp();
    if (timestamp != 0) builder.setWhen(timestamp);

    builder.addActions(notificationState.getMarkAsReadIntent(context, 0, SUMMARY_NOTIFICATION_ID));

    ListIterator<NotificationItem> iterator = notifications.listIterator(notifications.size());

    while(iterator.hasPrevious()) {
      NotificationItem item = iterator.previous();
      builder.addMessageBody(item.getRecipient(), item.getIndividualRecipient(), item.getText());
    }

    if (signal) {
      builder.setAlarms(notificationState.getRingtone(context), notificationState.getVibrate(context));
      builder.setTicker(firstItem.getIndividualRecipient(),
                        firstItem.getText());
    }

    NotificationManagerCompat.from(context).notify(SUMMARY_NOTIFICATION_ID, builder.build());
  }

  private void sendInChatNotification(int chatId) {
    if (!Prefs.isInChatNotifications(appContext) ||
        ServiceUtil.getAudioManager(appContext).getRingerMode() != AudioManager.RINGER_MODE_NORMAL)
    {
      return;
    }

    if(Prefs.isChatMuted(appContext, chatId)) {
      Log.d(TAG, "chat muted");
      return;
    }

    if (soundInLoaded) {
      soundPool.play(soundIn, 1.0f, 1.0f, 1, 0, 1.0f);
    }
  }

  void addMessageToNotificationState(ApplicationDcContext dcContext, int chatId, int msgId) {
      if (Prefs.isChatMuted(appContext, chatId)) {
        return;
      }

      DcMsg record = dcContext.getMsg(msgId);
      if (record.isInfo()) {
        return;
      }

      int          id                    = record.getId();
      CharSequence body                  = record.getDisplayBody();
      DcMsg        dcMsg                 = dcContext.getMsg(msgId);
      Recipient    chatRecipient         = new Recipient(appContext, dcContext.getChat(dcMsg.getChatId()), null);
      Recipient    individualRecipient   = new Recipient(appContext, null, dcContext.getContact(dcMsg.getFromId()));
      SlideDeck    slideDeck             = new SlideDeck(dcContext.context, record);
      long         timestamp             = record.getTimestamp();


    if(slideDeck.getSlides().isEmpty())
      slideDeck = null;

    // TODO: if message content should be hidden on screen lock, do it here.
    if (record.hasFile() && TextUtils.isEmpty(body)) {
      String summaryText = record.getSummarytext(100);
      if (summaryText.isEmpty()) {
        body = SpanUtil.italic(appContext.getString(R.string.notify_media_message));
      } else {
        body = SpanUtil.italic(summaryText);
      }
    } else if (record.hasFile() && !record.isMediaPending()) {
      String message      = appContext.getString(R.string.notify_media_message_with_text, body);
      int    italicLength = message.length() - body.length();
      body = SpanUtil.italic(message, italicLength);
    }

    notificationState.addNotification(new NotificationItem(id, chatRecipient, individualRecipient, chatId, body, timestamp, slideDeck));
  }
}
