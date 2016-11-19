/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package org.telegram.messenger.browser;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.telegram.messenger.FileLog;
import org.telegram.ui.LaunchActivity;

public class Browser {

    public static void openUrl(Context context, String url) {
        if (context == null || url == null) {
            return;
        }
        openUrl(context, Uri.parse(url));
    }

    public static void openUrl(Context context, Uri uri) {
        if (context == null || uri == null) {
            return;
        }

        try {
            boolean internalUri = isInternalUri(uri);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if (internalUri) {
                ComponentName componentName = new ComponentName(context.getPackageName(), LaunchActivity.class.getName());
                intent.setComponent(componentName);
            }
            intent.putExtra(android.provider.Browser.EXTRA_APPLICATION_ID, context.getPackageName());
            context.startActivity(intent);
        } catch (Exception e) {
            FileLog.e("messenger", e);
        }
    }

    public static boolean isInternalUrl(String url) {
        return isInternalUri(Uri.parse(url));
    }

    public static boolean isInternalUri(Uri uri) {
        return false;
    }
}
