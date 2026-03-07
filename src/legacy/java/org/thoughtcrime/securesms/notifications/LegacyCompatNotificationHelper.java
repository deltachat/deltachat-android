package org.thoughtcrime.securesms.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.ConversationActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.calls.CallActivity;
import org.thoughtcrime.securesms.util.IntentUtils;
import org.thoughtcrime.securesms.util.Util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import chat.delta.rpc.RpcException;

public class LegacyCompatNotificationHelper {

    public static final String CH_CALLS_PREFIX = "call_chan";

    private static String getCallNotificationChannel(NotificationCenter.ChatData chatData) {
        return CH_CALLS_PREFIX + "-" + chatData.accountId + "-"+ chatData.chatId;
    }

    private static PendingIntent getOpenCallIntent(Context context, String TAG, NotificationCenter.ChatData chatData, int callId, String payload, boolean autoAccept, boolean hasVideo) {
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
        intent.putExtra(CallActivity.EXTRA_HAS_VIDEO, hasVideo);
        intent.setPackage(context.getPackageName());
        return TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(chatIntent)
                .addNextIntent(intent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | IntentUtils.FLAG_MUTABLE());
    }

    private static PendingIntent getDeclineCallIntent(Context context, NotificationCenter.ChatData chatData, int callId) {
        Intent intent = new Intent(DeclineCallReceiver.DECLINE_ACTION);
        intent.setClass(context, DeclineCallReceiver.class);
        intent.putExtra(DeclineCallReceiver.ACCOUNT_ID_EXTRA, chatData.accountId);
        intent.putExtra(DeclineCallReceiver.CALL_ID_EXTRA, callId);
        intent.setPackage(context.getPackageName());
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | IntentUtils.FLAG_MUTABLE());
    }

    public static void notifyCall(ApplicationContext context, String TAG, int accId, int callId, String payload) {
        Util.runOnAnyBackgroundThread(() -> {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            DcContext dcContext = ApplicationContext.getDcAccounts().getAccount(accId);
            boolean hasVideo;
            try {
                hasVideo = context.getRpc().callInfo(accId, callId).hasVideo;
            } catch (RpcException e) {
                Log.e(TAG, "Rpc.callInfo() failed", e);
                hasVideo = false;
            }
            int chatId = dcContext.getMsg(callId).getChatId();
            DcChat dcChat = dcContext.getChat(chatId);
            String name = dcChat.getName();
            NotificationCenter.ChatData chatData = new NotificationCenter.ChatData(accId, chatId);
            String notificationChannel = getCallNotificationChannel(chatData);

            PendingIntent declineCallIntent = getDeclineCallIntent(context, chatData, callId);
            PendingIntent openCallIntent = getOpenCallIntent(context, TAG, chatData, callId, payload, false, hasVideo);

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
                            getOpenCallIntent(context, TAG, chatData, callId, payload, true, hasVideo)).build());

            Bitmap bitmap = NotificationCenter.getAvatar(context, dcChat);
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

    public static void removeCallNotification(Context context, String TAG, int accountId, int callId) {
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            String tag = "call-" + accountId;
            notificationManager.cancel(tag, callId);
        } catch (Exception e) { Log.w(TAG, e); }
    }
}