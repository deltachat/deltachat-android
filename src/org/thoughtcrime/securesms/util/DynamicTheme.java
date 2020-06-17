package org.thoughtcrime.securesms.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import org.thoughtcrime.securesms.R;

public class DynamicTheme {

  public static final String DARK  = "dark";
  public static final String LIGHT = "light";
  public static final String PURPLE = "purple";
  public static final String GREEN = "green";
  public static final String BLUE = "blue";
  public static final String RED = "red";

  private int currentTheme;

  public void onCreate(Activity activity) {
    currentTheme = getSelectedTheme(activity);
    activity.setTheme(currentTheme);
  }

  public void onResume(Activity activity) {
    if (currentTheme != getSelectedTheme(activity)) {
      Intent intent = activity.getIntent();
      activity.finish();
      OverridePendingTransition.invoke(activity);
      activity.startActivity(intent);
      OverridePendingTransition.invoke(activity);
    }
  }

  protected int getSelectedTheme(Activity activity) {
    String theme = Prefs.getTheme(activity);

    if (theme.equals(DARK)) return R.style.TextSecure_DarkTheme;
    if (theme.equals(PURPLE)) return R.style.TextSecure_PurpleTheme;
    if (theme.equals(GREEN)) return R.style.TextSecure_GreenTheme;
    if (theme.equals(BLUE)) return R.style.TextSecure_BlueTheme;
    if (theme.equals(RED)) return R.style.TextSecure_RedTheme;

    return R.style.TextSecure_LightTheme;
  }

  public static boolean isDarkTheme(Context context) {
    return Prefs.getTheme(context).equals(DARK);
  }

  private static final class OverridePendingTransition {
    static void invoke(Activity activity) {
      activity.overridePendingTransition(0, 0);
    }
  }
}
