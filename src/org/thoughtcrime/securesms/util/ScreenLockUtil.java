package org.thoughtcrime.securesms.util;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;

import org.thoughtcrime.securesms.R;

public class ScreenLockUtil {

    public static final int REQUEST_CODE_CONFIRM_CREDENTIALS = 1001;

    public static boolean applyScreenLock(Activity activity, int requestCode) {
        KeyguardManager keyguardManager = (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);
        Intent intent;
        if (keyguardManager != null && isScreenLockAvailable()) {
            intent = keyguardManager.createConfirmDeviceCredentialIntent(activity.getString(R.string.screenlock_unlock_title), activity.getString(R.string.screenlock_unlock_description));
            if (intent != null) {
                activity.startActivityForResult(intent, requestCode);
                return true;
            }
        }
        return false;
    }

    private static boolean isScreenLockAvailable() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    }
}
