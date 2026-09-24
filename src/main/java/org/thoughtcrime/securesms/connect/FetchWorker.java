package org.thoughtcrime.securesms.connect;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import org.thoughtcrime.securesms.service.FetchForegroundService;

public class FetchWorker extends Worker {
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
    Log.i("DeltaChat", "++++++++++++++++++ FetchWorker.doWork() started ++++++++++++++++++");

    // getAccounts() blocks until ApplicationContext.onCreate() has finished,
    // which also started I/O. backgroundFetch() returns when fetching is done or timeout,
    // so there is no need to sleep here.
    FetchForegroundService.fetchStarted();
    // stop() on DC_EVENT_ACCOUNTS_BACKGROUND_FETCH_DONE
    if (!DcHelper.getAccounts(context).backgroundFetch(60)) {
      FetchForegroundService.stop(context);
    }

    Log.i("DeltaChat", "++++++++++++++++++ FetchWorker.doWork() will return ++++++++++++++++++");
    return Result.success();
  }
}
