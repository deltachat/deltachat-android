package org.thoughtcrime.securesms.mms;

import android.content.Context;
import android.net.Uri;

import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.util.MediaUtil;

public class GifSlide extends ImageSlide {

  public GifSlide(Context context, DcMsg dcMsg) {
    super(context, dcMsg);
  }

  public GifSlide(Context context, Uri uri, String fileName, long size, int width, int height) {
    super(context, constructAttachmentFromUri(context, uri, MediaUtil.IMAGE_GIF, size, width, height, uri, fileName, false));
  }

}
