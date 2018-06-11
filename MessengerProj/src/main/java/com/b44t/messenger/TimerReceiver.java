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

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.coremedia.iso.boxes.apple.AppleItemListBox;


public class TimerReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {

        Log.i("DeltaChat", "-------------------- on receive timer --------------------");

        scheduleNextAlarm();

        ApplicationLoader.startImapThread();
        ApplicationLoader.waitForImapThreadRunning();
    }

    public static void scheduleNextAlarm()
    {
        try {
            Intent intent = new Intent(ApplicationLoader.applicationContext, TimerReceiver.class);
            PendingIntent alarmIntent = PendingIntent.getBroadcast(ApplicationLoader.applicationContext, 0, intent, 0);

            long triggerAtMillis = System.currentTimeMillis() + 60 * 1000;

            AlarmManager alarmManager = (AlarmManager) ApplicationLoader.applicationContext.getSystemService(Activity.ALARM_SERVICE);
            if( Build.VERSION.SDK_INT >= 23 ) {
                // a simple AlarmManager.set() is no longer send in the new DOZE mode
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, alarmIntent);
            }
            else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, alarmIntent);
            }
        }
        catch(Exception e) { Log.e("DeltaChat", "Cannot create alarm.", e); }
    }
}
