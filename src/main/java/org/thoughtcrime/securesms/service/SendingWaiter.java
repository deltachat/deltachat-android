package org.thoughtcrime.securesms.service;

import android.content.Context;
import android.util.Log;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.connect.ForegroundDetector;
import org.thoughtcrime.securesms.util.Util;

public final class SendingWaiter {
  private static final String TAG = "SendingWaiter";

  public static final long SENDING_MAX_RUNTIME_MS = 30 * 60 * 1000;

  public interface AbortCheck {
    boolean shouldAbort();
  }

  private SendingWaiter() {}

  public static boolean isSendingFinished(Context context) {
    try {
      return DcHelper.getRpc(context).isSendingFinished();
    } catch (Exception e) {
      Log.w(TAG, "RPC isSendingFinished failed", e);
      return true;
    }
  }

  public static boolean awaitQueueEmpty(Context context, long timeoutMs, AbortCheck abortCheck) {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
      if (isSendingFinished(context)) {
        return true;
      }
      ForegroundDetector foregroundDetector = ForegroundDetector.getInstance();
      if (foregroundDetector != null && foregroundDetector.isForeground()) {
        return false;
      }
      if (abortCheck != null && abortCheck.shouldAbort()) {
        return false;
      }
      Util.sleep(1000);
    }
    return false;
  }
}
