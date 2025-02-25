/*
 * Copyright (C) 2016 Open Whisper Systems
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

import static com.b44t.messenger.DcChat.DC_CHAT_NO_CHAT;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.app.RemoteInput;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Util;


/**
 * Get the response text from the Wearable Device and sends an message as a reply
 */
public class RemoteReplyReceiver extends BroadcastReceiver {

  public static final String TAG           = RemoteReplyReceiver.class.getSimpleName();
  public static final String REPLY_ACTION  = "org.thoughtcrime.securesms.notifications.WEAR_REPLY";
  public static final String ACCOUNT_ID_EXTRA = "account_id";
  public static final String CHAT_ID_EXTRA = "chat_id";
  public static final String MSG_ID_EXTRA = "msg_id";
  public static final String EXTRA_REMOTE_REPLY = "extra_remote_reply";

  @SuppressLint("StaticFieldLeak")
  @Override
  public void onReceive(final Context context, Intent intent) {
    if (!REPLY_ACTION.equals(intent.getAction())) return;
    Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
    final int accountId = intent.getIntExtra(ACCOUNT_ID_EXTRA, 0);
    final int chatId = intent.getIntExtra(CHAT_ID_EXTRA, DC_CHAT_NO_CHAT);
    final int msgId = intent.getIntExtra(MSG_ID_EXTRA, 0);

    if (remoteInput == null || chatId == DC_CHAT_NO_CHAT || accountId == 0) return;

    final CharSequence responseText = remoteInput.getCharSequence(EXTRA_REMOTE_REPLY);

    if (responseText != null) {
      Util.runOnAnyBackgroundThread(() -> {
        DcContext dcContext = DcHelper.getAccounts(context).getAccount(accountId);
        dcContext.marknoticedChat(chatId);
        dcContext.markseenMsgs(new int[]{msgId});
        if (dcContext.getChat(chatId).isContactRequest()) {
          dcContext.acceptChat(chatId);
        }

        DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
        msg.setText(responseText.toString());
        dcContext.sendMsg(chatId, msg);

        DcHelper.getNotificationCenter(context).removeNotifications(accountId, chatId);
      });
    }
  }
}
