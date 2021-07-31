// called when the user click the "clear" or "mark read" button in the system notification

package org.thoughtcrime.securesms.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Util;

public class MarkReadReceiver extends BroadcastReceiver {
  public static final  String MARK_NOTICED_ACTION   = "org.thoughtcrime.securesms.notifications.MARK_NOTICED";
  public static final  String CANCEL_ACTION         = "org.thoughtcrime.securesms.notifications.CANCEL";
  public static final  String CHAT_ID_EXTRA         = "chat_id";

  @Override
  public void onReceive(final Context context, Intent intent) {
    boolean markNoticed = MARK_NOTICED_ACTION.equals(intent.getAction());
    if (!markNoticed && !CANCEL_ACTION.equals(intent.getAction())) {
      return;
    }

    final int chatId = intent.getIntExtra(CHAT_ID_EXTRA, 0);
    if (chatId==0) {
      return;
    }

    Util.runOnAnyBackgroundThread(() -> {
      DcHelper.getNotificationCenter(context).removeNotifications(chatId);
      if (markNoticed) {
        DcHelper.getContext(context).marknoticedChat(chatId);
      }
    });
  }
}
