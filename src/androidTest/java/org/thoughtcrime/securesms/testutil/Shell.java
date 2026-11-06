package org.thoughtcrime.securesms.testutil;

import android.os.ParcelFileDescriptor;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class Shell {
  private Shell() {}

  public static String run(String command) {
    ParcelFileDescriptor pfd =
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
    try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(pfd)) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int n;
      while ((n = in.read(buffer)) != -1) {
        out.write(buffer, 0, n);
      }
      return out.toString("UTF-8").trim();
    } catch (IOException e) {
      throw new RuntimeException("Shell command failed: " + command, e);
    }
  }
}
