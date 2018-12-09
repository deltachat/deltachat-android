package org.thoughtcrime.securesms.notifications;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;

public class DeleteNotificationReceiver extends BroadcastReceiver {

  public static String DELETE_NOTIFICATION_ACTION = "org.thoughtcrime.securesms.DELETE_NOTIFICATION";

  public static String EXTRA_IDS = "message_ids";

  @Override
  public void onReceive(final Context context, Intent intent) {
    if (!DELETE_NOTIFICATION_ACTION.equals(intent.getAction()))
      return;
    MessageNotifier.clearReminder(context);

    final int[]    ids = intent.getIntArrayExtra(EXTRA_IDS);

    if (ids != null) {
      final ApplicationDcContext dcContext = DcHelper.getContext(context);
      new MarkAsNoticedAsyncTask(ids, dcContext, false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }
}
