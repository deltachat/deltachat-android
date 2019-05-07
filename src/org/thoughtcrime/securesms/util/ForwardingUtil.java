package org.thoughtcrime.securesms.util;

import android.app.Activity;

public class ForwardingUtil {
    public static final String FORWARDED_MESSAGE_IDS   = "forwarded_message_ids";
    public static final int REQUEST_FORWARD = 100;

    public static boolean isForwarding(Activity activity) {
        try {
            return activity.getIntent().getIntArrayExtra(FORWARDED_MESSAGE_IDS) != null;
        } catch (NullPointerException npe) {
            return false;
        }
    }

    public static int[] getForwardedMessageIDs(Activity activity) {
        try {
            return activity.getIntent().getIntArrayExtra(FORWARDED_MESSAGE_IDS);
        } catch (NullPointerException npe) {
            return null;
        }
    }

    public static void resetForwarding(Activity activity) {
        try {
            activity.getIntent().removeExtra(FORWARDED_MESSAGE_IDS);
        } catch (NullPointerException npe) {
            // eat me
        }
    }
}
