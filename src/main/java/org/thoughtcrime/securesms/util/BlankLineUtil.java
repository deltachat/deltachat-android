package org.thoughtcrime.securesms.util;

/**
 * Keeps runs of empty lines in outgoing messages intact.
 *
 * <p>The core simplifies received text and squeezes any number of consecutive empty lines down to a
 * single empty line. To keep the spacing the sender typed, every empty line inside a run of two or
 * more empty lines (between text) is replaced by an invisible zero-width space, which the core does
 * not treat as whitespace.
 */
public final class BlankLineUtil {

  public static final char ZERO_WIDTH_SPACE = '\u200B';

  private BlankLineUtil() {}

  public static String preserveBlankLines(String text) {
    if (text == null || text.indexOf('\n') == -1) return text;

    String[] lines = text.split("\n", -1);
    StringBuilder out = new StringBuilder(text.length() + 8);
    int i = 0;
    while (i < lines.length) {
      if (!isEmptyLine(lines[i])) {
        if (i > 0) out.append('\n');
        out.append(lines[i]);
        i++;
        continue;
      }
      int runEnd = i;
      while (runEnd < lines.length && isEmptyLine(lines[runEnd])) runEnd++;
      int runLength = runEnd - i;
      // a single empty line survives anyway; leading/trailing runs are stripped by the core
      boolean betweenText = i > 0 && runEnd < lines.length;
      for (int j = i; j < runEnd; j++) {
        if (j > 0) out.append('\n');
        out.append(runLength >= 2 && betweenText ? String.valueOf(ZERO_WIDTH_SPACE) : lines[j]);
      }
      i = runEnd;
    }
    return out.toString();
  }

  private static boolean isEmptyLine(String line) {
    for (int k = 0; k < line.length(); k++) {
      if (!Character.isWhitespace(line.charAt(k))) return false;
    }
    return true;
  }
}
