package org.thoughtcrime.securesms.mms;

import android.content.Context;
import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.attachments.Attachment;
import org.thoughtcrime.securesms.util.MediaUtil;

public abstract class MediaConstraints {
  public static MediaConstraints getPushMediaConstraints() {
    return new PushMediaConstraints();
  }

  public abstract int getImageMaxWidth(Context context);
  public abstract int getImageMaxHeight(Context context);
  public abstract int getImageMaxSize(Context context);
}
