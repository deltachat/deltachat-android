package org.thoughtcrime.securesms.connect;

import android.app.Notification;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.notifications.NotificationCenter;
import org.thoughtcrime.securesms.service.FetchForegroundService;
import org.thoughtcrime.securesms.service.GenericForegroundService;
import org.thoughtcrime.securesms.service.SendingWaiter;

public class FetchWorker extends Worker {
  private static final String TAG = "FetchWorker";
  private static final long SENDING_MAX_RUNTIME_MS = 5 * 60 * 1000;
  private final @NonNull Context context;

  public FetchWorker(@NonNull Context context, @NonNull WorkerParameters params) {
    super(context, params);
    this.context = context;
  }

  // doWork() is called in a background thread;
  // once we return, Worker is considered to have finished and will be destroyed,
  // this does not necessarily mean, that the app is killed, we may or may not keep running,
  // therefore we do not stopIo() here.
  @Override
  public @NonNull Result doWork() {
    Log.i(TAG, "++++++++++++++++++ doWork() started ++++++++++++++++++");

    // getAccounts() blocks until ApplicationContext.onCreate() has finished,
    // which also started I/O. backgroundFetch() returns when fetching is done or timeout.
    FetchForegroundService.fetchStarted();
    // stop() on DC_EVENT_ACCOUNTS_BACKGROUND_FETCH_DONE
    if (!DcHelper.getAccounts(context).backgroundFetch(60)) {
      FetchForegroundService.stop(context);
    }

    // If there are still outgoing messages, keep running until the queue is drained.
    // Workers are stopped after 10 minutes, the next run resumes the queue.
    if (!SendingWaiter.isSendingFinished(context) && SendingWaiter.tryStartSession()) {
      try {
        Log.i(TAG, "doWork(): sending not finished, continuing in foreground");
        setForegroundAsync(createSendingForegroundInfo()).get();
        SendingWaiter.awaitQueueEmpty(context, SENDING_MAX_RUNTIME_MS, null);
      } catch (Exception e) {
        Log.w(TAG, "Could not continue sending in foreground", e);
      } finally {
        SendingWaiter.endSession();
      }
    }

    Log.i(TAG, "++++++++++++++++++ doWork() will return ++++++++++++++++++");
    return Result.success();
  }

  private ForegroundInfo createSendingForegroundInfo() {
    GenericForegroundService.createFgNotificationChannel(context);
    Notification notification =
        new NotificationCompat.Builder(context, NotificationCenter.CH_GENERIC)
            .setContentTitle(context.getString(R.string.sending))
            .setSmallIcon(R.drawable.notification_permanent)
            .build();
    return new ForegroundInfo(
        NotificationCenter.ID_SENDING, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
  }
}
