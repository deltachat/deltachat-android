package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.database.AttachmentDatabase;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.mms.Slide;
import org.thoughtcrime.securesms.mms.SlideClickListener;
import org.thoughtcrime.securesms.util.ThemeUtil;
import org.thoughtcrime.securesms.util.Util;

import java.util.concurrent.ExecutionException;

import chat.delta.util.ListenableFuture;

public class ConversationItemThumbnail extends FrameLayout {

  private static final Paint LIGHT_THEME_OUTLINE_PAINT = new Paint();
  private static final Paint DARK_THEME_OUTLINE_PAINT = new Paint();
  public static final double IMAGE_ASPECT_RATIO = 1.0;

  static {
    LIGHT_THEME_OUTLINE_PAINT.setColor(Color.argb((int) (255 * 0.2), 0, 0, 0));
    LIGHT_THEME_OUTLINE_PAINT.setStyle(Paint.Style.STROKE);
    LIGHT_THEME_OUTLINE_PAINT.setStrokeWidth(1f);
    LIGHT_THEME_OUTLINE_PAINT.setAntiAlias(true);

    DARK_THEME_OUTLINE_PAINT.setColor(Color.argb((int) (255 * 0.2), 255, 255, 255));
    DARK_THEME_OUTLINE_PAINT.setStyle(Paint.Style.STROKE);
    DARK_THEME_OUTLINE_PAINT.setStrokeWidth(1f);
    DARK_THEME_OUTLINE_PAINT.setAntiAlias(true);
  }

  private final float[] radii   = new float[8];
  private final RectF   bounds  = new RectF();
  private final Path    corners = new Path();

  private ThumbnailView          thumbnail;
  private ImageView              shade;
  private ConversationItemFooter footer;
  private Paint                  outlinePaint;
  private CornerMask             cornerMask;
  private int naturalWidth;
  private int naturalHeight;

  public ConversationItemThumbnail(Context context) {
    super(context);
    init();
  }

  public ConversationItemThumbnail(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public ConversationItemThumbnail(final Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init();
  }

  private void init() {
    inflate(getContext(), R.layout.conversation_item_thumbnail, this);

    this.thumbnail    = findViewById(R.id.conversation_thumbnail_image);
    this.shade        = findViewById(R.id.conversation_thumbnail_shade);
    this.footer       = findViewById(R.id.conversation_thumbnail_footer);
    this.outlinePaint = ThemeUtil.isDarkTheme(getContext()) ? DARK_THEME_OUTLINE_PAINT : LIGHT_THEME_OUTLINE_PAINT;
    this.cornerMask   = new CornerMask(this);

    setTouchDelegate(thumbnail.getTouchDelegate());
  }

  public String getDescription() {
    String desc = thumbnail.getDescription();
    if (footer.getVisibility() == View.VISIBLE) {
      desc += "\n" + footer.getDescription();
    }
    return desc;
  }

  @Override
  protected void onMeasure(int originalWidthMeasureSpec, int originalHeightMeasureSpec) {
    int width = MeasureSpec.getSize(originalWidthMeasureSpec);
    int minHeight = readDimen(R.dimen.media_bubble_min_height);
    int availableHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.75);

    if (naturalWidth == 0 || naturalHeight == 0) {
      super.onMeasure(originalWidthMeasureSpec, originalHeightMeasureSpec);
      return;
    }

    // Compute height:
    int bestHeight = width * naturalHeight / naturalWidth;
    int maxHeight = (int) (width * IMAGE_ASPECT_RATIO);
    int height = Util.clamp(bestHeight, 0, maxHeight);

    height = Util.clamp(height, minHeight, availableHeight);
    int heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);

    super.onMeasure(originalWidthMeasureSpec, heightMeasureSpec);
  }

  @SuppressWarnings("SuspiciousNameCombination")
  @Override
  protected void dispatchDraw(@NonNull Canvas canvas) {

    super.dispatchDraw(canvas);

    cornerMask.mask(canvas);

    final float halfStrokeWidth = outlinePaint.getStrokeWidth() / 2;

    bounds.left   = halfStrokeWidth;
    bounds.top    = halfStrokeWidth;
    bounds.right  = canvas.getWidth() - halfStrokeWidth;
    bounds.bottom = canvas.getHeight() - halfStrokeWidth;

    corners.reset();
    corners.addRoundRect(bounds, radii, Path.Direction.CW);

    canvas.drawPath(corners, outlinePaint);
  }

  @Override
  public void setFocusable(boolean focusable) {
    thumbnail.setFocusable(focusable);
  }

  @Override
  public void setClickable(boolean clickable) {
    thumbnail.setClickable(clickable);
  }

  @Override
  public void setOnLongClickListener(@Nullable OnLongClickListener l) {
    thumbnail.setOnLongClickListener(l);
  }

  public void showShade(boolean show) {
    shade.setVisibility(show ? VISIBLE : GONE);
    forceLayout();
  }

  public void setOutlineCorners(int topLeft, int topRight, int bottomRight, int bottomLeft) {
    radii[0] = radii[1] = topLeft;
    radii[2] = radii[3] = topRight;
    radii[4] = radii[5] = bottomRight;
    radii[6] = radii[7] = bottomLeft;

    cornerMask.setRadii(topLeft, topRight, bottomRight, bottomLeft);
  }

  public ConversationItemFooter getFooter() {
    return footer;
  }

  private void refreshSlideAttachmentState(ListenableFuture<Boolean> signal, Slide slide) {
    signal.addListener(new ListenableFuture.Listener<Boolean>() {
      @Override
      public void onSuccess(Boolean result) {
        slide.asAttachment().setTransferState(AttachmentDatabase.TRANSFER_PROGRESS_DONE);
      }

      @Override
      public void onFailure(ExecutionException e) {
        slide.asAttachment().setTransferState(AttachmentDatabase.TRANSFER_PROGRESS_FAILED);
      }
    });
  }

  public void setThumbnailClickListener(SlideClickListener listener) {
    thumbnail.setThumbnailClickListener(listener);
  }

  @Override
  public boolean performClick() {
    return thumbnail.performClick();
  }

  @UiThread
  public void setImageResource(@NonNull GlideRequests glideRequests, @NonNull Slide slide,
                               int naturalWidth, int naturalHeight)
  {
    this.naturalWidth = naturalWidth;
    this.naturalHeight = naturalHeight;
    refreshSlideAttachmentState(thumbnail.setImageResource(glideRequests, slide), slide);
  }

  public void clear(GlideRequests glideRequests) {
    thumbnail.clear(glideRequests);
  }

  private int readDimen(@DimenRes int dimenId) {
    return getResources().getDimensionPixelOffset(dimenId);
  }
}
