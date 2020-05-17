package org.thoughtcrime.securesms.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcMsg;

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

    private Uri getEffectiveSound(int chatId) {
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

    private boolean getEffectiveVibrate(int chatId) {
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



    // handle notification channels
    // --------------------------------------------------------------------------------------------

    // - since oreo, a NotificationChannel is a MUST
    // - NotificationChannels have default values that have a higher precedence as the Notification.Builder setting
    // - once created, NotificationChannels cannot be modified programmatically
    // - NotificationChannels can be deleted, however, on re-creation it becomes un-deleted with the old settings
    // - the idea is that sound and vibrate are handled outside of the scope of the notification channel

    // our Channel-IDs have the format ""ch_msgV_HASH" or "ch_msgV_HASH.CHATID"
    // note, that there are other Channel-IDs as "dc_foreground_notification_ch"
    static private final String CH_PREFIX = "ch_msg";
    static private final String CH_VERSION = "4";

    private boolean notificationChannelsSupported() {
        return Build.VERSION.SDK_INT >= 26;
    }

    private boolean isNotificationChannelInUse(String chId) {
        try {
            if (chId.startsWith(CH_PREFIX + CH_VERSION)) {
                int point = chId.lastIndexOf(".");
                if (point == -1) {
                    return true; // this is the current standard channel for all chats that do not have explicit sound/vibrate set
                } else {
                    int chatId = Integer.parseInt(chId.substring(point + 1));
                    if (requiresIndependentChannel(chatId)) {
                        return true; // this is a channel for a chat with explicit sound/vibrate set
                    }

                }
            }
        } catch(Exception e) { }
        return false;
    }

    private String getNotificationChannel(NotificationManagerCompat notificationManager, DcChat dcChat) {
        int chatId = dcChat.getId();
        String channelId = CH_PREFIX + CH_VERSION + "_" + "unsupported";

        if(notificationChannelsSupported()) {
            try {
                // get all values we'll use as settings for the NotificationChannel
                String ledColor = Prefs.getNotificationLedColor(context);
                boolean defaultVibrate = getEffectiveVibrate(chatId);
                Uri ringtone = getEffectiveSound(chatId);
                boolean isIndependent = requiresIndependentChannel(chatId);

                // compute hash from these settings
                String hash = "";
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(ledColor.getBytes());
                md.update(defaultVibrate ? (byte) 1 : (byte) 0);
                md.update(ringtone.toString().getBytes());
                hash = String.format("%X", new BigInteger(1, md.digest())).substring(0, 16);

                // get channel id
                channelId = CH_PREFIX + CH_VERSION + "_" + hash;
                if (isIndependent) {
                    channelId += String.format(".%d", chatId);
                }

                // user-visible name of the channel
                String name = "New messages";
                if (isIndependent) {
                    name = dcChat.getName();
                }

                // check if there is already a channel with the given name,
                // delete unused `ch_msg` channel names (keep others as `dc_foreground_notification_ch`)
                List<NotificationChannel> channels = notificationManager.getNotificationChannels();
                boolean channelExists = false;
                for (int i = 0; i < channels.size(); i++) {
                    String testChannelId = channels.get(i).getId();
                    if (channelId.equals(testChannelId)) {
                        channelExists = true;
                        try {
                            channels.get(i).setName(name);
                        } catch(Exception e) { }
                    } else if (testChannelId.startsWith(CH_PREFIX) && !isNotificationChannelInUse(testChannelId)) {
                        try {
                            notificationManager.deleteNotificationChannel(testChannelId);
                        }
                        catch (Exception e) { }
                    }
                }

                // create a channel with the given settings;
                // we cannot change the settings, however, this is handled by using different values for chId
                if(!channelExists) {
                    NotificationChannel channel = new NotificationChannel(channelId,
                            name, NotificationManager.IMPORTANCE_HIGH);
                    channel.setDescription("Informs about new messages.");

                    if (!ledColor.equals("none")) {
                        channel.enableLights(true);
                        channel.setLightColor(getLedArgb(ledColor));
                    } else {
                        channel.enableLights(false);
                    }

                    channel.enableVibration(defaultVibrate);

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

            if (Prefs.isChatMuted(dcContext.context, chatId)) {
                return;
            }

            if (chatId == visibleChatId) {
                InChatSounds.getInstance(dcContext.context).playIncomingSound();
                return;
            }

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            // create a basic notification
            DcChat dcChat = dcContext.getChat(chatId);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, getNotificationChannel(notificationManager, dcChat))
                    .setSmallIcon(R.drawable.icon_notification)
                    .setColor(context.getResources().getColor(R.color.delta_primary))
                    .setPriority(Prefs.getNotificationPriority(context))
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setContentTitle(dcChat.getName())
                    .setContentText(dcContext.getMsg(msgId).getSummarytext(100));

            // set sound, vibrate, led for systems that do not have notification channels
            if (!notificationChannelsSupported()) {
                Uri sound = getEffectiveSound(chatId);
                if (sound != null) {
                    builder.setSound(sound);
                }
                boolean vibrate = getEffectiveVibrate(chatId);
                if (vibrate) {
                    builder.setDefaults(Notification.DEFAULT_VIBRATE);
                }
                String ledColor = Prefs.getNotificationLedColor(context);
                if (!ledColor.equals("none")) {
                    builder.setLights(getLedArgb(ledColor),500, 2000);
                }
            }

            // add notification
            notificationManager.notify(msgId, builder.build());
        });
    }

    public void removeNotifications(int chatId) {
        Util.runOnAnyBackgroundThread(() -> {

        });
    }

    public void updateVisibleChat(int chatId) {
        Util.runOnAnyBackgroundThread(() -> {

            visibleChatId = chatId;
            removeNotifications(chatId);

        });
    }
}
