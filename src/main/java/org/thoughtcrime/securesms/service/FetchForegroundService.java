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

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.ForegroundDetector;
import org.thoughtcrime.securesms.notifications.FcmReceiveService;
import org.thoughtcrime.securesms.notifications.NotificationCenter;
import org.thoughtcrime.securesms.util.Util;

public final class FetchForegroundService extends Service {
  private static final String TAG = FcmReceiveService.class.getSimpleName();
  private static final Object SERVICE_LOCK = new Object();
  private static final Object STOP_NOTIFIER = new Object();
  private static volatile boolean fetchingSynchronously = false;
  private static Intent service;

  public static void start(Context context) {
    ForegroundDetector foregroundDetector = ForegroundDetector.getInstance();
    if (foregroundDetector != null && foregroundDetector.isForeground()) {
      return;
    }

    GenericForegroundService.createFgNotificationChannel(context);
    try {
      synchronized (SERVICE_LOCK) {
        if (service == null) {
          service = new Intent(context, FetchForegroundService.class);
          ContextCompat.startForegroundService(context, service);
        }
      }
    } catch (Exception e) {
      Log.w(TAG, "Failed to start foreground service: " + e + ", fetching in background.");
      // According to the documentation https://firebase.google.com/docs/cloud-messaging/android/receive,
      // we need to handle the message within 20s, and the time window may be even shorter than 20s,
      // so, use 10s to be safe.
      fetchingSynchronously = true;
      if (ApplicationContext.getDcAccounts().backgroundFetch(10)) {
        // The background fetch was successful, but we need to wait until all events were processed.
        // After all events were processed, we will get DC_EVENT_ACCOUNTS_BACKGROUND_FETCH_DONE,
        // and stop() will be called.
        synchronized (STOP_NOTIFIER) {
          while (fetchingSynchronously) {
            try {
              // The `wait()` needs to be enclosed in a while loop because there may be
              // "spurious wake-ups", i.e. `wait()` may return even though `notifyAll()` wasn't called.
              STOP_NOTIFIER.wait();
            } catch (InterruptedException ex) {}
          }
        }
      }
    }
  }

  public static void stop(Context context) {
    if (fetchingSynchronously) {
      fetchingSynchronously = false;
      synchronized (STOP_NOTIFIER) {
        STOP_NOTIFIER.notifyAll();
      }
    }

    synchronized (SERVICE_LOCK) {
      if (service != null) {
        context.stopService(service);
        service = null;
      }
    }
  }

  @Override
  public void onCreate() {
    Log.i(TAG, "Creating fetch service");
    super.onCreate();

    Notification notification = new NotificationCompat.Builder(this, NotificationCenter.CH_GENERIC)
      .setContentTitle(getString(R.string.connectivity_updating))
      .setSmallIcon(R.drawable.notification_permanent)
      .build();

    startForeground(NotificationCenter.ID_FETCH, notification);

    Util.runOnAnyBackgroundThread(() -> {
      Log.i(TAG, "Starting fetch");
      if (!ApplicationContext.getDcAccounts().backgroundFetch(300)) { // as startForeground() was called, there is time
        FetchForegroundService.stop(this);
      } // else we stop FetchForegroundService on DC_EVENT_ACCOUNTS_BACKGROUND_FETCH_DONE
    });
  }

  @Override
  public void onDestroy() {
    stopForeground(true);
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onTimeout(int startId, int fgsType) {
    ApplicationContext.getDcAccounts().stopBackgroundFetch();
    stopSelf();
  }

}
