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
        Log.i("DeltaChat", "-------------------- FetchWorker.doWork() started --------------------");
        ApplicationDcContext dcContext = DcHelper.getContext(context);
        dcContext.maybeStartIo();
        Log.i("DeltaChat", "-------------------- FetchWorker.doWork() done --------------------");

        // TODO-ASYNC: check, that the threads get some minimal amount of time and the app is not terminated
        // directly after returning

        return Result.success(); // when returning, the os may terminate the app again
    }
}
