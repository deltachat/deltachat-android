/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *                        (C) 2013-2016 Nikolai Kudashov
 *                           (C) 2017 Björn Petersen
 *                    Contact: r10s@b44t.com, http://b44t.com
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see http://www.gnu.org/licenses/ .
 *
 ******************************************************************************/


package com.b44t.messenger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.util.SparseArray;

import com.b44t.ui.ActionBar.Theme;
import com.b44t.ui.LaunchActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class NotificationsController {

    public static final String EXTRA_VOICE_REPLY = "extra_voice_reply";

    private DispatchQueue notificationsQueue = new DispatchQueue("notificationsQueue");
    private ArrayList<MessageObject> pushMessages = new ArrayList<>();
    private ArrayList<MessageObject> delayedPushMessages = new ArrayList<>();
    private HashMap<Long, MessageObject> pushMessagesDict = new HashMap<>();
    private HashMap<Long, Point> smartNotificationsDialogs = new HashMap<>();
    private NotificationManagerCompat notificationManager = null;
    private HashMap<Integer, Integer> pushDialogs = new HashMap<>();
    private HashMap<Long, Integer> wearNotificationsIds = new HashMap<>();
    private HashMap<Long, Integer> autoNotificationsIds = new HashMap<>();
    private int wearNotificationId = 10000;
    private int autoNotificationId = 20000;
    public ArrayList<MessageObject> popupMessages = new ArrayList<>();
    private long opened_dialog_id = 0;
    private int total_unread_count = 0;
    private int personal_count = 0;
    private boolean notifyCheck = false;
    private int lastOnlineFromOtherDevice = 0;
    private boolean inChatSoundEnabled = true;
    private int lastBadgeCount;
    private String launcherClassName;

    private Runnable notificationDelayRunnable;
    private PowerManager.WakeLock notificationDelayWakelock;

    private long lastSoundPlay;
    private long lastSoundOutPlay;
    private SoundPool soundPool;
    private int soundIn;
    private int soundOut;
    private int soundRecord;
    private boolean soundInLoaded;
    private boolean soundOutLoaded;
    private boolean soundRecordLoaded;
    protected AudioManager audioManager;
    private AlarmManager alarmManager;

    private static volatile NotificationsController Instance = null;
    public static NotificationsController getInstance() {
        NotificationsController localInstance = Instance;
        if (localInstance == null) {
            synchronized (MessagesController.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new NotificationsController();
                }
            }
        }
        return localInstance;
    }

    public NotificationsController() {
        notificationManager = NotificationManagerCompat.from(ApplicationLoader.applicationContext);
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Context.MODE_PRIVATE);
        inChatSoundEnabled = preferences.getBoolean("EnableInChatSound", true);

        try {
            audioManager = (AudioManager) ApplicationLoader.applicationContext.getSystemService(Context.AUDIO_SERVICE);
        } catch (Exception e) {
            FileLog.e("messenger", e);
        }
        try {
            alarmManager = (AlarmManager) ApplicationLoader.applicationContext.getSystemService(Context.ALARM_SERVICE);
        } catch (Exception e) {
            FileLog.e("messenger", e);
        }

        try {
            PowerManager pm = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
            notificationDelayWakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "lock");
            notificationDelayWakelock.setReferenceCounted(false);
        } catch (Exception e) {
            FileLog.e("messenger", e);
        }

        notificationDelayRunnable = new Runnable() {
            @Override
            public void run() {
                FileLog.e("messenger", "delay reached");
                if (!delayedPushMessages.isEmpty()) {
                    showOrUpdateNotification(true);
                    delayedPushMessages.clear();
                }
                try {
                    if (notificationDelayWakelock.isHeld()) {
                        notificationDelayWakelock.release();
                    }
                } catch (Exception e) {
                    FileLog.e("messenger", e);
                }
            }
        };
    }

    public void cleanup() {
        popupMessages.clear();
        notificationsQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                opened_dialog_id = 0;
                total_unread_count = 0;
                personal_count = 0;
                pushMessages.clear();
                pushMessagesDict.clear();
                pushDialogs.clear();
                wearNotificationsIds.clear();
                autoNotificationsIds.clear();
                delayedPushMessages.clear();
                notifyCheck = false;
                lastBadgeCount = 0;
                try {
                    if (notificationDelayWakelock.isHeld()) {
                        notificationDelayWakelock.release();
                    }
                } catch (Exception e) {
                    FileLog.e("messenger", e);
                }
                setBadge(0);
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.clear();
                editor.commit();
            }
        });
    }

    public void setInChatSoundEnabled(boolean value) {
        inChatSoundEnabled = value;
    }

    public void setOpenedDialogId(final long dialog_id) {
        notificationsQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                opened_dialog_id = dialog_id;
            }
        });
    }

    /*
    public void setLastOnlineFromOtherDevice(final int time) {
        notificationsQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                FileLog.e("messenger", "set last online from other device = " + time);
                lastOnlineFromOtherDevice = time;
            }
        });
    }
    */

    /*
    public void removeNotificationsForDialog(long did) {
        NotificationsController.getInstance().processReadMessages(null, did, 0, Integer.MAX_VALUE, false);
        HashMap<Long, Integer> dialogsToUpdate = new HashMap<>();
        dialogsToUpdate.put(did, 0);
        NotificationsController.getInstance().processDialogsUpdateRead(dialogsToUpdate);
    }
    */

    /*
    public void removeDeletedMessagesFromNotifications(final SparseArray<ArrayList<Integer>> deletedMessages) {
        final ArrayList<MessageObject> popupArray = popupMessages.isEmpty() ? null : new ArrayList<>(popupMessages);
        notificationsQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                int old_unread_count = total_unread_count;
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Context.MODE_PRIVATE);
                for (int a = 0; a < deletedMessages.size(); a++) {
                    int key = deletedMessages.keyAt(a);
                    long dialog_id = -key;
                    ArrayList<Integer> mids = deletedMessages.get(key);
                    Integer currentCount = pushDialogs.get(dialog_id);
                    if (currentCount == null) {
                        currentCount = 0;
                    }
                    Integer newCount = currentCount;
                    for (int b = 0; b < mids.size(); b++) {
                        long mid = mids.get(b);
                        mid |= ((long) key) << 32;
                        MessageObject messageObject = pushMessagesDict.get(mid);
                        if (messageObject != null) {
                            pushMessagesDict.remove(mid);
                            delayedPushMessages.remove(messageObject);
                            pushMessages.remove(messageObject);
                            if (isPersonalMessage(messageObject)) {
                                personal_count--;
                            }
                            if (popupArray != null) {
                                popupArray.remove(messageObject);
                            }
                            newCount--;
                        }
                    }
                    if (newCount <= 0) {
                        newCount = 0;
                        smartNotificationsDialogs.remove(dialog_id);
                    }
                    if (!newCount.equals(currentCount)) {
                        total_unread_count -= currentCount;
                        total_unread_count += newCount;
                        pushDialogs.put(dialog_id, newCount);
                    }
                    if (newCount == 0) {
                        pushDialogs.remove(dialog_id);
                        pushDialogsOverrideMention.remove(dialog_id);
                        if (popupArray != null && pushMessages.isEmpty() && !popupArray.isEmpty()) {
                            popupArray.clear();
                        }
                    }
                }
                if (popupArray != null) {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            popupMessages = popupArray;
                        }
                    });
                }
                if (old_unread_count != total_unread_count) {
                    if (!notifyCheck) {
                        delayedPushMessages.clear();
                        showOrUpdateNotification(notifyCheck);
                    } else {
                        scheduleNotificationDelay(lastOnlineFromOtherDevice > ConnectionsManager.getInstance().getCurrentTime());
                    }
                }
                notifyCheck = false;
                if (preferences.getBoolean("badgeNumber", true)) {
                    setBadge(total_unread_count);
                }
            }
        });
    }
    */

    /*
    public void processReadMessages(final SparseArray<Long> inbox, final long dialog_id, final int max_date, final int max_id, final boolean isPopup) {
        final ArrayList<MessageObject> popupArray = popupMessages.isEmpty() ? null : new ArrayList<>(popupMessages);
        notificationsQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                int oldCount = popupArray != null ? popupArray.size() : 0;
                if (inbox != null) {
                    for (int b = 0; b < inbox.size(); b++) {
                        int key = inbox.keyAt(b);
                        long messageId = inbox.get(key);
                        for (int a = 0; a < pushMessages.size(); a++) {
                            MessageObject messageObject = pushMessages.get(a);
                            if (messageObject.getDialogId() == key && messageObject.getId() <= (int) messageId) {
                                if (isPersonalMessage(messageObject)) {
                                    personal_count--;
                                }
                                if (popupArray != null) {
                                    popupArray.remove(messageObject);
                                }
                                long mid = messageObject.messageOwner.id;
                                if (messageObject.messageOwner.to_id.channel_id != 0) {
                                    mid |= ((long) messageObject.messageOwner.to_id.channel_id) << 32;
                                }
                                pushMessagesDict.remove(mid);
                                delayedPushMessages.remove(messageObject);
                                pushMessages.remove(a);
                                a--;
                            }
                        }
                    }
                    if (popupArray != null && pushMessages.isEmpty() && !popupArray.isEmpty()) {
                        popupArray.clear();
                    }
                }
                if (dialog_id != 0 && (max_id != 0 || max_date != 0)) {
                    for (int a = 0; a < pushMessages.size(); a++) {
                        MessageObject messageObject = pushMessages.get(a);
                        if (messageObject.getDialogId() == dialog_id) {
                            boolean remove = false;
                            if (max_date != 0) {
                                if (messageObject.messageOwner.date <= max_date) {
                                    remove = true;
                                }
                            } else {
                                if (!isPopup) {
                                    if (messageObject.getId() <= max_id || max_id < 0) {
                                        remove = true;
                                    }
                                } else {
                                    if (messageObject.getId() == max_id || max_id < 0) {
                                        remove = true;
                                    }
                                }
                            }
                            if (remove) {
                                if (isPersonalMessage(messageObject)) {
                                    personal_count--;
                                }
                                pushMessages.remove(a);
                                delayedPushMessages.remove(messageObject);
                                if (popupArray != null) {
                                    popupArray.remove(messageObject);
                                }
                                long mid = messageObject.messageOwner.id;
                                if (messageObject.messageOwner.to_id.channel_id != 0) {
                                    mid |= ((long) messageObject.messageOwner.to_id.channel_id) << 32;
                                }
                                pushMessagesDict.remove(mid);
                                a--;
                            }
                        }
                    }
                    if (popupArray != null && pushMessages.isEmpty() && !popupArray.isEmpty()) {
                        popupArray.clear();
                    }
                }
                if (popupArray != null && oldCount != popupArray.size()) {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            popupMessages = popupArray;
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.pushMessagesUpdated);
                        }
                    });
                }
            }
        });
    }
    */

    public void processNewMessages(final int chat_id, final int msg_id) {
        if( chat_id <= 0 || msg_id <= 0 ) {
            return;
        }

        if( chat_id == opened_dialog_id && ApplicationLoader.isScreenOn ) {
            playInChatSound();
            return;
        }

        notificationsQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                if( pushMessagesDict.get((long)msg_id)!=null ) {
                    return; // already added
                }

                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Context.MODE_PRIVATE);
                int notifyOverride = getNotifyOverride(preferences, chat_id);
                if (notifyOverride == 2) {
                    return; // muted
                }

                MrChat mrChat = MrMailbox.getChat(chat_id);
                if (mrChat.getId() == 0) {
                    return;
                }
                boolean isGroupChat = mrChat.getType() == MrChat.MR_CHAT_GROUP;
                boolean value = !((!preferences.getBoolean("EnableAll", true) || isGroupChat && !preferences.getBoolean("EnableGroup", true)) && notifyOverride == 0);

                if (value) {
                    MrMsg mrMsg = MrMailbox.getMsg(msg_id);
                    if (mrMsg.getId() == 0 || mrMsg.getFromId() == MrContact.MR_CONTACT_ID_SELF) {
                        return;
                    }
                    TLRPC.Message tmsg = mrMsg.get_TLRPC_Message();
                    MessageObject msgDrawObj = new MessageObject(tmsg, null, true);

                    delayedPushMessages.add(msgDrawObj);
                    pushMessages.add(0, msgDrawObj);
                    pushMessagesDict.put((long) msg_id, msgDrawObj);

                    int chat_id = mrMsg.getChatId();
                    int old_cnt = pushDialogs.get(chat_id)==null? 0 : pushDialogs.get(chat_id);
                    pushDialogs.put(chat_id, old_cnt+1);
                    total_unread_count++;

                    showOrUpdateNotification(true /*play sound*/);
                }
            }
        });

        /* old func:
        public void processNewMessages(final ArrayList<MessageObject> messageObjects, final boolean isLast)
        if (messageObjects.isEmpty()) {
            return;
        }
        final ArrayList<MessageObject> popupArray = new ArrayList<>(popupMessages);
        notificationsQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                boolean added = false;

                int oldCount = popupArray.size();
                HashMap<Long, Boolean> settingsCache = new HashMap<>();
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Context.MODE_PRIVATE);
                boolean allowPinned = preferences.getBoolean("PinnedMessages", true);
                int popup = 0;

                for (int a = 0; a < messageObjects.size(); a++) {
                    MessageObject messageObject = messageObjects.get(a);
                    long mid = messageObject.messageOwner.id;
                    if (messageObject.messageOwner.to_id.channel_id != 0) {
                        mid |= ((long) messageObject.messageOwner.to_id.channel_id) << 32;
                    }
                    if (pushMessagesDict.containsKey(mid)) {
                        continue;
                    }
                    long dialog_id = messageObject.getDialogId();
                    long original_dialog_id = dialog_id;
                    if (dialog_id == opened_dialog_id && ApplicationLoader.isScreenOn) {
                        playInChatSound();
                        continue;
                    }
                    if (messageObject.messageOwner.mentioned) {
                        if (!allowPinned && messageObject.messageOwner.action instanceof TLRPC.TL_messageActionPinMessage) {
                            continue;
                        }
                        dialog_id = messageObject.messageOwner.from_id;
                    }
                    if (isPersonalMessage(messageObject)) {
                        personal_count++;
                    }
                    added = true;

                    Boolean value = settingsCache.get(dialog_id);
                    boolean isChat = (int) dialog_id < 0;
                    if (value == null) {
                        int notifyOverride = getNotifyOverride(preferences, dialog_id);
                        value = !(notifyOverride == 2 || (!preferences.getBoolean("EnableAll", true) || isChat && !preferences.getBoolean("EnableGroup", true)) && notifyOverride == 0);
                        settingsCache.put(dialog_id, value);
                    }
                    if (value) {
                        delayedPushMessages.add(messageObject);
                        pushMessages.add(0, messageObject);
                        pushMessagesDict.put(mid, messageObject);
                        if (original_dialog_id != dialog_id) {
                            pushDialogsOverrideMention.put(original_dialog_id, 1);
                        }
                    }
                }

                if (added) {
                    notifyCheck = isLast;
                }


            }
        });
        */
    }

    public void removeSeenMessages()
    {
        if( pushMessages.isEmpty() ) {
            return;
        }

        // get all unread messages as a hash
        int unseenArr[] = MrMailbox.getUnseenMsgs();
        int unseenCnt = unseenArr.length;
        HashMap<Integer, Boolean> unseenHash = new HashMap<>();
        for( int i = 0; i < unseenCnt; i++ ) {
            unseenHash.put(unseenArr[i], true);
        }

        // go through all objects and check if they're still unread
        boolean sthRemoved = false;
        for( int i = 0; i < pushMessages.size() /*do no cache, size may shrink in loop*/; i++ ) {
            MessageObject messageObject = pushMessages.get(i);
            if( unseenHash.get(messageObject.messageOwner.id)==null ) {
                // this message is no longer unseen
                int dialog_id = (int)messageObject.messageOwner.dialog_id;
                pushMessagesDict.remove((long)messageObject.messageOwner.id);
                delayedPushMessages.remove(messageObject);
                pushMessages.remove(i);
                i--;
                total_unread_count--;

                Integer oldDlgCnt = pushDialogs.get(dialog_id);
                if( oldDlgCnt != null ) {
                    if( oldDlgCnt<=1 ) {
                        pushDialogs.remove(dialog_id);
                    }
                    else {
                        pushDialogs.put(dialog_id, oldDlgCnt-1);
                    }
                }

                sthRemoved = true;
            }
        }

        if( sthRemoved ) {
            notificationsQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    showOrUpdateNotification(false);
                }
            });
        }
    }

    /*
    public void processDialogsUpdateRead(final HashMap<Long, Integer> dialogsToUpdate) {
        final ArrayList<MessageObject> popupArray = popupMessages.isEmpty() ? null : new ArrayList<>(popupMessages);
        notificationsQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                int old_unread_count = total_unread_count;
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Context.MODE_PRIVATE);
                for (HashMap.Entry<Long, Integer> entry : dialogsToUpdate.entrySet()) {
                    long dialog_id = entry.getKey();

                    int notifyOverride = getNotifyOverride(preferences, dialog_id);
                    if (notifyCheck) {
                        Integer override = pushDialogsOverrideMention.get(dialog_id);
                        if (override != null && override == 1) {
                            pushDialogsOverrideMention.put(dialog_id, 0);
                            notifyOverride = 1;
                        }
                    }
                    boolean canAddValue = !(notifyOverride == 2 || (!preferences.getBoolean("EnableAll", true) || ((int)dialog_id < 0) && !preferences.getBoolean("EnableGroup", true)) && notifyOverride == 0);

                    Integer currentCount = pushDialogs.get(dialog_id);
                    Integer newCount = entry.getValue();
                    if (newCount == 0) {
                        smartNotificationsDialogs.remove(dialog_id);
                    }

                    if (newCount < 0) {
                        if (currentCount == null) {
                            continue;
                        }
                        newCount = currentCount + newCount;
                    }
                    if (canAddValue || newCount == 0) {
                        if (currentCount != null) {
                            total_unread_count -= currentCount;
                        }
                    }
                    if (newCount == 0) {
                        pushDialogs.remove(dialog_id);
                        pushDialogsOverrideMention.remove(dialog_id);
                        for (int a = 0; a < pushMessages.size(); a++) {
                            MessageObject messageObject = pushMessages.get(a);
                            if (messageObject.getDialogId() == dialog_id) {
                                if (isPersonalMessage(messageObject)) {
                                    personal_count--;
                                }
                                pushMessages.remove(a);
                                a--;
                                delayedPushMessages.remove(messageObject);
                                long mid = messageObject.messageOwner.id;
                                if (messageObject.messageOwner.to_id.channel_id != 0) {
                                    mid |= ((long) messageObject.messageOwner.to_id.channel_id) << 32;
                                }
                                pushMessagesDict.remove(mid);
                                if (popupArray != null) {
                                    popupArray.remove(messageObject);
                                }
                            }
                        }
                        if (popupArray != null && pushMessages.isEmpty() && !popupArray.isEmpty()) {
                            popupArray.clear();
                        }
                    } else if (canAddValue) {
                        total_unread_count += newCount;
                        pushDialogs.put((int)dialog_id, newCount);
                    }
                }
                if (popupArray != null) {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            popupMessages = popupArray;
                        }
                    });
                }
                if (old_unread_count != total_unread_count) {
                    if (!notifyCheck) {
                        delayedPushMessages.clear();
                        showOrUpdateNotification(notifyCheck);
                    } else {
                        scheduleNotificationDelay(lastOnlineFromOtherDevice > ConnectionsManager.getInstance().getCurrentTime());
                    }
                }
                notifyCheck = false;
                if (preferences.getBoolean("badgeNumber", true)) {
                    setBadge(total_unread_count);
                }
            }
        });
    }
    */

    /*
    public void processLoadedUnreadMessages(final HashMap<Long, Integer> dialogs, final ArrayList<TLRPC.Message> messages, final ArrayList<TLRPC.User> users, final ArrayList<TLRPC.Chat> chats, final ArrayList<TLRPC.EncryptedChat> encryptedChats) {
        MessagesController.getInstance().putUsers(users, true);
        MessagesController.getInstance().putChats(chats, true);
        //MessagesController.getInstance().putEncryptedChats(encryptedChats, true);

        notificationsQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                pushDialogs.clear();
                pushMessages.clear();
                pushMessagesDict.clear();
                total_unread_count = 0;
                personal_count = 0;
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Context.MODE_PRIVATE);
                HashMap<Long, Boolean> settingsCache = new HashMap<>();

                if (messages != null) {
                    for (TLRPC.Message message : messages) {
                        long mid = message.id;
                        if (message.to_id.channel_id != 0) {
                            mid |= ((long) message.to_id.channel_id) << 32;
                        }
                        if (pushMessagesDict.containsKey(mid)) {
                            continue;
                        }
                        MessageObject messageObject = new MessageObject(message, null, false);
                        if (isPersonalMessage(messageObject)) {
                            personal_count++;
                        }
                        long dialog_id = messageObject.getDialogId();
                        long original_dialog_id = dialog_id;
                        if (messageObject.messageOwner.mentioned) {
                            dialog_id = messageObject.messageOwner.from_id;
                        }
                        Boolean value = settingsCache.get(dialog_id);
                        if (value == null) {
                            int notifyOverride = getNotifyOverride(preferences, dialog_id);
                            value = !(notifyOverride == 2 || (!preferences.getBoolean("EnableAll", true) || ((int) dialog_id < 0) && !preferences.getBoolean("EnableGroup", true)) && notifyOverride == 0);
                            settingsCache.put(dialog_id, value);
                        }
                        if (!value || dialog_id == opened_dialog_id && ApplicationLoader.isScreenOn) {
                            continue;
                        }
                        pushMessagesDict.put(mid, messageObject);
                        pushMessages.add(0, messageObject);
                        if (original_dialog_id != dialog_id) {
                            pushDialogsOverrideMention.put(original_dialog_id, 1);
                        }
                    }
                }
                for (HashMap.Entry<Long, Integer> entry : dialogs.entrySet()) {
                    long dialog_id = entry.getKey();
                    Boolean value = settingsCache.get(dialog_id);
                    if (value == null) {
                        int notifyOverride = getNotifyOverride(preferences, dialog_id);
                        Integer override = pushDialogsOverrideMention.get(dialog_id);
                        if (override != null && override == 1) {
                            pushDialogsOverrideMention.put(dialog_id, 0);
                            notifyOverride = 1;
                        }
                        value = !(notifyOverride == 2 || (!preferences.getBoolean("EnableAll", true) || ((int) dialog_id < 0) && !preferences.getBoolean("EnableGroup", true)) && notifyOverride == 0);
                        settingsCache.put(dialog_id, value);
                    }
                    if (!value) {
                        continue;
                    }
                    int count = entry.getValue();
                    pushDialogs.put(dialog_id, count);
                    total_unread_count += count;
                }
                if (total_unread_count == 0) {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            popupMessages.clear();
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.pushMessagesUpdated);
                        }
                    });
                }
                showOrUpdateNotification(SystemClock.uptimeMillis() / 1000 < 60);

                if (preferences.getBoolean("badgeNumber", true)) {
                    setBadge(total_unread_count);
                }
            }
        });
    }
    */

    public void setBadgeEnabled(boolean enabled) {
        setBadge(enabled ? total_unread_count : 0);
    }

    private void setBadge(final int count) {

        notificationsQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (lastBadgeCount == count) {
                    return;
                }
                lastBadgeCount = count;


                // support icon unread counts of "Nova Launcher", see http://novalauncher.com/teslaunread-api/
                try {
                    ContentValues cv = new ContentValues();
                    cv.put("tag", "com.b44t.messenger/com.b44t.ui.LaunchActivity");
                    cv.put("count", count);
                    ApplicationLoader.applicationContext.getContentResolver().insert(Uri.parse("content://com.teslacoilsw.notifier/unread_count"), cv);
                } catch (Throwable e) {
                }

                try {
                    if (launcherClassName == null) {
                        launcherClassName = getLauncherClassName(ApplicationLoader.applicationContext);
                    }
                    if (launcherClassName == null) {
                        return;
                    }
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Intent intent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
                                intent.putExtra("badge_count", count);
                                intent.putExtra("badge_count_package_name", ApplicationLoader.applicationContext.getPackageName());
                                intent.putExtra("badge_count_class_name", launcherClassName);
                                ApplicationLoader.applicationContext.sendBroadcast(intent);
                            } catch (Exception e) {
                                FileLog.e("messenger", e);
                            }
                        }
                    });
                } catch (Throwable e) {
                    FileLog.e("messenger", e);
                }
            }
        });
    }

    private final int ADD_USER = 0x01;
    private final int ADD_GROUP = 0x02;
    private String getStringForMessage(MessageObject messageObject, int flags) {
        long dialog_id = messageObject.messageOwner.dialog_id;
        int from_id = messageObject.messageOwner.from_id;

        MrChat mrChat = MrMailbox.getChat((int)dialog_id);
        MrContact mrContact = MrMailbox.getContact(from_id);
        String name = mrContact.getDisplayName();
        boolean is_group = mrChat.getType()==MrChat.MR_CHAT_GROUP;

        MrMsg  mrMsg = MrMailbox.getMsg(messageObject.getId());
        String msg = mrMsg.getSummary(160);

        String ret;
        if( (flags&ADD_GROUP)!=0 && is_group ) {
            ret = String.format("%s @ %s: %s", name, mrChat.getName(), msg);
        }
        else if( (flags&ADD_USER)!=0 ){
            ret = String.format("%s: %s", name, msg);
        }
        else {
            ret = msg;
        }

        return ret;
    }

    private void scheduleNotificationRepeat() {
        try {
            PendingIntent pintent = PendingIntent.getService(ApplicationLoader.applicationContext, 0, new Intent(ApplicationLoader.applicationContext, NotificationRepeat.class), 0);
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
            int minutes = preferences.getInt("repeat_messages", 0);
            if (minutes > 0 && personal_count > 0) {
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + minutes * 60 * 1000, pintent);
            } else {
                alarmManager.cancel(pintent);
            }
        } catch (Exception e) {
            FileLog.e("messenger", e);
        }
    }

    private static String getLauncherClassName(Context context) {
        try {
            PackageManager pm = context.getPackageManager();

            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
            for (ResolveInfo resolveInfo : resolveInfos) {
                String pkgName = resolveInfo.activityInfo.applicationInfo.packageName;
                if (pkgName.equalsIgnoreCase(context.getPackageName())) {
                    return resolveInfo.activityInfo.name;
                }
            }
        } catch (Throwable e) {
            FileLog.e("messenger", e);
        }
        return null;
    }

    private int getNotifyOverride(SharedPreferences preferences, long dialog_id) {
        int notifyOverride = preferences.getInt("notify2_" + dialog_id, 0);
        if (notifyOverride == 3) {
            int muteUntil = preferences.getInt("notifyuntil_" + dialog_id, 0);
            if (muteUntil >= ConnectionsManager.getInstance().getCurrentTime()) {
                notifyOverride = 2;
            }
        }
        return notifyOverride;
    }

    private void dismissNotification() {
        try {
            notificationManager.cancel(1);
            pushMessages.clear();
            pushMessagesDict.clear();
            for (HashMap.Entry<Long, Integer> entry : autoNotificationsIds.entrySet()) {
                notificationManager.cancel(entry.getValue());
            }
            autoNotificationsIds.clear();
            for (HashMap.Entry<Long, Integer> entry : wearNotificationsIds.entrySet()) {
                notificationManager.cancel(entry.getValue());
            }
            wearNotificationsIds.clear();
            AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.pushMessagesUpdated);
                }
            });
        } catch (Exception e) {
            FileLog.e("messenger", e);
        }
    }

    private void playInChatSound() {
        if (!inChatSoundEnabled || MediaController.getInstance().isRecordingAudio()) {
            return;
        }
        try {
            if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
                return;
            }
        } catch (Exception e) {
            FileLog.e("messenger", e);
        }

        try {
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Context.MODE_PRIVATE);
            int notifyOverride = getNotifyOverride(preferences, opened_dialog_id);
            if (notifyOverride == 2) {
                return;
            }
            notificationsQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    if (Math.abs(System.currentTimeMillis() - lastSoundPlay) <= 500) {
                        return;
                    }
                    lastSoundPlay = System.currentTimeMillis();
                    try {
                        if (soundPool == null) {
                            soundPool = new SoundPool(3, AudioManager.STREAM_SYSTEM, 0);
                            soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                                @Override
                                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                                    if (status == 0) {
                                        soundPool.play(sampleId, 1.0f, 1.0f, 1, 0, 1.0f);
                                    }
                                }
                            });
                        }
                        if (soundIn == 0 && !soundInLoaded) {
                            soundInLoaded = true;
                            soundIn = soundPool.load(ApplicationLoader.applicationContext, R.raw.sound_in, 1);
                        }
                        if (soundIn != 0) {
                            soundPool.play(soundIn, 1.0f, 1.0f, 1, 0, 1.0f);
                        }
                    } catch (Exception e) {
                        FileLog.e("messenger", e);
                    }
                }
            });
        } catch (Exception e) {
            FileLog.e("messenger", e);
        }
    }

    private void scheduleNotificationDelay(boolean onlineReason) {
        try {
            FileLog.e("messenger", "delay notification start, onlineReason = " + onlineReason);
            notificationDelayWakelock.acquire(10000);
            AndroidUtilities.cancelRunOnUIThread(notificationDelayRunnable);
            AndroidUtilities.runOnUIThread(notificationDelayRunnable, (onlineReason ? 3 * 1000 : 1000));
        } catch (Exception e) {
            FileLog.e("messenger", e);
            showOrUpdateNotification(notifyCheck);
        }
    }

    protected void repeatNotificationMaybe() {
        notificationsQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                if (hour >= 11 && hour <= 22) {
                    notificationManager.cancel(1);
                    showOrUpdateNotification(true);
                } else {
                    scheduleNotificationRepeat();
                }
            }
        });
    }

    private void showOrUpdateNotification(boolean notifyAboutLast) {
        if ( pushMessages.isEmpty()) {
            dismissNotification();
            return;
        }

        try {
            ConnectionsManager.getInstance().resumeNetworkMaybe();

            MessageObject lastMessageObject = pushMessages.get(0);
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Context.MODE_PRIVATE);
            int dismissDate = preferences.getInt("dismissDate", 0);
            if (lastMessageObject.messageOwner.date <= dismissDate) {
                dismissNotification();
                return;
            }

            final long dialog_id = lastMessageObject.getDialogId();

            //int mid = lastMessageObject.getId();
            final int user_id = lastMessageObject.messageOwner.from_id;

            //TLRPC.User user = MessagesController.getInstance().getUser(user_id);

            MrChat mrChat = MrMailbox.getChat((int)dialog_id);
            boolean isGroupChat = mrChat.getType() == MrChat.MR_CHAT_GROUP;


            //TLRPC.FileLocation photoPath = null;

            boolean notifyDisabled = false;
            int needVibrate = 0;
            String choosenSoundPath = null;
            int ledColor = 0xff00ff00;
            boolean inAppSounds;
            boolean inAppVibrate;
            //boolean inAppPreview = false;
            int priority = 0;
            int priorityOverride;
            int vibrateOverride;

            int notifyOverride = getNotifyOverride(preferences, dialog_id);
            if (!notifyAboutLast || notifyOverride == 2 || (!preferences.getBoolean("EnableAll", true) || isGroupChat && !preferences.getBoolean("EnableGroup", true)) && notifyOverride == 0) {
                notifyDisabled = true;
            }

            if (!notifyDisabled && isGroupChat ) {
                int notifyMaxCount = preferences.getInt("smart_max_count_" + dialog_id, 0);
                int notifyDelay = preferences.getInt("smart_delay_" + dialog_id, 3 * 60);
                if (notifyMaxCount != 0) {
                    Point dialogInfo = smartNotificationsDialogs.get(dialog_id);
                    if (dialogInfo == null) {
                        dialogInfo = new Point(1, (int) (System.currentTimeMillis() / 1000));
                        smartNotificationsDialogs.put(dialog_id, dialogInfo);
                    } else {
                        int lastTime = dialogInfo.y;
                        if (lastTime + notifyDelay < System.currentTimeMillis() / 1000) {
                            dialogInfo.set(1, (int) (System.currentTimeMillis() / 1000));
                        } else {
                            int count = dialogInfo.x;
                            if (count < notifyMaxCount) {
                                dialogInfo.set(count + 1, (int) (System.currentTimeMillis() / 1000));
                            } else {
                                notifyDisabled = true;
                            }
                        }
                    }
                }
            }

            String defaultPath = Settings.System.DEFAULT_NOTIFICATION_URI.getPath();
            if (!notifyDisabled) {
                inAppSounds = preferences.getBoolean("EnableInAppSounds", true);
                inAppVibrate = preferences.getBoolean("EnableInAppVibrate", true);
                vibrateOverride = preferences.getInt("vibrate_" + dialog_id, 0);
                priorityOverride = preferences.getInt("priority_" + dialog_id, 3);
                boolean vibrateOnlyIfSilent = false;

                choosenSoundPath = preferences.getString("sound_path_" + dialog_id, null);
                if (isGroupChat) {
                    if (choosenSoundPath != null && choosenSoundPath.equals(defaultPath)) {
                        choosenSoundPath = null;
                    } else if (choosenSoundPath == null) {
                        choosenSoundPath = preferences.getString("GroupSoundPath", defaultPath);
                    }
                    needVibrate = preferences.getInt("vibrate_group", 0);
                    priority = preferences.getInt("priority_group", 1);
                    ledColor = preferences.getInt("GroupLed", 0xff00ff00);
                } else {
                    if (choosenSoundPath != null && choosenSoundPath.equals(defaultPath)) {
                        choosenSoundPath = null;
                    } else if (choosenSoundPath == null) {
                        choosenSoundPath = preferences.getString("GlobalSoundPath", defaultPath);
                    }
                    needVibrate = preferences.getInt("vibrate_messages", 0);
                    priority = preferences.getInt("priority_messages", 1);
                    ledColor = preferences.getInt("MessagesLed", 0xff00ff00);
                }
                if (preferences.contains("color_" + dialog_id)) {
                    ledColor = preferences.getInt("color_" + dialog_id, 0);
                }

                if (priorityOverride != 3) {
                    priority = priorityOverride;
                }

                if (needVibrate == 4) {
                    vibrateOnlyIfSilent = true;
                    needVibrate = 0;
                }
                if (needVibrate == 2 && (vibrateOverride == 1 || vibrateOverride == 3 || vibrateOverride == 5) || needVibrate != 2 && vibrateOverride == 2 || vibrateOverride != 0) {
                    needVibrate = vibrateOverride;
                }
                if (!ApplicationLoader.mainInterfacePaused) {
                    if (!inAppSounds) {
                        choosenSoundPath = null;
                    }
                    if (!inAppVibrate) {
                        needVibrate = 2;
                    }
                    priority = preferences.getInt("priority_inapp", 0);
                }
                if (vibrateOnlyIfSilent && needVibrate != 2) {
                    try {
                        int mode = audioManager.getRingerMode();
                        if (mode != AudioManager.RINGER_MODE_SILENT && mode != AudioManager.RINGER_MODE_VIBRATE) {
                            needVibrate = 2;
                        }
                    } catch (Exception e) {
                        FileLog.e("messenger", e);
                    }
                }
            }

            Intent intent = new Intent(ApplicationLoader.applicationContext, LaunchActivity.class);
            intent.setAction("com.b44t.messenger.openchat" + (pushDialogs.size() == 1? dialog_id : 0));
            intent.setFlags(32768);
            PendingIntent contentIntent = PendingIntent.getActivity(ApplicationLoader.applicationContext, 0, intent, PendingIntent.FLAG_ONE_SHOT);

            boolean showPreview = preferences.getBoolean("EnablePreviewAll", true);
            if( AndroidUtilities.needShowPasscode(false)
             || UserConfig.isWaitingForPasscodeEnter) {
                showPreview = false;
            }

            String name;
            if ( pushDialogs.size() > 1  || !showPreview ) {
                name = LocaleController.getString("AppName", R.string.AppName);
            } else {
                if ( isGroupChat ) {
                    name = mrChat.getName();
                } else {
                    name = MrMailbox.getContact(user_id).getDisplayName();
                }
            }

            String detailText;
            if (pushDialogs.size() == 1) {
                detailText = LocaleController.formatPluralString("NewMessages", total_unread_count);
            } else {
                detailText = LocaleController.formatString("NotificationMessagesPeopleDisplayOrder", R.string.NotificationMessagesPeopleDisplayOrder,
                        LocaleController.formatPluralString("NewMessages", total_unread_count),
                        LocaleController.formatPluralString("FromChats", pushDialogs.size()));
            }

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ApplicationLoader.applicationContext)
                    .setContentTitle(name)
                    .setSmallIcon(R.drawable.notification)
                    .setAutoCancel(true)
                    .setNumber(total_unread_count)
                    .setContentIntent(contentIntent)
                    .setGroup("messages")
                    .setGroupSummary(true)
                    .setColor(Theme.ACTION_BAR_COLOR);

            mBuilder.setCategory(NotificationCompat.CATEGORY_MESSAGE);

            int silent = 2;
            String lastMessage = null;
            boolean hasNewMessages = false;
            if( !showPreview ) {
                mBuilder.setContentText(detailText);
                lastMessage = detailText;
            }
            else if (pushMessages.size() == 1 ) {
                MessageObject messageObject = pushMessages.get(0);
                String message = lastMessage = getStringForMessage(messageObject, isGroupChat? ADD_USER : 0);
                silent = messageObject.messageOwner.silent ? 1 : 0;
                if (message == null) {
                    return;
                }

                mBuilder.setContentText(message);
                mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
            } else {
                mBuilder.setContentText(detailText);
                NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
                inboxStyle.setBigContentTitle(name);
                int count = Math.min(10, pushMessages.size());

                int string_flags = 0;
                if( pushDialogs.size() > 1 ) {
                    string_flags |= ADD_GROUP;
                }
                for (int i = 1/*user_id is #0*/; i < pushMessages.size(); i++) {
                    MessageObject messageObject = pushMessages.get(i);
                    if( messageObject.messageOwner.from_id != user_id ) {
                        string_flags |= ADD_USER;
                        break;
                    }
                }

                for (int i = 0; i < count; i++) {
                    MessageObject messageObject = pushMessages.get(i);
                    String message = getStringForMessage(messageObject, string_flags);
                    if (message == null || messageObject.messageOwner.date <= dismissDate) {
                        continue;
                    }
                    if (silent == 2) {
                        lastMessage = message;
                        silent = messageObject.messageOwner.silent ? 1 : 0;
                    }
                    /*if (pushDialogs.size() == 1) {
                        if (replace) {
                            if (chat != null) {
                                message = message.replace(" @ " + name, "");
                            } else {
                                message = message.replace(name + ": ", "").replace(name + " ", "");
                            }
                        }
                    }*/
                    inboxStyle.addLine(message);
                }
                inboxStyle.setSummaryText(detailText);
                mBuilder.setStyle(inboxStyle);
            }

            Intent dismissIntent = new Intent(ApplicationLoader.applicationContext, NotificationDismissReceiver.class);
            dismissIntent.putExtra("messageDate", lastMessageObject.messageOwner.date);
            mBuilder.setDeleteIntent(PendingIntent.getBroadcast(ApplicationLoader.applicationContext, 1, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT));

            /*if (photoPath != null) {
                BitmapDrawable img = ImageLoader.getInstance().getImageFromMemory(photoPath, null, "50_50");
                if (img != null) {
                    mBuilder.setLargeIcon(img.getBitmap());
                } else {
                    try {
                        float scaleFactor = 160.0f / AndroidUtilities.dp(50);
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = scaleFactor < 1 ? 1 : (int) scaleFactor;
                        Bitmap bitmap = BitmapFactory.decodeFile(FileLoader.getPathToAttach(photoPath, true).toString(), options);
                        if (bitmap != null) {
                            mBuilder.setLargeIcon(bitmap);
                        }
                    } catch (Throwable e) {
                        //ignore
                    }
                }
            }*/

            if (!notifyAboutLast || silent == 1) {
                mBuilder.setPriority(NotificationCompat.PRIORITY_LOW);
            } else {
                if (priority == 0) {
                    mBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
                } else if (priority == 1) {
                    mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
                } else if (priority == 2) {
                    mBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
                }
            }

            if (silent != 1 && !notifyDisabled) {
                /*if (ApplicationLoader.mainInterfacePaused || inAppPreview)*/ {
                    if (lastMessage.length() > 100) {
                        lastMessage = lastMessage.substring(0, 100).replace('\n', ' ').trim() + "...";
                    }
                    mBuilder.setTicker(lastMessage);
                }
                if (!MediaController.getInstance().isRecordingAudio()) {
                    if (choosenSoundPath != null && !choosenSoundPath.equals("NoSound")) {
                        if (choosenSoundPath.equals(defaultPath)) {
                            mBuilder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, AudioManager.STREAM_NOTIFICATION);
                        } else {
                            mBuilder.setSound(Uri.parse(choosenSoundPath), AudioManager.STREAM_NOTIFICATION);
                        }
                    }
                }
                if (ledColor != 0) {
                    mBuilder.setLights(ledColor, 1000, 1000);
                }
                if (needVibrate == 2 || MediaController.getInstance().isRecordingAudio()) {
                    mBuilder.setVibrate(new long[]{0, 0});
                } else if (needVibrate == 1) {
                    mBuilder.setVibrate(new long[]{0, 100, 0, 100});
                } else if (needVibrate == 0 || needVibrate == 4) {
                    mBuilder.setDefaults(NotificationCompat.DEFAULT_VIBRATE);
                } else if (needVibrate == 3) {
                    mBuilder.setVibrate(new long[]{0, 1000});
                }
            } else {
                mBuilder.setVibrate(new long[]{0, 0});
            }

            //showExtraNotifications(mBuilder, notifyAboutLast);
            notificationManager.notify(1, mBuilder.build());

            scheduleNotificationRepeat();

            if (preferences.getBoolean("badgeNumber", true)) {
                setBadge(total_unread_count);
            }

        } catch (Exception e) {
            FileLog.e("messenger", e);
        }

    }

    @SuppressLint("InlinedApi")
    private void showExtraNotifications(NotificationCompat.Builder notificationBuilder, boolean notifyAboutLast) {
        // TODO: support Android wear by calling this function above from showOrUpdateNotification
        if (Build.VERSION.SDK_INT < 18) {
            return;
        }

        ArrayList<Long> sortedDialogs = new ArrayList<>();
        HashMap<Long, ArrayList<MessageObject>> messagesByDialogs = new HashMap<>();
        for (int a = 0; a < pushMessages.size(); a++) {
            MessageObject messageObject = pushMessages.get(a);
            long dialog_id = messageObject.getDialogId();
            if ((int)dialog_id == 0) {
                continue;
            }

            ArrayList<MessageObject> arrayList = messagesByDialogs.get(dialog_id);
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                messagesByDialogs.put(dialog_id, arrayList);
                sortedDialogs.add(0, dialog_id);
            }
            arrayList.add(messageObject);
        }

        HashMap<Long, Integer> oldIdsWear = new HashMap<>();
        oldIdsWear.putAll(wearNotificationsIds);
        wearNotificationsIds.clear();

        HashMap<Long, Integer> oldIdsAuto = new HashMap<>();
        oldIdsAuto.putAll(autoNotificationsIds);
        autoNotificationsIds.clear();

        for (int b = 0; b < sortedDialogs.size(); b++) {
            long dialog_id = sortedDialogs.get(b);
            ArrayList<MessageObject> messageObjects = messagesByDialogs.get(dialog_id);
            int max_id = messageObjects.get(0).getId();
            int max_date = messageObjects.get(0).messageOwner.date;
            TLRPC.Chat chat = null;
            TLRPC.User user = null;
            String name;
            if (dialog_id > 0) {
                user = MessagesController.getInstance().getUser((int)dialog_id);
                if (user == null) {
                    continue;
                }
            } else {
                chat = MessagesController.getInstance().getChat(-(int)dialog_id);
                if (chat == null) {
                    continue;
                }
            }
            TLRPC.FileLocation photoPath = null;
            if (AndroidUtilities.needShowPasscode(false) || UserConfig.isWaitingForPasscodeEnter) {
                name = LocaleController.getString("AppName", R.string.AppName);
            } else {
                if (chat != null) {
                    name = chat.title;
                } else {
                    name = UserObject.getUserName(user);
                }
                /*if (chat != null) {
                    if (chat.photo != null && chat.photo.photo_small != null && chat.photo.photo_small.volume_id != 0 && chat.photo.photo_small.local_id != 0) {
                        photoPath = chat.photo.photo_small;
                    }
                } else {
                    if (user.photo != null && user.photo.photo_small != null && user.photo.photo_small.volume_id != 0 && user.photo.photo_small.local_id != 0) {
                        photoPath = user.photo.photo_small;
                    }
                }*/
            }

            Integer notificationIdWear = oldIdsWear.get(dialog_id);
            if (notificationIdWear == null) {
                notificationIdWear = wearNotificationId++;
            } else {
                oldIdsWear.remove(dialog_id);
            }

            Integer notificationIdAuto = oldIdsAuto.get(dialog_id);
            if (notificationIdAuto == null) {
                notificationIdAuto = autoNotificationId++;
            } else {
                oldIdsAuto.remove(dialog_id);
            }

            NotificationCompat.CarExtender.UnreadConversation.Builder unreadConvBuilder = new NotificationCompat.CarExtender.UnreadConversation.Builder(name).setLatestTimestamp((long) max_date * 1000);

            Intent msgHeardIntent = new Intent();
            msgHeardIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            msgHeardIntent.setAction("com.b44t.messenger.ACTION_MESSAGE_HEARD");
            msgHeardIntent.putExtra("dialog_id", dialog_id);
            msgHeardIntent.putExtra("max_id", max_id);
            PendingIntent msgHeardPendingIntent = PendingIntent.getBroadcast(ApplicationLoader.applicationContext, notificationIdAuto, msgHeardIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            unreadConvBuilder.setReadPendingIntent(msgHeardPendingIntent);

            NotificationCompat.Action wearReplyAction = null;

            if (/*!ChatObject.isChannel(chat) &&*/ !AndroidUtilities.needShowPasscode(false) && !UserConfig.isWaitingForPasscodeEnter) {
                Intent msgReplyIntent = new Intent();
                msgReplyIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                msgReplyIntent.setAction("com.b44t.messenger.ACTION_MESSAGE_REPLY");
                msgReplyIntent.putExtra("dialog_id", dialog_id);
                msgReplyIntent.putExtra("max_id", max_id);
                PendingIntent msgReplyPendingIntent = PendingIntent.getBroadcast(ApplicationLoader.applicationContext, notificationIdAuto, msgReplyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                RemoteInput remoteInputAuto = new RemoteInput.Builder(NotificationsController.EXTRA_VOICE_REPLY).setLabel(LocaleController.getString("Reply", R.string.Reply)).build();
                unreadConvBuilder.setReplyAction(msgReplyPendingIntent, remoteInputAuto);

                Intent replyIntent = new Intent(ApplicationLoader.applicationContext, WearReplyReceiver.class);
                replyIntent.putExtra("dialog_id", dialog_id);
                replyIntent.putExtra("max_id", max_id);
                PendingIntent replyPendingIntent = PendingIntent.getBroadcast(ApplicationLoader.applicationContext, notificationIdWear, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                RemoteInput remoteInputWear = new RemoteInput.Builder(EXTRA_VOICE_REPLY).setLabel(LocaleController.getString("Reply", R.string.Reply)).build();
                String replyToString;
                if (chat != null) {
                    replyToString = LocaleController.formatString("ReplyToGroup", R.string.ReplyToGroup, name);
                } else {
                    replyToString = LocaleController.formatString("ReplyToUser", R.string.ReplyToUser, name);
                }
                wearReplyAction = new NotificationCompat.Action.Builder(R.drawable.ic_reply_icon, replyToString, replyPendingIntent).addRemoteInput(remoteInputWear).build();
            }

            String text = "";
            for (int a = messageObjects.size() - 1; a >= 0; a--) {
                MessageObject messageObject = messageObjects.get(a);
                String message = getStringForMessage(messageObject, ADD_GROUP|ADD_USER);
                if (message == null) {
                    continue;
                }
                /*if (chat != null) {
                    message = message.replace(" @ " + name, "");
                } else {
                    message = message.replace(name + ": ", "").replace(name + " ", "");
                }*/
                if (text.length() > 0) {
                    text += "\n\n";
                }
                text += message;

                unreadConvBuilder.addMessage(message);
            }

            Intent intent = new Intent(ApplicationLoader.applicationContext, LaunchActivity.class);
            intent.setAction("com.b44t.messenger.openchat" + Math.random() + Integer.MAX_VALUE);
            intent.setFlags(32768);
            if (chat != null) {
                intent.putExtra("chatId", chat.id);
            } else if (user != null) {
                intent.putExtra("userId", user.id);
            }
            PendingIntent contentIntent = PendingIntent.getActivity(ApplicationLoader.applicationContext, 0, intent, PendingIntent.FLAG_ONE_SHOT);

            NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();
            if (wearReplyAction != null) {
                wearableExtender.addAction(wearReplyAction);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(ApplicationLoader.applicationContext)
                    .setContentTitle(name)
                    .setSmallIcon(R.drawable.notification)
                    .setGroup("messages")
                    .setContentText(text)
                    .setAutoCancel(true)
                    .setColor(Theme.ACTION_BAR_COLOR)
                    .setGroupSummary(false)
                    .setContentIntent(contentIntent)
                    .extend(wearableExtender)
                    .extend(new NotificationCompat.CarExtender().setUnreadConversation(unreadConvBuilder.build()))
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE);
            /*if (photoPath != null) {
                BitmapDrawable img = ImageLoader.getInstance().getImageFromMemory(photoPath, null, "50_50");
                if (img != null) {
                    builder.setLargeIcon(img.getBitmap());
                }
            }*/

            notificationManager.notify(notificationIdWear, builder.build());
            wearNotificationsIds.put(dialog_id, notificationIdWear);
        }

        for (HashMap.Entry<Long, Integer> entry : oldIdsWear.entrySet()) {
            notificationManager.cancel(entry.getValue());
        }
    }

    public void playOutChatSound() {
        if (!inChatSoundEnabled || MediaController.getInstance().isRecordingAudio()) {
            return;
        }
        try {
            if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
                return;
            }
        } catch (Exception e) {
            FileLog.e("messenger", e);
        }
        notificationsQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    if (Math.abs(System.currentTimeMillis() - lastSoundOutPlay) <= 100) {
                        return;
                    }
                    lastSoundOutPlay = System.currentTimeMillis();
                    if (soundPool == null) {
                        soundPool = new SoundPool(3, AudioManager.STREAM_SYSTEM, 0);
                        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                            @Override
                            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                                if (status == 0) {
                                    soundPool.play(sampleId, 1.0f, 1.0f, 1, 0, 1.0f);
                                }
                            }
                        });
                    }
                    if (soundOut == 0 && !soundOutLoaded) {
                        soundOutLoaded = true;
                        soundOut = soundPool.load(ApplicationLoader.applicationContext, R.raw.sound_out, 1);
                    }
                    if (soundOut != 0) {
                        soundPool.play(soundOut, 1.0f, 1.0f, 1, 0, 1.0f);
                    }
                } catch (Exception e) {
                    FileLog.e("messenger", e);
                }
            }
        });
    }

    public static void updateServerNotificationsSettings(long dialog_id) {
        // the following command is needed to reflect the changes in the GUI
        NotificationCenter.getInstance().postNotificationName(NotificationCenter.notificationsSettingsUpdated);

        /*
        if ((int) dialog_id == 0) {
            return;
        }
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
        TLRPC.TL_account_updateNotifySettings req = new TLRPC.TL_account_updateNotifySettings();
        req.settings = new TLRPC.TL_inputPeerNotifySettings();
        req.settings.sound = "default";
        int mute_type = preferences.getInt("notify2_" + dialog_id, 0);
        if (mute_type == 3) {
            req.settings.mute_until = preferences.getInt("notifyuntil_" + dialog_id, 0);
        } else {
            req.settings.mute_until = mute_type != 2 ? 0 : Integer.MAX_VALUE;
        }
        req.settings.show_previews = preferences.getBoolean("preview_" + dialog_id, true);
        req.settings.silent = preferences.getBoolean("silent_" + dialog_id, false);
        req.peer = new TLRPC.TL_inputNotifyPeer();
        ((TLRPC.TL_inputNotifyPeer) req.peer).peer = MessagesController.getInputPeer((int) dialog_id);
        ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {

            }
        });
        */
    }
}
