package org.thoughtcrime.securesms.testutil;

import android.util.Log;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;
import org.junit.rules.ExternalResource;

public final class SystemUiDemoMode extends ExternalResource {
  private final boolean showNotificationIcons;

  private SystemUiDemoMode(boolean showNotificationIcons) {
    this.showNotificationIcons = showNotificationIcons;
  }

  public static SystemUiDemoMode clean() {
    return new SystemUiDemoMode(false);
  }

  public static SystemUiDemoMode withNotificationIcons() {
    return new SystemUiDemoMode(true);
  }

  @Override
  protected void before() {
    Shell.run("settings put global sysui_demo_allowed 1");
    demo("enter");
    demo("battery -e level 100 -e plugged false");
    demo("network -e wifi show -e level 4 -e fully true");
    demo("network -e mobile hide");
    demo("notifications -e visible " + showNotificationIcons);
    demo("clock -e hhmm 1200");

    UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    boolean applied =
        device.wait(Until.hasObject(By.pkg("com.android.systemui").text("12:00")), 10_000);
    if (!applied) {
      Log.w("SystemUiDemoMode", "Demo mode not confirmed within 10s; status bar may be unclean");
    }
  }

  @Override
  protected void after() {
    demo("exit");
  }

  private static void demo(String args) {
    Shell.run("am broadcast -a com.android.systemui.demo -e command " + args);
  }
}
