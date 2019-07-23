package org.thoughtcrime.securesms.util;

import android.content.Context;
import android.util.Log;

import me.leolin.shortcutbadger.ShortcutBadger;

public class BadgeUtil {
    public static void update(Context context, int count) {
        try {
            if (count == 0) ShortcutBadger.removeCount(context);
            else            ShortcutBadger.applyCount(context, count);
        } catch (RuntimeException t) {
            // NOTE :: I don't totally trust this thing, so I'm catching
            // everything.
            Log.w("MessageNotifier", t);
        }
    }
}
