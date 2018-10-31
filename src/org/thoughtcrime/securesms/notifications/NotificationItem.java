package org.thoughtcrime.securesms.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;

import org.thoughtcrime.securesms.ConversationActivity;
import org.thoughtcrime.securesms.mms.SlideDeck;
import org.thoughtcrime.securesms.recipients.Recipient;

public class NotificationItem {

  private final int                         id;
  private final boolean                     mms;
  private final @Nullable Recipient         threadRecipient;
  private final int                         chatId;
  private final @Nullable CharSequence      text;
  private final long                        timestamp;
  private final @Nullable SlideDeck         slideDeck;

  public NotificationItem(int id, boolean mms,
                          @Nullable  Recipient threadRecipient,
                          int chatId, @Nullable CharSequence text, long timestamp,
                          @Nullable SlideDeck slideDeck)
  {
    this.id                    = id;
    this.mms                   = mms;
    this.threadRecipient       = threadRecipient;
    this.text                  = text;
    this.chatId                = chatId;
    this.timestamp             = timestamp;
    this.slideDeck             = slideDeck;
  }

  public @NonNull  Recipient getRecipient() {
    return threadRecipient;
  }

  public CharSequence getText() {
    return text;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public int getChatId() {
    return chatId;
  }

  /**
   * @deprecated Use getThreadRecipient instead.
   */
  @Deprecated
  public @NonNull Recipient getIndividualRecipient() {
    return threadRecipient;
  }

  @Deprecated
  public int getThreadId() {
    return chatId;
  }

  public @Nullable SlideDeck getSlideDeck() {
    return slideDeck;
  }

  public PendingIntent getPendingIntent(Context context) {
    Intent     intent           = new Intent(context, ConversationActivity.class);

    intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, chatId);
    intent.setData((Uri.parse("custom://"+System.currentTimeMillis())));

    return TaskStackBuilder.create(context)
                           .addNextIntentWithParentStack(intent)
                           .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  public int getId() {
    return id;
  }

  public boolean isMms() {
    return mms;
  }
}
