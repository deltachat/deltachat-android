package org.thoughtcrime.securesms.video.editor;

public class VideoEditedInfo {
  public String originalPath;
  public float  originalDurationMs;
  public long   originalAudioBytes;
  public int    originalRotationValue;
  public int    originalWidth;
  public int    originalHeight;
  public int    originalVideoBitrate;

  public long   startTime;
  public long   endTime;
  public int    rotationValue;

  public int    resultWidth;
  public int    resultHeight;
  public int    resultVideoBitrate;

  public int    estimatedBytes;
}