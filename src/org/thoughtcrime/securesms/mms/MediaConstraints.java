package org.thoughtcrime.securesms.mms;

import android.content.Context;

public abstract class MediaConstraints {
  public abstract int getImageMaxWidth(Context context);
  public abstract int getImageMaxHeight(Context context);
  public abstract int getImageMaxSize(Context context);
}
