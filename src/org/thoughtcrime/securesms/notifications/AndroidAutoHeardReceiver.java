/*
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.thoughtcrime.securesms.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.NotificationManagerCompat;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;

/**
 * Marks an Android Auto as read after the driver have listened to it
 */
public class AndroidAutoHeardReceiver extends BroadcastReceiver {

  public static final String TAG                   = AndroidAutoHeardReceiver.class.getSimpleName();
  public static final String HEARD_ACTION          = "org.thoughtcrime.securesms.notifications.ANDROID_AUTO_HEARD";
  public static final String CHAT_IDS_EXTRA = "car_heard_thread_ids";
  public static final String NOTIFICATION_ID_EXTRA = "car_notification_id";

  @Override
  public void onReceive(final Context context, Intent intent)
  {
    if (!HEARD_ACTION.equals(intent.getAction()))
      return;

    final int[] threadIds = intent.getIntArrayExtra(CHAT_IDS_EXTRA);

    if (threadIds != null) {
      int notificationId = intent.getIntExtra(NOTIFICATION_ID_EXTRA, -1);
      NotificationManagerCompat.from(context).cancel(notificationId);

      final ApplicationDcContext dcContext = DcHelper.getContext(context);
      new MarkAsNoticedAsyncTask(threadIds, dcContext, true).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }
}
