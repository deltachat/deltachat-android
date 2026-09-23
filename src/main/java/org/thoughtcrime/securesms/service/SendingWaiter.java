package org.thoughtcrime.securesms.service;

import android.content.Context;
import android.util.Log;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.connect.ForegroundDetector;
import org.thoughtcrime.securesms.util.Util;

public final class SendingWaiter {
  private static final String TAG = "SendingWaiter";

  public static final long SENDING_MAX_RUNTIME_MS = 30 * 60 * 1000;
  private static final Object SESSION_LOCK = new Object();
  private static boolean sessionActive = false;

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

  public static boolean tryStartSession() {
    synchronized (SESSION_LOCK) {
      if (sessionActive) {
        return false;
      }
      sessionActive = true;
      return true;
    }
  }

  public static void endSession() {
    synchronized (SESSION_LOCK) {
      sessionActive = false;
    }
  }

  public static boolean awaitQueueEmpty(Context context, long timeoutMs, AbortCheck abortCheck) {
    long deadline = System.currentTimeMillis() + timeoutMs;
    int polls = 0;
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
      polls++;
      if (polls % 30 == 0) {
        Log.i(TAG, "still sending after " + polls + " polls");
      }
    }
    return false;
  }
}
