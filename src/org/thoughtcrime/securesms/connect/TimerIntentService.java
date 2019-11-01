package org.thoughtcrime.securesms.connect;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.util.Log;

public class TimerIntentService extends JobIntentService {
    private static final int JOB_ID = 161030;
    private static final String TAG = TimerIntentService.class.getSimpleName();

    @Override
    protected void onHandleWork(@NonNull Intent intent) {

        scheduleNextAlarm(getApplicationContext());

        ApplicationDcContext dcContext = DcHelper.getContext(getApplicationContext());
        dcContext.startThreads(ApplicationDcContext.INTERRUPT_IDLE);
        dcContext.waitForThreadsRunning();
    }

    public static void scheduleNextAlarm(Context context)
    {
        try {
            Intent intent = new Intent(context, TimerReceiver.class);
            PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

            long triggerAtMillis = System.currentTimeMillis() + 5 * 60 * 1000;
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

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, TimerIntentService.class, JOB_ID, work);
    }
}
