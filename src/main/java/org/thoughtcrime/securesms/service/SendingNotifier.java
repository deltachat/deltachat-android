package org.thoughtcrime.securesms.service;

import android.content.Context;
import android.util.Log;
import org.thoughtcrime.securesms.connect.FetchWorker;
import org.thoughtcrime.securesms.connect.ForegroundDetector;
import org.thoughtcrime.securesms.connect.KeepAliveService;
import org.thoughtcrime.securesms.util.Util;

/** Shows an FGS while app is in background and the outgoing message queue is not empty. */
public class SendingNotifier {
  private static final String TAG = "SendingNotifier";

  // A message may still be on its way from ui to SMTP queue when app goes background;
  // re-check for some more seconds.
  private static final long LINGER_MS = 5000;

  public static void onAppBackgrounded(final Context context) {
    // called either when the last activity stops, or while the app is in the background
    Util.runOnAnyBackgroundThread(
        () -> {
          long started = System.currentTimeMillis();
          Log.i(TAG, "onAppBackgrounded()");

          ForegroundDetector foregroundDetector = ForegroundDetector.getInstance();
          if (foregroundDetector != null && foregroundDetector.isForeground()) {
            return;
          }
          if (KeepAliveService.getInstance() != null) {
            return;
          }
          if (FetchForegroundService.getInstance() != null) {
            return;
          }
          if (!SendingWaiter.isSendingFinished(context)) {
            Log.i(
                TAG,
                "isSendingFinished()=false after " + (System.currentTimeMillis() - started) + "ms");
            startSendingService(context, started);
            return;
          }
          long deadline = System.currentTimeMillis() + LINGER_MS;
          while (System.currentTimeMillis() < deadline) {
            Util.sleep(1000);
            if (foregroundDetector != null && foregroundDetector.isForeground()) {
              return;
            }
            if (!SendingWaiter.isSendingFinished(context)) {
              Log.i(
                  TAG,
                  "isSendingFinished()=false after "
                      + (System.currentTimeMillis() - started)
                      + "ms (lingering)");
              startSendingService(context, started);
              return;
            }
          }
        });
  }

  private static void startSendingService(Context context, long started) {
    if (!SendingWaiter.tryStartSession()) {
      return;
    }
    boolean serviceStarted = SendingForegroundService.start(context);
    Log.d(
        TAG,
        "SendingForegroundService.start()="
            + serviceStarted
            + " after "
            + (System.currentTimeMillis() - started)
            + "ms");
    if (!serviceStarted) {
      SendingWaiter.endSession();
      FetchWorker.enqueueFlushJob(context);
    }
  }
}
