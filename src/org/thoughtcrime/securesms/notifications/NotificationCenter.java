package org.thoughtcrime.securesms.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.ConversationActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.List;

public class NotificationCenter {
    private static final String TAG = InChatSounds.class.getSimpleName();
    @NonNull private ApplicationDcContext dcContext;
    @NonNull private Context context;
    private volatile int visibleChatId = 0;

    public NotificationCenter(ApplicationDcContext dcContext) {
        this.dcContext = dcContext;
        this.context = dcContext.context.getApplicationContext();
    }

    private Uri effectiveSound(int chatId) { // chatId=0: return app-global setting
        Uri chatRingtone = Prefs.getChatRingtone(context, chatId);
        if (chatRingtone!=null) {
            return chatRingtone;
        } else {
            Uri appDefaultRingtone = Prefs.getNotificationRingtone(context);
            if (!TextUtils.isEmpty(appDefaultRingtone.toString())) {
                return appDefaultRingtone;
            }
        }
        return null;
    }

    private boolean effectiveVibrate(int chatId) { // chatId=0: return app-global setting
        Prefs.VibrateState vibrate = Prefs.getChatVibrate(context, chatId);
        if (vibrate == Prefs.VibrateState.ENABLED) {
            return true;
        } else if (vibrate == Prefs.VibrateState.DISABLED) {
            return false;
        }
        return Prefs.isNotificationVibrateEnabled(context);
    }

    private boolean requiresIndependentChannel(int chatId) {
        if (Prefs.getChatRingtone(context, chatId)!=null || Prefs.getChatVibrate(context, chatId)!=Prefs.VibrateState.DEFAULT) {
            return true;
        }
        return false;
    }

    private int getLedArgb(String ledColor) {
        int argb;
        try {
            argb = Color.parseColor(ledColor);
        }
        catch (Exception e) {
            argb = Color.rgb(0xFF, 0xFF, 0xFF);
        }
        return argb;
    }
    
    private PendingIntent getPendingIntent(int chatId) {
        Intent intent = new Intent(context, ConversationActivity.class);
        intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, chatId);
        intent.setData((Uri.parse("custom://"+System.currentTimeMillis())));
        return TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(intent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    // Groups and Notifcation channel groups
    // --------------------------------------------------------------------------------------------

    public static final String GRP_MSG = "chgrp_msg";


    // Notification IDs
    // --------------------------------------------------------------------------------------------

    public static final int ID_PERMANTENT = 1;
    public static final int ID_MSG_OFFSET = 0; // msgId is added - as msgId start at 10, there are no conflicts with lower numbers


    // Notification channels
    // --------------------------------------------------------------------------------------------

    // Overview:
    // - since SDK 26 (Oreo), a NotificationChannel is a MUST for notifications
    // - NotificationChannels are defined by a channelId
    //   and its user-editable settings have a higher precedence as the Notification.Builder setting
    // - once created, NotificationChannels cannot be modified programmatically
    // - NotificationChannels can be deleted, however, on re-creation with the same id,
    //   it becomes un-deleted with the old user-defined settings
    //
    // How we use Notification channel:
    // - We include the delta-chat-notifications settings into the name of the channelId
    // - The chatId is included only, if there are separate sound- or vibration-settings for a chat
    // - This way, we have stable and few channelIds and the user
    //   can edit the notifications in Delta Chat as well as in the system

    // channelIds: CH_MSG_* are used here, the other ones from outside (defined here to have some overview)
    public static final String CH_MSG_PREFIX = "ch_msg";
    public static final String CH_MSG_VERSION = "4";
    public static final String CH_PERMANENT = "dc_foreground_notification_ch";

    private boolean notificationChannelsSupported() {
        return Build.VERSION.SDK_INT >= 26;
    }

    // full name is "ch_msgV_HASH" or "ch_msgV_HASH.CHATID"
    private String computeChannelId(String ledColor, boolean vibrate, Uri ringtone, int chatId) {
        String channelId = CH_MSG_PREFIX;
        try {
            String hash = "";
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(ledColor.getBytes());
            md.update(vibrate ? (byte) 1 : (byte) 0);
            md.update(ringtone.toString().getBytes());
            if (chatId!=0) {
                // for multi-account, force different channelIds for maybe the same chatIds in multiple accounts
                md.update(dcContext.getDbName().getBytes());
            }
            hash = String.format("%X", new BigInteger(1, md.digest())).substring(0, 16);

            channelId = CH_MSG_PREFIX + CH_MSG_VERSION + "_" + hash;
            if (chatId!=0) {
                channelId += String.format(".%d", chatId);
            }

        } catch(Exception e) { }
        return channelId;
    }

    // return chatId from "ch_msgV_HASH.CHATID" or 0
    private int parseNotificationChannelChatId(String channelId) {
        try {
            int point = channelId.lastIndexOf(".");
            if (point>0) {
                return Integer.parseInt(channelId.substring(point + 1));
            }
        } catch(Exception e) { }
        return 0;
    }

    private String getNotificationChannelGroup(NotificationManagerCompat notificationManager) {
        if (notificationChannelsSupported() && notificationManager.getNotificationChannelGroup(GRP_MSG) == null) {
            NotificationChannelGroup chGrp = new NotificationChannelGroup(GRP_MSG, context.getString(R.string.pref_chats));
            notificationManager.createNotificationChannelGroup(chGrp);
        }
        return GRP_MSG;
    }

    private String getNotificationChannel(NotificationManagerCompat notificationManager, DcChat dcChat) {
        int chatId = dcChat.getId();
        String channelId = CH_MSG_PREFIX;

        if(notificationChannelsSupported()) {
            try {
                // get all values we'll use as settings for the NotificationChannel
                String  ledColor       = Prefs.getNotificationLedColor(context);
                boolean defaultVibrate = effectiveVibrate(chatId);
                Uri     ringtone       = effectiveSound(chatId);
                boolean isIndependent  = requiresIndependentChannel(chatId);

                // get channel id from these settings
                channelId = computeChannelId(ledColor, defaultVibrate, ringtone, isIndependent? chatId : 0);

                // user-visible name of the channel -
                // we just use the name of the chat or "Default"
                // (the name is shown in the context of the group "Chats" - that should be enough context)
                String name = context.getString(R.string.def);
                if (isIndependent) {
                    name = dcChat.getName();
                }

                // check if there is already a channel with the given name
                List<NotificationChannel> channels = notificationManager.getNotificationChannels();
                boolean channelExists = false;
                for (int i = 0; i < channels.size(); i++) {
                    String currChannelId = channels.get(i).getId();
                    if (currChannelId.startsWith(CH_MSG_PREFIX)) {
                        // this is one of the message channels handled here ...
                        if (currChannelId.equals(channelId)) {
                            // ... this is the actually required channel, fine :)
                            // update the name to reflect localize changes and chat renames
                            channelExists = true;
                            channels.get(i).setName(name);
                        } else {
                            // ... another message channel, delete if it is not in use.
                            int currChatId = parseNotificationChannelChatId(currChannelId);
                            if (!currChannelId.equals(computeChannelId(ledColor, effectiveVibrate(currChatId), effectiveSound(currChatId), currChatId))) {
                                notificationManager.deleteNotificationChannel(currChannelId);
                            }
                        }
                    }
                }

                // create a channel with the given settings;
                // we cannot change the settings, however, this is handled by using different values for chId
                if(!channelExists) {
                    NotificationChannel channel = new NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_HIGH);
                    channel.setDescription("Informs about new messages.");
                    channel.setGroup(getNotificationChannelGroup(notificationManager));
                    channel.enableVibration(defaultVibrate);

                    if (!ledColor.equals("none")) {
                        channel.enableLights(true);
                        channel.setLightColor(getLedArgb(ledColor));
                    } else {
                        channel.enableLights(false);
                    }

                    if (!TextUtils.isEmpty(ringtone.toString())) {
                        channel.setSound(ringtone,
                                new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                                        .build());
                    }

                    notificationManager.createNotificationChannel(channel);
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }

        return channelId;
    }


    // add notifications & co.
    // --------------------------------------------------------------------------------------------

    public void addNotification(int chatId, int msgId) {
        Util.runOnAnyBackgroundThread(() -> {

            DcChat dcChat = dcContext.getChat(chatId);

            if (Prefs.isChatMuted(dcContext.context, chatId)) {
                return;
            }

            if (dcChat.isDeviceTalk()) {
                // currently, we just never notify on device chat.
                // esp. on first start, this is annoying.
                return;
            }

            if (chatId == visibleChatId) {
                InChatSounds.getInstance(dcContext.context).playIncomingSound();
                return;
            }

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            // get notification text
            DcMsg dcMsg = dcContext.getMsg(msgId);
            String text = dcMsg.getSummarytext(100);
            if (dcChat.isGroup()) {
                text = dcContext.getContact(dcMsg.getFromId()).getFirstName() + ": " + text;
            }

            // create a basic notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, getNotificationChannel(notificationManager, dcChat))
                    .setSmallIcon(R.drawable.icon_notification)
                    .setColor(context.getResources().getColor(R.color.delta_primary))
                    .setPriority(Prefs.getNotificationPriority(context))
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setGroup(GRP_MSG)
                    .setContentTitle(dcChat.getName())
                    .setContentText(text)
                    .setContentIntent(getPendingIntent(chatId));

            // set sound, vibrate, led for systems that do not have notification channels
            if (!notificationChannelsSupported()) {
                Uri sound = effectiveSound(chatId);
                if (sound != null) {
                    builder.setSound(sound);
                }
                boolean vibrate = effectiveVibrate(chatId);
                if (vibrate) {
                    builder.setDefaults(Notification.DEFAULT_VIBRATE);
                }
                String ledColor = Prefs.getNotificationLedColor(context);
                if (!ledColor.equals("none")) {
                    builder.setLights(getLedArgb(ledColor),500, 2000);
                }
            }

            // add notification, we use one notification per chat,
            // esp. older android are not that great at grouping
            notificationManager.notify(ID_MSG_OFFSET+chatId, builder.build());
        });
    }

    public void removeNotifications(int chatId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(ID_MSG_OFFSET + chatId);
    }

    public void removeAllNotifiations() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancelAll();
    }

    public void updateVisibleChat(int chatId) {
        Util.runOnAnyBackgroundThread(() -> {

            visibleChatId = chatId;
            if (chatId!=0) {
                removeNotifications(chatId);
            }

        });
    }
}
