// called when the user click the "clear" or "mark read" button in the system notification

package org.thoughtcrime.securesms.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Util;

public class MarkReadReceiver extends BroadcastReceiver {

  public static final  String CLEAR_ACTION          = "org.thoughtcrime.securesms.notifications.CLEAR";
  public static final  String CHAT_ID_EXTRA         = "chat_id";

  @Override
  public void onReceive(final Context context, Intent intent) {
    if (!CLEAR_ACTION.equals(intent.getAction())) {
      return;
    }

    final int chatId = intent.getIntExtra(CHAT_ID_EXTRA, 0);
    if (chatId==0) {
      return;
    }

    final ApplicationDcContext dcContext = DcHelper.getContext(context);

    Util.runOnAnyBackgroundThread(() -> {
      dcContext.notificationCenter.removeNotifications(chatId);
      dcContext.marknoticedChat(chatId);
    });
  }
}
