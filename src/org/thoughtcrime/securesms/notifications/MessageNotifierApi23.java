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

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import org.thoughtcrime.securesms.util.ServiceUtil;


/**
 * Handles posting system notifications for new messages.
 *
 */

@TargetApi(23)
class MessageNotifierApi23 extends MessageNotifier {

  MessageNotifierApi23(Context context) {
    super(context);
  }

  void cancelActiveNotifications() {
    super.cancelActiveNotifications();
    NotificationManager notifications = ServiceUtil.getNotificationManager(appContext);
    try {

        StatusBarNotification[]  activeNotifications = notifications.getActiveNotifications();
        for (StatusBarNotification activeNotification : activeNotifications) {
          notifications.cancel(activeNotification.getId());
        }
    } catch (Throwable e) {
      // XXX Appears to be a ROM bug, see #6043
      Log.w(TAG, e);
      notifications.cancelAll();
    }
  }

  }
