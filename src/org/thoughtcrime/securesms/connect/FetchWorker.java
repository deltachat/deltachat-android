package org.thoughtcrime.securesms.connect;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class FetchWorker extends Worker {
    private @NonNull Context context;

    public FetchWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    @Override
    public @NonNull Result doWork() {
        // MAYBE TODO:
        // - when no threads are running: fetch-inbox, maybe fetch-mvbox, do smtp-jobs.
        //   fetch-sendbox is not needed as these messages shall not be notified.
        // - when threads are running: interrupt-all-idle

        Log.i("DeltaChat", "-------------------- FetchWorker.doWork() started --------------------");
        ApplicationDcContext dcContext = DcHelper.getContext(context);
        dcContext.startThreads(ApplicationDcContext.INTERRUPT_IDLE);
        dcContext.waitForThreadsExecutedOnce();
        Log.i("DeltaChat", "-------------------- FetchWorker.doWork() done --------------------");

        return Result.success(); // when returning, the os may terminate the app again
    }
}
