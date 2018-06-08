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
import android.util.Log;

public class ScreenReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            MrMailbox.log_i("DeltaChat", "*** Screen off");
            ApplicationLoader.isScreenOn = false;

            // also stop the idle-thread when the screen goes off, this is not always in sync with the ForegroundDetector
            if( !ApplicationLoader.getPermanentPush() ) {
                ApplicationLoader.scheduleStopImapThread();
            }
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            MrMailbox.log_i("DeltaChat", "*** Screen on");
            ApplicationLoader.isScreenOn = true;
        }
        //NotificationCenter.getInstance().postNotificationName(NotificationCenter.screenStateChanged);
    }
}
