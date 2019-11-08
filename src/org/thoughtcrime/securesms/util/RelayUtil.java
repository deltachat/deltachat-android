package org.thoughtcrime.securesms.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;

import java.util.ArrayList;

import static org.thoughtcrime.securesms.ConversationActivity.TEXT_EXTRA;

public class RelayUtil {
    private static final String FORWARDED_MESSAGE_IDS   = "forwarded_message_ids";
    private static final String SHARED_URIS             = "shared_uris";
    private static final String IS_SHARING              = "is_sharing";
    public static final int REQUEST_RELAY = 100;
    private static final String DIRECT_SHARING_CHAT_ID = "direct_sharing_chat_id";

    public static boolean isRelayingMessageContent(Activity activity) {
        return isForwarding(activity) || isSharing(activity);
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
            return activity.getIntent().getBooleanExtra(IS_SHARING, false);
        } catch (NullPointerException npe) {
            return false;
        }
    }

    public static boolean isDirectSharing(Activity activity) {
        try {
            return activity.getIntent().getIntExtra(DIRECT_SHARING_CHAT_ID, -1) != -1;
        } catch (NullPointerException npe) {
            return false;
        }
    }

    public static int getDirectSharingChatId(Activity activity) {
        try {
            return activity.getIntent().getIntExtra(DIRECT_SHARING_CHAT_ID, -1);
        } catch (NullPointerException npe) {
            return -1;
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

    public static String getSharedText(Activity activity) {
        try {
            return activity.getIntent().getStringExtra(TEXT_EXTRA);
        } catch (NullPointerException npe) {
            return null;
        }
    }

    public static void resetRelayingMessageContent(Activity activity) {
        try {
            activity.getIntent().removeExtra(FORWARDED_MESSAGE_IDS);
            activity.getIntent().removeExtra(SHARED_URIS);
            activity.getIntent().removeExtra(IS_SHARING);
            activity.getIntent().removeExtra(DIRECT_SHARING_CHAT_ID);
            activity.getIntent().removeExtra(TEXT_EXTRA);
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }
    }

    public static void acquireRelayMessageContent(Activity currentActivity, @NonNull Intent newActivityIntent) {
        if (isForwarding(currentActivity)) {
            newActivityIntent.putExtra(FORWARDED_MESSAGE_IDS, getForwardedMessageIDs(currentActivity));
        } else if (isSharing(currentActivity)) {
            newActivityIntent.putExtra(IS_SHARING, true);
            if (isDirectSharing(currentActivity)) {
                newActivityIntent.putExtra(DIRECT_SHARING_CHAT_ID, getDirectSharingChatId(currentActivity));
            }
            if (getSharedUris(currentActivity) != null) {
                newActivityIntent.putParcelableArrayListExtra(SHARED_URIS, getSharedUris(currentActivity));
            }
            if (getSharedText(currentActivity) != null) {
                newActivityIntent.putExtra(TEXT_EXTRA, getSharedText(currentActivity));
            }
        }
    }

    public static void setForwardingMessageIds(Intent composeIntent, int[] messageIds) {
        composeIntent.putExtra(FORWARDED_MESSAGE_IDS, messageIds);
    }

    public static void setSharedUris(Intent composeIntent, ArrayList<Uri> uris) {
        composeIntent.putParcelableArrayListExtra(SHARED_URIS, uris);
        composeIntent.putExtra(IS_SHARING, true);
    }

    public static void setSharedText(Intent composeIntent, String text) {
        composeIntent.putExtra(TEXT_EXTRA, text);
        composeIntent.putExtra(IS_SHARING, true);
    }

    public static void setDirectSharing(Intent composeIntent, int chatId) {
        composeIntent.putExtra(DIRECT_SHARING_CHAT_ID, chatId);
    }


}
