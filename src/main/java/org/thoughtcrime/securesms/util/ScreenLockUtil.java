package org.thoughtcrime.securesms.util;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;

public class ScreenLockUtil {

    public static final int REQUEST_CODE_CONFIRM_CREDENTIALS = 1001;

    public static boolean applyScreenLock(Activity activity, String title, String descr, int requestCode) {
        KeyguardManager keyguardManager = (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);
        Intent intent;
        if (keyguardManager != null) {
            intent = keyguardManager.createConfirmDeviceCredentialIntent(title, descr);
            if (intent != null) {
                activity.startActivityForResult(intent, requestCode);
                return true;
            }
        }
        return false;
    }

}
