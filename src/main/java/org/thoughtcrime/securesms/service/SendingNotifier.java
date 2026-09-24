package org.thoughtcrime.securesms.service;

import android.content.Context;
import android.util.Log;
import org.thoughtcrime.securesms.connect.KeepAliveService;
import org.thoughtcrime.securesms.util.Util;

/** Shows an FGS while the app is in background and the outgoing message queue is not empty. */
public class SendingNotifier {
  private static final String TAG = "SendingNotifier";

  public static void onAppBackgrounded(final Context context) {
    Util.runOnAnyBackgroundThread(
        () -> {
          long started = System.currentTimeMillis();
          Log.i(TAG, "onAppBackgrounded()");

          if (KeepAliveService.getInstance() != null) {
            return;
          }
          if (FetchForegroundService.getInstance() != null) {
            return;
          }
          boolean sendingFinished = SendingWaiter.isSendingFinished(context);
          Log.d(
              TAG,
              "isSendingFinished()="
                  + sendingFinished
                  + " after "
                  + (System.currentTimeMillis() - started)
                  + "ms");
          if (sendingFinished) {
            return;
          }
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
          }
        });
  }
}
