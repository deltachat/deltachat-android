package org.thoughtcrime.securesms.connect;

import android.content.Context;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.ApplicationContext;

public class DcContextHelper {

    public static DcContext getDcContext(Context context) {
        return ApplicationContext.getInstance(context).dcContext;
    }

    public static boolean isConfigured(Context context) {
        DcContext dcContext = getDcContext(context);
        return dcContext.isConfigured() == 1;
    }

}
