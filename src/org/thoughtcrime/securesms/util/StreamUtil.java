package org.thoughtcrime.securesms.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamUtil {

  public static long copy(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[64 * 1024];
    int read;
    long total = 0;

    while ((read = in.read(buffer)) != -1) {
      out.write(buffer, 0, read);
      total += read;
    }

    in.close();
    out.close();

    return total;
  }

}
