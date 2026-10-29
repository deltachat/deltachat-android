package org.thoughtcrime.securesms.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.security.SecureRandom;
import org.junit.Test;

public class HexTest {

  @Test
  public void toStringCondensed_isLowercaseAndZeroPadded() {
    assertEquals("01abff", Hex.toStringCondensed(new byte[] {0x01, (byte) 0xab, (byte) 0xff}));
  }

  @Test
  public void roundTrip_databaseSecretStaysByteIdentical() throws IOException {
    byte[] secret = new byte[32];
    new SecureRandom().nextBytes(secret);
    assertArrayEquals(secret, Hex.fromStringCondensed(Hex.toStringCondensed(secret)));
  }

  @Test
  public void fromStringCondensed_acceptsUppercase() throws IOException {
    assertArrayEquals(new byte[] {(byte) 0xab, (byte) 0xff}, Hex.fromStringCondensed("ABFF"));
  }

  @Test
  public void emptyInput() throws IOException {
    assertEquals("", Hex.toStringCondensed(new byte[0]));
    assertArrayEquals(new byte[0], Hex.fromStringCondensed(""));
  }

  @Test
  public void fromStringCondensed_oddLengthThrows() {
    assertThrows(IOException.class, () -> Hex.fromStringCondensed("abc"));
  }

  @Test
  public void fromStringCondensed_invalidCharactersSilentlyDecodeToGarbage() throws IOException {
    // Current behavior is invalid input decodes to 0xFF instead of throwing. So a corrupted
    // stored secret fails in DcContext.open(), not when decoding.
    assertArrayEquals(new byte[] {(byte) 0xff}, Hex.fromStringCondensed("xy"));
  }
}
