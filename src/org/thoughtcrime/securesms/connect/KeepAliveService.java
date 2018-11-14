package org.thoughtcrime.securesms.connect;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;

import java.util.Locale;


public class KeepAliveService extends Service {

    static KeepAliveService s_this = null;

    @Override
    public void onCreate() {
        Log.i("DeltaChat", "*** KeepAliveService.onCreate()");
        // there's nothing more to do here as all initialisation stuff is already done in
        // ApplicationLoader.onCreate() which is called before this broadcast is sended.
        s_this = this;
        setSelfAsForeground();
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

    private void setSelfAsForeground() {
        stopForeground(true);
        startForeground(FG_NOTIFICATION_ID, createNotification()); // TODO: if we target Android O, we should use startServiceInForeground()
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
        ApplicationDcContext dcContext = DcHelper.getContext(this);

        Intent intent = new Intent(this, ConversationListActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // a notification _must_ contain a small icon, a title and a text, see https://developer.android.com/guide/topics/ui/notifiers/notifications.html#Required
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText(getString(R.string.permanent_notification_subtitle));

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ) {
            builder.setPriority(NotificationCompat.PRIORITY_MIN);
        }
        builder.setWhen(0);
        builder.setContentIntent(contentIntent);
        builder.setSmallIcon(R.drawable.icon_notification);
        return builder.build();
    }
}
