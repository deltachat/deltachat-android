package org.thoughtcrime.securesms.util;

import android.text.TextUtils;

import java.io.FileDescriptor;
import java.nio.charset.StandardCharsets;

public class FileUtils {

  public static native int getFileDescriptorOwner(FileDescriptor fileDescriptor);

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
    if ((0x00 <= c && c <= 0x1f)) {
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
      case '\0':
      case '|':
      case 0x7F:
        return false;
      default:
        return true;
    }
  }
}
