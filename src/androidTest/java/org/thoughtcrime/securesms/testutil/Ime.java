package org.thoughtcrime.securesms.testutil;

import android.app.Activity;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import androidx.annotation.IdRes;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

public final class Ime {
  private static final String TAG = "Ime";

  private Ime() {}

  /**
   * Asks the system to show the soft keyboard for the given view using the documented way. The
   * request is scheduled after the window gains focus.
   */
  public static void requestFor(@IdRes int viewId) {
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              for (Activity a :
                  ActivityLifecycleMonitorRegistry.getInstance()
                      .getActivitiesInStage(Stage.RESUMED)) {
                View view = a.findViewById(viewId);
                if (view != null) {
                  WindowCompat.getInsetsController(a.getWindow(), view)
                      .show(WindowInsetsCompat.Type.ime());
                }
              }
            });
  }

  public static void waitUntilVisibleBestEffort(long limit) {
    long start = SystemClock.uptimeMillis();
    while (SystemClock.uptimeMillis() - start < limit) {
      if (isVisible()) return;
      SystemClock.sleep(100);
    }
    Log.w(TAG, "Soft keyboard not visible within " + limit + " ms");
  }

  public static boolean isVisible() {
    boolean[] visible = new boolean[1];
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              for (Activity a :
                  ActivityLifecycleMonitorRegistry.getInstance()
                      .getActivitiesInStage(Stage.RESUMED)) {
                WindowInsetsCompat insets =
                    ViewCompat.getRootWindowInsets(a.getWindow().getDecorView());
                if (insets != null && insets.isVisible(WindowInsetsCompat.Type.ime())) {
                  visible[0] = true;
                }
              }
            });
    return visible[0];
  }

  public static void disableStylusHandwriting() {
    Shell.run("settings put secure stylus_handwriting_enabled 0");
    Shell.run("am force-stop com.google.android.inputmethod.latin");
  }
}
