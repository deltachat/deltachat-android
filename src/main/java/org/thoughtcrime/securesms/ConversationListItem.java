/*
 * Copyright (C) 2014-2017 Open Whisper Systems
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
package org.thoughtcrime.securesms;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amulyakhare.textdrawable.TextDrawable;
import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcLot;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.components.AvatarView;
import org.thoughtcrime.securesms.components.DeliveryStatusView;
import org.thoughtcrime.securesms.components.FromTextView;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.model.ThreadRecord;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.ThemeUtil;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.Collections;
import java.util.Set;

public class ConversationListItem extends RelativeLayout
                                  implements BindableConversationListItem, Unbindable
{
  private final static Typeface  BOLD_TYPEFACE  = Typeface.create("sans-serif-medium", Typeface.NORMAL);
  private final static Typeface  LIGHT_TYPEFACE = Typeface.create("sans-serif", Typeface.NORMAL);

  private Set<Long>          selectedThreads;
  private long               chatId;
  private int                msgId;
  private TextView           subjectView;
  private FromTextView       fromView;
  private TextView           dateView;
  private TextView           archivedBadgeView;
  private TextView           requestBadgeView;
  private DeliveryStatusView deliveryStatusIndicator;
  private ImageView          unreadIndicator;

  private AvatarView avatar;

  public ConversationListItem(Context context) {
    this(context, null);
  }

  public ConversationListItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    this.subjectView             = findViewById(R.id.subject);
    this.fromView                = findViewById(R.id.from_text);
    this.dateView                = findViewById(R.id.date);
    this.deliveryStatusIndicator = new DeliveryStatusView(findViewById(R.id.delivery_indicator));
    this.avatar                  = findViewById(R.id.avatar);
    this.archivedBadgeView       = findViewById(R.id.archived_badge);
    this.requestBadgeView        = findViewById(R.id.request_badge);
    this.unreadIndicator         = findViewById(R.id.unread_indicator);

    ViewUtil.setTextViewGravityStart(this.fromView, getContext());
    ViewUtil.setTextViewGravityStart(this.subjectView, getContext());
  }

  @Override
  public void bind(@NonNull ThreadRecord thread,
                   int msgId,
                   @NonNull DcLot dcSummary,
                   @NonNull GlideRequests glideRequests,
                   @NonNull Set<Long> selectedThreads,
                   boolean batchMode)
  {
    bind(thread, msgId, dcSummary, glideRequests, selectedThreads, batchMode, null);
  }

  public void bind(@NonNull ThreadRecord thread,
                   int msgId,
                   @NonNull DcLot dcSummary,
                   @NonNull GlideRequests glideRequests,
                   @NonNull Set<Long> selectedThreads,
                   boolean batchMode,
                   @Nullable String highlightSubstring)
  {
    this.selectedThreads  = selectedThreads;
    Recipient recipient   = thread.getRecipient();
    this.chatId           = thread.getThreadId();
    this.msgId            = msgId;

    int state       = dcSummary.getState();
    int unreadCount = thread.getUnreadCount();

    if (highlightSubstring != null) {
      this.fromView.setText(getHighlightedSpan(recipient.getName(), highlightSubstring));
    } else {
      this.fromView.setText(recipient, state!=DcMsg.DC_STATE_IN_FRESH);
    }

    subjectView.setVisibility(chatId == DcChat.DC_CHAT_ID_ARCHIVED_LINK? GONE : VISIBLE);
    this.subjectView.setText(thread.getDisplayBody());
    this.subjectView.setTypeface(state==DcMsg.DC_STATE_IN_FRESH ? BOLD_TYPEFACE : LIGHT_TYPEFACE);
    this.subjectView.setTextColor(state==DcMsg.DC_STATE_IN_FRESH ? ThemeUtil.getThemedColor(getContext(), R.attr.conversation_list_item_unread_color)
                                                                 : ThemeUtil.getThemedColor(getContext(), R.attr.conversation_list_item_subject_color));

    if (thread.getDate() > 0) {
      CharSequence date = DateUtils.getBriefRelativeTimeSpanString(getContext(), thread.getDate());
      dateView.setText(date);
    }
    else {
      dateView.setText("");
    }

    dateView.setCompoundDrawablesWithIntrinsicBounds(
      thread.isSendingLocations()? R.drawable.ic_location_chatlist : 0, 0,
      thread.getVisibility()==DcChat.DC_CHAT_VISIBILITY_PINNED? R.drawable.ic_pinned_chatlist : 0, 0
    );

    setStatusIcons(thread.getVisibility(), state, unreadCount, thread.isContactRequest(), thread.isMuted() || chatId == DcChat.DC_CHAT_ID_ARCHIVED_LINK);
    setBatchState(batchMode);
    setBgColor(thread);

    this.avatar.setAvatar(glideRequests, recipient, false);

    DcContact contact = recipient.getDcContact();
    avatar.setSeenRecently(contact != null && contact.wasSeenRecently());

    fromView.setCompoundDrawablesWithIntrinsicBounds(
        thread.isMuted()? R.drawable.ic_volume_off_grey600_18dp : 0,
        0,
        0,
        0);
  }

  public void bind(@NonNull  DcContact     contact,
                   @NonNull  GlideRequests glideRequests,
                   @Nullable String        highlightSubstring)
  {
    this.selectedThreads = Collections.emptySet();
    Recipient recipient  = new Recipient(getContext(), contact);

    fromView.setText(getHighlightedSpan(contact.getDisplayName(), highlightSubstring));
    fromView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    subjectView.setVisibility(GONE);
    dateView.setText("");
    dateView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    archivedBadgeView.setVisibility(GONE);
    requestBadgeView.setVisibility(GONE);
    unreadIndicator.setVisibility(GONE);
    deliveryStatusIndicator.setNone();

    setBatchState(false);
    avatar.setAvatar(glideRequests, recipient, false);
    avatar.setSeenRecently(contact.wasSeenRecently());
  }

  public void bind(@NonNull  DcMsg         messageResult,
                   @NonNull  GlideRequests glideRequests,
                   @Nullable String        highlightSubstring)
  {
    DcContext dcContext = DcHelper.getContext(getContext());
    DcContact sender = dcContext.getContact(messageResult.getFromId());
    this.selectedThreads = Collections.emptySet();
    Recipient recipient  = new Recipient(getContext(), sender);

    fromView.setText(recipient, true);
    fromView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    subjectView.setVisibility(VISIBLE);
    subjectView.setText(getHighlightedSpan(messageResult.getSummarytext(512), highlightSubstring));

    long timestamp = messageResult.getTimestamp();
    if(timestamp>0) {
      dateView.setText(DateUtils.getBriefRelativeTimeSpanString(getContext(), messageResult.getTimestamp()));
    }
    else {
      dateView.setText("");
    }
    dateView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    archivedBadgeView.setVisibility(GONE);
    requestBadgeView.setVisibility(GONE);
    unreadIndicator.setVisibility(GONE);
    deliveryStatusIndicator.setNone();

    setBatchState(false);
    avatar.setAvatar(glideRequests, recipient, false);
    avatar.setSeenRecently(false);
  }

  @Override
  public void unbind() {
  }

  private void setBatchState(boolean batch) {
    setSelected(batch && selectedThreads.contains(chatId));
  }

  public long getChatId() {
    return chatId;
  }

  public int getMsgId() {
    return msgId;
  }

  private void setStatusIcons(int visibility, int state, int unreadCount, boolean isContactRequest, boolean isMuted) {
    if (visibility==DcChat.DC_CHAT_VISIBILITY_ARCHIVED)
    {
      archivedBadgeView.setVisibility(View.VISIBLE);
      requestBadgeView.setVisibility(isContactRequest ? View.VISIBLE : View.GONE);
      deliveryStatusIndicator.setNone();
    }
    else if (isContactRequest) {
      requestBadgeView.setVisibility(View.VISIBLE);
      archivedBadgeView.setVisibility(View.GONE);
      deliveryStatusIndicator.setNone();
    }
    else
    {
      requestBadgeView.setVisibility(View.GONE);
      archivedBadgeView.setVisibility(View.GONE);

      if (state == DcMsg.DC_STATE_OUT_FAILED) {
        deliveryStatusIndicator.setFailed();
      } else if (state == DcMsg.DC_STATE_OUT_MDN_RCVD) {
        deliveryStatusIndicator.setRead();
      } else if (state == DcMsg.DC_STATE_OUT_DELIVERED) {
        deliveryStatusIndicator.setSent();
      } else if (state == DcMsg.DC_STATE_OUT_PREPARING) {
        deliveryStatusIndicator.setPreparing();
      } else if (state == DcMsg.DC_STATE_OUT_PENDING) {
        deliveryStatusIndicator.setPending();
      } else {
        deliveryStatusIndicator.setNone();
      }

      if (state == DcMsg.DC_STATE_OUT_FAILED) {
        deliveryStatusIndicator.setTint(Color.RED);
      } else {
        deliveryStatusIndicator.resetTint();
      }
    }

    if(unreadCount==0 || isContactRequest) {
      unreadIndicator.setVisibility(View.GONE);
    } else {
      final int color = getResources().getColor(isMuted ? (ThemeUtil.isDarkTheme(getContext()) ? R.color.unread_count_muted_dark : R.color.unread_count_muted) : R.color.unread_count);
      unreadIndicator.setImageDrawable(TextDrawable.builder()
        .beginConfig()
        .width(ViewUtil.dpToPx(getContext(), 24))
        .height(ViewUtil.dpToPx(getContext(), 24))
        .textColor(Color.WHITE)
        .bold()
        .endConfig()
        .buildRound(String.valueOf(unreadCount), color));
      unreadIndicator.setVisibility(View.VISIBLE);
    }
  }

  private void setBgColor(ThreadRecord thread) {
    int bg = R.attr.conversation_list_item_background;
    if (thread!=null && thread.getVisibility()==DcChat.DC_CHAT_VISIBILITY_PINNED) {
        bg = R.attr.pinned_list_item_background;
    }
    try (TypedArray ta = getContext().obtainStyledAttributes(new int[]{bg})) {
      ViewUtil.setBackground(this, ta.getDrawable(0));
    }
  }

  private Spanned getHighlightedSpan(@Nullable String value,
                                     @Nullable String highlight)
  {
    if (TextUtils.isEmpty(value)) {
      return new SpannableString("");
    }

    value = value.replaceAll("\n", " ");

    if (TextUtils.isEmpty(highlight)) {
      return new SpannableString(value);
    }

    String       normalizedValue  = value.toLowerCase(Util.getLocale());
    String       normalizedTest   = highlight.toLowerCase(Util.getLocale());

    Spannable spanned          = new SpannableString(value);
    int       searchStartIndex = 0;

    for (String token : normalizedTest.split(" ")) {
      if (token.trim().isEmpty()) continue;
      if (searchStartIndex >= spanned.length()) {
        break;
      }

      int start = normalizedValue.indexOf(token, searchStartIndex);

      if (start >= 0) {
        int end = Math.min(start + token.length(), spanned.length());
        spanned.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        searchStartIndex = end;
      }
    }

    return spanned;
  }

  public void hideItemDivider() {
    View itemDivider = findViewById(R.id.item_divider);
    itemDivider.setVisibility(View.GONE);
  }
}
