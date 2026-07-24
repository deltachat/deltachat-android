package org.thoughtcrime.securesms.webrtc;

public final class SdpLinkToken {
  private SdpLinkToken() {}

  public static String of(String sdp) {
    if (sdp == null) return null;
    java.util.TreeSet<String> lines = new java.util.TreeSet<>();
    for (String raw : sdp.split("\\r?\\n")) {
      String line = raw.trim();
      if (line.startsWith("a=ice-ufrag:")
          || line.startsWith("a=ice-pwd:")
          || line.startsWith("a=fingerprint:")) {
        lines.add(line);
      }
    }
    return lines.isEmpty() ? null : String.join("\n", lines);
  }
}
