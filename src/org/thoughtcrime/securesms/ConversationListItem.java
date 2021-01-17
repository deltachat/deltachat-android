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
import com.annimon.stream.Stream;
import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcLot;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.components.AvatarImageView;
import org.thoughtcrime.securesms.components.DeliveryStatusView;
import org.thoughtcrime.securesms.components.FromTextView;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.model.ThreadRecord;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.ThemeUtil;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ConversationListItem extends RelativeLayout
                                  implements BindableConversationListItem, Unbindable
{
  @SuppressWarnings("unused")
  private final static String TAG = ConversationListItem.class.getSimpleName();

  private final static Typeface  BOLD_TYPEFACE  = Typeface.create("sans-serif-medium", Typeface.NORMAL);
  private final static Typeface  LIGHT_TYPEFACE = Typeface.create("sans-serif", Typeface.NORMAL);

  private DcLot              dcSummary;
  private Set<Long>          selectedThreads;
  private long               chatId;
  private int                msgId;
  private GlideRequests      glideRequests;
  private TextView           subjectView;
  private FromTextView       fromView;
  private TextView           dateView;
  private TextView           archivedView;
  private DeliveryStatusView deliveryStatusIndicator;
  private ImageView          unreadIndicator;

  private AvatarImageView contactPhotoImage;

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
    this.contactPhotoImage       = findViewById(R.id.contact_photo_image);
    this.archivedView            = findViewById(R.id.archived);
    this.unreadIndicator         = findViewById(R.id.unread_indicator);

    ViewUtil.setTextViewGravityStart(this.fromView, getContext());
    ViewUtil.setTextViewGravityStart(this.subjectView, getContext());
  }

  @Override
  public void bind(@NonNull ThreadRecord thread,
                   int msgId,
                   @NonNull DcLot dcSummary,
                   @NonNull GlideRequests glideRequests,
                   @NonNull Locale locale,
                   @NonNull Set<Long> selectedThreads,
                   boolean batchMode)
  {
    bind(thread, msgId, dcSummary, glideRequests, locale, selectedThreads, batchMode, null);
  }

  public void bind(@NonNull ThreadRecord thread,
                   int msgId,
                   @NonNull DcLot dcSummary,
                   @NonNull GlideRequests glideRequests,
                   @NonNull Locale locale,
                   @NonNull Set<Long> selectedThreads,
                   boolean batchMode,
                   @Nullable String highlightSubstring)
  {
    ApplicationDcContext dcContext = DcHelper.getContext(getContext());

    this.dcSummary        = dcSummary;
    this.selectedThreads  = selectedThreads;
    Recipient recipient   = thread.getRecipient();
    this.chatId           = thread.getThreadId();
    this.msgId            = msgId;
    this.glideRequests    = glideRequests;

    int state       = dcSummary.getState();
    int unreadCount = (state==DcMsg.DC_STATE_IN_FRESH || state==DcMsg.DC_STATE_IN_NOTICED)? thread.getUnreadCount() : 0;

    if (highlightSubstring != null) {
      this.fromView.setText(getHighlightedSpan(locale, recipient.getName(), highlightSubstring));
    } else {
      this.fromView.setText(recipient, unreadCount == 0);
    }

    this.subjectView.setText(thread.getDisplayBody());
    this.subjectView.setTypeface(unreadCount == 0 ? LIGHT_TYPEFACE : BOLD_TYPEFACE);
    this.subjectView.setTextColor(unreadCount == 0 ? ThemeUtil.getThemedColor(getContext(), R.attr.conversation_list_item_subject_color)
                                                   : ThemeUtil.getThemedColor(getContext(), R.attr.conversation_list_item_unread_color));

    if (thread.getDate() > 0) {
      CharSequence date = DateUtils.getBriefRelativeTimeSpanString(getContext(), locale, thread.getDate());
      dateView.setText(date);
    }
    else {
      dateView.setText("");
    }

    dateView.setCompoundDrawablesWithIntrinsicBounds(
        thread.getVisibility()==DcChat.DC_CHAT_VISIBILITY_PINNED? R.drawable.ic_pinned_chatlist : 0, 0,
        thread.isSendingLocations()? R.drawable.ic_location_chatlist : 0, 0
    );

    setStatusIcons(thread.getVisibility(), state, unreadCount);
    setBatchState(batchMode);
    setBgColor(thread);

    if(chatId == DcChat.DC_CHAT_ID_DEADDROP) {
      DcContact dcContact = dcContext.getContact(dcContext.getMsg(msgId).getFromId());
      this.contactPhotoImage.setAvatar(glideRequests, dcContext.getRecipient(dcContact), false);
    }
    else {
      this.contactPhotoImage.setAvatar(glideRequests, recipient, false);
    }

    fromView.setCompoundDrawablesWithIntrinsicBounds(
        thread.isMuted()? R.drawable.ic_volume_off_grey600_18dp : 0,
        0,
        thread.isProtected()? R.drawable.ic_verified : 0,
        0);
  }

  public void bind(@NonNull  DcContact     contact,
                   @NonNull  GlideRequests glideRequests,
                   @NonNull  Locale        locale,
                   @Nullable String        highlightSubstring)
  {
    this.selectedThreads = Collections.emptySet();
    Recipient recipient  = DcHelper.getContext(getContext()).getRecipient(contact);
    this.glideRequests   = glideRequests;

    fromView.setText(getHighlightedSpan(locale, contact.getDisplayName(), highlightSubstring));
    fromView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    subjectView.setText(getHighlightedSpan(locale, contact.getAddr(), highlightSubstring));
    dateView.setText("");
    dateView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    archivedView.setVisibility(GONE);
    unreadIndicator.setVisibility(GONE);
    deliveryStatusIndicator.setNone();

    setBatchState(false);
    contactPhotoImage.setAvatar(glideRequests, recipient, false);
  }

  public void bind(@NonNull  DcMsg         messageResult,
                   @NonNull  GlideRequests glideRequests,
                   @NonNull  Locale        locale,
                   @Nullable String        highlightSubstring)
  {
    ApplicationDcContext dcContext = DcHelper.getContext(getContext());
    DcContact sender = dcContext.getContact(messageResult.getFromId());
    this.selectedThreads = Collections.emptySet();
    Recipient recipient  = DcHelper.getContext(getContext()).getRecipient(sender);
    this.glideRequests   = glideRequests;

    fromView.setText(recipient, true);
    fromView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    subjectView.setText(getHighlightedSpan(locale, messageResult.getSummarytext(512), highlightSubstring));

    long timestamp = messageResult.getTimestamp();
    if(timestamp>0) {
      dateView.setText(DateUtils.getBriefRelativeTimeSpanString(getContext(), locale, messageResult.getTimestamp()));
    }
    else {
      dateView.setText("");
    }
    dateView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    archivedView.setVisibility(GONE);
    unreadIndicator.setVisibility(GONE);
    deliveryStatusIndicator.setNone();

    setBatchState(false);
    contactPhotoImage.setAvatar(glideRequests, recipient, false);
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

  public int getContactId() {
    DcContext dcContext = DcHelper.getContext(getContext());
    return dcContext.getMsg(msgId).getFromId();
  }

  private void setStatusIcons(int visibility, int state, int unreadCount) {
    if (visibility==DcChat.DC_CHAT_VISIBILITY_ARCHIVED)
    {
      this.archivedView.setVisibility(View.VISIBLE);
      deliveryStatusIndicator.setNone();
      unreadIndicator.setVisibility(View.GONE);
    }
    else
    {
      this.archivedView.setVisibility(View.GONE);
      if (state==DcMsg.DC_STATE_IN_FRESH || state==DcMsg.DC_STATE_IN_NOTICED)
      {
        deliveryStatusIndicator.setNone();
        if(unreadCount==0) {
          unreadIndicator.setVisibility(View.GONE);
        }
        else {
          unreadIndicator.setImageDrawable(TextDrawable.builder()
              .beginConfig()
              .width(ViewUtil.dpToPx(getContext(), 24))
              .height(ViewUtil.dpToPx(getContext(), 24))
              .textColor(Color.WHITE)
              .bold()
              .endConfig()
              .buildRound(String.valueOf(unreadCount), getResources().getColor(R.color.green_A700)));
          unreadIndicator.setVisibility(View.VISIBLE);
        }
      }
      else
      {
        unreadIndicator.setVisibility(View.GONE);
        if (state == DcMsg.DC_STATE_OUT_ERROR) {
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

        if (state == DcMsg.DC_STATE_OUT_ERROR) {
          deliveryStatusIndicator.setTint(Color.RED);
        } else {
          deliveryStatusIndicator.resetTint();
        }
      }
    }
  }

  private void setBgColor(ThreadRecord thread) {
    int bg = R.attr.conversation_list_item_background;
    if (chatId == DcChat.DC_CHAT_ID_DEADDROP
     || (thread!=null && thread.getVisibility()==DcChat.DC_CHAT_VISIBILITY_PINNED)) {
        bg = R.attr.pinned_list_item_background;
    }
    TypedArray ta = getContext().obtainStyledAttributes(new int[] { bg });
    ViewUtil.setBackground(this, ta.getDrawable(0));
    ta.recycle();
  }

  private Spanned getHighlightedSpan(@NonNull  Locale locale,
                                     @Nullable String value,
                                     @Nullable String highlight)
  {
    if (TextUtils.isEmpty(value)) {
      return new SpannableString("");
    }

    value = value.replaceAll("\n", " ");

    if (TextUtils.isEmpty(highlight)) {
      return new SpannableString(value);
    }

    String       normalizedValue  = value.toLowerCase(locale);
    String       normalizedTest   = highlight.toLowerCase(locale);
    List<String> testTokens       = Stream.of(normalizedTest.split(" ")).filter(s -> s.trim().length() > 0).toList();

    Spannable spanned          = new SpannableString(value);
    int       searchStartIndex = 0;

    for (String token : testTokens) {
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
