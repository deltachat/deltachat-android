package org.thoughtcrime.securesms.mms;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.attachments.DcAttachment;


public class StickerSlide extends Slide {

  public static final int WIDTH  = 512;
  public static final int HEIGHT = 512;

  public StickerSlide(@NonNull Context context, @NonNull DcMsg dcMsg) {
    super(context, new DcAttachment(dcMsg));
  }

  public StickerSlide(@NonNull Context context, @NonNull Uri uri,
                      long size, @NonNull String contentType)
  {
    super(context, constructAttachmentFromUri(context, uri, contentType, size, WIDTH, HEIGHT, uri, null, false));
  }

  @Override
  public boolean hasSticker() {
    return true;
  }
}
