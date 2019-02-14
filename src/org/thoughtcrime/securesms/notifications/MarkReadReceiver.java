// called when the user click the "clear" or "mark read" button in the system notification

package org.thoughtcrime.securesms.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.NotificationManagerCompat;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;

public class MarkReadReceiver extends BroadcastReceiver {

  private static final String TAG                   = MarkReadReceiver.class.getSimpleName();
  public static final  String CLEAR_ACTION          = "org.thoughtcrime.securesms.notifications.CLEAR";
  public static final  String CHAT_IDS_EXTRA = "thread_ids";
  public static final  String NOTIFICATION_ID_EXTRA = "notification_id";

  @Override
  public void onReceive(final Context context, Intent intent) {
    if (!CLEAR_ACTION.equals(intent.getAction()))
      return;

    final int[] threadIds = intent.getIntArrayExtra(CHAT_IDS_EXTRA);

    if (threadIds != null) {
      NotificationManagerCompat.from(context).cancel(intent.getIntExtra(NOTIFICATION_ID_EXTRA, -1));

      ApplicationDcContext dcContext = DcHelper.getContext(context);
      new MarkAsNoticedAsyncTask(threadIds, dcContext, true).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }
}
