package org.thoughtcrime.securesms.notifications;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.DatabaseFactory;

public class DeleteNotificationReceiver extends BroadcastReceiver {

  public static String DELETE_NOTIFICATION_ACTION = "org.thoughtcrime.securesms.DELETE_NOTIFICATION";

  public static String EXTRA_IDS = "message_ids";

  @Override
  public void onReceive(final Context context, Intent intent) {
    if (DELETE_NOTIFICATION_ACTION.equals(intent.getAction())) {
      MessageNotifier.clearReminder(context);

      final int[]    ids = intent.getIntArrayExtra(EXTRA_IDS);
      final ApplicationDcContext dcContext = DcHelper.getContext(context);

      if (ids == null || ids.length == 0) return;

      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          for (int i=0;i<ids.length;i++) {
            dcContext.marknoticedChat(dcContext.getMsg(ids[i]).getChatId());
          }

          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }
}
