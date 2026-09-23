package org.thoughtcrime.securesms.service;

import android.content.Context;
import android.util.Log;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.KeepAliveService;
import org.thoughtcrime.securesms.util.Util;

/** Shows an FGS while the app is in background and the outgoing message queue is not empty. */
public class SendingNotifier {
  private static final String TAG = "SendingNotifier";

  public static void onAppBackgrounded(final Context context) {
    Util.runOnAnyBackgroundThread(
        () -> {
          if (KeepAliveService.getInstance() != null) {
            return;
          }
          if (FetchForegroundService.getInstance() != null) {
            return;
          }
          if (SendingWaiter.isSendingFinished(context)) {
            return;
          }
          if (!SendingWaiter.tryStartSession()) {
            return;
          }
          try {
            poll(context.getApplicationContext());
          } finally {
            SendingWaiter.endSession();
          }
        });
  }

  private static void poll(Context context) {
    NotificationController controller;
    try {
      controller =
          GenericForegroundService.startForegroundTask(
              context, context.getString(R.string.sending));
    } catch (Exception e) {
      Log.w(TAG, "cannot start sending notification", e);
      return;
    }
    try {
      SendingWaiter.awaitQueueEmpty(context, SendingWaiter.SENDING_MAX_RUNTIME_MS, null);
    } finally {
      try {
        controller.close();
      } catch (Exception e) {
        Log.w(TAG, "cannot remove sending notification", e);
      }
    }
  }
}
