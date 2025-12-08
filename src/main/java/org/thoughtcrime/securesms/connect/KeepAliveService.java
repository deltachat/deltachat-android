package org.thoughtcrime.securesms.connect;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.notifications.NotificationCenter;
import org.thoughtcrime.securesms.util.IntentUtils;
import org.thoughtcrime.securesms.util.Prefs;

public class KeepAliveService extends Service {

    private static final String TAG = KeepAliveService.class.getSimpleName();

    static KeepAliveService s_this = null;

    public static void maybeStartSelf(Context context) {
        // note, that unfortunately, the check for isIgnoringBatteryOptimizations() is not sufficient,
        // this checks only stock-android settings, several os have additional "optimizers" that ignore this setting.
        // therefore, the most reliable way to not get killed is a permanent-foreground-notification.
        if (Prefs.reliableService(context))  {
            startSelf(context);
        }
    }

    public static void startSelf(Context context)
    {
        try {
            ContextCompat.startForegroundService(context, new Intent(context, KeepAliveService.class));
        }
        catch(Exception e) {
            Log.i(TAG, "Error calling ContextCompat.startForegroundService()", e);
        }
    }

    @Override
    public void onCreate() {
        Log.i("DeltaChat", "*** KeepAliveService.onCreate()");
        // there's nothing more to do here as all initialisation stuff is already done in
        // ApplicationLoader.onCreate() which is called before this broadcast is sended.
        s_this = this;

        // set self as foreground
        try {
            stopForeground(true);
            startForeground(NotificationCenter.ID_PERMANENT, createNotification());
        }
        catch (Exception e) {
            Log.i(TAG, "Error in onCreate()", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // START_STICKY ensured, the service is recreated as soon it is terminated for any reasons.
        // as ApplicationLoader.onCreate() is called before a service starts, there is no more to do here,
        // the app is just running fine.
        Log.i("DeltaChat", "*** KeepAliveService.onStartCommand()");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.i("DeltaChat", "*** KeepAliveService.onDestroy()");
        // the service will be restarted due to START_STICKY automatically, there's nothing more to do.
    }

    @Override
    public void onTimeout(int startId, int fgsType) {
        stopSelf();
    }

    static public KeepAliveService getInstance()
    {
        return s_this; // may be null
    }

    /* The notification
     * A notification is required for a foreground service; and without a foreground service,
     * Delta Chat won't get new messages reliable
     **********************************************************************************************/

    private Notification createNotification()
    {
        Intent intent = new Intent(this, ConversationListActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | IntentUtils.FLAG_MUTABLE());
        // a notification _must_ contain a small icon, a title and a text, see https://developer.android.com/guide/topics/ui/notifiers/notifications.html#Required
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText(getString(R.string.notify_background_connection_enabled));

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
            channel.setShowBadge(false);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
