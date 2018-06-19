/*******************************************************************************
 *
 *                              Delta Chat Android
 *                        (C) 2013-2016 Nikolai Kudashov
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


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;


public class NetworkStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent == null || intent.getExtras() == null)
            return;

        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = manager.getActiveNetworkInfo();

        boolean connected = false;
        if(ni != null && ni.getState() == NetworkInfo.State.CONNECTED) {
            MrMailbox.log_i("DeltaChat", "++++++++++++++++++ Connected ++++++++++++++++++");
            connected = true;
        } else if(intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY,Boolean.FALSE)) {
            MrMailbox.log_i("DeltaChat", "++++++++++++++++++ Disconnected ++++++++++++++++++");
            connected = false;
        }

        // we interrupt idle also when going disconnected - otherwise the core will recognize the disconnected change
        // only after a timeout of typically 30 seconds; during this time a _reconnect_ will not be possible as the imap-thread
        // still hangs somewhere and waiting for response
        ApplicationLoader.startThreads();
        ApplicationLoader.waitForThreadsRunning();
        MrMailbox.interruptSmtpIdle();
        MrMailbox.interruptIdle();
    }


}
