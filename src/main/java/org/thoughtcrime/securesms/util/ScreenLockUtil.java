package org.thoughtcrime.securesms.util;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;

public class ScreenLockUtil {

    public static boolean applyScreenLock(Activity activity, String title, String descr, ActivityResultLauncher<Intent> launcher) {
        KeyguardManager keyguardManager = (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);
        Intent intent;
        if (keyguardManager != null) {
            intent = keyguardManager.createConfirmDeviceCredentialIntent(title, descr);
            if (intent != null) {
                launcher.launch(intent);
                return true;
            }
        }
        return false;
    }

}
