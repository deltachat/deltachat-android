package org.thoughtcrime.securesms.util;

import android.text.TextUtils;

public class FileUtils {

  public static String sanitizeFilename(String name) {
    if (TextUtils.isEmpty(name) || ".".equals(name) || "..".equals(name)) {
      return "(invalid)";
    }
    final StringBuilder res = new StringBuilder(name.length());
    for (int i = 0; i < name.length(); i++) {
      final char c = name.charAt(i);
      if (isValidFilenameChar(c)) {
        res.append(c);
      } else {
        res.append('_');
      }
    }
    return res.toString();
  }

  private static boolean isValidFilenameChar(char c) {
    if (c <= 0x1f) {
      return false;
    }
    switch (c) {
      case '"':
      case '*':
      case '/':
      case ':':
      case '<':
      case '>':
      case '?':
      case '\\':
      case '|':
      case 0x7F:
        return false;
      default:
        return true;
    }
  }
}
