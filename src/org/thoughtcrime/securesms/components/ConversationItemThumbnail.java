package org.thoughtcrime.securesms.components;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.net.Uri;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.database.AttachmentDatabase;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.mms.Slide;
import org.thoughtcrime.securesms.mms.SlideClickListener;
import org.thoughtcrime.securesms.util.ThemeUtil;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class ConversationItemThumbnail extends FrameLayout {

  private static final String TAG = ConversationItemThumbnail.class.getSimpleName();

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
  private int charCount;

  public ConversationItemThumbnail(Context context) {
    super(context);
    init(null);
  }

  public ConversationItemThumbnail(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(attrs);
  }

  public ConversationItemThumbnail(final Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(attrs);
  }

  private void init(@Nullable AttributeSet attrs) {
    inflate(getContext(), R.layout.conversation_item_thumbnail, this);

    this.thumbnail    = findViewById(R.id.conversation_thumbnail_image);
    this.shade        = findViewById(R.id.conversation_thumbnail_shade);
    this.footer       = findViewById(R.id.conversation_thumbnail_footer);
    this.outlinePaint = ThemeUtil.isDarkTheme(getContext()) ? DARK_THEME_OUTLINE_PAINT : LIGHT_THEME_OUTLINE_PAINT;
    this.cornerMask   = new CornerMask(this);

    setTouchDelegate(thumbnail.getTouchDelegate());
  }

  @Override
  protected void onMeasure(int originalWidthMeasureSpec, int originalHeightMeasureSpec) {
    int originalWidth = MeasureSpec.getSize(originalWidthMeasureSpec);
    int minHeight = readDimen(R.dimen.media_bubble_min_height);
    int availableHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.9);

    if (naturalWidth == 0 || naturalHeight == 0) {
      super.onMeasure(originalWidthMeasureSpec, originalHeightMeasureSpec);
      return;
    }

    // Compute height:
    int bestHeight = originalWidth * naturalHeight / naturalWidth;
    int maxHeight = (int) (originalWidth * IMAGE_ASPECT_RATIO);
    int height = Util.clamp(bestHeight, 0, maxHeight);

    height = Util.clamp(height, minHeight, availableHeight);
    int heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);

    int widthMeasureSpec = originalWidthMeasureSpec;
    if (this.charCount < 200) {
      // For short messages, if the height has been cropped, restore the image ratio by limiting the width.
      // We don't do this for longer messages not to create very thin and difficult-to-read messages.
      int bestWidth = height * naturalWidth / naturalHeight;
      int minWidth = (int) (height / IMAGE_ASPECT_RATIO);
      int width = Util.clamp(bestWidth, minWidth, originalWidth);
      widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.getMode(originalWidthMeasureSpec));
    }

    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }

  @SuppressWarnings("SuspiciousNameCombination")
  @Override
  protected void dispatchDraw(Canvas canvas) {
    if (cornerMask.isLegacy()) {
      cornerMask.mask(canvas);
    }

    super.dispatchDraw(canvas);

    if (!cornerMask.isLegacy()) {
      cornerMask.mask(canvas);
    }

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

  @UiThread
  public void setImageResource(@NonNull GlideRequests glideRequests, @NonNull Slide slide,
                               int naturalWidth, int naturalHeight, int charCount)
  {
    this.naturalWidth = naturalWidth;
    this.naturalHeight = naturalHeight;
    this.charCount = charCount;
    refreshSlideAttachmentState(thumbnail.setImageResource(glideRequests, slide), slide);
  }

  public void clear(GlideRequests glideRequests) {
    thumbnail.clear(glideRequests);
  }

  private int readDimen(@DimenRes int dimenId) {
    return getResources().getDimensionPixelOffset(dimenId);
  }
}
