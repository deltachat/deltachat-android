package org.thoughtcrime.securesms.attachments;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public abstract class Attachment {

  @NonNull
  private final String  contentType;
  private       int     transferState;
  private final long    size;

  @Nullable
  private final String fileName;

  @Nullable
  private final String fastPreflightId;

  private final boolean voiceNote;
  private final int width;
  private final int height;

  private final boolean quote;

  public Attachment(@NonNull String contentType, int transferState, long size, @Nullable String fileName,
                    @Nullable String location, @Nullable String key, @Nullable String relay,
                    @Nullable byte[] digest, @Nullable String fastPreflightId, boolean voiceNote,
                    int width, int height, boolean quote)
  {
    this.contentType     = contentType;
    this.transferState   = transferState;
    this.size            = size;
    this.fileName        = fileName;
    this.fastPreflightId = fastPreflightId;
    this.voiceNote       = voiceNote;
    this.width           = width;
    this.height          = height;
    this.quote           = quote;
  }

  @Nullable
  public abstract Uri getDataUri();

  @Nullable
  public abstract Uri getThumbnailUri();

  public void setTransferState(int transferState) {
    this.transferState = transferState;
  }

  public int getTransferState() {
    return transferState;
  }

  public long getSize() {
    return size;
  }

  @Nullable
  public String getFileName() {
    return fileName;
  }

  @NonNull
  public String getContentType() {
    return contentType;
  }

  @Nullable
  public String getFastPreflightId() {
    return fastPreflightId;
  }

  public boolean isVoiceNote() {
    return voiceNote;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public boolean isQuote() {
    return quote;
  }
}
