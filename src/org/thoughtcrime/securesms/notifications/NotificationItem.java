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
  private final @NonNull Recipient          threadRecipient;
  private final @NonNull Recipient          individualRecipient;
  private final int                         chatId;
  private final @Nullable CharSequence      text;
  private final long                        timestamp;
  private final @Nullable SlideDeck         slideDeck;

  NotificationItem(int id,
                          @NonNull  Recipient threadRecipient,
                          @NonNull  Recipient individualRecipient,
                          int chatId, @Nullable CharSequence text, long timestamp,
                          @Nullable SlideDeck slideDeck)
  {
    this.id                    = id;
    this.threadRecipient       = threadRecipient;
    this.individualRecipient   = individualRecipient;
    this.text                  = text;
    this.chatId                = chatId;
    this.timestamp             = timestamp;
    this.slideDeck             = slideDeck;
  }

  public @NonNull  Recipient getRecipient() {
    return threadRecipient;
  }

  public @Nullable CharSequence getText() {
    return text;
  }

  public @NonNull CharSequence getText(@NonNull CharSequence defaul) {
    return (text == null ? defaul : text);
  }

  public long getTimestamp() {
    return timestamp;
  }

  public int getChatId() {
    return chatId;
  }

  @NonNull Recipient getIndividualRecipient() {
    return individualRecipient;
  }

  @Deprecated
  public int getThreadId() {
    return chatId;
  }

  @Nullable SlideDeck getSlideDeck() {
    return slideDeck;
  }

  @NonNull PendingIntent getPendingIntent(Context context) {
    Intent     intent           = new Intent(context, ConversationActivity.class);

    intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, chatId);
    intent.setData((Uri.parse("custom://"+System.currentTimeMillis())));

    return TaskStackBuilder.create(context)
                           .addNextIntentWithParentStack(intent)
                           .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  public int getId() {
    return id;
  }
}
