/*******************************************************************************
 *
 *                          Messenger Android Frontend
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

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;


public class KeepAliveService extends Service {

    @Override
    public void onCreate() {
        MrMailbox.log_i("DeltaChat", "*** KeepAliveService.onCreate()");
        // there's nothing more to do here as all initialisation stuff is already done in
        // ApplicationLoader.onCreate() which is called before this broadcast is sended.

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

    public static final int SERVICE_RUNNING_ID = 4141;

    private void setSelfAsForeground() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle(getString(R.string.AppName));
        builder.setContentText(getString(R.string.BackgroundConnectionEnabled));
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ) {
            builder.setPriority(NotificationCompat.PRIORITY_MIN);
        }
        builder.setWhen(0);
        builder.setSmallIcon(R.drawable.notification_permanent);

        stopForeground(true);
        startForeground(SERVICE_RUNNING_ID, builder.build()); // TODO: if we target Android O, we should use startServiceInForeground()
    }
}
