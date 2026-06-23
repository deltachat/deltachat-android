package org.thoughtcrime.securesms.calls;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationManagerCompat;
import org.thoughtcrime.securesms.ConversationActivity;
import org.thoughtcrime.securesms.R;

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
    } else if (CallActivity.ACTION_CALL_BACK.equals(action)) {
      int chatId = intent.getIntExtra(ConversationActivity.CHAT_ID_EXTRA, -1);
      int accId = intent.getIntExtra(ConversationActivity.ACCOUNT_ID_EXTRA, -1);
      boolean video = intent.getBooleanExtra(CallActivity.EXTRA_STARTS_WITH_VIDEO, false);
      if (chatId > 0 && accId > 0) {
        NotificationManagerCompat.from(context).cancel(CallCoordinator.NOTIFICATION_ID_MISSED_CALL);
        CallCoordinator coordinator = CallCoordinator.getInstance(context);
        if (coordinator.hasActiveCall()) {
          Toast.makeText(context, R.string.already_in_call, Toast.LENGTH_SHORT).show();
        } else {
          coordinator.initiateOutgoingCall(accId, chatId, video);
        }
      }
    } else if (CallActivity.ACTION_MESSAGE.equals(action)) {
      int chatId = intent.getIntExtra(ConversationActivity.CHAT_ID_EXTRA, -1);
      int accId = intent.getIntExtra(ConversationActivity.ACCOUNT_ID_EXTRA, -1);
      NotificationManagerCompat.from(context).cancel(CallCoordinator.NOTIFICATION_ID_MISSED_CALL);
      if (chatId > 0 && accId > 0) {
        Intent convIntent = new Intent(context, ConversationActivity.class);
        convIntent.putExtra(ConversationActivity.CHAT_ID_EXTRA, chatId);
        convIntent.putExtra(ConversationActivity.ACCOUNT_ID_EXTRA, accId);
        convIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(convIntent);
      }
    }
  }
}
