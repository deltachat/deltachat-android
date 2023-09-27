package org.thoughtcrime.securesms.util;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import androidx.annotation.RequiresApi;
import androidx.core.os.ConfigurationCompat;
import androidx.core.view.ViewCompat;

import android.text.TextUtils;
import android.util.Log;

import java.util.Locale;

public class DynamicLanguage {

  private static final String TAG = DynamicLanguage.class.getSimpleName();

  private static final String DEFAULT = "zz";

  private Locale currentLocale;

  public void onCreate(Activity activity) {
    currentLocale = getSelectedLocale(activity);
    setContextLocale(activity, currentLocale);
  }

  public void onResume(Activity activity) {
    if (!getSelectedLocale(activity).equals(currentLocale)) {
      Intent intent = activity.getIntent();
      activity.finish();
      OverridePendingTransition.invoke(activity);
      activity.startActivity(intent);
      OverridePendingTransition.invoke(activity);
    }
  }

  public void updateServiceLocale(Service service) {
    currentLocale = getSelectedLocale(service);
    setContextLocale(service, currentLocale);
  }

  public Locale getCurrentLocale() {
    return currentLocale;
  }

  public static int getLayoutDirection(Context context) {
    if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
      Configuration configuration = context.getResources().getConfiguration();
      return configuration.getLayoutDirection();
    }
    return ViewCompat.LAYOUT_DIRECTION_LTR;
  }

  public static void setContextLocale(Context context, Locale selectedLocale) {
    Configuration configuration = context.getResources().getConfiguration();

    if (!configuration.locale.equals(selectedLocale)) {
      configuration.locale = selectedLocale;
      if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
        configuration.setLayoutDirection(selectedLocale);
      }
      context.getResources().updateConfiguration(configuration,
                                                 context.getResources().getDisplayMetrics());
    }
  }

  private static Locale getActivityLocale(Activity activity) {
    return activity.getResources().getConfiguration().locale;
  }

  // Beware that Locale.getDefault() returns the locale the App was STARTED in, not the locale of the system.
  // It just happens to be the same for the majority of use cases.
  private static Locale getDefaultLocale() {
    Locale locale = null;
    try {
      locale = ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration()).get(0);
    } catch(Exception e) {
      Log.e(TAG, "could not determine the system locale.", e);
    }
    return locale != null ? locale : Locale.getDefault();
  }

  public static Locale getSelectedLocale(Context context) {
    String language[] = TextUtils.split(Prefs.getLanguage(context), "_");

    if (language[0].equals(DEFAULT)) {
      return getDefaultLocale();
    } else if (language.length == 2) {
      return new Locale(language[0], language[1]);
    } else {
      return new Locale(language[0]);
    }
  }

  private static final class OverridePendingTransition {
    static void invoke(Activity activity) {
      activity.overridePendingTransition(0, 0);
    }
  }
}
