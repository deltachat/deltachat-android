package org.thoughtcrime.securesms.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.junit.Test;

public class ConversionsTest {

  @Test
  public void longToByteArray_isBigEndian() {
    // Current behavior, keep stable
    assertArrayEquals(
        new byte[] {1, 2, 3, 4, 5, 6, 7, 8}, Conversions.longToByteArray(0x0102030405060708L));
  }

  @Test
  public void longToByteArray_edgeValues() {
    assertArrayEquals(new byte[8], Conversions.longToByteArray(0L));
    byte[] expected = new byte[8];
    Arrays.fill(expected, (byte) 0xff);
    assertArrayEquals(expected, Conversions.longToByteArray(-1L));
  }

  @Test
  public void longToByteArray_offsetLeavesOtherBytesUntouched() {
    byte[] buffer = new byte[10];
    Arrays.fill(buffer, (byte) 0x55);
    int written = Conversions.longToByteArray(buffer, 2, 0x0102030405060708L);
    assertEquals(8, written);
    assertArrayEquals(new byte[] {0x55, 0x55, 1, 2, 3, 4, 5, 6, 7, 8}, buffer);
  }
}
