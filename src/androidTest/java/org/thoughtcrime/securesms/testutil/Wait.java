package org.thoughtcrime.securesms.testutil;

import android.app.Activity;
import android.os.SystemClock;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

public final class Wait {
  private Wait() {}

  public static <T extends Activity> T forActivity(
      Class<T> activityClass, long timeout, Screenshots shots) {
    long deadline = SystemClock.uptimeMillis() + timeout;
    while (SystemClock.uptimeMillis() < deadline) {
      Activity[] found = new Activity[1];
      InstrumentationRegistry.getInstrumentation()
          .runOnMainSync(
              () -> {
                for (Activity a :
                    ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED)) {
                  if (activityClass.isInstance(a)) found[0] = a;
                }
              });
      if (found[0] != null) return activityClass.cast(found[0]);
      SystemClock.sleep(250);
    }
    shots.capture("TIMEOUT_waiting_for_" + activityClass.getSimpleName());
    throw new AssertionError(
        activityClass.getSimpleName() + " not resumed within " + timeout + " ms");
  }
}
