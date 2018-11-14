// restart the smtp and imap threads every 60 seconds if they were killed by the os

package org.thoughtcrime.securesms.connect;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;

public class TimerReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {

        Log.i("DeltaChat", "-------------------- on receive timer --------------------");

        scheduleNextAlarm(context);

        ApplicationDcContext dcContext = DcHelper.getContext(context);
        dcContext.startThreads();
        dcContext.waitForThreadsRunning();
    }

    public static void scheduleNextAlarm(Context context)
    {
        try {
            Intent intent = new Intent(context, TimerReceiver.class);
            PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

            long triggerAtMillis = System.currentTimeMillis() + 60 * 1000;

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
            if( Build.VERSION.SDK_INT >= 23 ) {
                // a simple AlarmManager.set() is no longer send in the new DOZE mode
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, alarmIntent);
            }
            else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, alarmIntent);
            }
        }
        catch(Exception e) {
            Log.e("DeltaChat", "Cannot create alarm.", e);
        }
    }
}
