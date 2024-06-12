package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.mms.Slide;
import org.thoughtcrime.securesms.mms.SlideClickListener;

public class BorderlessImageView extends FrameLayout {

  private ThumbnailView image;
  private View          missingShade;
  private ConversationItemFooter footer;

  public BorderlessImageView(@NonNull Context context) {
    super(context);
    init();
  }

  public BorderlessImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private void init() {
    inflate(getContext(), R.layout.sticker_view, this);

    this.image        = findViewById(R.id.sticker_thumbnail);
    this.missingShade = findViewById(R.id.sticker_missing_shade);
    this.footer       = findViewById(R.id.sticker_footer);
  }

  @Override
  public void setFocusable(boolean focusable) {
    image.setFocusable(focusable);
  }

  @Override
  public void setClickable(boolean clickable) {
    image.setClickable(clickable);
  }

  @Override
  public void setOnLongClickListener(@Nullable OnLongClickListener l) {
    image.setOnLongClickListener(l);
  }

  public void setSlide(@NonNull GlideRequests glideRequests, @NonNull Slide slide) {
    boolean showControls = slide.asAttachment().getDataUri() == null;

    if (slide.hasSticker()) {
      image.setScaleType(ImageView.ScaleType.FIT_CENTER);
      image.setImageResource(glideRequests, slide);
    } else {
      image.setScaleType(ImageView.ScaleType.CENTER_CROP);
      image.setImageResource(glideRequests, slide, slide.asAttachment().getWidth(), slide.asAttachment().getHeight());
    }

    missingShade.setVisibility(showControls ? View.VISIBLE : View.GONE);
  }

  public String getDescription() {
    return getContext().getString(R.string.sticker) + "\n" + footer.getDescription();
  }

  public ConversationItemFooter getFooter() {
    return footer;
  }

  public void setThumbnailClickListener(@NonNull SlideClickListener listener) {
    image.setThumbnailClickListener(listener);
  }

}
