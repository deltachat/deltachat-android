package org.thoughtcrime.securesms.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;

import org.thoughtcrime.securesms.R;

import java.util.Timer;
import java.util.TimerTask;

public class ScreenLockUtil {

    public static final int REQUEST_CODE_CONFIRM_CREDENTIALS = 1001;

    private static boolean shouldLockApp = true;

    public static void applyScreenLock(Activity activity) {
        applyScreenLock(activity, REQUEST_CODE_CONFIRM_CREDENTIALS);
    }

    @TargetApi(21)
    public static boolean applyScreenLock(Activity activity, int requestCode) {
        KeyguardManager keyguardManager = (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);
        Intent intent;
        if (keyguardManager != null) {
            intent = keyguardManager.createConfirmDeviceCredentialIntent(activity.getString(R.string.screenlock_unlock_title), activity.getString(R.string.screenlock_unlock_description));
            if (intent != null) {
                activity.startActivityForResult(intent, requestCode);
                return true;
            }
        }
        return false;
    }

    public static boolean isScreenLockEnabled(Context context) {
        return Prefs.isScreenLockEnabled(context)
                && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean isScreenLockTimeoutEnabled(Context context) {
        return Prefs.isScreenLockTimeoutEnabled(context)
                && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    }

    public static Timer scheduleScreenLockTimer(Timer timer, Activity activity) {
        cancelScreenLockTimer(timer);
        Timer newTimer = new Timer();
        newTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isScreenLockTimeoutEnabled(activity)) {
                    ScreenLockUtil.applyScreenLock(activity);
                }
            }
        }, Prefs.getScreenLockTimeoutInterval(activity) * 1000);
        return newTimer;
    }

    public static void cancelScreenLockTimer(Timer timer) {
        if (timer != null) {
            timer.cancel();
        }
    }

    public static boolean getShouldLockApp() {
        return shouldLockApp;
    }

    public static void setShouldLockApp(boolean newShouldLockApp) {
        shouldLockApp = newShouldLockApp;
    }

}
