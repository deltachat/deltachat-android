package org.thoughtcrime.securesms.util;

import android.app.Activity;
import android.content.Context;
import android.os.Vibrator;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

public class ServiceUtil {
  public static InputMethodManager getInputMethodManager(Context context) {
    return (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
  }

  public static WindowManager getWindowManager(Context context) {
    return (WindowManager) context.getSystemService(Activity.WINDOW_SERVICE);
  }

  public static Vibrator getVibrator(Context context) {
    return  (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
  }
}
