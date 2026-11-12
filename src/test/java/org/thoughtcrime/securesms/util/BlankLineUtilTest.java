package org.thoughtcrime.securesms.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BlankLineUtilTest {

  private static final String Z = String.valueOf(BlankLineUtil.ZERO_WIDTH_SPACE);

  @Test
  public void leavesTextWithoutLineBreaksAlone() {
    assertEquals("hello", BlankLineUtil.preserveBlankLines("hello"));
    assertEquals("", BlankLineUtil.preserveBlankLines(""));
  }

  @Test
  public void leavesSingleLineBreaksAndSingleEmptyLinesAlone() {
    assertEquals("a\nb", BlankLineUtil.preserveBlankLines("a\nb"));
    assertEquals("a\n\nb", BlankLineUtil.preserveBlankLines("a\n\nb"));
  }

  @Test
  public void marksEveryLineOfARunOfEmptyLines() {
    assertEquals("a\n" + Z + "\n" + Z + "\nb", BlankLineUtil.preserveBlankLines("a\n\n\nb"));
    assertEquals(
        "a\n" + Z + "\n" + Z + "\n" + Z + "\nb", BlankLineUtil.preserveBlankLines("a\n\n \n\nb"));
  }

  @Test
  public void handlesSeveralRuns() {
    assertEquals(
        "a\n\nb\n" + Z + "\n" + Z + "\nc", BlankLineUtil.preserveBlankLines("a\n\nb\n\n\nc"));
  }

  @Test
  public void isIdempotent() {
    String once = BlankLineUtil.preserveBlankLines("a\n\n\n\nb");
    assertEquals(once, BlankLineUtil.preserveBlankLines(once));
  }
}
