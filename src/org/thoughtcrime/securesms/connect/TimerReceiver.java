// restart the smtp and imap threads every 5 minutes if they were killed by the os.
// however, we try to prevent the killing by the permanent notification KeepAliveService.

package org.thoughtcrime.securesms.connect;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;

public class TimerReceiver extends BroadcastReceiver {

    // we trigger an interupt-idle or a start-threads every 5 minutes,
    // normally, the core reconnects on its own, however, if for some reasons,
    // the thread was killed, we recreate it here.
    // also the IMAP NOOP is helpful on some servers, see above.
    // (re-doing IMAP-IDLE should imply a NOOP call then, https://www.isode.com/whitepapers/imap-idle.html )
    public void onReceive(Context context, Intent intent) {

        Log.i("DeltaChat", "-------------------- on receive timer --------------------");

        TimerIntentService.enqueueWork(context.getApplicationContext(), intent);
    }
}
