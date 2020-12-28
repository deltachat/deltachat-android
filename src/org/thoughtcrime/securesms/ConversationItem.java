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

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.audio.AudioSlidePlayer;
import org.thoughtcrime.securesms.components.AudioView;
import org.thoughtcrime.securesms.components.AvatarImageView;
import org.thoughtcrime.securesms.components.ConversationItemFooter;
import org.thoughtcrime.securesms.components.ConversationItemThumbnail;
import org.thoughtcrime.securesms.components.DocumentView;
import org.thoughtcrime.securesms.components.QuoteView;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.AudioSlide;
import org.thoughtcrime.securesms.mms.DocumentSlide;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.mms.Slide;
import org.thoughtcrime.securesms.mms.SlideClickListener;
import org.thoughtcrime.securesms.mms.SlideDeck;
import org.thoughtcrime.securesms.mms.VideoSlide;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.LongClickCopySpan;
import org.thoughtcrime.securesms.util.LongClickMovementMethod;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.views.Stub;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A view that displays an individual conversation item within a conversation
 * thread.  Used by ComposeMessageActivity's ListActivity via a ConversationAdapter.
 *
 * @author Moxie Marlinspike
 *
 */

public class ConversationItem extends LinearLayout
    implements BindableConversationItem
{
  private static final String TAG = ConversationItem.class.getSimpleName();

  private static final Rect SWIPE_RECT = new Rect();

  private static final Pattern CMD_PATTERN = Pattern.compile("(?<=^|\\s)/[a-zA-Z][a-zA-Z@\\d_/.-]{0,254}");
  private static final int MAX_MEASURE_CALLS = 3;
  static long PULSE_HIGHLIGHT_MILLIS = 500;

  private DcMsg         messageRecord;
  private DcChat        dcChat;
  private DcContact     dcContact;
  private Locale        locale;
  private boolean       groupThread;
  private GlideRequests glideRequests;

  protected ViewGroup              bodyBubble;
  protected View                   replyView;
  @Nullable private QuoteView      quoteView;
  private   TextView               bodyText;
  private   ConversationItemFooter footer;
  private   TextView               groupSender;
  private   View                   groupSenderHolder;
  private   AvatarImageView        contactPhoto;
  protected ViewGroup              contactPhotoHolder;
  private   ViewGroup              container;
  private   Button                 showFullMessage;

  private @NonNull  Set<DcMsg>                      batchSelected = new HashSet<>();
  private @NonNull  Recipient                       conversationRecipient;
  private @NonNull  Stub<ConversationItemThumbnail> mediaThumbnailStub;
  private @NonNull  Stub<AudioView>                 audioViewStub;
  private @NonNull  Stub<DocumentView>              documentViewStub;
  private @Nullable EventListener                   eventListener;

  private int measureCalls;

  private int incomingBubbleColor;
  private int outgoingBubbleColor;

  private final PassthroughClickListener        passthroughClickListener   = new PassthroughClickListener();

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
    this.contactPhoto            =            findViewById(R.id.contact_photo);
    this.contactPhotoHolder      =            findViewById(R.id.contact_photo_container);
    this.bodyBubble              =            findViewById(R.id.body_bubble);
    this.mediaThumbnailStub      = new Stub<>(findViewById(R.id.image_view_stub));
    this.audioViewStub           = new Stub<>(findViewById(R.id.audio_view_stub));
    this.documentViewStub        = new Stub<>(findViewById(R.id.document_view_stub));
    this.groupSenderHolder       =            findViewById(R.id.group_sender_holder);
    this.quoteView               =            findViewById(R.id.quote_view);
    this.container               =            findViewById(R.id.container);
    this.replyView               =            findViewById(R.id.reply_icon);
    this.showFullMessage         =            findViewById(R.id.show_full_message);

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
    this.dcChat                 = dcChat;
    this.locale                 = locale;
    this.glideRequests          = glideRequests;
    this.batchSelected          = batchSelected;
    this.conversationRecipient  = recipients;
    this.groupThread            = dcChat.isGroup();

    if (groupThread && !messageRecord.isOutgoing()) {
      this.dcContact = dcContext.getContact(messageRecord.getFromId());
    }

    setGutterSizes(messageRecord, groupThread);
    setMessageShape(messageRecord);
    setMediaAttributes(messageRecord, groupThread);
    setInteractionState(messageRecord, pulseHighlight);
    setBodyText(messageRecord);
    setBubbleState(messageRecord);
    setContactPhoto();
    setGroupMessageStatus();
    setAuthor(messageRecord, groupThread);
    setMessageSpacing(context);
    setFooter(messageRecord, locale);
    setQuote(messageRecord);

  }


  @Override
  public void setEventListener(@Nullable EventListener eventListener) {
    this.eventListener = eventListener;
  }

  public boolean disallowSwipe(float downX, float downY) {
    // If it is possible to reply to a message, it should also be possible to swipe it.
    // For this to be possible we need a non-null reply icon.
    // This means that `replyView != null` must always be the same as ConversationFragment.canReplyToMsg(messageRecord).
    if (replyView == null) return true;
    if (!dcChat.canSend()) return true;

    if (!hasAudio(messageRecord)) return false;
    audioViewStub.get().getSeekBarGlobalVisibleRect(SWIPE_RECT);
    return SWIPE_RECT.contains((int) downX, (int) downY);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    if (isInEditMode()) {
      return;
    }

    boolean needsMeasure = false;

    if (hasQuote(messageRecord)) {
      if (quoteView == null) {
        throw new AssertionError();
      }
      int quoteWidth     = quoteView.getMeasuredWidth();
      int availableWidth = getAvailableMessageBubbleWidth(quoteView);

      if (quoteWidth != availableWidth) {
        quoteView.getLayoutParams().width = availableWidth;
        needsMeasure = true;
      }
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

  private void initializeAttributes() {
    final int[]      attributes = new int[] {
        R.attr.conversation_item_incoming_bubble_color,
        R.attr.conversation_item_outgoing_bubble_color,
    };
    final TypedArray attrs      = context.obtainStyledAttributes(attributes);

    incomingBubbleColor = attrs.getColor(0, Color.WHITE);
    outgoingBubbleColor = attrs.getColor(1, Color.WHITE);
    attrs.recycle();
  }

  @Override
  public void unbind() {
  }

  public DcMsg getMessageRecord() {
    return messageRecord;
  }

  /// DcMsg Attribute Parsers

  private void setBubbleState(DcMsg messageRecord) {
    if (messageRecord.isOutgoing()) {
      bodyBubble.getBackground().setColorFilter(outgoingBubbleColor, PorterDuff.Mode.MULTIPLY);
    } else {
      bodyBubble.getBackground().setColorFilter(incomingBubbleColor, PorterDuff.Mode.MULTIPLY);
    }
  }

  private void setInteractionState(DcMsg messageRecord, boolean pulseHighlight) {
    if (batchSelected.contains(messageRecord)) {
      setBackgroundResource(R.drawable.conversation_item_background);
      setSelected(true);
    } else if (pulseHighlight) {
      setBackgroundResource(R.drawable.conversation_item_background_animated);
      setSelected(true);
      postDelayed(() -> setSelected(false), PULSE_HIGHLIGHT_MILLIS);
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

  private boolean hasQuote(DcMsg messageRecord) {
    return !"".equals(messageRecord.getQuotedText());
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

  private void setBodyText(DcMsg messageRecord) {
    bodyText.setClickable(false);
    bodyText.setFocusable(false);
    bodyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, Prefs.getMessageBodyTextSize(context));

    String text = messageRecord.getText();

    if (messageRecord.isSetupMessage()) {
      bodyText.setText(context.getString(R.string.autocrypt_asm_click_body));
      bodyText.setVisibility(View.VISIBLE);
    }
    else if (text.isEmpty()) {
      bodyText.setVisibility(View.GONE);
    }
    else {
      bodyText.setText(linkifyMessageBody(new SpannableString(text), batchSelected.isEmpty()));
      bodyText.setVisibility(View.VISIBLE);
    }

    if (messageRecord.isMimeModified()) {
      showFullMessage.setVisibility(View.VISIBLE);
      showFullMessage.setOnClickListener(view -> {
        if (eventListener != null && batchSelected.isEmpty()) {
          eventListener.onShowFullClicked(messageRecord);
        } else {
          passthroughClickListener.onClick(view);
        }
      });
    } else {
      showFullMessage.setVisibility(View.GONE);
    }
  }

  private void setMediaAttributes(@NonNull DcMsg           messageRecord,
                                           boolean         isGroupThread)
  {
    class SetDurationListener implements AudioSlidePlayer.Listener {
      @Override
      public void onStart() {}

      @Override
      public void onStop() {}

      @Override
      public void onProgress(double progress, long millis) {}

      @Override
      public void onReceivedDuration(int millis) {
        messageRecord.lateFilingMediaSize(0,0, millis);
        audioViewStub.get().setDuration(millis);
      }
    }
    if (hasAudio(messageRecord)) {
      audioViewStub.get().setVisibility(View.VISIBLE);
      if (mediaThumbnailStub.resolved()) mediaThumbnailStub.get().setVisibility(View.GONE);
      if (documentViewStub.resolved())   documentViewStub.get().setVisibility(View.GONE);

      //noinspection ConstantConditions
      if(dcChat.getId() == DcChat.DC_CHAT_ID_DEADDROP) {  // no audio on dead drops
        // TODO: replace the currently defunct display of a play button with some notification text
        // to inform the user that here would be audio, if this were a proper chat, then ask the user
        // if he wants to start the chat on click.
        audioViewStub.get().setEnabled(false);
        audioViewStub.get().setOnClickListener(passthroughClickListener);
      } else {
        int duration = messageRecord.getDuration();
        if (duration == 0) {
          AudioSlide audio = new AudioSlide(context, messageRecord);
          AudioSlidePlayer audioSlidePlayer = AudioSlidePlayer.createFor(getContext(), audio, new SetDurationListener());
          audioSlidePlayer.requestDuration();
        }

        audioViewStub.get().setAudio(new AudioSlide(context, messageRecord), duration);
      }
      audioViewStub.get().setOnLongClickListener(passthroughClickListener);

      ViewUtil.updateLayoutParams(bodyText, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      ViewUtil.updateLayoutParams(groupSenderHolder, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      footer.setVisibility(VISIBLE);
    }
    else if (hasDocument(messageRecord)) {
      documentViewStub.get().setVisibility(View.VISIBLE);
      if (mediaThumbnailStub.resolved()) mediaThumbnailStub.get().setVisibility(View.GONE);
      if (audioViewStub.resolved())      audioViewStub.get().setVisibility(View.GONE);

      //noinspection ConstantConditions
      documentViewStub.get().setDocument(new DocumentSlide(context, messageRecord));
      documentViewStub.get().setDocumentClickListener(new ThumbnailClickListener());
      documentViewStub.get().setOnLongClickListener(passthroughClickListener);

      ViewUtil.updateLayoutParams(bodyText, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      ViewUtil.updateLayoutParams(groupSenderHolder, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      footer.setVisibility(VISIBLE);
    }
    else if (hasThumbnail(messageRecord)) {
      mediaThumbnailStub.get().setVisibility(View.VISIBLE);
      if (audioViewStub.resolved())    audioViewStub.get().setVisibility(View.GONE);
      if (documentViewStub.resolved()) documentViewStub.get().setVisibility(View.GONE);

      Slide slide;
      if (messageRecord.getType()==DcMsg.DC_MSG_VIDEO) {
        slide = new VideoSlide(context, messageRecord);
      }
      else {
        slide = new DocumentSlide(context, messageRecord);
      }

      MediaUtil.ThumbnailSize thumbnailSize = new MediaUtil.ThumbnailSize(messageRecord.getWidth(0), messageRecord.getHeight(0));
      if ((thumbnailSize.width<=0||thumbnailSize.height<=0)) {
        if(messageRecord.getType()==DcMsg.DC_MSG_VIDEO) {
          MediaUtil.createVideoThumbnailIfNeeded(context, slide.getUri(), slide.getThumbnailUri(), thumbnailSize);
        }
        if (thumbnailSize.width<=0||thumbnailSize.height<=0) {
          thumbnailSize.width = 180;
          thumbnailSize.height = 180;
        }
        messageRecord.lateFilingMediaSize(thumbnailSize.width, thumbnailSize.height, 0);
      }

      mediaThumbnailStub.get().setImageResource(glideRequests,
                                                slide,
                                                thumbnailSize.width,
                                                thumbnailSize.height);
      mediaThumbnailStub.get().setThumbnailClickListener(new ThumbnailClickListener());
      mediaThumbnailStub.get().setOnLongClickListener(passthroughClickListener);
      mediaThumbnailStub.get().setOnClickListener(passthroughClickListener);
      mediaThumbnailStub.get().showShade(TextUtils.isEmpty(messageRecord.getText()));

      setThumbnailOutlineCorners(messageRecord, isGroupThread);

      bodyBubble.getLayoutParams().width = ViewUtil.dpToPx(readDimen(R.dimen.media_bubble_max_width));
      ViewUtil.updateLayoutParams(bodyText, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      ViewUtil.updateLayoutParams(groupSenderHolder, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      footer.setVisibility(VISIBLE);
    }
    else {
      if (mediaThumbnailStub.resolved()) mediaThumbnailStub.get().setVisibility(View.GONE);
      if (audioViewStub.resolved())      audioViewStub.get().setVisibility(View.GONE);
      if (documentViewStub.resolved())   documentViewStub.get().setVisibility(View.GONE);

      ViewUtil.updateLayoutParams(bodyText, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      ViewUtil.updateLayoutParams(groupSenderHolder, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      footer.setVisibility(VISIBLE);
    }
  }

  private void setThumbnailOutlineCorners(@NonNull DcMsg           current,
                                          boolean                  isGroupThread)
  {
    int defaultRadius  = readDimen(R.dimen.message_corner_radius);

    int topLeft     = defaultRadius;
    int topRight    = defaultRadius;
    int bottomLeft  = defaultRadius;
    int bottomRight = defaultRadius;

    if (!TextUtils.isEmpty(current.getText())) {
      bottomLeft  = 0;
      bottomRight = 0;
    }

    if ((!current.isOutgoing() && isGroupThread)
     || current.isForwarded()
     || hasQuote(current)) {
      topLeft  = 0;
      topRight = 0;
    }

    if(bottomLeft != 0 && bottomRight !=0) {
      if(current.isOutgoing()) {
        bottomRight = 0;
      }
      else {
        bottomLeft = 0;
      }
    }

    mediaThumbnailStub.get().setOutlineCorners(topLeft, topRight, bottomRight, bottomLeft);
  }

  private void setContactPhoto() {
    if (contactPhoto == null) return;

    if (messageRecord.isOutgoing() || !groupThread || dcContact ==null) {
      contactPhoto.setVisibility(View.GONE);
    } else {
      contactPhoto.setAvatar(glideRequests, dcContext.getRecipient(dcContact), true);
      contactPhoto.setVisibility(View.VISIBLE);
    }
  }

  private void replaceURLSpan(SpannableString messageBody) {
    URLSpan[] urlSpans = messageBody.getSpans(0, messageBody.length(), URLSpan.class);
    for (URLSpan urlSpan : urlSpans) {
      int start = messageBody.getSpanStart(urlSpan);
      int end = messageBody.getSpanEnd(urlSpan);
      // LongClickCopySpan must not be derived from URLSpan, otherwise links will be removed on the next addLinks() call
      messageBody.setSpan(new LongClickCopySpan(urlSpan.getURL(), this.dcChat.getId()), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
  }

  private SpannableString linkifyMessageBody(SpannableString messageBody, boolean shouldLinkifyAllLinks) {
    if (!shouldLinkifyAllLinks) {
      return messageBody;
    }

    // linkify commands such as `/echo` -
    // do this first to avoid `/xkcd_123456` to be treated partly as a phone number
    if (Linkify.addLinks(messageBody, CMD_PATTERN, "cmd:", null, null)) {
      replaceURLSpan(messageBody); // replace URLSpan so that it is not removed on the next addLinks() call
    }

    // linkyfiy urls etc., this removes all existing URLSpan
    if (Linkify.addLinks(messageBody, Linkify.EMAIL_ADDRESSES|Linkify.WEB_URLS|Linkify.PHONE_NUMBERS)) {
      replaceURLSpan(messageBody);
    }

    return messageBody;
  }

  private void setQuote(@NonNull DcMsg current) {
    if (quoteView == null) {
      throw new AssertionError();
    }
    String quoteTxt = current.getQuotedText();
    if (quoteTxt == null || quoteTxt.isEmpty()) {
      quoteView.dismiss();
      if (mediaThumbnailStub.resolved()) {
        ViewUtil.setTopMargin(mediaThumbnailStub.get(), 0);
      }
      return;
    }
    DcMsg msg = current.getQuotedMsg();

    // If you modify these lines you may also want to modify ConversationActivity.handleReplyMessage():
    Recipient author = null;
    SlideDeck slideDeck = new SlideDeck();
    if (msg != null) {
      author = dcContext.getRecipient(dcContext.getContact(msg.getFromId()));
      if (msg.getType() != DcMsg.DC_MSG_TEXT) {
        Slide slide = MediaUtil.getSlideForMsg(context, msg);
        if (slide != null) {
          slideDeck.addSlide(slide);
        }
      }
    }

    quoteView.setQuote(GlideApp.with(this),
            msg,
            author,
            quoteTxt,
            slideDeck);

    quoteView.setVisibility(View.VISIBLE);
    quoteView.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;

    quoteView.setOnClickListener(view -> {
      if (eventListener != null && batchSelected.isEmpty()) {
        eventListener.onQuoteClicked(current);
      } else {
        passthroughClickListener.onClick(view);
      }
    });

    quoteView.setOnLongClickListener(passthroughClickListener);

    if (mediaThumbnailStub.resolved()) {
      ViewUtil.setTopMargin(mediaThumbnailStub.get(), readDimen(R.dimen.message_bubble_top_padding));
    }
  }

  private void setGutterSizes(@NonNull DcMsg current, boolean isGroupThread) {
    if (isGroupThread && current.isOutgoing()) {
      ViewUtil.setLeftMargin(container, readDimen(R.dimen.conversation_group_left_gutter));
    } else if (current.isOutgoing()) {
      ViewUtil.setLeftMargin(container, readDimen(R.dimen.conversation_individual_left_gutter));
    }
  }

  private void setFooter(@NonNull DcMsg current, @NonNull Locale locale) {
    ViewUtil.updateLayoutParams(footer, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

    footer.setVisibility(GONE);
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

  private void setGroupMessageStatus() {
    if (messageRecord.isForwarded()) {
      this.groupSender.setText(context.getString(R.string.forwarded_message));
      this.groupSender.setTextColor(context.getResources().getColor(R.color.unknown_sender));
    }
    else if (groupThread && !messageRecord.isOutgoing() && dcContact !=null) {
      this.groupSender.setText(dcContact.getDisplayName());

      this.groupSender.setTextColor(dcContact.getArgbColor());
    }
  }

  private void setAuthor(@NonNull DcMsg current, boolean isGroupThread) {
    int groupSenderHolderVisibility = GONE;
    if (isGroupThread && !current.isOutgoing()) {
      if (contactPhotoHolder != null) {
        contactPhotoHolder.setVisibility(VISIBLE);
      }
      groupSenderHolderVisibility = VISIBLE;
      contactPhoto.setVisibility(VISIBLE);
    } else {
      if (contactPhotoHolder != null) {
        contactPhotoHolder.setVisibility(GONE);
      }
    }

    if(current.isForwarded()) {
      groupSenderHolderVisibility = VISIBLE;
    }

    groupSenderHolder.setVisibility(groupSenderHolderVisibility);

    boolean collapse = false;
    if(groupSenderHolderVisibility==VISIBLE && current.getType()==DcMsg.DC_MSG_TEXT) {
      collapse = true;
    }

    int spacingTop = collapse? 0 /*2dp border come from the senderHolder*/ : readDimen(context, R.dimen.message_bubble_top_padding);
    ViewUtil.setPaddingTop(bodyText, spacingTop);
  }

  private void setMessageShape(@NonNull DcMsg current) {
    int background;
    background = current.isOutgoing() ? R.drawable.message_bubble_background_sent_alone
                                      : R.drawable.message_bubble_background_received_alone;
    bodyBubble.setBackgroundResource(background);
  }

  private void setMessageSpacing(@NonNull Context context) {
    int spacingTop = readDimen(context, R.dimen.conversation_vertical_message_spacing_collapse);
    int spacingBottom = spacingTop;

    ViewUtil.setPaddingTop(this, spacingTop);
    ViewUtil.setPaddingBottom(this, spacingBottom);
  }

  private int readDimen(@NonNull Context context, @DimenRes int dimenId) {
    return context.getResources().getDimensionPixelOffset(dimenId);
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

  /// Event handlers

  private void handleDeadDropClick() {
    new AlertDialog.Builder(context)
      .setPositiveButton(android.R.string.ok, (dialog, which) -> {
        int chatId = dcContext.createChatByMsgId(messageRecord.getId());
        if( chatId != 0 ) {
          Intent intent = new Intent(context, ConversationActivity.class);
          intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, chatId);
          context.startActivity(intent);
        }
      })
      .setNegativeButton(android.R.string.cancel, null)
      .setNeutralButton(R.string.menu_block_contact, (dialog, which) -> {
        dcContext.blockContact(messageRecord.getFromId(), 1);
      })
      .setMessage(context.getString(R.string.ask_start_chat_with, dcContext.getContact(messageRecord.getFromId()).getDisplayName()))
      .show();
  }

  private class ThumbnailClickListener implements SlideClickListener {
    public void onClick(final View v, final Slide slide) {
      if (dcChat.getId() == DcChat.DC_CHAT_ID_DEADDROP && batchSelected.isEmpty()) {
        handleDeadDropClick();
      } else if (shouldInterceptClicks(messageRecord) || !batchSelected.isEmpty()) {
        performClick();
      } else if (MediaPreviewActivity.isContentTypeSupported(slide.getContentType()) && slide.getUri() != null) {
        Intent intent = new Intent(context, MediaPreviewActivity.class);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(MediaPreviewActivity.DC_MSG_ID, slide.getDcMsgId());
        intent.putExtra(MediaPreviewActivity.ADDRESS_EXTRA, conversationRecipient.getAddress());
        intent.putExtra(MediaPreviewActivity.OUTGOING_EXTRA, messageRecord.isOutgoing());
        intent.putExtra(MediaPreviewActivity.LEFT_IS_RECENT_EXTRA, false);

        context.startActivity(intent);
      } else if (slide.getUri() != null) {
        dcContext.openForViewOrShare(context, slide.getDcMsgId(), Intent.ACTION_VIEW);
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
      if (dcChat.getId() == DcChat.DC_CHAT_ID_DEADDROP && batchSelected.isEmpty()) {
        handleDeadDropClick();
      } else if (!shouldInterceptClicks(messageRecord) && parent != null) {
        parent.onClick(v);
      } else if (messageRecord.isFailed()) {
        AlertDialog d = new AlertDialog.Builder(context)
                .setMessage(messageRecord.getError())
                .setTitle(R.string.error)
                .setPositiveButton(R.string.ok, null)
                .create();
        d.show();
        try {
          //noinspection ConstantConditions
          Linkify.addLinks((TextView) d.findViewById(android.R.id.message), Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
        } catch(NullPointerException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
