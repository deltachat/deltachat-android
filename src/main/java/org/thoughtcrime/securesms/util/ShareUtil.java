package org.thoughtcrime.securesms.util;

import static org.thoughtcrime.securesms.ConversationActivity.TEXT_EXTRA;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class ShareUtil {
    private static final String FORWARDED_MESSAGE_IDS   = "forwarded_message_ids";
    private static final String SHARED_URIS             = "shared_uris";
    private static final String SHARED_CONTACT_ID       = "shared_contact_id";
    private static final String IS_SHARING              = "is_sharing";
    private static final String IS_FROM_WEBXDC          = "is_from_webxdc";
    private static final String SHARED_TITLE           = "shared_title";
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

    public static boolean isFromWebxdc(Activity activity) {
        try {
            return activity.getIntent().getBooleanExtra(IS_FROM_WEBXDC, false);
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

    static int[] getForwardedMessageIDs(Activity activity) {
        try {
            return activity.getIntent().getIntArrayExtra(FORWARDED_MESSAGE_IDS);
        } catch (NullPointerException npe) {
            return null;
        }
    }

    public static @NonNull ArrayList<Uri> getSharedUris(Activity activity) {
        if (activity != null) {
            Intent i = activity.getIntent();
            if (i != null) {
                ArrayList<Uri> uris = i.getParcelableArrayListExtra(SHARED_URIS);
                if (uris != null) return uris;
            }
        }
        return new ArrayList<>();
    }


    public static int getSharedContactId(Activity activity) {
      try {
        return activity.getIntent().getIntExtra(SHARED_CONTACT_ID, 0);
      } catch(Exception e) {
        e.printStackTrace();
        return 0;
      }
    }

    public static String getSharedText(Activity activity) {
        try {
            return activity.getIntent().getStringExtra(TEXT_EXTRA);
        } catch (NullPointerException npe) {
            return null;
        }
    }

    public static String getSharedTitle(Activity activity) {
        try {
            return activity.getIntent().getStringExtra(SHARED_TITLE);
        } catch (NullPointerException npe) {
            return null;
        }
    }


    public static void resetRelayingMessageContent(Activity activity) {
        try {
            activity.getIntent().removeExtra(FORWARDED_MESSAGE_IDS);
            activity.getIntent().removeExtra(SHARED_URIS);
            activity.getIntent().removeExtra(SHARED_CONTACT_ID);
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
            if (!getSharedUris(currentActivity).isEmpty()) {
                newActivityIntent.putParcelableArrayListExtra(SHARED_URIS, getSharedUris(currentActivity));
            }
            if (getSharedContactId(currentActivity) != 0) {
                newActivityIntent.putExtra(SHARED_CONTACT_ID, getSharedContactId(currentActivity));
            }
            if (getSharedText(currentActivity) != null) {
                newActivityIntent.putExtra(TEXT_EXTRA, getSharedText(currentActivity));
            }
        }
    }

    public static void setForwardingMessageIds(Intent composeIntent, int[] messageIds) {
        composeIntent.putExtra(FORWARDED_MESSAGE_IDS, messageIds);
    }

    public static void setIsFromWebxdc(Intent composeIntent, boolean fromWebxdc) {
        composeIntent.putExtra(IS_FROM_WEBXDC, fromWebxdc);
        composeIntent.putExtra(IS_SHARING, true);
    }

    public static void setSharedUris(Intent composeIntent, ArrayList<Uri> uris) {
        composeIntent.putParcelableArrayListExtra(SHARED_URIS, uris);
        composeIntent.putExtra(IS_SHARING, true);
    }

    public static void setSharedText(Intent composeIntent, String text) {
        composeIntent.putExtra(TEXT_EXTRA, text);
        composeIntent.putExtra(IS_SHARING, true);
    }

    public static void setSharedContactId(Intent composeIntent, int contactId) {
      composeIntent.putExtra(SHARED_CONTACT_ID, contactId);
      composeIntent.putExtra(IS_SHARING, true);
    }

    public static void setSharedTitle(Intent composeIntent, String text) {
        composeIntent.putExtra(SHARED_TITLE, text);
    }

    public static void setDirectSharing(Intent composeIntent, int chatId) {
        composeIntent.putExtra(DIRECT_SHARING_CHAT_ID, chatId);
    }

}
