package org.thoughtcrime.securesms.testutil;

import java.util.Locale;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public final class Screenshots extends TestWatcher {
  private String folder;
  private int counter;

  @Override
  protected void starting(Description description) {
    folder = description.getTestClass().getSimpleName() + "." + description.getMethodName();
    counter = 0;
  }

  public void capture(String name) {
    counter++;
    ScreenShot.save(String.format(Locale.ROOT, "%s/%02d_%s", folder, counter, name));
  }
}
