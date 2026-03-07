package org.thoughtcrime.securesms;

import android.content.Context;

import org.thoughtcrime.securesms.calls.CallCoordinator;

public class LegacyCompatHelper {
    public static void appContextInit(Context context) {
        EglUtils.getEglBase();

        CallCoordinator.getInstance(context);
    }

    public static void appContextOnTerminate() {
        EglUtils.release();
    }
}
