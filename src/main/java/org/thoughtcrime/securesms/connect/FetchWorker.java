package org.thoughtcrime.securesms.connect;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.thoughtcrime.securesms.util.Util;

public class FetchWorker extends Worker {
    private final @NonNull Context context;

    public FetchWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
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
        DcHelper.getAccounts(context).startIo();

        // as we do not know when startIo() has done it's work or if is even doable in one step,
        // we go the easy way and just wait for some amount of time.
        // the core has to handle interrupts at any point anyway,
        // and work also maybe continued when doWork() returns.
        // however, we should not wait too long here to avoid getting bad battery ratings.
        Util.sleep(60 * 1000);

        Log.i("DeltaChat", "++++++++++++++++++ FetchWorker.doWork() will return ++++++++++++++++++");
        return Result.success();
    }
}
