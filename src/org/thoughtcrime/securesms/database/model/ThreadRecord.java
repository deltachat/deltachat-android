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
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
public class ThreadRecord extends DisplayRecord {

  private           final long    count;
  private           final int     unreadCount;
  private           final int     distributionType;
  private           final boolean archived;
  private           final long    expiresIn;
  private           final long    lastSeen;
  private           final boolean verified;
  private @Nullable final DcLot   dcSummary;

  public ThreadRecord(@NonNull Context context, @NonNull String body, @Nullable Uri snippetUri,
                      @NonNull Recipient recipient, long date, long count, int unreadCount,
                      long threadId, int deliveryReceiptCount, int status, long snippetType,
                      int distributionType, boolean archived, long expiresIn, long lastSeen,
                      int readReceiptCount, boolean verified, @Nullable DcLot dcSummary)
  {
    super(context, body, recipient, date, date, threadId, status, deliveryReceiptCount, snippetType, readReceiptCount);
    this.count            = count;
    this.unreadCount      = unreadCount;
    this.distributionType = distributionType;
    this.archived         = archived;
    this.expiresIn        = expiresIn;
    this.lastSeen         = lastSeen;
    this.verified         = verified;
    this.dcSummary        = dcSummary;
  }

  @Override
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

  public long getCount() {
    return count;
  }

  public int getUnreadCount() {
    return unreadCount;
  }

  public long getDate() {
    return getDateReceived();
  }

  public boolean isArchived() {
    return archived;
  }

  public int getDistributionType() {
    return distributionType;
  }

  public long getExpiresIn() {
    return expiresIn;
  }

  public long getLastSeen() {
    return lastSeen;
  }

  public boolean isVerified() {
    return verified;
  }
}
