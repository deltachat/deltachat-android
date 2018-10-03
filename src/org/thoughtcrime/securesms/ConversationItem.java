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
package org.thoughtcrime.securesms;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.attachments.DatabaseAttachment;
import org.thoughtcrime.securesms.components.AlertView;
import org.thoughtcrime.securesms.components.AudioView;
import org.thoughtcrime.securesms.components.AvatarImageView;
import org.thoughtcrime.securesms.components.ConversationItemFooter;
import org.thoughtcrime.securesms.components.ConversationItemThumbnail;
import org.thoughtcrime.securesms.components.DocumentView;
import org.thoughtcrime.securesms.components.QuoteView;
import org.thoughtcrime.securesms.components.SharedContactView;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contactshare.Contact;
import org.thoughtcrime.securesms.database.AttachmentDatabase;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.jobs.AttachmentDownloadJob;
import org.thoughtcrime.securesms.mms.AudioSlide;
import org.thoughtcrime.securesms.mms.DocumentSlide;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.mms.PartAuthority;
import org.thoughtcrime.securesms.mms.Slide;
import org.thoughtcrime.securesms.mms.SlideClickListener;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientModifiedListener;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.LongClickCopySpan;
import org.thoughtcrime.securesms.util.LongClickMovementMethod;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.views.Stub;
import org.whispersystems.libsignal.util.guava.Optional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A view that displays an individual conversation item within a conversation
 * thread.  Used by ComposeMessageActivity's ListActivity via a ConversationAdapter.
 *
 * @author Moxie Marlinspike
 *
 */

public class ConversationItem extends LinearLayout
    implements RecipientModifiedListener, BindableConversationItem
{
  private static final String TAG = ConversationItem.class.getSimpleName();

  private static final int MAX_MEASURE_CALLS = 3;

  private DcMsg         messageRecord;
  private Locale        locale;
  private boolean       groupThread;
  private Recipient     recipient;
  private GlideRequests glideRequests;

  protected ViewGroup              bodyBubble;
  private   QuoteView              quoteView;
  private   TextView               bodyText;
  private   ConversationItemFooter footer;
  private   TextView               groupSender;
  private   TextView               groupSenderProfileName;
  private   View                   groupSenderHolder;
  private   AvatarImageView        contactPhoto;
  private   ViewGroup              contactPhotoHolder;
  private   AlertView              alertView;
  private   ViewGroup              container;

  private @NonNull  Set<DcMsg>                      batchSelected = new HashSet<>();
  private @NonNull  Recipient                       conversationRecipient;
  private @NonNull  Stub<ConversationItemThumbnail> mediaThumbnailStub;
  private @NonNull  Stub<AudioView>                 audioViewStub;
  private @NonNull  Stub<DocumentView>              documentViewStub;
  private @NonNull  Stub<SharedContactView>         sharedContactStub;
  private @Nullable EventListener                   eventListener;

  private int defaultBubbleColor;
  private int measureCalls;

  private final PassthroughClickListener        passthroughClickListener   = new PassthroughClickListener();
  private final AttachmentDownloadClickListener downloadClickListener      = new AttachmentDownloadClickListener();
  private final SharedContactEventListener      sharedContactEventListener = new SharedContactEventListener();
  private final SharedContactClickListener      sharedContactClickListener = new SharedContactClickListener();

  private final Context context;
  private final ApplicationDcContext dcContext;

  public ConversationItem(Context context) {
    this(context, null);
  }

  public ConversationItem(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.context = context;
    this.dcContext = DcHelper.getContext(context);
  }

  @Override
  public void setOnClickListener(OnClickListener l) {
    super.setOnClickListener(new ClickListener(l));
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    initializeAttributes();

    this.bodyText                =            findViewById(R.id.conversation_item_body);
    this.footer                  =            findViewById(R.id.conversation_item_footer);
    this.groupSender             =            findViewById(R.id.group_message_sender);
    this.groupSenderProfileName  =            findViewById(R.id.group_message_sender_profile);
    this.alertView               =            findViewById(R.id.indicators_parent);
    this.contactPhoto            =            findViewById(R.id.contact_photo);
    this.contactPhotoHolder      =            findViewById(R.id.contact_photo_container);
    this.bodyBubble              =            findViewById(R.id.body_bubble);
    this.mediaThumbnailStub      = new Stub<>(findViewById(R.id.image_view_stub));
    this.audioViewStub           = new Stub<>(findViewById(R.id.audio_view_stub));
    this.documentViewStub        = new Stub<>(findViewById(R.id.document_view_stub));
    this.sharedContactStub       = new Stub<>(findViewById(R.id.shared_contact_view_stub));
    this.groupSenderHolder       =            findViewById(R.id.group_sender_holder);
    this.quoteView               =            findViewById(R.id.quote_view);
    this.container               =            findViewById(R.id.container);

    setOnClickListener(new ClickListener(null));

    bodyText.setOnLongClickListener(passthroughClickListener);
    bodyText.setOnClickListener(passthroughClickListener);

    bodyText.setMovementMethod(LongClickMovementMethod.getInstance(getContext()));
  }

  @Override
  public void bind(@NonNull DcMsg                   messageRecord,
                   @NonNull DcChat                  dcChat,
                   @NonNull GlideRequests           glideRequests,
                   @NonNull Locale                  locale,
                   @NonNull Set<DcMsg>              batchSelected,
                   @NonNull Recipient               recipients,
                   boolean                          pulseHighlight)
  {
    this.messageRecord          = messageRecord;
    this.locale                 = locale;
    this.glideRequests          = glideRequests;
    this.batchSelected          = batchSelected;
    this.conversationRecipient  = recipients;
    this.groupThread            = dcChat.isGroup();
    this.recipient              = dcContext.getRecipient(dcContext.getChat(messageRecord.getChatId()));

    this.recipient.addListener(this);
    this.conversationRecipient.addListener(this);

    setGutterSizes(messageRecord, groupThread);
    setMessageShape(messageRecord, groupThread);
    setMediaAttributes(messageRecord, conversationRecipient, groupThread);
    setInteractionState(messageRecord, pulseHighlight);
    setBodyText(messageRecord);
    setBubbleState(messageRecord);
    setStatusIcons(messageRecord);
    setContactPhoto(recipient);
    setGroupMessageStatus(messageRecord, recipient);
    setAuthor(messageRecord, groupThread);
    setQuote(messageRecord, groupThread);
    setMessageSpacing(context, messageRecord, groupThread);
    setFooter(messageRecord, locale, groupThread);
  }

  @Override
  public void setEventListener(@Nullable EventListener eventListener) {
    this.eventListener = eventListener;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    if (isInEditMode()) {
      return;
    }

    boolean needsMeasure = false;

    if (hasQuote(messageRecord)) {
      int quoteWidth     = quoteView.getMeasuredWidth();
      int availableWidth = getAvailableMessageBubbleWidth(quoteView);

      if (quoteWidth != availableWidth) {
        quoteView.getLayoutParams().width = availableWidth;
        needsMeasure = true;
      }
    }

    ConversationItemFooter activeFooter   = getActiveFooter(messageRecord);
    int                    availableWidth = getAvailableMessageBubbleWidth(footer);

    if (activeFooter.getVisibility() != GONE && activeFooter.getMeasuredWidth() != availableWidth) {
      activeFooter.getLayoutParams().width = availableWidth;
      needsMeasure = true;
    }

    if (needsMeasure) {
      if (measureCalls < MAX_MEASURE_CALLS) {
        measureCalls++;
        measure(widthMeasureSpec, heightMeasureSpec);
      } else {
        Log.w(TAG, "Hit measure() cap of " + MAX_MEASURE_CALLS);
      }
    } else {
      measureCalls = 0;
    }
  }

  private int getAvailableMessageBubbleWidth(@NonNull View forView) {
    int availableWidth;
    if (hasAudio(messageRecord)) {
      availableWidth = audioViewStub.get().getMeasuredWidth() + ViewUtil.getLeftMargin(audioViewStub.get()) + ViewUtil.getRightMargin(audioViewStub.get());
    } else if (hasThumbnail(messageRecord)) {
      availableWidth = mediaThumbnailStub.get().getMeasuredWidth();
    } else {
      availableWidth = bodyBubble.getMeasuredWidth() - bodyBubble.getPaddingLeft() - bodyBubble.getPaddingRight();
    }

    availableWidth -= ViewUtil.getLeftMargin(forView) + ViewUtil.getRightMargin(forView);

    return availableWidth;
  }

  private void initializeAttributes() {
    final int[]      attributes = new int[] {R.attr.conversation_item_bubble_background};
    final TypedArray attrs      = context.obtainStyledAttributes(attributes);

    defaultBubbleColor = attrs.getColor(0, Color.WHITE);
    attrs.recycle();
  }

  @Override
  public void unbind() {
    if (recipient != null) {
      recipient.removeListener(this);
    }
  }

  public DcMsg getMessageRecord() {
    return messageRecord;
  }

  /// DcMsg Attribute Parsers

  private void setBubbleState(DcMsg messageRecord) {
    if (messageRecord.isOutgoing()) {
      bodyBubble.getBackground().setColorFilter(defaultBubbleColor, PorterDuff.Mode.MULTIPLY);
    } else {
      bodyBubble.getBackground().setColorFilter(defaultBubbleColor, PorterDuff.Mode.MULTIPLY);
      bodyBubble.getBackground().setColorFilter(dcContext.getRecipient(messageRecord.getChatId()).getColor().toConversationColor(context), PorterDuff.Mode.MULTIPLY);
    }

    if (audioViewStub.resolved()) {
      setAudioViewTint(messageRecord, this.conversationRecipient);
    }
  }

  private void setAudioViewTint(DcMsg messageRecord, Recipient recipient) {
    if (messageRecord.isOutgoing()) {
      if (DynamicTheme.LIGHT.equals(TextSecurePreferences.getTheme(context))) {
        audioViewStub.get().setTint(getContext().getResources().getColor(R.color.core_light_60), defaultBubbleColor);
      } else {
        audioViewStub.get().setTint(Color.WHITE, defaultBubbleColor);
      }
    } else {
      audioViewStub.get().setTint(Color.WHITE, recipient.getColor().toConversationColor(context));
    }
  }

  private void setInteractionState(DcMsg messageRecord, boolean pulseHighlight) {
    if (batchSelected.contains(messageRecord)) {
      setBackgroundResource(R.drawable.conversation_item_background);
      setSelected(true);
    } else if (pulseHighlight) {
      setBackgroundResource(R.drawable.conversation_item_background_animated);
      setSelected(true);
      postDelayed(() -> setSelected(false), 500);
    } else {
      setSelected(false);
    }

    if (mediaThumbnailStub.resolved()) {
      mediaThumbnailStub.get().setFocusable(!shouldInterceptClicks(messageRecord) && batchSelected.isEmpty());
      mediaThumbnailStub.get().setClickable(!shouldInterceptClicks(messageRecord) && batchSelected.isEmpty());
      mediaThumbnailStub.get().setLongClickable(batchSelected.isEmpty());
    }

    if (audioViewStub.resolved()) {
      audioViewStub.get().setFocusable(!shouldInterceptClicks(messageRecord) && batchSelected.isEmpty());
      audioViewStub.get().setClickable(batchSelected.isEmpty());
      audioViewStub.get().setEnabled(batchSelected.isEmpty());
    }

    if (documentViewStub.resolved()) {
      documentViewStub.get().setFocusable(!shouldInterceptClicks(messageRecord) && batchSelected.isEmpty());
      documentViewStub.get().setClickable(batchSelected.isEmpty());
    }
  }

  private boolean hasAudio(DcMsg messageRecord) {
    int type = messageRecord.getType();
    return type==DcMsg.DC_MSG_AUDIO || type==DcMsg.DC_MSG_VOICE;
  }

  private boolean hasThumbnail(DcMsg messageRecord) {
    int type = messageRecord.getType();
    return type==DcMsg.DC_MSG_GIF || type==DcMsg.DC_MSG_IMAGE || type==DcMsg.DC_MSG_VIDEO;
  }

  private boolean hasOnlyThumbnail(DcMsg messageRecord) {
    return hasThumbnail(messageRecord) && !hasAudio(messageRecord) && !hasDocument(messageRecord);
  }

  private boolean hasDocument(DcMsg dcMsg) {
    return dcMsg.getType()==DcMsg.DC_MSG_FILE && !dcMsg.isSetupMessage();
  }

  private boolean hasQuote(DcMsg messageRecord) {
    return false;
  }

  private void setBodyText(DcMsg messageRecord) {
    bodyText.setClickable(false);
    bodyText.setFocusable(false);
    bodyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, TextSecurePreferences.getMessageBodyTextSize(context));

    if (messageRecord.isSetupMessage()) {
     bodyText.setText(context.getString(R.string.autocrypt__asm_tap_body));
    }
    else {
      bodyText.setText(linkifyMessageBody(new SpannableString(messageRecord.getText()), batchSelected.isEmpty()));
    }

    bodyText.setVisibility(View.VISIBLE);
  }

  private void setMediaAttributes(@NonNull DcMsg           messageRecord,
                                  @NonNull Recipient       conversationRecipient,
                                           boolean         isGroupThread)
  {
    boolean showControls = !messageRecord.isFailed() && !Util.isOwnNumber(context, conversationRecipient.getAddress());

    if (hasAudio(messageRecord)) {
      audioViewStub.get().setVisibility(View.VISIBLE);
      if (mediaThumbnailStub.resolved()) mediaThumbnailStub.get().setVisibility(View.GONE);
      if (documentViewStub.resolved())   documentViewStub.get().setVisibility(View.GONE);
      if (sharedContactStub.resolved())  sharedContactStub.get().setVisibility(GONE);

      //noinspection ConstantConditions
      audioViewStub.get().setAudio(new AudioSlide(context, messageRecord), showControls);
      audioViewStub.get().setDownloadClickListener(downloadClickListener);
      audioViewStub.get().setOnLongClickListener(passthroughClickListener);

      ViewUtil.updateLayoutParams(bodyText, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      ViewUtil.updateLayoutParams(groupSenderHolder, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      footer.setVisibility(VISIBLE);
    }
    else if (hasDocument(messageRecord)) {
      documentViewStub.get().setVisibility(View.VISIBLE);
      if (mediaThumbnailStub.resolved()) mediaThumbnailStub.get().setVisibility(View.GONE);
      if (audioViewStub.resolved())      audioViewStub.get().setVisibility(View.GONE);
      if (sharedContactStub.resolved())  sharedContactStub.get().setVisibility(GONE);

      //noinspection ConstantConditions
      documentViewStub.get().setDocument(new DocumentSlide(context, messageRecord), showControls);
      documentViewStub.get().setDocumentClickListener(new ThumbnailClickListener());
      documentViewStub.get().setDownloadClickListener(downloadClickListener);
      documentViewStub.get().setOnLongClickListener(passthroughClickListener);

      ViewUtil.updateLayoutParams(bodyText, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      ViewUtil.updateLayoutParams(groupSenderHolder, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      footer.setVisibility(VISIBLE);
    }
    else if (hasThumbnail(messageRecord)) {
      mediaThumbnailStub.get().setVisibility(View.VISIBLE);
      if (audioViewStub.resolved())    audioViewStub.get().setVisibility(View.GONE);
      if (documentViewStub.resolved()) documentViewStub.get().setVisibility(View.GONE);
      if (sharedContactStub.resolved())  sharedContactStub.get().setVisibility(GONE);

      //noinspection ConstantConditions
      mediaThumbnailStub.get().setImageResource(glideRequests,
                                                new AudioSlide(context, messageRecord), // TODO: this is should a same as returned from getThumbnailSlide()
                                                showControls,
                                                false,
                                                messageRecord.getWidth(100),
                                                messageRecord.getHeight(100));
      mediaThumbnailStub.get().setThumbnailClickListener(new ThumbnailClickListener());
      mediaThumbnailStub.get().setDownloadClickListener(downloadClickListener);
      mediaThumbnailStub.get().setOnLongClickListener(passthroughClickListener);
      mediaThumbnailStub.get().setOnClickListener(passthroughClickListener);
      mediaThumbnailStub.get().showShade(TextUtils.isEmpty(messageRecord.getText()));

      setThumbnailOutlineCorners(messageRecord, isGroupThread);

      ViewUtil.updateLayoutParams(bodyText, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      ViewUtil.updateLayoutParams(groupSenderHolder, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      footer.setVisibility(VISIBLE);
    }
    else {
      if (mediaThumbnailStub.resolved()) mediaThumbnailStub.get().setVisibility(View.GONE);
      if (audioViewStub.resolved())      audioViewStub.get().setVisibility(View.GONE);
      if (documentViewStub.resolved())   documentViewStub.get().setVisibility(View.GONE);
      if (sharedContactStub.resolved())  sharedContactStub.get().setVisibility(GONE);

      ViewUtil.updateLayoutParams(bodyText, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      ViewUtil.updateLayoutParams(groupSenderHolder, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      footer.setVisibility(VISIBLE);
    }
  }

  private void setThumbnailOutlineCorners(@NonNull DcMsg           current,
                                          boolean                  isGroupThread)
  {
    int defaultRadius  = readDimen(R.dimen.message_corner_radius);
    int collapseRadius = readDimen(R.dimen.message_corner_collapse_radius);

    int topLeft     = defaultRadius;
    int topRight    = defaultRadius;
    int bottomLeft  = defaultRadius;
    int bottomRight = defaultRadius;

    topLeft     = defaultRadius;
    topRight    = defaultRadius;
    bottomLeft  = defaultRadius;
    bottomRight = defaultRadius;

    if (!TextUtils.isEmpty(current.getText())) {
      bottomLeft  = 0;
      bottomRight = 0;
    }

    if (!current.isOutgoing() && isGroupThread) {
      topLeft  = 0;
      topRight = 0;
    }

    if (hasQuote(messageRecord)) {
      topLeft  = 0;
      topRight = 0;
    }

    mediaThumbnailStub.get().setOutlineCorners(topLeft, topRight, bottomRight, bottomLeft);
  }

  private void setSharedContactCorners(@NonNull DcMsg current, boolean isGroupThread) {
    sharedContactStub.get().setSingularStyle();
  }

  private void setContactPhoto(@NonNull Recipient recipient) {
    if (contactPhoto == null) return;

    if (messageRecord.isOutgoing() || !groupThread) {
      contactPhoto.setVisibility(View.GONE);
    } else {
      contactPhoto.setAvatar(glideRequests, recipient, true);
      contactPhoto.setVisibility(View.VISIBLE);
    }
  }

  private SpannableString linkifyMessageBody(SpannableString messageBody, boolean shouldLinkifyAllLinks) {
    boolean hasLinks = Linkify.addLinks(messageBody, shouldLinkifyAllLinks ? Linkify.ALL : 0);

    if (hasLinks) {
      URLSpan[] urlSpans = messageBody.getSpans(0, messageBody.length(), URLSpan.class);
      for (URLSpan urlSpan : urlSpans) {
        int start = messageBody.getSpanStart(urlSpan);
        int end = messageBody.getSpanEnd(urlSpan);
        messageBody.setSpan(new LongClickCopySpan(urlSpan.getURL()), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
    }
    return messageBody;
  }

  private void setStatusIcons(DcMsg messageRecord) {
    // disable the icon for for, we should use the same ui on desktop/ios here, so, for now
    //bodyText.setCompoundDrawablesWithIntrinsicBounds(0, 0, messageRecord.isSetupMessage() ? R.drawable.ic_menu_login : 0, 0);

    if (messageRecord.isFailed()) {
      alertView.setFailed();
    } else {
      alertView.setNone();
    }
  }

  private void setQuote(@NonNull DcMsg current, boolean isGroupThread) {
//    if (current.isMms() && !current.isMmsNotification() && ((MediaMmsDcMsg)current).getQuote() != null) {
//      Quote quote = ((MediaMmsDcMsg)current).getQuote();
//      assert quote != null;
//      quoteView.setQuote(glideRequests, quote.getId(), Recipient.from(context, quote.getAuthor(), true), quote.getText(), quote.getAttachment());
//      quoteView.setVisibility(View.VISIBLE);
//      quoteView.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
//
//      quoteView.setOnClickListener(view -> {
//        if (eventListener != null && batchIsEmpty()) {
//          eventListener.onQuoteClicked((MmsDcMsg) current);
//        } else {
//          passthroughClickListener.onClick(view);
//        }
//      });
//
//      quoteView.setOnLongClickListener(passthroughClickListener);
//
//      if (isStartOfMessageCluster(current, previous, isGroupThread)) {
//        if (current.isOutgoing()) {
//          quoteView.setTopCornerSizes(true, true);
//        } else if (isGroupThread) {
//          quoteView.setTopCornerSizes(false, false);
//        } else {
//          quoteView.setTopCornerSizes(true, true);
//        }
//      } else if (!isSingularMessage(current, previous, next, isGroupThread)) {
//        if (current.isOutgoing()) {
//          quoteView.setTopCornerSizes(true, false);
//        } else {
//          quoteView.setTopCornerSizes(false, true);
//        }
//      }
//
//      if (mediaThumbnailStub.resolved()) {
//        ViewUtil.setTopMargin(mediaThumbnailStub.get(), readDimen(R.dimen.message_bubble_top_padding));
//      }
//    } else {
//      quoteView.dismiss();
//
//      if (mediaThumbnailStub.resolved()) {
//        ViewUtil.setTopMargin(mediaThumbnailStub.get(), 0);
//      }
//    }
  }

  private void setGutterSizes(@NonNull DcMsg current, boolean isGroupThread) {
    if (isGroupThread && current.isOutgoing()) {
      ViewUtil.setLeftMargin(container, readDimen(R.dimen.conversation_group_left_gutter));
    } else if (current.isOutgoing()) {
      ViewUtil.setLeftMargin(container, readDimen(R.dimen.conversation_individual_left_gutter));
    }
  }

  private void setFooter(@NonNull DcMsg current, @NonNull Locale locale, boolean isGroupThread) {
    ViewUtil.updateLayoutParams(footer, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

    footer.setVisibility(GONE);
    if (sharedContactStub.resolved()) sharedContactStub.get().getFooter().setVisibility(GONE);
    if (mediaThumbnailStub.resolved()) mediaThumbnailStub.get().getFooter().setVisibility(GONE);

    ConversationItemFooter activeFooter = getActiveFooter(current);
    activeFooter.setVisibility(VISIBLE);
    activeFooter.setMessageRecord(current, locale);
  }

  private ConversationItemFooter getActiveFooter(@NonNull DcMsg messageRecord) {
    if (hasOnlyThumbnail(messageRecord) && TextUtils.isEmpty(messageRecord.getText())) {
      return mediaThumbnailStub.get().getFooter();
    } else {
      return footer;
    }
  }

  private int readDimen(@DimenRes int dimenId) {
    return context.getResources().getDimensionPixelOffset(dimenId);
  }

  private boolean shouldInterceptClicks(DcMsg messageRecord) {
    return batchSelected.isEmpty() && (messageRecord.isFailed());
  }

  @SuppressLint("SetTextI18n")
  private void setGroupMessageStatus(DcMsg messageRecord, Recipient recipient) {
    if (groupThread && !messageRecord.isOutgoing()) {
      this.groupSender.setText(recipient.toShortString());

      if (recipient.getName() == null && !TextUtils.isEmpty(recipient.getProfileName())) {
        this.groupSenderProfileName.setText("~" + recipient.getProfileName());
        this.groupSenderProfileName.setVisibility(View.VISIBLE);
      } else {
        this.groupSenderProfileName.setText(null);
        this.groupSenderProfileName.setVisibility(View.GONE);
      }
    }
  }

  private void setAuthor(@NonNull DcMsg current, boolean isGroupThread) {
    if (isGroupThread && !current.isOutgoing()) {

      if (contactPhotoHolder != null) {
        contactPhotoHolder.setVisibility(VISIBLE);
      }
      /*if (!previous.isPresent() || previous.get().isInfo() || current.getFromId() != previous.get().getFromId() ||
          !DateUtils.isSameDay(previous.get().getTimestamp(), current.getTimestamp()))
      {*/
        groupSenderHolder.setVisibility(VISIBLE);
      /*} else {
        groupSenderHolder.setVisibility(GONE);
      }*/

      //if (!next.isPresent() || next.get().isInfo() || current.getFromId() != previous.get().getFromId()) {
        contactPhoto.setVisibility(VISIBLE);
      //} else {
      //  contactPhoto.setVisibility(GONE);
      //}
    } else {
      groupSenderHolder.setVisibility(GONE);

      if (contactPhotoHolder != null) {
        contactPhotoHolder.setVisibility(GONE);
      }
    }
  }

  private void setMessageShape(@NonNull DcMsg current, boolean isGroupThread) {
    int background;
    background = current.isOutgoing() ? R.drawable.message_bubble_background_sent_alone
                                      : R.drawable.message_bubble_background_received_alone;
    bodyBubble.setBackgroundResource(background);
  }

  private void setMessageSpacing(@NonNull Context context, @NonNull DcMsg current, boolean isGroupThread) {
    int spacingTop = readDimen(context, R.dimen.conversation_vertical_message_spacing_collapse);
    int spacingBottom = spacingTop;

    ViewUtil.setPaddingTop(this, spacingTop);
    ViewUtil.setPaddingBottom(this, spacingBottom);
  }

  private int readDimen(@NonNull Context context, @DimenRes int dimenId) {
    return context.getResources().getDimensionPixelOffset(dimenId);
  }

  /// Event handlers

  @Override
  public void onModified(final Recipient modified) {
    Util.runOnMain(() -> {
      setBubbleState(messageRecord);
      setContactPhoto(recipient);
      setGroupMessageStatus(messageRecord, recipient);
      setAudioViewTint(messageRecord, conversationRecipient);
    });
  }

  private class SharedContactEventListener implements SharedContactView.EventListener {
    @Override
    public void onAddToContactsClicked(@NonNull Contact contact) {
      if (eventListener != null && batchSelected.isEmpty()) {
        eventListener.onAddToContactsClicked(contact);
      } else {
        passthroughClickListener.onClick(sharedContactStub.get());
      }
    }

    @Override
    public void onInviteClicked(@NonNull List<Recipient> choices) {
      if (eventListener != null && batchSelected.isEmpty()) {
        eventListener.onInviteSharedContactClicked(choices);
      } else {
        passthroughClickListener.onClick(sharedContactStub.get());
      }
    }

    @Override
    public void onMessageClicked(@NonNull List<Recipient> choices) {
      if (eventListener != null && batchSelected.isEmpty()) {
        eventListener.onMessageSharedContactClicked(choices);
      } else {
        passthroughClickListener.onClick(sharedContactStub.get());
      }
    }
  }

  private class SharedContactClickListener implements View.OnClickListener {
    @Override
    public void onClick(View view) {
//      if (eventListener != null && batchIsEmpty() && messageRecord.isMms() && !((MmsDcMsg) messageRecord).getSharedContacts().isEmpty()) {
//        eventListener.onSharedContactDetailsClicked(((MmsDcMsg) messageRecord).getSharedContacts().get(0), sharedContactStub.get().getAvatarView());
//      } else {
        passthroughClickListener.onClick(view);
//      }
    }
  }

  private class AttachmentDownloadClickListener implements SlideClickListener {
    @Override
    public void onClick(View v, final Slide slide) {
//      if (messageRecord.isMmsNotification()) {
//        ApplicationContext.getInstance(context)
//                          .getJobManager()
//                          .add(new MmsDownloadJob(context, messageRecord.getId(),
//                                                  messageRecord.getThreadId(), false));
//      } else {
        DatabaseFactory.getAttachmentDatabase(context).setTransferState(messageRecord.getId(),
                                                                        slide.asAttachment(),
                                                                        AttachmentDatabase.TRANSFER_PROGRESS_STARTED);

        ApplicationContext.getInstance(context)
                          .getJobManager()
                          .add(new AttachmentDownloadJob(context, messageRecord.getId(),
                                                         ((DatabaseAttachment)slide.asAttachment()).getAttachmentId(), true));
//      }
    }
  }

  private class ThumbnailClickListener implements SlideClickListener {
    public void onClick(final View v, final Slide slide) {
      if (shouldInterceptClicks(messageRecord) || !batchSelected.isEmpty()) {
        performClick();
      } else if (MediaPreviewActivity.isContentTypeSupported(slide.getContentType()) && slide.getUri() != null) {
        Intent intent = new Intent(context, MediaPreviewActivity.class);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(slide.getUri(), slide.getContentType());
        intent.putExtra(MediaPreviewActivity.ADDRESS_EXTRA, conversationRecipient.getAddress());
        intent.putExtra(MediaPreviewActivity.OUTGOING_EXTRA, messageRecord.isOutgoing());
        intent.putExtra(MediaPreviewActivity.DATE_EXTRA, messageRecord.getTimestamp());
        intent.putExtra(MediaPreviewActivity.SIZE_EXTRA, slide.asAttachment().getSize());
        intent.putExtra(MediaPreviewActivity.LEFT_IS_RECENT_EXTRA, false);

        context.startActivity(intent);
      } else if (slide.getUri() != null) {
        Log.w(TAG, "Clicked: " + slide.getUri() + " , " + slide.getContentType());
        Uri publicUri = PartAuthority.getAttachmentPublicUri(slide.getUri());
        Log.w(TAG, "Public URI: " + publicUri);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(PartAuthority.getAttachmentPublicUri(slide.getUri()), slide.getContentType());
        try {
          context.startActivity(intent);
        } catch (ActivityNotFoundException anfe) {
          Log.w(TAG, "No activity existed to view the media.");
          Toast.makeText(context, R.string.ConversationItem_unable_to_open_media, Toast.LENGTH_LONG).show();
        }
      }
    }
  }

  private class PassthroughClickListener implements View.OnLongClickListener, View.OnClickListener {

    @Override
    public boolean onLongClick(View v) {
      if (bodyText.hasSelection()) {
        return false;
      }
      performLongClick();
      return true;
    }

    @Override
    public void onClick(View v) {
      performClick();
    }
  }

  private class ClickListener implements View.OnClickListener {
    private OnClickListener parent;

    ClickListener(@Nullable OnClickListener parent) {
      this.parent = parent;
    }

    public void onClick(View v) {
      if (!shouldInterceptClicks(messageRecord) && parent != null) {
        parent.onClick(v);
      }
    }
  }
}
