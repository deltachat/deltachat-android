package org.thoughtcrime.securesms.mms;


import android.content.Context;
import android.support.annotation.NonNull;

import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.attachments.Attachment;

public class MmsSlide extends ImageSlide {

  public MmsSlide(@NonNull Context context, @NonNull DcMsg dcMsg) {
    super(context, dcMsg);
  }

  public MmsSlide(@NonNull Context context, @NonNull Attachment attachment) {
    super(context, attachment);
  }
}
