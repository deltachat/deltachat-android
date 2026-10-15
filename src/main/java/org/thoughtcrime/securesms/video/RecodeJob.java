package org.thoughtcrime.securesms.video;

import com.b44t.messenger.DcMsg;

public class RecodeJob {

  public int chatId;
  public DcMsg msg;
  public String inputPath;
  public String outputPath;
  public long maxBytes;
  public long originalBytes;
  public long durationMs;
  public boolean hasAudio;
  public int dispW;
  public int dispH;
  public int lastProgress;
  public boolean fromComposer;
  public boolean compatOnly;

  public int[] sizeForPass(boolean low) {
    int w = dispW;
    int h = dispH;
    if (!compatOnly) {
      int maxSide = low ? VideoRecodeManager.LOW_MAX_SIDE : VideoRecodeManager.MEDIUM_MAX_SIDE;
      if (w > maxSide || h > maxSide) {
        float scale = w > h ? (float) maxSide / w : (float) maxSide / h;
        w = Math.round(w * scale);
        h = Math.round(h * scale);
      }
    }
    // encoders require even dimensions
    return new int[] {Math.max(2, w & ~1), Math.max(2, h & ~1)};
  }

  public int videoBitrateForPass(boolean low) {
    int total = low ? VideoRecodeManager.LOW_BPS : VideoRecodeManager.MEDIUM_BPS;
    return Math.max(100_000, total - (hasAudio ? VideoRecodeManager.AUDIO_BPS : 0));
  }
}
