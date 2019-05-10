package org.thoughtcrime.securesms.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.util.ArrayList;

public class RelayUtil {
    private static final String FORWARDED_MESSAGE_IDS   = "forwarded_message_ids";
    private static final String SHARED_URIS             = "shared_uris";
    public static final int REQUEST_RELAY = 100;

    public static boolean isRelayingMessageContent(Activity activity) {
        try {
            return activity.getIntent().getIntArrayExtra(FORWARDED_MESSAGE_IDS) != null ||
                    activity.getIntent().getParcelableArrayListExtra(SHARED_URIS) != null;
        } catch (NullPointerException npe) {
            return false;
        }
    }

    public static boolean isForwarding(Activity activity) {
        try {
            return activity.getIntent().getIntArrayExtra(FORWARDED_MESSAGE_IDS) != null;
        } catch (NullPointerException npe) {
            return false;
        }
    }

    public static boolean isSharing(Activity activity) {
        try {
            return activity.getIntent().getParcelableArrayListExtra(SHARED_URIS) != null;
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

    public static ArrayList<Uri> getSharedUris(Activity activity) {
        try {
            return activity.getIntent().getParcelableArrayListExtra(SHARED_URIS);
        } catch (NullPointerException npe) {
            return null;
        }
    }

    public static void resetRelayingMessageContent(Activity activity) {
        try {
            activity.getIntent().removeExtra(FORWARDED_MESSAGE_IDS);
            activity.getIntent().removeExtra(SHARED_URIS);
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }
    }

    public static void acquireRelayMessageContent(Activity currentActivity, @NonNull Intent newActivityIntent) {
        if (isForwarding(currentActivity)) {
            newActivityIntent.putExtra(FORWARDED_MESSAGE_IDS, getForwardedMessageIDs(currentActivity));
        } else if (isSharing(currentActivity)) {
            newActivityIntent.putParcelableArrayListExtra(SHARED_URIS, getSharedUris(currentActivity));
        }
    }

    public static void setForwardingMessageIds(Intent composeIntent, int[] messageIds) {
        composeIntent.putExtra(FORWARDED_MESSAGE_IDS, messageIds);
    }

    public static void setSharedUris(Intent composeIntent, ArrayList<Uri> uris) {
        composeIntent.putParcelableArrayListExtra(SHARED_URIS, uris);
    }


}
