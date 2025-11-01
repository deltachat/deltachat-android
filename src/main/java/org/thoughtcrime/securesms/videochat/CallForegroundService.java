package org.thoughtcrime.securesms.videochat;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.notifications.NotificationCenter;
import org.thoughtcrime.securesms.util.IntentUtils;

public class CallForegroundService extends Service {

  private static final String TAG = CallForegroundService.class.getSimpleName();

  private static final String ACC_ID_EXTRA = "acc_id";
  private static final String CALL_ID_EXTRA = "call_id";
  private static final String PAYLOAD_EXTRA = "payload";

  static CallForegroundService s_this = null;

  public static void startSelf(Context context, int accId, int callId, String payload) {
    Intent intent = new Intent(context, CallForegroundService.class);
    intent.putExtra(ACC_ID_EXTRA, accId);
    intent.putExtra(CALL_ID_EXTRA, callId);
    intent.putExtra(PAYLOAD_EXTRA, payload);
    try {
      ContextCompat.startForegroundService(context, intent);
    } catch(Exception e) {
      Log.i(TAG, "Error calling ContextCompat.startForegroundService()", e);
    }
  }

  @Override
  public void onCreate() {
    Log.i("DeltaChat", "*** CallForegroundService.onCreate()");
    s_this = this;
    startForeground(NotificationCenter.ID_PERMANENT, createNotification());
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.i("DeltaChat", "*** CallForegroundService.onStartCommand()");
    int accId = intent.getIntExtra(ACC_ID_EXTRA, -1);
    int callId = intent.getIntExtra(CALL_ID_EXTRA, 0);
    String payload = intent.getStringExtra(PAYLOAD_EXTRA);
    Notification notif = buildIncomingCall(this, accId, callId, payload);
    try {
      startForeground(NotificationCenter.ID_PERMANENT, notif);
    }
    catch (Exception e) {
      Log.i(TAG, "Error", e);
    }
    return START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  public void onDestroy() {
    Log.i("DeltaChat", "*** CallForegroundService.onDestroy()");
    // the service will be restarted due to START_STICKY automatically, there's nothing more to do.
  }

  static public CallForegroundService getInstance()
  {
    return s_this; // may be null
  }

  public Notification buildIncomingCall(Context service, int accId, int callId, String payload) {
    NotificationCenter notificationCenter = ApplicationContext.getInstance(this).notificationCenter;
    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(service);
    DcContext dcContext = ApplicationContext.dcAccounts.getAccount(accId);
    int chatId = dcContext.getMsg(callId).getChatId();
    DcChat dcChat = dcContext.getChat(chatId);
    String name = dcChat.getName();
    NotificationCenter.ChatData chatData = new NotificationCenter.ChatData(accId, chatId);
    String notificationChannel = notificationCenter.getCallNotificationChannel(notificationManager, chatData, name);

    PendingIntent declineIntent = notificationCenter.getDeclineCallIntent(chatData, callId);
    PendingIntent answerIntent = notificationCenter.getAnswerIntent(chatData, callId, payload);
    Bitmap bitmap = notificationCenter.getAvatar(dcChat);

    Person.Builder callerBuilder = new Person.Builder()
      .setName(name);
        
    if (bitmap != null) {
      callerBuilder.setIcon(IconCompat.createWithBitmap(bitmap));
    }

    NotificationCompat.CallStyle style = NotificationCompat.CallStyle
      .forIncomingCall(callerBuilder.build(), declineIntent, answerIntent);

    NotificationCompat.Builder builder = new NotificationCompat.Builder(service, notificationChannel)
      .setSmallIcon(R.drawable.icon_notification)
      .setColor(getResources().getColor(R.color.delta_primary))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setOngoing(true)
      .setOnlyAlertOnce(false)
      .setTicker(name)
      .setContentTitle(name)
      .setContentText("Incoming Call")
      .setStyle(style);

    if (bitmap != null) {
      builder.setLargeIcon(bitmap);
    }

    Notification notif = builder.build();
    notif.flags = notif.flags | Notification.FLAG_INSISTENT;
    return notif;
  }

  private Notification createNotification()
  {
    Intent intent = new Intent(this, ConversationListActivity.class);
    PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | IntentUtils.FLAG_MUTABLE());
    // a notification _must_ contain a small icon, a title and a text, see https://developer.android.com/guide/topics/ui/notifiers/notifications.html#Required
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

    builder.setContentTitle(getString(R.string.app_name));
    builder.setContentText("Incoming Call");

    builder.setPriority(NotificationCompat.PRIORITY_MIN);
    builder.setWhen(0);
    builder.setContentIntent(contentIntent);
    builder.setSmallIcon(R.drawable.notification_permanent);
    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
      createFgNotificationChannel(this);
      builder.setChannelId(NotificationCenter.CH_PERMANENT);
    }
    return builder.build();
  }

  private static boolean ch_created = false;
  @TargetApi(Build.VERSION_CODES.O)
  static private void createFgNotificationChannel(Context context) {
    if(!ch_created) {
      ch_created = true;
      NotificationChannel channel = new NotificationChannel(NotificationCenter.CH_PERMANENT,
                                                            "Receive messages in background.", NotificationManager.IMPORTANCE_MIN); // IMPORTANCE_DEFAULT will play a sound
      channel.setDescription("Ensure reliable message receiving.");
      NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
      notificationManager.createNotificationChannel(channel);
    }
  }
}
