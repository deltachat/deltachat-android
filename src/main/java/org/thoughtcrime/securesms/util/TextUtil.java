package org.thoughtcrime.securesms.util;

import androidx.annotation.NonNull;
import androidx.core.text.BidiFormatter;

public class TextUtil {
  @NonNull
  public static String bidiAppend(String text, String suffix) {
    return BidiFormatter.getInstance().unicodeWrap(text) + suffix;
  }

  @NonNull
  public static String markAsExternal(String text) {
    return bidiAppend(text, " ↗");
  }
}
