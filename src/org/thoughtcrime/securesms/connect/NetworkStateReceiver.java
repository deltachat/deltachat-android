package org.thoughtcrime.securesms.connect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        try {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = manager.getActiveNetworkInfo();

            if (ni != null && ni.getState() == NetworkInfo.State.CONNECTED) {
                Log.i("DeltaChat", "++++++++++++++++++ Connected ++++++++++++++++++");
                ApplicationDcContext dcContext = DcHelper.getContext(context);
                dcContext.maybeNetwork();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
