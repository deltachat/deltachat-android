package org.thoughtcrime.securesms.notifications;

import android.os.AsyncTask;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.util.BadgeUtil;

/**
 * This marks the messages the user has acknowledged as noticed.
 * @author Angelo Fuchs
 */
public  class MarkAsNoticedAsyncTask extends AsyncTask<Void, Void, Void> {
  private final int[] ids;
  private final ApplicationDcContext dcContext;
  private final boolean isChat;

  /**
   * goes through the given messages or chats and marks them as notified.
   * @param ids chat or messages ids to be marked.
   * @param dcContext the applications context
   * @param isChat true if the ids are chat ids, false to signify message ids.
   */
  MarkAsNoticedAsyncTask(int[] ids, ApplicationDcContext dcContext, boolean isChat) {
    this.ids = ids;
    this.dcContext = dcContext;
    this.isChat = isChat;
  }

  @Override
  protected Void doInBackground(Void... params) {
    for (int id : ids) {
      if (isChat)
        dcContext.marknoticedChat(id);
      else
        dcContext.marknoticedChat(dcContext.getMsg(id).getChatId());
    }
    BadgeUtil.update(dcContext.context, dcContext.getFreshMsgs().length);

    return null;
  }
}

