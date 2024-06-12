package org.thoughtcrime.securesms.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatDelegate;

import org.thoughtcrime.securesms.R;

public class DynamicTheme {

  public static final String DARK   = "dark";
  public static final String LIGHT  = "light";
  public static final String SYSTEM = "system";

  //private static boolean isDarkTheme;

  private int currentTheme;

  public void onCreate(Activity activity) {
    //boolean wasDarkTheme = isDarkTheme;

    currentTheme = getSelectedTheme(activity);
    //isDarkTheme  = isDarkTheme(activity);

    activity.setTheme(currentTheme);

    // In case you introduce a CachedInflater and there are problems with the dark mode, uncomment
    // this line and the line in onResume():
    //if (isDarkTheme != wasDarkTheme) {
      //CachedInflater.from(activity).clear();
    //}
  }

  public void onResume(Activity activity) {
    if (currentTheme != getSelectedTheme(activity)) {
      Intent intent = activity.getIntent();
      activity.finish();
      OverridePendingTransition.invoke(activity);
      activity.startActivity(intent);
      OverridePendingTransition.invoke(activity);
      //CachedInflater.from(activity).clear();
    }
  }

  public static void setDefaultDayNightMode(@NonNull Context context) {
    String theme = Prefs.getTheme(context);

    if (theme.equals(SYSTEM)) {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    } else if (DynamicTheme.isDarkTheme(context)) {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    } else {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    //CachedInflater.from(context).clear();
  }


  private @StyleRes int getSelectedTheme(Activity activity) {
    if (isDarkTheme(activity)) {
      return getDarkThemeStyle();
    } else {
      return getLightThemeStyle();
    }
  }

  protected @StyleRes int getLightThemeStyle() {
    return R.style.TextSecure_LightTheme;
  }

  protected @StyleRes int getDarkThemeStyle() {
    return R.style.TextSecure_DarkTheme;
  }

  static boolean systemThemeAvailable() {
    return Build.VERSION.SDK_INT >= 29;
  }

  /**
   * Takes the system theme into account.
   */
  public static boolean isDarkTheme(@NonNull Context context) {
    String theme = Prefs.getTheme(context);

    if (theme.equals(SYSTEM) && systemThemeAvailable()) {
      return isSystemInDarkTheme(context);
    } else {
      return theme.equals(DARK);
    }
  }

  // return a checkmark emoji that fits to the background of the selected theme.
  public static String getCheckmarkEmoji(@NonNull Context context) {
    return isDarkTheme(context) ? "✅" /*blue, white or white in a box*/ : "✔️" /*blue or black*/;
  }

  private static boolean isSystemInDarkTheme(@NonNull Context context) {
    return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
  }

  private static final class OverridePendingTransition {
    static void invoke(Activity activity) {
      activity.overridePendingTransition(0, 0);
    }
  }
}
