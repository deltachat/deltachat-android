package org.thoughtcrime.securesms.mms;

import android.content.Context;

import androidx.annotation.NonNull;

import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.attachments.DcAttachment;


public class StickerSlide extends Slide {

  public StickerSlide(@NonNull Context context, @NonNull DcMsg dcMsg) {
    super(context, new DcAttachment(dcMsg));
  }

  @Override
  public boolean hasSticker() {
    return true;
  }
}
