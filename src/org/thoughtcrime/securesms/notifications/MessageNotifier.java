package org.thoughtcrime.securesms.notifications;

import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Vibrator;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;
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

abstract class MessageNotifier {

    static final String TAG = org.thoughtcrime.securesms.notifications.MessageNotifierApi23.class.getSimpleName();

    private static final String NOTIFICATION_GROUP        = "messages";
    private static final long   MIN_AUDIBLE_PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(20);
    private static final long   STARTUP_SILENCE_DELTA     = TimeUnit.MINUTES.toMillis(1);
    private static final long   INITIAL_STARTUP           = System.currentTimeMillis();

            static volatile int                visibleChatId                = NO_VISIBLE_CHAT_ID;
            static volatile long               lastAudibleNotification      = -1;
                    final   NotificationState  notificationState;
                    final   Context            appContext;
                    final   Object             lock;
    private         final   SoundPool          soundPool;
    private         final   AudioManager       audioManager;
    private         final   int                soundIn;
    private         final   int                soundOut;
    private                 boolean            soundInLoaded;
    private                 boolean            soundOutLoaded;

    MessageNotifier(Context context) {
        appContext = context.getApplicationContext();
        soundPool = new SoundPool(3, AudioManager.STREAM_SYSTEM, 0);
        audioManager = ServiceUtil.getAudioManager(appContext);
        soundIn = soundPool.load(context, R.raw.sound_in, 1);
        soundOut = soundPool.load(context, R.raw.sound_out, 1);
        notificationState = new NotificationState();
        lock = new Object();

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

    public void playSendSound() {
        if (Prefs.isInChatNotifications(appContext) && soundOutLoaded) {
            soundPool.play(soundOut, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    public void updateVisibleChat(int chatId) {
        visibleChatId = chatId;
        if (visibleChatId != NO_VISIBLE_CHAT_ID) {
            removeNotifications(visibleChatId);
        }
    }

    void updateNotification(int chatId, int messageId) {
        updateNotification(DcHelper.getContext(appContext).getChat(chatId), messageId);
    }

    private void updateNotification(DcChat chat, int messageId) {
        boolean isVisible = visibleChatId == chat.getId();

        if (!Prefs.isNotificationsEnabled(appContext) ||
                Prefs.isChatMuted(chat))
        {
            return;
        }

        if (isVisible) {
            sendInChatNotification(chat);
        } else if (visibleChatId != NO_VISIBLE_CHAT_ID) {
            //different chat is on top
            sendNotifications(chat, messageId, false);
        } else {
            //app is in background or different Activity is on top
            sendNotifications(chat, messageId, true);
        }
    }

    /**
     * On notification privacy preference changed,
     * the notification state needs to be updated.
     */
    public void onNotificationPrivacyChanged() {
        if (!Prefs.isNotificationsEnabled(appContext)) {
            return;
        }

        clearNotifications();
        ApplicationDcContext dcContext = DcHelper.getContext(appContext);
        int[] freshMessages = dcContext.getFreshMsgs();
        for (int message : freshMessages) {
            DcMsg record = dcContext.getMsg(message);
            updateNotification(dcContext.getChat(record.getChatId()), record.getId());
        }
    }

    public void removeNotifications(int[] chatIds) {
        List<NotificationItem> removedItems = new LinkedList<>();
        synchronized (lock) {
            for (int id : chatIds) {
                removedItems.addAll(notificationState.removeNotificationsForChat(id));
            }
        }
        cancelNotifications(removedItems);
        recreateSummaryNotification();
    }

    public void removeNotifications(int chatId) {
        List<NotificationItem> removedItems;
        synchronized (lock) {
            removedItems = notificationState.removeNotificationsForChat(chatId);
        }
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

        synchronized (lock) {
            if (notificationState.hasMultipleChats()) {
                for (Integer id : notificationState.getChats()) {
                    sendSingleChatNotification(appContext, new NotificationState(notificationState.getNotificationsForChat(id)), false, true);
                }
                sendMultipleChatNotification(appContext, notificationState, false);
            } else {
                sendSingleChatNotification(appContext, notificationState, false, false);
            }
        }
    }

    void cancelActiveNotifications() {
        NotificationManager notifications = ServiceUtil.getNotificationManager(appContext);
        notifications.cancel(SUMMARY_NOTIFICATION_ID);
    }

    void sendNotifications(DcChat chat, int messageId, boolean signal) {
        ApplicationDcContext dcContext = DcHelper.getContext(appContext);
        if (signal = isSignalAllowed(signal)) {
            lastAudibleNotification = System.currentTimeMillis();
        }

        if (chat.isDeviceTalk()) {
            // currently, we just never notify on device chat.
            // esp. on first start, this is annoying.
            return;
        }

        synchronized (lock) {
            addMessageToNotificationState(dcContext, chat, messageId);
            if (notificationState.hasMultipleChats()) {
                for (int id : notificationState.getChats()) {
                    sendSingleChatNotification(appContext, new NotificationState(notificationState.getNotificationsForChat(id)), false, true);
                }
                sendMultipleChatNotification(appContext, notificationState, signal);
            } else {
                sendSingleChatNotification(appContext, notificationState, signal, false);
            }
        }
    }

    boolean isSignalAllowed(boolean signalRequested) {
        long now = System.currentTimeMillis();
        return signalRequested &&
                (now - INITIAL_STARTUP) > STARTUP_SILENCE_DELTA &&
                (now - lastAudibleNotification) > MIN_AUDIBLE_PERIOD_MILLIS;
    }

    private void clearNotifications() {
        synchronized (lock) {
            notificationState.reset();
        }
        cancelActiveNotifications();
    }

    void sendSingleChatNotification(@NonNull Context context,
                                    @NonNull  NotificationState notificationState,
                                    boolean signal,
                                    boolean bundled)
    {
        AbstractNotificationBuilder notificationBuilder = createSingleChatNotification(context, notificationState, signal, bundled);
        if (notificationBuilder != null)
            notify(context, notificationBuilder.getNotificationId(), notificationBuilder, signal);
    }

    void sendMultipleChatNotification(@NonNull  Context context,
                                      @NonNull  NotificationState notificationState,
                                      boolean signal)
    {
        AbstractNotificationBuilder notificationBuilder = createMultipleChatNotification(context, notificationState, signal);
        if (notificationBuilder != null)
            notify(context, SUMMARY_NOTIFICATION_ID, notificationBuilder, signal);
    }

    protected AbstractNotificationBuilder createSingleChatNotification(@NonNull  Context context,
                                                                       @NonNull  NotificationState notificationState,
                                                                       boolean signal,
                                                                       boolean bundled) {
        if (notificationState.getNotifications().isEmpty()) {
             if (!bundled) cancelActiveNotifications();
          return null;
        }

        SingleRecipientNotificationBuilder builder               = new SingleRecipientNotificationBuilder(context, Prefs.getNotificationPrivacy(context));
        List<NotificationItem>             notifications         = notificationState.getNotifications();
        NotificationItem                   firstItem             = notifications.get(0);
                                           Recipient recipient   = firstItem.getRecipient();
        int                                chatId                = firstItem.getChatId();
        int                                notificationId        = (SUMMARY_NOTIFICATION_ID + (bundled ? chatId : 0));

        builder.setNotificationId(notificationId);
        builder.setChat(firstItem.getRecipient());
        builder.setMessageCount(notificationState.getMessageCount());
        builder.setPrimaryMessageBody(recipient, firstItem.getIndividualRecipient(),
                firstItem.getText(""), firstItem.getSlideDeck());
        builder.setContentIntent(firstItem.getPendingIntent(context));
        builder.setGroup(NOTIFICATION_GROUP);
        builder.setDeleteIntent(notificationState.getMarkAsReadIntent(context, chatId, notificationId));

        long timestamp = firstItem.getTimestamp();
        if (timestamp != 0) builder.setWhen(timestamp);

        //builder.addActions(notificationState.getMarkAsReadIntent(context, chatId, notificationId),
        //        notificationState.getRemoteReplyIntent(context, recipient));

        ListIterator<NotificationItem> iterator = notifications.listIterator(notifications.size());

        while(iterator.hasPrevious()) {
            NotificationItem item = iterator.previous();
            builder.addMessageBody(item.getRecipient(), item.getIndividualRecipient(), item.getText());
        }

        if (signal) {
            builder.setAlarms(audioManager.getRingerMode(),
                    notificationState.getRingtone(context),
                    notificationState.getVibrate(context));
            builder.setTicker(firstItem.getIndividualRecipient(),
                    firstItem.getText());
        }

        if (!bundled) {
            builder.setGroupSummary(true);
        }

        return builder;


    }

    private void playNotificationSound(Uri uri, boolean vibrate) {
        if(uri != null) {
            Ringtone ringtone = RingtoneManager.getRingtone(appContext, uri);
            
            if (ringtone != null) {
                ringtone.play();
            }
        } // else we selected "no sound"

        if (vibrate) {
            Vibrator v = (Vibrator) appContext.getSystemService(Context.VIBRATOR_SERVICE);
            if (v!=null) {
                v.vibrate(100);
                v.vibrate(200);
            }
        }
    }

    protected AbstractNotificationBuilder createMultipleChatNotification(@NonNull Context context,
                                                                         @NonNull NotificationState notificationState,
                                                                         boolean signal) {
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
            builder.setAlarms(audioManager.getRingerMode(),
                    notificationState.getRingtone(context),
                    notificationState.getVibrate(context));
            builder.setTicker(firstItem.getIndividualRecipient(),
                    firstItem.getText());
        }

        return builder;
    }

    private void notify(Context context, int notificationId, AbstractNotificationBuilder notificationBuilder, boolean signal) {
        if (signal) {
            playNotificationSound(notificationBuilder.getRingtone(), notificationBuilder.getVibrate());
        }
        NotificationManagerCompat.from(context).notify(notificationId, notificationBuilder.build());
    }

    private void sendInChatNotification(DcChat chat) {
        if (!Prefs.isInChatNotifications(appContext) ||
                audioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL)
        {
            return;
        }

        if(Prefs.isChatMuted(chat)) {
            Log.d(TAG, "chat muted");
            return;
        }

        if (soundInLoaded) {
            soundPool.play(soundIn, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    void addMessageToNotificationState(ApplicationDcContext dcContext, DcChat chat, int msgId) {
        if (Prefs.isChatMuted(chat)) {
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
        SlideDeck slideDeck             = new SlideDeck(dcContext.context, record);
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

        synchronized (lock) {
            notificationState.addNotification(new NotificationItem(id, chatRecipient, individualRecipient, chat.getId(), body, timestamp, slideDeck));
        }
    }
}

