package org.thoughtcrime.securesms.service;

import android.content.Context;
import android.util.Log;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.KeepAliveService;
import org.thoughtcrime.securesms.util.Util;

/** Shows an FGS while the app is in background and the outgoing message queue is not empty. */
public class SendingNotifier {
  private static final String TAG = "SendingNotifier";
  private static final long POLL_INTERVAL_MS = 1000;
  private static final long MAX_RUNTIME_MS = 30 * 60 * 1000;

  private static boolean active = false;

  public static void onAppBackgrounded(final Context context) {
    Util.runOnAnyBackgroundThread(
        () -> {
          synchronized (SendingNotifier.class) {
            if (active) {
              return;
            }
            active = true;
          }
          try {
            if (KeepAliveService.getInstance() != null) {
              return;
            }
            if (FetchForegroundService.getInstance() != null) {
              return;
            }
            if (!SendingWaiter.isSendingFinished(context)) {
              poll(context.getApplicationContext());
            }
          } finally {
            synchronized (SendingNotifier.class) {
              active = false;
            }
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
      controller.close();
    }
  }
}
