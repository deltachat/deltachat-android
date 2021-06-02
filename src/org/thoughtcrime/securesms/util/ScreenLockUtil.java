package org.thoughtcrime.securesms.util;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;

import org.thoughtcrime.securesms.R;

public class ScreenLockUtil {

    public static final int REQUEST_CODE_CONFIRM_CREDENTIALS = 1001;

    public static boolean applyScreenLock(Activity activity, String title, int requestCode) {
        KeyguardManager keyguardManager = (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);
        Intent intent;
        if (keyguardManager != null && isScreenLockAvailable()) {
            intent = keyguardManager.createConfirmDeviceCredentialIntent(title, activity.getString(R.string.enter_system_secret_to_continue));
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
