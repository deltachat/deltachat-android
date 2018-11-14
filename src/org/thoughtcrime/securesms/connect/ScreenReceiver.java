package org.thoughtcrime.securesms.connect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ScreenReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        ApplicationDcContext dcContext = DcHelper.getContext(context);

        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            Log.i("DeltaChat", "-------------------- Screen off --------------------");
            dcContext.isScreenOn = false;
            context.startService(new Intent(context, KeepAliveService.class));
        }
        else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            Log.i("DeltaChat", "-------------------- Screen on --------------------");
            dcContext.isScreenOn = true;
            context.stopService(new Intent(context, KeepAliveService.class));
        }
    }
}
