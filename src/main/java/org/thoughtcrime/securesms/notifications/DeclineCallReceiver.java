// called when the user click the "clear" or "mark read" button in the system notification

package org.thoughtcrime.securesms.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Util;

public class DeclineCallReceiver extends BroadcastReceiver {
  public static final  String DECLINE_ACTION   = "org.thoughtcrime.securesms.notifications.DECLINE_CALL";
  public static final  String ACCOUNT_ID_EXTRA      = "account_id";
  public static final  String CALL_ID_EXTRA          = "call_id";

  @Override
  public void onReceive(final Context context, Intent intent) {
    if (!DECLINE_ACTION.equals(intent.getAction())) {
      return;
    }

    final int accountId = intent.getIntExtra(ACCOUNT_ID_EXTRA, 0);
    final int callId = intent.getIntExtra(CALL_ID_EXTRA, 0);
    if (accountId == 0) {
      return;
    }

    Util.runOnAnyBackgroundThread(() -> {
      DcHelper.getNotificationCenter(context).removeCallNotification(accountId, callId);
      DcContext dcContext = DcHelper.getAccounts(context).getAccount(accountId);
      dcContext.endCall(callId);
    });
  }
}
