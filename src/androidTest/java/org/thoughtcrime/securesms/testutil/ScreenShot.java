package org.thoughtcrime.securesms.testutil;

import android.graphics.Bitmap;
import androidx.test.core.graphics.BitmapStorage;
import androidx.test.espresso.Espresso;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.IOException;

public final class ScreenShot {
  private ScreenShot() {}

  static void save(String fileName) {
    Espresso.onIdle();
    // Use UiAutomation to capture. DeviceCapture.takeScreenshot() is likely designed for Compose
    // as it force-redraws every window and times out when one is hidden
    // (checkandroid/android-test#2273).
    Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
    if (bitmap == null) {
      throw new IllegalStateException("UiAutomation returned no screenshot for " + fileName);
    }
    try {
      BitmapStorage.writeToTestStorage(bitmap, fileName);
    } catch (IOException e) {
      throw new RuntimeException("Screenshot failed: " + fileName, e);
    } finally {
      bitmap.recycle();
    }
  }
}
