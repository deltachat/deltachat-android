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
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;

public class KeepAliveService extends Service {

    static KeepAliveService s_this = null;

    public static void startSelf(Context context)
    {
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
                if(powerManager.isIgnoringBatteryOptimizations(context.getPackageName())) {
                    return; // fine, the user has disabled the battery optimisations for us
                }
            }
            else {
                return; // android <= lollipop does not have a doze mode
            }

            // if we're here, we're on a system with doze mode and battery optimisations enabled.
            // add foreground notification to stay alive.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // the started service has to call startForeground() within 5 seconds,
                // see https://developer.android.com/about/versions/oreo/android-8.0-changes
                context.startForegroundService(new Intent(context, KeepAliveService.class));
            }
            else {
                context.startService(new Intent(context, KeepAliveService.class));
            }
        }
        catch(Exception e) {
            e.printStackTrace();
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
            startForeground(FG_NOTIFICATION_ID, createNotification());
        }
        catch (Exception e) {
            e.printStackTrace();
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

    public void onDestroy() {
        Log.i("DeltaChat", "*** KeepAliveService.onDestroy()");
        // the service will be restarted due to START_STICKY automatically, there's nothing more to do.
    }

    static public KeepAliveService getInstance()
    {
        return s_this; // may be null
    }

    /* The notification
     * A notification is required for a foreground service; and without a foreground service,
     * Delta Chat won't get new messages reliable
     **********************************************************************************************/

    public static final int FG_NOTIFICATION_ID = 4142;
    private Notification createNotification()
    {
        Intent intent = new Intent(this, ConversationListActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // a notification _must_ contain a small icon, a title and a text, see https://developer.android.com/guide/topics/ui/notifiers/notifications.html#Required
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText(getString(R.string.notify_background_connection_enabled));

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ) {
            builder.setPriority(NotificationCompat.PRIORITY_MIN);
        }
        builder.setWhen(0);
        builder.setContentIntent(contentIntent);
        builder.setSmallIcon(R.drawable.notification_permanent);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            createFgNotificationChannel(this);
            builder.setChannelId(FG_CHANNEL_ID);
        }
        return builder.build();
    }

    private static boolean ch_created = false;
    private static final String FG_CHANNEL_ID = "dc_foreground_notification_ch";
    @TargetApi(Build.VERSION_CODES.O)
    static private void createFgNotificationChannel(Context context) {
        if(!ch_created) {
            ch_created = true;
            NotificationChannel channel = new NotificationChannel(FG_CHANNEL_ID,
                "Receive messages in background.", NotificationManager.IMPORTANCE_MIN); // IMPORTANCE_DEFAULT will play a sound
            channel.setDescription("Ensure reliable message receiving.");
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
