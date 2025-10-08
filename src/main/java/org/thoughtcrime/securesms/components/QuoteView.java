package org.thoughtcrime.securesms.components;


import android.content.Context;
import android.content.res.TypedArray;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcMsg;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.json.JSONObject;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.attachments.Attachment;
import org.thoughtcrime.securesms.connect.DcHelper;
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

import chat.delta.rpc.RpcException;
import chat.delta.rpc.types.VcardContact;

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
  private boolean       hasSticker;
  private boolean       isEdit;

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

  public void setQuote(GlideRequests glideRequests,
                       DcMsg msg,
                       @Nullable Recipient author,
                       @Nullable CharSequence body,
                       @NonNull SlideDeck attachments,
                       boolean hasSticker,
                       boolean isEdit)
  {
    quotedMsg        = msg;
    this.author      = author != null ? author.getDcContact() : null;
    this.body        = body;
    this.attachments = attachments;
    this.hasSticker  = hasSticker;
    this.isEdit = isEdit;

    if (hasSticker) {
      this.setBackgroundResource(R.drawable.conversation_item_update_background);
      bodyView.setTextColor(getResources().getColor(R.color.core_dark_05));
    }
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
    if (isEdit) {
      authorView.setVisibility(VISIBLE);
      authorView.setTextColor(getEditColor());
      quoteBarView.setBackgroundColor(getEditColor());
      authorView.setText(getContext().getString(R.string.edit_message));
    } else if (author == null) {
      authorView.setVisibility(GONE);
      quoteBarView.setBackgroundColor(getForwardedColor());
    } else if (quotedMsg.isForwarded()) {
      DcContact contact = author.getDcContact();
      authorView.setVisibility(VISIBLE);
      if (contact == null) {
        authorView.setText(getContext().getString(R.string.forwarded_message));
      } else {
        authorView.setText(getContext().getString(R.string.forwarded_by, quotedMsg.getSenderName(contact)));
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
        authorView.setText(quotedMsg.getSenderName(contact));
        if (hasSticker) {
          authorView.setTextColor(getResources().getColor(R.color.core_dark_05));
          quoteBarView.setBackgroundColor(getResources().getColor(R.color.core_dark_05));
        } else {
          authorView.setTextColor(Util.rgbToArgbColor(contact.getColor()));
          quoteBarView.setBackgroundColor(Util.rgbToArgbColor(contact.getColor()));
        }
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
    List<Slide> slides = slideDeck.getSlides();
    Slide slide = slides.isEmpty()? null : slides.get(0);

    attachmentVideoOverlayView.setVisibility(GONE);

    if (slide != null && slide.hasQuoteThumbnail()) {
      thumbnailView.setVisibility(VISIBLE);
      attachmentContainerView.setVisibility(GONE);
      dismissView.setBackgroundResource(R.drawable.dismiss_background);

      if (slide.isWebxdcDocument()) {
        try {
          JSONObject info = quotedMsg.getWebxdcInfo();
          byte[] blob = quotedMsg.getWebxdcBlob(info.getString("icon"));
          glideRequests.load(blob)
                  .centerCrop()
                  .override(getContext().getResources().getDimensionPixelSize(R.dimen.quote_thumb_size))
                  .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                  .into(thumbnailView);
        } catch (Exception e) {
          Log.e(TAG, "failed to get webxdc icon", e);
          thumbnailView.setVisibility(GONE);
        }
      } else if (slide.isVcard()) {
        try {
          VcardContact vcardContact = DcHelper.getRpc(getContext()).parseVcard(quotedMsg.getFile()).get(0);
          Recipient recipient = new Recipient(getContext(), vcardContact);
          glideRequests.load(recipient.getContactPhoto(getContext()))
            .error(recipient.getFallbackAvatarDrawable(getContext(), false))
            .centerCrop()
            .override(getContext().getResources().getDimensionPixelSize(R.dimen.quote_thumb_size))
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(thumbnailView);
        } catch (RpcException e) {
          Log.e(TAG, "failed to parse vCard", e);
          thumbnailView.setVisibility(GONE);
        }
      } else {
        Uri thumbnailUri = slide.getUri();
        if (slide.hasVideo()) {
          attachmentVideoOverlayView.setVisibility(VISIBLE);
          MediaUtil.createVideoThumbnailIfNeeded(getContext(), slide.getUri(), slide.getThumbnailUri(), null);
          thumbnailUri = slide.getThumbnailUri();
        }
        if (thumbnailUri != null) {
          glideRequests.load(new DecryptableUri(thumbnailUri))
                  .centerCrop()
                  .override(getContext().getResources().getDimensionPixelSize(R.dimen.quote_thumb_size))
                  .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                  .into(thumbnailView);
        }
      }
    } else if(slide != null && slide.hasAudio()) {
      thumbnailView.setVisibility(GONE);
      attachmentContainerView.setVisibility(GONE);
    } else if (slide != null && slide.hasDocument()) {
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
    return getResources().getColor(hasSticker? R.color.core_dark_05 : R.color.unknown_sender);
  }

  private int getEditColor() {
    return getResources().getColor(R.color.delta_accent);
  }
}
