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
import android.util.Log;

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

public class MsgNotificationManager {
    private static final String TAG = InChatSounds.class.getSimpleName();
    @NonNull private ApplicationDcContext dcContext;
    @NonNull private Context context;
    private volatile int visibleChatId = 0;

    public MsgNotificationManager(ApplicationDcContext dcContext) {
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

    private boolean notificationChannelsSupported() {
        return Build.VERSION.SDK_INT >= 26;
    }

    private String getNotificationChannel(NotificationManagerCompat notificationManager) {
        final String chBase = "ch_msg4_";
        String chId = chBase + "unsupported";

        if(notificationChannelsSupported()) {
            try {
                // get all values we'll use as settings for the NotificationChannel
                String ledColor = Prefs.getNotificationLedColor(context);
                boolean defaultVibrate = Prefs.isNotificationVibrateEnabled(context);
                Uri ringtone = Prefs.getNotificationRingtone(context);

                // compute hash from these settings
                String hash = "";
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(ledColor.getBytes());
                md.update(defaultVibrate ? (byte) 1 : (byte) 0);
                md.update(ringtone.toString().getBytes());
                hash = String.format("%X", new BigInteger(1, md.digest())).substring(0, 16);

                // get channel name
                chId = chBase + hash;

                // delete previously used channel, if changed
                String oldChId = Prefs.getStringPreference(context, "ch_curr_" + chBase, "");
                if (!oldChId.equals(chId)) {
                    try {
                        notificationManager.deleteNotificationChannel(oldChId);
                    }
                    catch (Exception e) {
                        // channel not created before
                    }
                    Prefs.setStringPreference(context, "ch_curr_" + chBase, chId);
                }

                // check if there is already a channel with the given name
                List<NotificationChannel> channels = notificationManager.getNotificationChannels();
                boolean channelExists = false;
                for (int i = 0; i < channels.size(); i++) {
                    if (chId.equals(channels.get(i).getId())) {
                        channelExists = true;
                        break;
                    }
                }

                // create a channel with the given settings;
                // we cannot change the settings, however, this is handled by using different values for chId
                if(!channelExists) {
                    NotificationChannel channel = new NotificationChannel(chId,
                            "New messages", NotificationManager.IMPORTANCE_HIGH);
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

        return chId;
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
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, getNotificationChannel(notificationManager))
                    .setSmallIcon(R.drawable.icon_notification)
                    .setColor(context.getResources().getColor(R.color.delta_primary))
                    .setPriority(Prefs.getNotificationPriority(context))
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setContentTitle(dcContext.getChat(chatId).getName())
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
