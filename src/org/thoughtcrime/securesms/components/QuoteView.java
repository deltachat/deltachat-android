package org.thoughtcrime.securesms.components;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
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
import com.b44t.messenger.DcChat;
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
import org.thoughtcrime.securesms.util.ThemeUtil;

import java.util.List;

public class QuoteView extends FrameLayout implements RecipientForeverObserver {

  private static final String TAG = QuoteView.class.getSimpleName();

  private static final int MESSAGE_TYPE_PREVIEW  = 0;
  private static final int MESSAGE_TYPE_OUTGOING = 1;
  private static final int MESSAGE_TYPE_INCOMING = 2;

  private ViewGroup mainView;
  //private ViewGroup footerView;
  private TextView  authorView;
  private TextView  bodyView;
  private ImageView      quoteBarView;
  private ImageView thumbnailView;
  private View      attachmentVideoOverlayView;
  //private ViewGroup attachmentContainerView;
  private DocumentView attachmentView;
  private ImageView dismissView;

  private DcMsg quotedMsg;
  private long          id;
  private DcContact     author;
  private CharSequence  body;
  //private TextView      missingLinkText;
  private SlideDeck     attachments;
  private int           messageType;
  private int           largeCornerRadius;
  private int           smallCornerRadius;
  private CornerMask    cornerMask;


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

    //this.footerView                   = findViewById(R.id.quote_missing_footer);
    this.authorView                   = findViewById(R.id.quote_author);
    this.bodyView                     = findViewById(R.id.quote_text);
    this.quoteBarView                 = findViewById(R.id.quote_bar);
    this.thumbnailView                = findViewById(R.id.quote_thumbnail);
    this.attachmentVideoOverlayView   = findViewById(R.id.quote_video_overlay);
    this.attachmentView               = findViewById(R.id.attachment_view);
    this.dismissView                  = findViewById(R.id.quote_dismiss);
    this.largeCornerRadius            = getResources().getDimensionPixelSize(R.dimen.quote_corner_radius_large);
    this.smallCornerRadius            = getResources().getDimensionPixelSize(R.dimen.quote_corner_radius_bottom);

    cornerMask = new CornerMask(this);
    cornerMask.setRadii(largeCornerRadius, largeCornerRadius, smallCornerRadius, smallCornerRadius);

    if (attrs != null) {
      TypedArray typedArray     = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.QuoteView, 0, 0);
      int        secondaryColor = typedArray.getColor(R.styleable.QuoteView_quote_colorSecondary, Color.BLACK);
      messageType = typedArray.getInt(R.styleable.QuoteView_message_type, 0);
      typedArray.recycle();

      dismissView.setVisibility(messageType == MESSAGE_TYPE_PREVIEW ? VISIBLE : GONE);

      //missingLinkText.setTextColor(primaryColor);

      if (messageType == MESSAGE_TYPE_PREVIEW) {
        int radius = getResources().getDimensionPixelOffset(R.dimen.quote_corner_radius_preview);
        cornerMask.setTopLeftRadius(radius);
        cornerMask.setTopRightRadius(radius);
      }
    }

    dismissView.setOnClickListener(view -> setVisibility(GONE));
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    super.dispatchDraw(canvas);
    cornerMask.mask(canvas);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    //if (author != null) author.removeForeverObserver(this);
  }

  public void setQuote(GlideRequests glideRequests,
                       DcMsg msg,
                       long id,
                       @NonNull Recipient author,
                       @Nullable CharSequence body,
                       boolean originalMissing,
                       @NonNull SlideDeck attachments)
  {
//    if (this.author != null) this.author.removeForeverObserver(this);

    this.id          = id;
    quotedMsg        = msg;
    this.author      = author.getDcContact();
    this.body        = body;
    this.attachments = attachments;

    //this.author.observeForever(this);
    setQuoteAuthor(author);
    setQuoteText(body, attachments);
    setQuoteAttachment(glideRequests, attachments);
    //setQuoteMissingFooter(originalMissing);
  }

  public void setTopCornerSizes(boolean topLeftLarge, boolean topRightLarge) {
    cornerMask.setTopLeftRadius(topLeftLarge ? largeCornerRadius : smallCornerRadius);
    cornerMask.setTopRightRadius(topRightLarge ? largeCornerRadius : smallCornerRadius);
  }

  public void dismiss() {
    //if (this.author != null) this.author.removeForeverObserver(this);

    this.id     = 0;
    this.author = null;
    this.body   = null;

    setVisibility(GONE);
  }

  @Override
  public void onRecipientChanged(@NonNull Recipient recipient) {
    setQuoteAuthor(recipient);
  }

  private void setQuoteAuthor(@NonNull Recipient author) {
    boolean outgoing = messageType != MESSAGE_TYPE_INCOMING;

    DcContact contact = author.getDcContact();
    if (contact != null) {
      authorView.setText(contact.getDisplayName());
      quoteBarView.setBackgroundColor(contact.getArgbColor());
      authorView.setTextColor(contact.getArgbColor());
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
    List<Slide> documentSlides = Stream.of(attachments.getSlides()).filter(Slide::hasDocument).limit(1).toList();

    attachmentVideoOverlayView.setVisibility(GONE);

    if (!imageVideoSlides.isEmpty() && imageVideoSlides.get(0).getUri() != null) {
      thumbnailView.setVisibility(VISIBLE);
      attachmentView.setVisibility(GONE);
      dismissView.setBackgroundResource(R.drawable.dismiss_background);
      if (imageVideoSlides.get(0).hasVideo()) {
        attachmentVideoOverlayView.setVisibility(VISIBLE);
      }
      glideRequests.load(new DecryptableUri(imageVideoSlides.get(0).getUri()))
              .centerCrop()
              .override(getContext().getResources().getDimensionPixelSize(R.dimen.quote_thumb_size))
              .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
              .into(thumbnailView);
    } else if (!documentSlides.isEmpty()) {
      thumbnailView.setVisibility(GONE);
      attachmentView.setVisibility(VISIBLE);
      //attachmentNameView.setText(documentSlides.get(0).getFileName().or(""));
      attachmentView.setDocument(attachments.getDocumentSlide());
    } else {
      thumbnailView.setVisibility(GONE);
      attachmentView.setVisibility(GONE);
      dismissView.setBackground(null);
    }

    if (ThemeUtil.isDarkTheme(getContext())) {
      dismissView.setBackgroundResource(R.drawable.circle_alpha);
    }
  }
//
//  private void setQuoteMissingFooter(boolean missing) {
//    footerView.setVisibility(missing ? VISIBLE : GONE);
//    footerView.setBackgroundColor(author.getColor());
//  }

  public long getQuoteId() {
    return id;
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
}
