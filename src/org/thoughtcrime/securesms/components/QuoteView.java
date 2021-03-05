package org.thoughtcrime.securesms.components;


import android.content.Context;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.annimon.stream.Stream;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcMsg;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.attachments.Attachment;
import org.thoughtcrime.securesms.mms.DecryptableStreamUriLoader.DecryptableUri;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.mms.Slide;
import org.thoughtcrime.securesms.mms.SlideDeck;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientForeverObserver;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.ThemeUtil;
import org.thoughtcrime.securesms.util.Util;

import java.util.List;

public class QuoteView extends FrameLayout implements RecipientForeverObserver {

  private static final String TAG = QuoteView.class.getSimpleName();

  private static final int MESSAGE_TYPE_PREVIEW  = 0;

  private ViewGroup mainView;
  private TextView  authorView;
  private TextView  bodyView;
  private ImageView quoteBarView;
  private ImageView thumbnailView;
  private View      attachmentVideoOverlayView;
  private ViewGroup attachmentContainerView;
  private ImageView dismissView;

  private DcMsg quotedMsg;
  private DcContact     author;
  private CharSequence  body;
  private SlideDeck     attachments;
  private int           messageType;

  public QuoteView(Context context) {
    super(context);
    initialize(null);
  }

  public QuoteView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize(attrs);
  }

  public QuoteView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize(attrs);
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public QuoteView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    initialize(attrs);
  }

  private void initialize(@Nullable AttributeSet attrs) {
    inflate(getContext(), R.layout.quote_view, this);

    this.mainView                     = findViewById(R.id.quote_main);
    this.authorView                   = findViewById(R.id.quote_author);
    this.bodyView                     = findViewById(R.id.quote_text);
    this.quoteBarView                 = findViewById(R.id.quote_bar);
    this.thumbnailView                = findViewById(R.id.quote_thumbnail);
    this.attachmentVideoOverlayView   = findViewById(R.id.quote_video_overlay);
    this.attachmentContainerView      = findViewById(R.id.quote_attachment_container);
    this.dismissView                  = findViewById(R.id.quote_dismiss);

    if (attrs != null) {
      TypedArray typedArray     = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.QuoteView, 0, 0);
      messageType = typedArray.getInt(R.styleable.QuoteView_message_type, 0);
      typedArray.recycle();

      dismissView.setVisibility(messageType == MESSAGE_TYPE_PREVIEW ? VISIBLE : GONE);
      if (messageType == MESSAGE_TYPE_PREVIEW) {
        bodyView.setSingleLine();
      } else {
        bodyView.setMaxLines(3);
      }
    }

    dismissView.setOnClickListener(view -> setVisibility(GONE));
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
  }

  public void setQuote(GlideRequests glideRequests,
                       DcMsg msg,
                       @Nullable Recipient author,
                       @Nullable CharSequence body,
                       @NonNull SlideDeck attachments)
  {
    quotedMsg        = msg;
    this.author      = author != null ? author.getDcContact() : null;
    this.body        = body;
    this.attachments = attachments;

    setQuoteAuthor(author);
    setQuoteText(body, attachments);
    setQuoteAttachment(glideRequests, attachments);
  }

  public void dismiss() {
    this.author = null;
    this.body   = null;

    setVisibility(GONE);
  }

  @Override
  public void onRecipientChanged(@NonNull Recipient recipient) {
    setQuoteAuthor(recipient);
  }

  private void setQuoteAuthor(@Nullable Recipient author) {
    if (author == null) {
      authorView.setVisibility(GONE);
      quoteBarView.setBackgroundColor(getForwardedColor());
    } else if (quotedMsg.isForwarded()) {
      DcContact contact = author.getDcContact();
      authorView.setVisibility(VISIBLE);
      if (contact == null) {
        authorView.setText(getContext().getString(R.string.forwarded_message));
      } else {
        authorView.setText(getContext().getString(R.string.forwarded_by, quotedMsg.getSenderName(contact, false)));
      }
      authorView.setTextColor(getForwardedColor());
      quoteBarView.setBackgroundColor(getForwardedColor());
    } else {
      DcContact contact = author.getDcContact();
      if (contact == null) {
        authorView.setVisibility(GONE);
        quoteBarView.setBackgroundColor(getForwardedColor());
      } else {
        authorView.setVisibility(VISIBLE);
        authorView.setText(quotedMsg.getSenderName(contact, true));
        authorView.setTextColor(Util.rgbToArgbColor(contact.getColor()));
        quoteBarView.setBackgroundColor(Util.rgbToArgbColor(contact.getColor()));
      }
    }
  }

  private void setQuoteText(@Nullable CharSequence body, @NonNull SlideDeck attachments) {
    if (!TextUtils.isEmpty(body) || !attachments.containsMediaSlide()) {
      bodyView.setVisibility(VISIBLE);
      bodyView.setText(body == null ? "" : body);
    } else {
      bodyView.setVisibility(GONE);
    }
  }

  private void setQuoteAttachment(@NonNull GlideRequests glideRequests, @NonNull SlideDeck slideDeck) {
    List<Slide> imageVideoSlides = Stream.of(slideDeck.getSlides()).filter(s -> s.hasImage() || s.hasVideo()).limit(1).toList();
    List<Slide> audioSlides = Stream.of(slideDeck.getSlides()).filter(s -> s.hasAudio()).limit(1).toList();
    List<Slide> documentSlides = Stream.of(attachments.getSlides()).filter(Slide::hasDocument).limit(1).toList();

    attachmentVideoOverlayView.setVisibility(GONE);

    if (!imageVideoSlides.isEmpty() && imageVideoSlides.get(0).getUri() != null) {
      thumbnailView.setVisibility(VISIBLE);
      attachmentContainerView.setVisibility(GONE);
      dismissView.setBackgroundResource(R.drawable.dismiss_background);

      Uri thumbnailUri = imageVideoSlides.get(0).getUri();
      if (imageVideoSlides.get(0).hasVideo()) {
        attachmentVideoOverlayView.setVisibility(VISIBLE);
        MediaUtil.createVideoThumbnailIfNeeded(getContext(), imageVideoSlides.get(0).getUri(), imageVideoSlides.get(0).getThumbnailUri(), null);
        thumbnailUri = imageVideoSlides.get(0).getThumbnailUri();
      }
      glideRequests.load(new DecryptableUri(thumbnailUri))
              .centerCrop()
              .override(getContext().getResources().getDimensionPixelSize(R.dimen.quote_thumb_size))
              .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
              .into(thumbnailView);

    } else if(!audioSlides.isEmpty()) {
      thumbnailView.setVisibility(GONE);
      attachmentContainerView.setVisibility(GONE);
    } else if (!documentSlides.isEmpty()) {
      thumbnailView.setVisibility(GONE);
      attachmentContainerView.setVisibility(VISIBLE);
    } else {
      thumbnailView.setVisibility(GONE);
      attachmentContainerView.setVisibility(GONE);
    }

    if (ThemeUtil.isDarkTheme(getContext())) {
      dismissView.setBackgroundResource(R.drawable.circle_alpha);
    }
  }

  public CharSequence getBody() {
    return body;
  }

  public List<Attachment> getAttachments() {
    return attachments.asAttachments();
  }

  public DcContact getDcContact() {
    return author;
  }

  public DcMsg getOriginalMsg() {
    return quotedMsg;
  }

  private int getForwardedColor() {
    return getResources().getColor(R.color.unknown_sender);
  }
}
