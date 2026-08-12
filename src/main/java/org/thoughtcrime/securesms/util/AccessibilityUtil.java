package org.thoughtcrime.securesms.util;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class AccessibilityUtil {

  private AccessibilityUtil() {}

  public static boolean areAnimationsDisabled(Context context) {
    if (context == null) {
      Log.e("AccessibilityUtil", "animationsDisabled: context was null");
      return false;
    }
    return Settings.Global.getFloat(
            context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1)
        == 0f;
  }

  public static void append(@NonNull StringBuilder sb, @Nullable CharSequence text) {
    if (text == null || text.length() == 0) return;
    if (sb.length() > 0) sb.append(", ");
    sb.append(text);
  }

  public static @Nullable String toDescription(@NonNull StringBuilder sb) {
    return sb.length() == 0 ? null : sb.toString();
  }
}
