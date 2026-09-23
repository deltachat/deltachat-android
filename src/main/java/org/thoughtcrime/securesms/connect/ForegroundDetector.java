package org.thoughtcrime.securesms.connect;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.service.SendingNotifier;
import org.thoughtcrime.securesms.util.Util;

public class ForegroundDetector implements Application.ActivityLifecycleCallbacks {
  private static final String TAG = "ForegroundDetector";
  private int refs = 0;
  private static ForegroundDetector Instance = null;
  private final ApplicationContext application;

  public static ForegroundDetector getInstance() {
    return Instance;
  }

  public ForegroundDetector(ApplicationContext application) {
    Instance = this;
    this.application = application;
    application.registerActivityLifecycleCallbacks(this);
  }

  public boolean isForeground() {
    return refs > 0;
  }

  public boolean isBackground() {
    return refs == 0;
  }

  @Override
  public void onActivityStarted(@NonNull Activity activity) {
    if (refs == 0) {
      Log.i(TAG, "++++++++++++++++++ first onActivityStarted() ++++++++++++++++++");
      DcHelper.getAccounts(application).startIo();
      if (DcHelper.isNetworkConnected(application)) {
        new Thread(
                () -> {
                  Log.i(TAG, "calling maybeNetwork()");
                  DcHelper.getAccounts(application).maybeNetwork();
                  Log.i(TAG, "maybeNetwork() returned");
                })
            .start();
      }
    }

    refs++;
  }

  @Override
  public void onActivityStopped(@NonNull Activity activity) {
    if (refs <= 0) {
      Log.w(TAG, "invalid call to onActivityStopped()");
      return;
    }

    refs--;

    if (refs == 0) {
      Log.i(TAG, "++++++++++++++++++ last onActivityStopped() ++++++++++++++++++");
      // Check if the app has to keep running for unfinished sending;
      // delay for activity restarts on configuration changes.
      Util.runOnMainDelayed(
          () -> {
            if (isBackground()) {
              SendingNotifier.onAppBackgrounded(application);
            }
          },
          1000);
    }
  }

  @Override
  public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {}

  @Override
  public void onActivityResumed(@NonNull Activity activity) {}

  @Override
  public void onActivityPaused(@NonNull Activity activity) {
    // pause/resume will also be called when the app is partially covered by a dialog
  }

  @Override
  public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

  @Override
  public void onActivityDestroyed(@NonNull Activity activity) {}
}
