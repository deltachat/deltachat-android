/*******************************************************************************
 *
 *                              Delta Chat Android
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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Locale;


public class KeepAliveService extends Service {

    static KeepAliveService s_this = null;

    @Override
    public void onCreate() {
        MrMailbox.log_i("DeltaChat", "*** KeepAliveService.onCreate()");
        // there's nothing more to do here as all initialisation stuff is already done in
        // ApplicationLoader.onCreate() which is called before this broadcast is sended.
        s_this = this;
        setSelfAsForeground();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // START_STICKY ensured, the service is recreated as soon it is terminted for any reasons.
        // as ApplicationLoader.onCreate() is called before a service starts, there is no more to do here,
        // the app is just running fine.
        MrMailbox.log_i("DeltaChat", "*** KeepAliveService.onStartCommand()");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        MrMailbox.log_i("DeltaChat", "*** KeepAliveService.onDestroy()");
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
        // a notification _must_ contain a small icon, a title and a text, see https://developer.android.com/guide/topics/ui/notifiers/notifications.html#Required
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        if( MrMailbox.isConfigured()!=0) {
            builder.setContentTitle(String.format(getString(R.string.PermNotificationTitle), MrMailbox.getConfig("addr", "")));
            builder.setContentText(getString(R.string.PermNotificationText));
        }
        else {
            builder.setContentTitle(getString(R.string.AppName));
            builder.setContentText(getString(R.string.AccountNotConfigured));
        }

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ) {
            builder.setPriority(NotificationCompat.PRIORITY_MIN);
        }
        builder.setWhen(0);
        builder.setSmallIcon(R.drawable.notification_permanent);
        return builder.build();
    }

    public void updateForegroundNotification()
    {
        // update the notification by simply creating a new notification with the same ID, see https://developer.android.com/training/notify-user/managing.html#Updating
        NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(FG_NOTIFICATION_ID, createNotification());
    }
}
