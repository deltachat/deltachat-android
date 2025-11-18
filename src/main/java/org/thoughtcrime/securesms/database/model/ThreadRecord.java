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

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.b44t.messenger.DcLot;

import org.thoughtcrime.securesms.recipients.Recipient;

/**
 * The message record model which represents thread heading messages.
 *
 * @author Moxie Marlinspike
 *
 */
public class ThreadRecord {

  private final Recipient  recipient;
  private final long       dateReceived;
  private final long       threadId;
  private final String     body;

  private           final int     unreadCount;
  private           final int     visibility;
  private           final boolean isSendingLocations;
  private           final boolean isMuted;
  private           final boolean isContactRequest;
  private @Nullable final DcLot   dcSummary;

  public ThreadRecord(@NonNull String body,
                      @NonNull Recipient recipient, long dateReceived, int unreadCount,
                      long threadId,
                      int visibility,
                      boolean isSendingLocations,
                      boolean isMuted,
                      boolean isContactRequest,
                      @Nullable DcLot dcSummary)
  {
    this.threadId             = threadId;
    this.recipient            = recipient;
    this.dateReceived         = dateReceived;
    this.body                 = body;
    this.unreadCount      = unreadCount;
    this.visibility       = visibility;
    this.isSendingLocations = isSendingLocations;
    this.isMuted          = isMuted;
    this.isContactRequest = isContactRequest;
    this.dcSummary        = dcSummary;
  }

  public @NonNull String getBody() {
    return body;
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

  public int getVisibility() {
    return visibility;
  }

  public boolean isSendingLocations() {
    return  isSendingLocations;
  }

  public boolean isMuted() {
    return  isMuted;
  }

  public boolean isContactRequest() {
    return isContactRequest;
  }
}
