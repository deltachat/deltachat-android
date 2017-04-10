/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *                        (C) 2013-2016 Nikolai Kudashov
 *                           (C) 2017 Björn Petersen
 *                    Contact: r10s@b44t.com, http://b44t.com
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see http://www.gnu.org/licenses/ .
 *
 ******************************************************************************/


package com.b44t.messenger.browser;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.b44t.messenger.FileLog;
import com.b44t.ui.LaunchActivity;

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

        }
    }

    public static boolean isInternalUrl(String url) {
        return isInternalUri(Uri.parse(url));
    }

    public static boolean isInternalUri(Uri uri) {
        return false;
    }
}
