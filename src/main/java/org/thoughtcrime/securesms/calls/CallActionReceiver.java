package org.thoughtcrime.securesms.calls;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.O)
public class CallActionReceiver extends BroadcastReceiver {
  private static final String TAG = "CallActionReceiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent == null) return;

    String action = intent.getAction();
    Log.d(TAG, "Received action: " + action);

    if (CallActivity.ACTION_DECLINE_CALL.equals(action)) {
      CallCoordinator.getInstance(context).declineCall();
    } else if (CallActivity.ACTION_HANGUP_CALL.equals(action)) {
      CallCoordinator.getInstance(context).hangUp();
    }
  }
}
