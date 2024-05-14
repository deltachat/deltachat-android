package org.thoughtcrime.securesms.mms;


import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.b44t.messenger.DcMsg;

public class VcardSlide extends DocumentSlide {

  public VcardSlide(Context context, DcMsg dcMsg) {
    super(context, dcMsg);
  }

  public VcardSlide(@NonNull Context context, @NonNull Uri uri, long size, @Nullable String fileName) {
    super(context, uri, "text/vcard", size, fileName);
  }

  @Override
  public boolean isVcard() {
    return true;
  }
}
