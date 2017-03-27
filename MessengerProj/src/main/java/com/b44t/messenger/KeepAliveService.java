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
import android.os.IBinder;
import android.util.Log;


public class KeepAliveService extends Service {

    @Override
    public void onCreate() {
        Log.i("DeltaChat", "*** KeepAliveService.onCreate()");
        // there's nothing more to do here as all initialisation stuff is already done in
        // ApplicationLoader.onCreate() which is called before this broadcast is sended.
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // START_STICKY ensured, the service is recreated as soon it is terminted for any reasons.
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
}
