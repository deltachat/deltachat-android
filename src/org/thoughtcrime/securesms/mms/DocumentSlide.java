package org.thoughtcrime.securesms.mms;


import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.attachments.Attachment;
import org.thoughtcrime.securesms.attachments.DcAttachment;
import org.thoughtcrime.securesms.util.StorageUtil;

public class DocumentSlide extends Slide {
  public Integer dcMsgId;

  public DocumentSlide(Context context, DcMsg dcMsg) {
    super(context, new DcAttachment(dcMsg));
    dcMsgId = dcMsg.getId();
  }

  public DocumentSlide(@NonNull Context context, @NonNull Uri uri,
                       @NonNull String contentType,  long size,
                       @Nullable String fileName)
  {
    super(context, constructAttachmentFromUri(context, uri, contentType, size, 0, 0, uri, StorageUtil.getCleanFileName(fileName), false));
  }

  @Override
  public boolean hasDocument() {
    return true;
  }

}
