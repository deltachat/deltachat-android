package org.thoughtcrime.securesms.components;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.b44t.messenger.DcMsg;
import org.thoughtcrime.securesms.R;

public enum MessageState {
  NONE,
  DOWNLOADING,
  PREPARING,
  PENDING,
  SENT,
  READ,
  FAILED;

  public static @NonNull MessageState of(@NonNull DcMsg msg, boolean isOutBroadcast) {
    if (msg.getDownloadState() == DcMsg.DC_DOWNLOAD_IN_PROGRESS) return DOWNLOADING;
    if (msg.isPending()) return PENDING;
    if (msg.isFailed()) return FAILED;
    if (!msg.isOutgoing() || isOutBroadcast) return NONE;
    if (msg.isRemoteRead()) return READ;
    if (msg.isDelivered()) return SENT;
    if (msg.isPreparing()) return PREPARING;
    return PENDING;
  }

  public static @NonNull MessageState ofInfo(@NonNull DcMsg msg) {
    if (msg.isFailed()) return FAILED;
    if (!msg.isOutgoing()) return NONE;
    if (msg.isPreparing()) return PREPARING;
    if (msg.isPending()) return PENDING;
    return NONE;
  }

  public @Nullable String getA11yLabel(@NonNull Context context) {
    switch (this) {
      case DOWNLOADING:
        return context.getString(R.string.downloading);
      case PREPARING:
      case PENDING:
        return context.getString(R.string.a11y_delivery_status_sending);
      case SENT:
        return context.getString(R.string.a11y_delivery_status_delivered);
      case READ:
        return context.getString(R.string.a11y_delivery_status_read);
      case FAILED:
        return context.getString(R.string.a11y_delivery_status_error);
      case NONE:
      default:
        return null;
    }
  }
}
