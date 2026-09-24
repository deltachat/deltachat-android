package org.thoughtcrime.securesms.service;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.notifications.NotificationCenter;
import org.thoughtcrime.securesms.util.Util;

/** Shows a "Sending..." foreground notification until the outgoing message queue is empty. */
public final class SendingForegroundService extends Service {
  private static final String TAG = "SendingFGS";

  private static final Object SERVICE_LOCK = new Object();
  private static Intent service;

  static SendingForegroundService s_this = null;

  /** Caller must have claimed the sending session with SendingWaiter.tryStartSession(). */
  public static boolean start(Context context) {
    synchronized (SERVICE_LOCK) {
      if (service != null) {
        return true;
      }
      service = new Intent(context, SendingForegroundService.class);
      try {
        ContextCompat.startForegroundService(context, service);
      } catch (Exception e) {
        service = null;
        Log.w(TAG, "Failed to start foreground service: " + e);
        return false;
      }
    }
    return true;
  }

  public static void stop(Context context) {
    synchronized (SERVICE_LOCK) {
      if (service != null) {
        context.stopService(service);
        service = null;
      }
    }
    SendingWaiter.endSession();
  }

  @Override
  public void onCreate() {
    Log.i(TAG, "Creating sending foreground service");
    super.onCreate();
    s_this = this;

    GenericForegroundService.createFgNotificationChannel(this);
    Notification notification =
        new NotificationCompat.Builder(this, NotificationCenter.CH_GENERIC)
            .setContentTitle(getString(R.string.sending))
            .setSmallIcon(R.drawable.notification_permanent)
            .build();

    try {
      startForeground(NotificationCenter.ID_SENDING, notification);

      Util.runOnAnyBackgroundThread(
          () -> {
            try {
              SendingWaiter.awaitQueueEmpty(
                  this, SendingWaiter.SENDING_MAX_RUNTIME_MS, () -> s_this == null);
            } finally {
              stop(this);
            }
          });
    } catch (Exception e) {
      Log.e(TAG, "Error calling startForeground()", e);
      stop(this);
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return START_NOT_STICKY;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onDestroy() {
    Log.i(TAG, "Destroying sending foreground service");
    s_this = null;
    stopForeground(true);
  }

  @Override
  public void onTimeout(int startId, int fgsType) {
    stop(this);
  }
}
