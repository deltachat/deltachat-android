package org.thoughtcrime.securesms.attachments;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.PartAuthority;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.Util;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class Attachment {

  @NonNull
  private final String  contentType;
  private       int     transferState;
  private final long    size;

  @Nullable
  private final String fileName;

  @Nullable
  private final String  location;

  @Nullable
  private final String fastPreflightId;

  private final boolean voiceNote;
  private final int width;
  private final int height;

  public Attachment(@NonNull String contentType, int transferState, long size, @Nullable String fileName,
                    @Nullable String location, @Nullable String fastPreflightId, boolean voiceNote,
                    int width, int height)
  {
    this.contentType     = contentType;
    this.transferState   = transferState;
    this.size            = size;
    this.fileName        = fileName;
    this.location        = location;
    this.fastPreflightId = fastPreflightId;
    this.voiceNote       = voiceNote;
    this.width           = width;
    this.height          = height;
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
  public String getLocation() {
    return location;
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

  public String getRealPath(Context context) {
    try {
      // get file in the blobdir as `<blobdir>/<name>[-<uniqueNumber>].<ext>`
      String filename = getFileName();
      String ext = "";
      if(filename==null) {
        filename = new SimpleDateFormat("yyyy-MM-dd-HH-mm").format(new Date());
        ext = "." + MediaUtil.getExtensionFromMimeType(getContentType());
      }
      else {
        int i = filename.lastIndexOf(".");
        if(i>=0) {
          ext = filename.substring(i);
          filename = filename.substring(0, i);
        }
      }
      String path = DcHelper.getBlobdirFile(DcHelper.getContext(context), filename, ext);

      // copy content to this file
        InputStream inputStream = PartAuthority.getAttachmentStream(context, getDataUri());
        OutputStream outputStream = new FileOutputStream(path);
        Util.copy(inputStream, outputStream);

        return path;
    }
    catch(Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
