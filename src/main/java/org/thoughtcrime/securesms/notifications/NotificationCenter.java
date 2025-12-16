package org.thoughtcrime.securesms.notifications;

import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_PRIVATE_TAG;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import androidx.core.app.TaskStackBuilder;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.json.JSONObject;
import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.ConversationActivity;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.contacts.avatars.ContactPhoto;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.preferences.widgets.NotificationPrivacyPreference;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.BitmapUtil;
import org.thoughtcrime.securesms.util.IntentUtils;
import org.thoughtcrime.securesms.util.JsonUtils;
import org.thoughtcrime.securesms.util.Pair;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.calls.CallActivity;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NotificationCenter {
    private static final String TAG = NotificationCenter.class.getSimpleName();
    @NonNull private final ApplicationContext context;
    private volatile ChatData visibleChat = null;
    private volatile Pair<Integer, Integer> visibleWebxdc = null;
    private volatile long lastAudibleNotification = 0;
    private static final long MIN_AUDIBLE_PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(2);

    // Map<accountId, Map<chatId, lines>, contains the last lines of each chat for each account
    private final HashMap<Integer, HashMap<Integer, ArrayList<String>>> inboxes = new HashMap<>();

    public NotificationCenter(Context context) {
        this.context = ApplicationContext.getInstance(context);
    }

    private @Nullable Uri effectiveSound(ChatData chatData) { // chatData=null: return app-global setting
        if (chatData == null) {
            chatData = new ChatData(0, 0);
        }
        @Nullable Uri chatRingtone = Prefs.getChatRingtone(context, chatData.accountId, chatData.chatId);
        if (chatRingtone!=null) {
            return chatRingtone;
        } else {
            @NonNull Uri appDefaultRingtone = Prefs.getNotificationRingtone(context);
            if (!TextUtils.isEmpty(appDefaultRingtone.toString())) {
                return appDefaultRingtone;
            }
        }
        return null;
    }

    private boolean effectiveVibrate(ChatData chatData) { // chatData=null: return app-global setting
        if (chatData == null) {
            chatData = new ChatData(0, 0);
        }
        Prefs.VibrateState vibrate = Prefs.getChatVibrate(context, chatData.accountId, chatData.chatId);
        if (vibrate == Prefs.VibrateState.ENABLED) {
            return true;
        } else if (vibrate == Prefs.VibrateState.DISABLED) {
            return false;
        }
        return Prefs.isNotificationVibrateEnabled(context);
    }

    private boolean requiresIndependentChannel(ChatData chatData) {
        if (chatData == null) {
            chatData = new ChatData(0, 0);
        }
        return Prefs.getChatRingtone(context, chatData.accountId, chatData.chatId) != null
                || Prefs.getChatVibrate(context, chatData.accountId, chatData.chatId) != Prefs.VibrateState.DEFAULT;
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

    private PendingIntent getOpenChatlistIntent(int accountId) {
        Intent intent = new Intent(context, ConversationListActivity.class);
        intent.putExtra(ConversationListActivity.ACCOUNT_ID_EXTRA, accountId);
        intent.putExtra(ConversationListActivity.CLEAR_NOTIFICATIONS, true);
        intent.setData(Uri.parse("custom://"+accountId));
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | IntentUtils.FLAG_MUTABLE());
    }

    private PendingIntent getOpenChatIntent(ChatData chatData) {
        Intent intent = new Intent(context, ConversationActivity.class);
        intent.putExtra(ConversationActivity.ACCOUNT_ID_EXTRA, chatData.accountId);
        intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, chatData.chatId);
        intent.setData(Uri.parse("custom://"+chatData.accountId+"."+chatData.chatId));
        return TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(intent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | IntentUtils.FLAG_MUTABLE());
    }

    private PendingIntent getRemoteReplyIntent(ChatData chatData, int msgId) {
        Intent intent = new Intent(RemoteReplyReceiver.REPLY_ACTION);
        intent.setClass(context, RemoteReplyReceiver.class);
        intent.setData(Uri.parse("custom://"+chatData.accountId+"."+chatData.chatId));
        intent.putExtra(RemoteReplyReceiver.ACCOUNT_ID_EXTRA, chatData.accountId);
        intent.putExtra(RemoteReplyReceiver.CHAT_ID_EXTRA, chatData.chatId);
        intent.putExtra(RemoteReplyReceiver.MSG_ID_EXTRA, msgId);
        intent.setPackage(context.getPackageName());
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | IntentUtils.FLAG_MUTABLE());
    }

    private PendingIntent getMarkAsReadIntent(ChatData chatData, int msgId, boolean markNoticed) {
        Intent intent = new Intent(markNoticed? MarkReadReceiver.MARK_NOTICED_ACTION : MarkReadReceiver.CANCEL_ACTION);
        intent.setClass(context, MarkReadReceiver.class);
        intent.setData(Uri.parse("custom://"+chatData.accountId+"."+chatData.chatId));
        intent.putExtra(MarkReadReceiver.ACCOUNT_ID_EXTRA, chatData.accountId);
        intent.putExtra(MarkReadReceiver.CHAT_ID_EXTRA, chatData.chatId);
        intent.putExtra(MarkReadReceiver.MSG_ID_EXTRA, msgId);
        intent.setPackage(context.getPackageName());
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | IntentUtils.FLAG_MUTABLE());
    }

    public PendingIntent getOpenCallIntent(ChatData chatData, int callId, String payload, boolean autoAccept) {
        final Intent chatIntent = new Intent(context, ConversationActivity.class)
            .putExtra(ConversationActivity.ACCOUNT_ID_EXTRA, chatData.accountId)
            .putExtra(ConversationActivity.CHAT_ID_EXTRA, chatData.chatId)
            .setAction(Intent.ACTION_VIEW);

        String base64 = Base64.encodeToString(payload.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        String hash = "";
        try {
          hash = (autoAccept? "#acceptCall=" : "#offerIncomingCall=") + URLEncoder.encode(base64, "UTF-8");
        } catch (UnsupportedEncodingException e) {
          Log.e(TAG, "Error", e);
        }

        Intent intent = new Intent(context, CallActivity.class);
        intent.setAction(autoAccept? Intent.ACTION_ANSWER : Intent.ACTION_VIEW);
        intent.putExtra(CallActivity.EXTRA_ACCOUNT_ID, chatData.accountId);
        intent.putExtra(CallActivity.EXTRA_CHAT_ID, chatData.chatId);
        intent.putExtra(CallActivity.EXTRA_CALL_ID, callId);
        intent.putExtra(CallActivity.EXTRA_HASH, hash);
        intent.setPackage(context.getPackageName());
        return TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(chatIntent)
            .addNextIntent(intent)
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | IntentUtils.FLAG_MUTABLE());
    }

    public PendingIntent getDeclineCallIntent(ChatData chatData, int callId) {
        Intent intent = new Intent(DeclineCallReceiver.DECLINE_ACTION);
        intent.setClass(context, DeclineCallReceiver.class);
        intent.putExtra(DeclineCallReceiver.ACCOUNT_ID_EXTRA, chatData.accountId);
        intent.putExtra(DeclineCallReceiver.CALL_ID_EXTRA, callId);
        intent.setPackage(context.getPackageName());
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | IntentUtils.FLAG_MUTABLE());
    }

    // Groups and Notification channel groups
    // --------------------------------------------------------------------------------------------

    // this is just to further organize the appearance of channels in the settings UI
    private static final String CH_GRP_MSG = "chgrp_msg";

    // this is to group together notifications as such, maybe including a summary,
    // see https://developer.android.com/training/notify-user/group.html
    private static final String GRP_MSG = "grp_msg";


    // Notification IDs
    // --------------------------------------------------------------------------------------------

    public static final int ID_PERMANENT   = 1;
    public static final int ID_MSG_SUMMARY = 2;
    public static final int ID_GENERIC     = 3;
    public static final int ID_FETCH       = 4;
    public static final int ID_MSG_OFFSET  = 0; // msgId is added - as msgId start at 10, there are no conflicts with lower numbers


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
    public static final String CH_MSG_VERSION = "5";
    public static final String CH_PERMANENT = "dc_fg_notification_ch";
    public static final String CH_GENERIC = "ch_generic";
    public static final String CH_CALLS_PREFIX = "call_chan";

    private boolean notificationChannelsSupported() {
        return Build.VERSION.SDK_INT >= 26;
    }

    // full name is "ch_msgV_HASH" or "ch_msgV_HASH.ACCOUNTID.CHATID"
    private String computeChannelId(String ledColor, boolean vibrate, @Nullable Uri ringtone, ChatData chatData) {
        String channelId = CH_MSG_PREFIX;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(ledColor.getBytes());
            md.update(vibrate ? (byte) 1 : (byte) 0);
            md.update((ringtone != null ? ringtone.toString() : "").getBytes());
            String hash = String.format("%X", new BigInteger(1, md.digest())).substring(0, 16);

            channelId = CH_MSG_PREFIX + CH_MSG_VERSION + "_" + hash;
            if (chatData != null) {
                channelId += String.format(".%d.%d", chatData.accountId, chatData.chatId);
            }

        } catch(Exception e) {
            Log.e(TAG, e.toString());
        }
        return channelId;
    }

    // return ChatData(ACCOUNTID, CHATID) from "ch_msgV_HASH.ACCOUNTID.CHATID" or null
    private ChatData parseNotificationChannelChat(String channelId) {
        try {
            int point = channelId.lastIndexOf(".");
            if (point>0) {
                int chatId = Integer.parseInt(channelId.substring(point + 1));
                channelId = channelId.substring(0, point);
                point = channelId.lastIndexOf(".");
                if (point>0) {
                    int accountId = Integer.parseInt(channelId.substring(point + 1));
                    return new ChatData(accountId, chatId);
                }
            }
        } catch(Exception ignored) { }
        return null;
    }

    private String getNotificationChannelGroup(NotificationManagerCompat notificationManager) {
        if (notificationChannelsSupported() && notificationManager.getNotificationChannelGroup(CH_GRP_MSG) == null) {
            NotificationChannelGroup chGrp = new NotificationChannelGroup(CH_GRP_MSG, context.getString(R.string.pref_chats));
            notificationManager.createNotificationChannelGroup(chGrp);
        }
        return CH_GRP_MSG;
    }

    private String getNotificationChannel(NotificationManagerCompat notificationManager, ChatData chatData, DcChat dcChat) {
        String channelId = CH_MSG_PREFIX;

        if (notificationChannelsSupported()) {
            try {
                // get all values we'll use as settings for the NotificationChannel
                String        ledColor       = Prefs.getNotificationLedColor(context);
                boolean       defaultVibrate = effectiveVibrate(chatData);
                @Nullable Uri ringtone       = effectiveSound(chatData);
                boolean       isIndependent  = requiresIndependentChannel(chatData);

                // get channel id from these settings
                channelId = computeChannelId(ledColor, defaultVibrate, ringtone, isIndependent? chatData : null);

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
                            ChatData currChat = parseNotificationChannelChat(currChannelId);
                            if (!currChannelId.equals(computeChannelId(ledColor, effectiveVibrate(currChat), effectiveSound(currChat), currChat))) {
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
                    channel.setShowBadge(true);

                    if (!ledColor.equals("none")) {
                        channel.enableLights(true);
                        channel.setLightColor(getLedArgb(ledColor));
                    } else {
                        channel.enableLights(false);
                    }

                    if (ringtone != null && !TextUtils.isEmpty(ringtone.toString())) {
                        channel.setSound(ringtone,
                                new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                                        .build());
                    } else {
                        channel.setSound(null, null);
                    }

                    notificationManager.createNotificationChannel(channel);
                }
            }
            catch(Exception e) {
                Log.e(TAG, "Error in getNotificationChannel()", e);
            }
        }

        return channelId;
    }

    public String getCallNotificationChannel(NotificationManagerCompat notificationManager, ChatData chatData, String name) {
        String channelId = CH_CALLS_PREFIX + "-" + chatData.accountId + "-"+ chatData.chatId;

        if (notificationChannelsSupported()) {
            try {
                name = "(calls) " + name;

                // check if there is already a channel with the given name
                List<NotificationChannel> channels = notificationManager.getNotificationChannels();
                boolean channelExists = false;
                for (int i = 0; i < channels.size(); i++) {
                    String currChannelId = channels.get(i).getId();
                    if (currChannelId.startsWith(CH_CALLS_PREFIX)) {
                        // this is one of the calls channels handled here ...
                        if (currChannelId.equals(channelId)) {
                            // ... this is the actually required channel, fine :)
                            // update the name to reflect localize changes and chat renames
                            channelExists = true;
                            channels.get(i).setName(name);
                        }
                    }
                }

                // create a the channel
                if(!channelExists) {
                    NotificationChannel channel = new NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_MAX);
                    channel.setDescription("Informs about incoming calls.");
                    channel.setShowBadge(true);

                    Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                    channel.setSound(ringtone, new AudioAttributes.Builder()
                             .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                             .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                             .build());
                    notificationManager.createNotificationChannel(channel);
                }
            } catch(Exception e) {
              Log.e(TAG, "Error in getCallNotificationChannel()", e);
            }
        }

        return channelId;
    }


    // add notifications & co.
    // --------------------------------------------------------------------------------------------

    public void notifyCall(int accId, int callId, String payload) {
      Util.runOnAnyBackgroundThread(() -> {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        DcContext dcContext = context.getDcAccounts().getAccount(accId);
        int chatId = dcContext.getMsg(callId).getChatId();
        DcChat dcChat = dcContext.getChat(chatId);
        String name = dcChat.getName();
        ChatData chatData = new ChatData(accId, chatId);
        String notificationChannel = getCallNotificationChannel(notificationManager, chatData, name);

        PendingIntent declineCallIntent = getDeclineCallIntent(chatData, callId);
        PendingIntent openCallIntent = getOpenCallIntent(chatData, callId, payload, false);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, notificationChannel)
          .setSmallIcon(R.drawable.icon_notification)
          .setColor(context.getResources().getColor(R.color.delta_primary))
          .setPriority(NotificationCompat.PRIORITY_HIGH)
          .setCategory(NotificationCompat.CATEGORY_CALL)
          .setOngoing(true)
          .setOnlyAlertOnce(false)
          .setTicker(name)
          .setContentTitle(name)
          .setFullScreenIntent(openCallIntent, true)
          .setContentIntent(openCallIntent)
          .setContentText("Incoming Call");

        builder.addAction(
          new NotificationCompat.Action.Builder(
            R.drawable.baseline_call_end_24,
            context.getString(R.string.end_call),
            declineCallIntent).build());

        builder.addAction(
          new NotificationCompat.Action.Builder(
            R.drawable.baseline_call_24,
            context.getString(R.string.answer_call),
            getOpenCallIntent(chatData, callId, payload, true)).build());

        Bitmap bitmap = getAvatar(dcChat);
        if (bitmap != null) {
          builder.setLargeIcon(bitmap);
        }

        Notification notif = builder.build();
        notif.flags = notif.flags | Notification.FLAG_INSISTENT;
        try {
          notificationManager.notify("call-" + accId, callId, notif);
        } catch (Exception e) {
          Log.e(TAG, "cannot add notification", e);
        }
      });
    }

    public void notifyMessage(int accountId, int chatId, int msgId) {
      Util.runOnAnyBackgroundThread(() -> {
        DcContext dcContext = context.getDcAccounts().getAccount(accountId);
        DcChat dcChat = dcContext.getChat(chatId);

        DcMsg dcMsg = dcContext.getMsg(msgId);
        NotificationPrivacyPreference privacy = Prefs.getNotificationPrivacy(context);

        String shortLine = privacy.isDisplayMessage()? dcMsg.getSummarytext(2000) : context.getString(R.string.notify_new_message);
        if (dcChat.isMultiUser() && privacy.isDisplayContact()) {
          shortLine = dcMsg.getSenderName(dcContext.getContact(dcMsg.getFromId())) + ": " + shortLine;
        }
        String tickerLine = shortLine;
        if (!dcChat.isMultiUser() && privacy.isDisplayContact()) {
          tickerLine = dcMsg.getSenderName(dcContext.getContact(dcMsg.getFromId())) + ": " + tickerLine;

          if (dcMsg.getOverrideSenderName() != null) {
            // There is an "overridden" display name on the message, so, we need to prepend the display name to the message,
            // i.e. set the shortLine to be the same as the tickerLine.
            shortLine = tickerLine;
          }
        }

        DcMsg quotedMsg = dcMsg.getQuotedMsg();
        boolean isMention = dcChat.isMultiUser() && quotedMsg != null && quotedMsg.isOutgoing();

        maybeAddNotification(accountId, dcChat, msgId, shortLine, tickerLine, true, isMention);
      });
    }

    public void notifyReaction(int accountId, int contactId, int msgId, String reaction) {
      Util.runOnAnyBackgroundThread(() -> {
        DcContext dcContext = context.getDcAccounts().getAccount(accountId);
        DcMsg dcMsg = dcContext.getMsg(msgId);

        NotificationPrivacyPreference privacy = Prefs.getNotificationPrivacy(context);
        if (!privacy.isDisplayContact() || !privacy.isDisplayMessage()) {
          return; // showing "New Message" is wrong and showing "New Reaction" is already content. just do nothing.
        }

        DcContact sender = dcContext.getContact(contactId);
        String shortLine = context.getString(R.string.reaction_by_other, sender.getDisplayName(), reaction, dcMsg.getSummarytext(2000));
        DcChat dcChat = dcContext.getChat(dcMsg.getChatId());
        maybeAddNotification(accountId, dcChat, msgId, shortLine, shortLine, false, dcChat.isMultiUser());
      });
    }

    public void notifyWebxdc(int accountId, int contactId, int msgId, String text) {
      Util.runOnAnyBackgroundThread(() -> {
        NotificationPrivacyPreference privacy = Prefs.getNotificationPrivacy(context);
        if (!privacy.isDisplayContact() || !privacy.isDisplayMessage()) {
          return; // showing "New Message" is wrong, just do nothing.
        }

        DcContext dcContext = context.getDcAccounts().getAccount(accountId);
        DcMsg dcMsg = dcContext.getMsg(msgId);
        DcMsg parentMsg;
        if(dcMsg.getType() == DcMsg.DC_MSG_WEBXDC) {
            parentMsg = dcMsg;
        } else { // info message, get parent xdc
            parentMsg = dcMsg.getParent() != null? dcMsg.getParent() : dcMsg;
        }

        if (Util.equals(visibleWebxdc, new Pair<>(accountId, parentMsg.getId()))) {
            return; // do not notify if the app is already open
        }

        JSONObject info = parentMsg.getWebxdcInfo();
        final String name = JsonUtils.optString(info, "name");
        String shortLine = name.isEmpty()? text : (name + ": " + text);
        DcChat dcChat = dcContext.getChat(dcMsg.getChatId());
        maybeAddNotification(accountId, dcChat, msgId, shortLine, shortLine, false, dcChat.isMultiUser());
      });
    }

    @WorkerThread
    private void maybeAddNotification(int accountId, DcChat dcChat, int msgId, String shortLine, String tickerLine, boolean playInChatSound, boolean isMention) {

            DcContext dcContext = context.getDcAccounts().getAccount(accountId);
            int chatId = dcChat.getId();
            ChatData chatData = new ChatData(accountId, chatId);
            isMention = isMention && dcContext.isMentionsEnabled();

            if (dcContext.isMuted() || (!isMention &&  dcChat.isMuted())) {
                return;
            }

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationManager.areNotificationsEnabled()) {
                return;
            }

            if (Util.equals(visibleChat, chatData)) {
                if (playInChatSound && Prefs.isInChatNotifications(context)) {
                    InChatSounds.getInstance(context).playIncomingSound();
                }
                return;
            }

            NotificationPrivacyPreference privacy = Prefs.getNotificationPrivacy(context);
            long now = System.currentTimeMillis();
            boolean signal = (now - lastAudibleNotification) > MIN_AUDIBLE_PERIOD_MILLIS;
            if (signal) {
                lastAudibleNotification = now;
            }

            // create a basic notification
            // even without a name or message displayed,
            // it makes sense to use separate notification channels and to open the respective chat directly -
            // the user may eg. have chosen a different sound
            String notificationChannel = getNotificationChannel(notificationManager, chatData, dcChat);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, notificationChannel)
                    .setSmallIcon(R.drawable.icon_notification)
                    .setColor(context.getResources().getColor(R.color.delta_primary))
                    .setPriority(Prefs.getNotificationPriority(context))
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setOnlyAlertOnce(!signal)
                    .setContentText(shortLine)
                    .setDeleteIntent(getMarkAsReadIntent(chatData, msgId, false))
                    .setContentIntent(getOpenChatIntent(chatData));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setGroup(GRP_MSG + "." + accountId);
            }

            String accountTag = dcContext.getConfig(CONFIG_PRIVATE_TAG);
            if (accountTag.isEmpty() && context.getDcAccounts().getAll().length > 1) {
                accountTag = dcContext.getName();
            }

            if (privacy.isDisplayContact()) {
                builder.setContentTitle(dcChat.getName());
                if (!TextUtils.isEmpty(accountTag)) {
                    builder.setSubText(accountTag);
                }
            }

            builder.setTicker(tickerLine);

            // set sound, vibrate, led for systems that do not have notification channels
            if (!notificationChannelsSupported()) {
                if (signal) {
                    Uri sound = effectiveSound(chatData);
                    if (sound != null && !TextUtils.isEmpty(sound.toString())) {
                        builder.setSound(sound);
                    }
                    boolean vibrate = effectiveVibrate(chatData);
                    if (vibrate) {
                        builder.setDefaults(Notification.DEFAULT_VIBRATE);
                    }
                }
                String ledColor = Prefs.getNotificationLedColor(context);
                if (!ledColor.equals("none")) {
                    builder.setLights(getLedArgb(ledColor),500, 2000);
                }
            }

            // set avatar
            if (privacy.isDisplayContact()) {
              Bitmap bitmap = getAvatar(dcChat);
              if (bitmap != null) {
                builder.setLargeIcon(bitmap);
              }
            }

            // add buttons that allow some actions without opening Delta Chat.
            // if privacy options are enabled, the buttons are not added.
            if (privacy.isDisplayContact() && privacy.isDisplayMessage()) {
                try {
                    PendingIntent inNotificationReplyIntent = getRemoteReplyIntent(chatData, msgId);
                    PendingIntent markReadIntent = getMarkAsReadIntent(chatData, msgId, true);

                    NotificationCompat.Action markAsReadAction = new NotificationCompat.Action(R.drawable.check,
                            context.getString(R.string.mark_as_read_short),
                            markReadIntent);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(R.drawable.ic_reply_white_36dp,
                                context.getString(R.string.notify_reply_button),
                                inNotificationReplyIntent)
                                .addRemoteInput(new RemoteInput.Builder(RemoteReplyReceiver.EXTRA_REMOTE_REPLY)
                                        .setLabel(context.getString(R.string.notify_reply_button)).build())
                                .build();
                        builder.addAction(replyAction);
                    }

                    NotificationCompat.Action wearableReplyAction = new NotificationCompat.Action.Builder(R.drawable.ic_reply,
                            context.getString(R.string.notify_reply_button),
                            inNotificationReplyIntent)
                            .addRemoteInput(new RemoteInput.Builder(RemoteReplyReceiver.EXTRA_REMOTE_REPLY)
                                    .setLabel(context.getString(R.string.notify_reply_button)).build())
                            .build();
                    builder.addAction(markAsReadAction);
                    builder.extend(new NotificationCompat.WearableExtender().addAction(markAsReadAction).addAction(wearableReplyAction));
                } catch(Exception e) { Log.w(TAG, e); }
            }

            // create a tiny inbox (gets visible if the notification is expanded)
            if (privacy.isDisplayContact() && privacy.isDisplayMessage()) {
                try {
                    NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
                    synchronized (inboxes) {
                        HashMap<Integer, ArrayList<String>> accountInbox = inboxes.get(accountId);
                        if (accountInbox == null) {
                            accountInbox = new HashMap<>();
                            inboxes.put(accountId, accountInbox);
                        }
                        ArrayList<String> lines = accountInbox.get(chatId);
                        if (lines == null) {
                            lines = new ArrayList<>();
                            accountInbox.put(chatId, lines);
                        }
                        lines.add(shortLine);

                        for (int l = 0; l < lines.size(); l++) {
                            inboxStyle.addLine(lines.get(l));
                        }
                    }
                    builder.setStyle(inboxStyle);
                } catch(Exception e) { Log.w(TAG, e); }
            }

            // messages count, some os make some use of that
            // - do not use setSubText() as this is displayed together with setContentInfo() eg. on Lollipop
            // - setNumber() may overwrite setContentInfo(), should be called last
            // weird stuff.
            int cnt = dcContext.getFreshMsgCount(chatId);
            builder.setContentInfo(String.valueOf(cnt));
            builder.setNumber(cnt);

            // add notification, we use one notification per chat,
            // esp. older android are not that great at grouping
            try {
              notificationManager.notify(String.valueOf(accountId), ID_MSG_OFFSET + chatId, builder.build());
            } catch (Exception e) {
              Log.e(TAG, "cannot add notification", e);
            }

            // group notifications together in a summary, this is possible since SDK 24 (Android 7)
            // https://developer.android.com/training/notify-user/group.html
            // in theory, this won't be needed due to setGroup(), however, in practise, it is needed up to at least Android 10.
            if (Build.VERSION.SDK_INT >= 24) {
                try {
                  NotificationCompat.Builder summary = new NotificationCompat.Builder(context, notificationChannel)
                    .setGroup(GRP_MSG + "." + accountId)
                    .setGroupSummary(true)
                    .setSmallIcon(R.drawable.icon_notification)
                    .setColor(context.getResources().getColor(R.color.delta_primary, null))
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setContentTitle("Delta Chat") // content title would only be used on SDK <24
                    .setContentText("New messages") // content text would only be used on SDK <24
                    .setContentIntent(getOpenChatlistIntent(accountId));
                  if (privacy.isDisplayContact() && !TextUtils.isEmpty(accountTag)) {
                    summary.setSubText(accountTag);
                  }
                  notificationManager.notify(String.valueOf(accountId), ID_MSG_SUMMARY, summary.build());
                } catch (Exception e) {
                  Log.e(TAG, "cannot add notification summary", e);
                }
            }
    }

    public Bitmap getAvatar(DcChat dcChat) {
      Recipient recipient = new Recipient(context, dcChat);
      try {
        Drawable drawable;
        ContactPhoto contactPhoto = recipient.getContactPhoto(context);
        if (contactPhoto != null) {
          drawable = GlideApp.with(context.getApplicationContext())
            .load(contactPhoto)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .circleCrop()
            .submit(context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                    context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height))
            .get();

        } else {
          drawable = recipient.getFallbackContactPhoto().asDrawable(context, recipient.getFallbackAvatarColor());
        }
        if (drawable != null) {
          int wh = context.getResources().getDimensionPixelSize(R.dimen.contact_photo_target_size);
          return BitmapUtil.createFromDrawable(drawable, wh, wh);
        }
      } catch (Exception e) { Log.w(TAG, e); }

      return null;
    }

    public void removeCallNotification(int accountId, int callId) {
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            String tag = "call-" + accountId;
            notificationManager.cancel(tag, callId);
        } catch (Exception e) { Log.w(TAG, e); }
    }

    public void removeNotifications(int accountId, int chatId) {
        boolean removeSummary;
        synchronized (inboxes) {
            HashMap<Integer, ArrayList<String>> accountInbox = inboxes.get(accountId);
            if (accountInbox == null) {
                accountInbox = new HashMap<>();
            }
            accountInbox.remove(chatId);
            removeSummary = accountInbox.isEmpty();
        }

        // cancel notification independently of inboxes array,
        // due to restarts, the app may have notification even when inboxes is empty.
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            String tag = String.valueOf(accountId);
            notificationManager.cancel(tag, ID_MSG_OFFSET + chatId);
            if (removeSummary) {
                notificationManager.cancel(tag, ID_MSG_SUMMARY);
            }
        } catch (Exception e) { Log.w(TAG, e); }
    }

    public void removeAllNotifications(int accountId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        String tag = String.valueOf(accountId);
        synchronized (inboxes) {
            HashMap<Integer, ArrayList<String>> accountInbox = inboxes.get(accountId);
            notificationManager.cancel(tag, ID_MSG_SUMMARY);
            if (accountInbox != null) {
                for (Integer chatId : accountInbox.keySet()) {
                    notificationManager.cancel(tag, chatId);
                }
                accountInbox.clear();
            }
        }
    }

    public void updateVisibleChat(int accountId, int chatId) {
        Util.runOnAnyBackgroundThread(() -> {

            if (accountId != 0 && chatId != 0) {
                visibleChat = new ChatData(accountId, chatId);
                removeNotifications(accountId, chatId);
            } else {
                visibleChat = null;
            }

        });
    }

    public void clearVisibleChat() {
        visibleChat = null;
    }

    public void updateVisibleWebxdc(int accountId, int msgId) {
        if (accountId != 0 && msgId != 0) {
            visibleWebxdc = new Pair<>(accountId, msgId);
        } else {
            visibleWebxdc = null;
        }
    }

    public void clearVisibleWebxdc() {
        visibleWebxdc = null;
    }

    public void maybePlaySendSound(DcChat dcChat) {
        if (Prefs.isInChatNotifications(context) && !dcChat.isMuted()) {
            InChatSounds.getInstance(context).playSendSound();
        }
    }

  public static class ChatData {
    public final int accountId;
    public final int chatId;

    public ChatData(int accountId, int chatId) {
        this.accountId = accountId;
        this.chatId = chatId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ChatData chatData = (ChatData) o;
      return accountId == chatData.accountId && chatId == chatData.chatId;
    }
  }
}
