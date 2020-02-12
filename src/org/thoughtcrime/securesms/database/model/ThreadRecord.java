/*
 * Copyright (C) 2012 Moxie Marlinspike
 * Copyright (C) 2013-2017 Open Whisper Systems
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
package org.thoughtcrime.securesms.database.model;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;

import com.b44t.messenger.DcLot;

import org.thoughtcrime.securesms.recipients.Recipient;

/**
 * The message record model which represents thread heading messages.
 *
 * @author Moxie Marlinspike
 *
 */
public class ThreadRecord {

  protected final Context context;

  private final Recipient  recipient;
  private final long       dateReceived;
  private final long       threadId;
  private final String     body;

  private           final int     unreadCount;
  private           final int     archived;
  private           final boolean verified;
  private           final boolean isSendingLocations;
  private @Nullable final DcLot   dcSummary;

  public ThreadRecord(@NonNull Context context, @NonNull String body,
                      @NonNull Recipient recipient, long dateReceived, int unreadCount,
                      long threadId,
                      int archived,
                      boolean verified,
                      boolean isSendingLocations,
                      @Nullable DcLot dcSummary)
  {
    this.context              = context.getApplicationContext();
    this.threadId             = threadId;
    this.recipient            = recipient;
    this.dateReceived         = dateReceived;
    this.body                 = body;
    this.unreadCount      = unreadCount;
    this.archived         = archived;
    this.verified         = verified;
    this.isSendingLocations = isSendingLocations;
    this.dcSummary        = dcSummary;
  }

  public @NonNull String getBody() {
    return body == null ? "" : body;
  }

  public Recipient getRecipient() {
    return recipient;
  }

  public long getDateReceived() {
    return dateReceived;
  }

  public long getThreadId() {
    return threadId;
  }

  public SpannableString getDisplayBody() {
    if(dcSummary!=null && dcSummary.getText1Meaning()==DcLot.DC_TEXT1_DRAFT) {
      String draftText = dcSummary.getText1() + ":";
      return emphasisAdded(draftText + " " + dcSummary.getText2(), 0, draftText.length());
    } else {
      return new SpannableString(getBody());
    }
  }

  private SpannableString emphasisAdded(String sequence, int start, int end) {
    SpannableString spannable = new SpannableString(sequence);
    spannable.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC),
                      start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    return spannable;
  }

  public int getUnreadCount() {
    return unreadCount;
  }

  public long getDate() {
    return getDateReceived();
  }

  public int getArchived() {
    return archived;
  }

  public boolean isVerified() {
    return verified;
  }

  public boolean isSendingLocations() {
    return  isSendingLocations;
  }
}
