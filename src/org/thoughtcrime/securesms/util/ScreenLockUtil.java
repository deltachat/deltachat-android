package org.thoughtcrime.securesms.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class ScreenLockUtil {

    public static final int REQUEST_CODE_CONFIRM_CREDENTIALS = 1001;

    public static boolean shouldLockApp = true;

    @TargetApi(21)
    public static void applyScreenLock(Activity activity) {
        KeyguardManager keyguardManager = (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);
        Intent intent;
        if (keyguardManager != null) {
            intent = keyguardManager.createConfirmDeviceCredentialIntent(null, null);
            if (intent != null) {
                activity.startActivityForResult(intent, REQUEST_CODE_CONFIRM_CREDENTIALS);
            }
        }
    }

    public static boolean isScreenLockEnabled(Context context) {
        return TextSecurePreferences.isScreenLockEnabled(context)
                && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    }

    public static Timer scheduleScreenLockTimer(Timer timer, Activity activity) {
        cancelScreenLockTimer(timer);
        Timer newTimer = new Timer();
        newTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                ScreenLockUtil.applyScreenLock(activity);
            }
        }, TextSecurePreferences.getScreenLockTimeoutInterval(activity) * 1000);
        return newTimer;
    }

    public static void cancelScreenLockTimer(Timer timer) {
        if (timer != null) {
            timer.cancel();
        }
    }

}
