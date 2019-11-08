package org.thoughtcrime.securesms.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.database.AttachmentDatabase;
import org.thoughtcrime.securesms.mms.DecryptableStreamUriLoader.DecryptableUri;
import org.thoughtcrime.securesms.mms.GlideRequest;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.mms.Slide;
import org.thoughtcrime.securesms.mms.SlideClickListener;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.concurrent.ListenableFuture;
import org.thoughtcrime.securesms.util.concurrent.SettableFuture;

import java.util.Locale;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

public class ThumbnailView extends FrameLayout {

  private static final String TAG        = ThumbnailView.class.getSimpleName();
  private static final int    WIDTH      = 0;
  private static final int    HEIGHT     = 1;
  private static final int    MIN_WIDTH  = 0;
  private static final int    MAX_WIDTH  = 1;
  private static final int    MIN_HEIGHT = 2;
  private static final int    MAX_HEIGHT = 3;

  private ImageView       image;
  private View            playOverlay;
  private OnClickListener parentClickListener;

  private final int[] dimens        = new int[2];
  private final int[] bounds        = new int[4];
  private final int[] measureDimens = new int[2];

  private SlideClickListener            thumbnailClickListener = null;
  private Slide                         slide                  = null;

  private int radius;

  public ThumbnailView(Context context) {
    this(context, null);
  }

  public ThumbnailView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ThumbnailView(final Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    inflate(context, R.layout.thumbnail_view, this);

    this.image       = findViewById(R.id.thumbnail_image);
    this.playOverlay = findViewById(R.id.play_overlay);
    super.setOnClickListener(new ThumbnailClickDispatcher());

    if (attrs != null) {
      TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ThumbnailView, 0, 0);
      bounds[MIN_WIDTH]  = typedArray.getDimensionPixelSize(R.styleable.ThumbnailView_minWidth, 0);
      bounds[MAX_WIDTH]  = typedArray.getDimensionPixelSize(R.styleable.ThumbnailView_maxWidth, 0);
      bounds[MIN_HEIGHT] = typedArray.getDimensionPixelSize(R.styleable.ThumbnailView_minHeight, 0);
      bounds[MAX_HEIGHT] = typedArray.getDimensionPixelSize(R.styleable.ThumbnailView_maxHeight, 0);
      radius             = typedArray.getDimensionPixelSize(R.styleable.ThumbnailView_thumbnail_radius, getResources().getDimensionPixelSize(R.dimen.gallery_thumbnail_radius));
      typedArray.recycle();
    } else {
      radius = getResources().getDimensionPixelSize(R.dimen.gallery_thumbnail_radius);
    }

  }

  @Override
  protected void onMeasure(int originalWidthMeasureSpec, int originalHeightMeasureSpec) {
    fillTargetDimensions(measureDimens, dimens, bounds);
    if (measureDimens[WIDTH] == 0 && measureDimens[HEIGHT] == 0) {
      super.onMeasure(originalWidthMeasureSpec, originalHeightMeasureSpec);
      return;
    }

    int finalWidth  = measureDimens[WIDTH] + getPaddingLeft() + getPaddingRight();
    int finalHeight = measureDimens[HEIGHT] + getPaddingTop() + getPaddingBottom();

    super.onMeasure(MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY));
  }

  @SuppressWarnings("SuspiciousNameCombination")
  private void fillTargetDimensions(int[] targetDimens, int[] dimens, int[] bounds) {
    int dimensFilledCount = getNonZeroCount(dimens);
    int boundsFilledCount = getNonZeroCount(bounds);

    if (dimensFilledCount == 0 || boundsFilledCount == 0) {
      targetDimens[WIDTH] = 0;
      targetDimens[HEIGHT] = 0;
      return;
    }

    double naturalWidth  = dimens[WIDTH];
    double naturalHeight = dimens[HEIGHT];

    int minWidth  = bounds[MIN_WIDTH];
    int maxWidth  = bounds[MAX_WIDTH];
    int minHeight = bounds[MIN_HEIGHT];
    int maxHeight = bounds[MAX_HEIGHT];

    if (dimensFilledCount > 0 && dimensFilledCount < dimens.length) {
      throw new IllegalStateException(String.format(Locale.ENGLISH, "Width or height has been specified, but not both. Dimens: %f x %f",
                                                    naturalWidth, naturalHeight));
    }
    if (boundsFilledCount > 0 && boundsFilledCount < bounds.length) {
      throw new IllegalStateException(String.format(Locale.ENGLISH, "One or more min/max dimensions have been specified, but not all. Bounds: [%d, %d, %d, %d]",
                                                    minWidth, maxWidth, minHeight, maxHeight));
    }

    double measuredWidth  = naturalWidth;
    double measuredHeight = naturalHeight;

    boolean widthInBounds  = measuredWidth >= minWidth && measuredWidth <= maxWidth;
    boolean heightInBounds = measuredHeight >= minHeight && measuredHeight <= maxHeight;

    if (!widthInBounds || !heightInBounds) {
      double minWidthRatio  = naturalWidth / minWidth;
      double maxWidthRatio  = naturalWidth / maxWidth;
      double minHeightRatio = naturalHeight / minHeight;
      double maxHeightRatio = naturalHeight / maxHeight;

      if (maxWidthRatio > 1 || maxHeightRatio > 1) {
        if (maxWidthRatio >= maxHeightRatio) {
          measuredWidth  /= maxWidthRatio;
          measuredHeight /= maxWidthRatio;
        } else {
          measuredWidth  /= maxHeightRatio;
          measuredHeight /= maxHeightRatio;
        }

        measuredWidth  = Math.max(measuredWidth, minWidth);
        measuredHeight = Math.max(measuredHeight, minHeight);

      } else if (minWidthRatio < 1 || minHeightRatio < 1) {
        if (minWidthRatio <= minHeightRatio) {
          measuredWidth  /= minWidthRatio;
          measuredHeight /= minWidthRatio;
        } else {
          measuredWidth  /= minHeightRatio;
          measuredHeight /= minHeightRatio;
        }

        measuredWidth  = Math.min(measuredWidth, maxWidth);
        measuredHeight = Math.min(measuredHeight, maxHeight);
      }
    }

    targetDimens[WIDTH]  = (int) measuredWidth;
    targetDimens[HEIGHT] = (int) measuredHeight;
  }

  private int getNonZeroCount(int[] vals) {
    int count = 0;
    for (int val : vals) {
      if (val > 0) {
        count++;
      }
    }
    return count;
  }

  @Override
  public void setOnClickListener(OnClickListener l) {
    parentClickListener = l;
  }

  @Override
  public void setFocusable(boolean focusable) {
    super.setFocusable(focusable);
  }

  @Override
  public void setClickable(boolean clickable) {
    super.setClickable(clickable);
  }

  public void setBounds(int minWidth, int maxWidth, int minHeight, int maxHeight) {
    bounds[MIN_WIDTH]  = minWidth;
    bounds[MAX_WIDTH]  = maxWidth;
    bounds[MIN_HEIGHT] = minHeight;
    bounds[MAX_HEIGHT] = maxHeight;

    forceLayout();
  }

  @UiThread
  public ListenableFuture<Boolean> setImageResource(@NonNull GlideRequests glideRequests, @NonNull Slide slide)
  {
    return setImageResource(glideRequests, slide, 0, 0);
  }

  @SuppressLint("StaticFieldLeak")
  @UiThread
  public ListenableFuture<Boolean> setImageResource(@NonNull GlideRequests glideRequests, @NonNull Slide slide,
                                                    int naturalWidth, int naturalHeight)
  {
    if (slide.hasPlayOverlay()) {
      this.playOverlay.setVisibility(View.VISIBLE);
    }
    else {
      this.playOverlay.setVisibility(View.GONE);
    }

    if (Util.equals(slide, this.slide)) {
      Log.w(TAG, "Not re-loading slide " + slide.asAttachment().getDataUri());
      return new SettableFuture<>(false);
    }

    if (this.slide != null && this.slide.getFastPreflightId() != null &&
        this.slide.getFastPreflightId().equals(slide.getFastPreflightId()))
    {
      Log.w(TAG, "Not re-loading slide for fast preflight: " + slide.getFastPreflightId());
      this.slide = slide;
      return new SettableFuture<>(false);
    }

    Log.w(TAG, "loading part with id " + slide.asAttachment().getDataUri()
               + ", progress " + slide.getTransferState() + ", fast preflight id: " +
               slide.asAttachment().getFastPreflightId());

    this.slide = slide;

    dimens[WIDTH]  = naturalWidth;
    dimens[HEIGHT] = naturalHeight;
    invalidate();

    SettableFuture<Boolean> result = new SettableFuture<>();

    if (slide.getThumbnailUri() != null)
    {
      if(slide.hasVideo())
      {
        Uri dataUri = slide.getUri();
        Uri thumbnailUri = slide.getThumbnailUri();
        ImageView img = findViewById(R.id.thumbnail_image);
        Context context = getContext();
        new AsyncTask<Void, Void, Boolean>() {
          @Override
          protected Boolean doInBackground(Void... params) {
            return MediaUtil.createVideoThumbnailIfNeeded(context, dataUri, thumbnailUri, null);
          }
          @Override
          protected void onPostExecute(Boolean success) {
            GlideRequest request = applySizing(glideRequests.load(new DecryptableUri(thumbnailUri))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .transition(withCrossFade()), new CenterCrop());
            request.into(new GlideDrawableListeningTarget(img, result));
          }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      }
      else
      {
        GlideRequest request = applySizing(glideRequests.load(new DecryptableUri(slide.getThumbnailUri()))
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .transition(withCrossFade()), new CenterCrop());
        request.into(new GlideDrawableListeningTarget(image, result));
      }
    }
    else
    {
      glideRequests.clear(image);
      result.set(false);
    }

    return result;
  }

  public ListenableFuture<Boolean> setImageResource(@NonNull GlideRequests glideRequests, @NonNull Uri uri) {
    SettableFuture<Boolean> future = new SettableFuture<>();

    glideRequests.load(new DecryptableUri(uri))
                 .diskCacheStrategy(DiskCacheStrategy.NONE)
                 .transforms(new CenterCrop(), new RoundedCorners(radius))
                 .transition(withCrossFade())
                 .into(new GlideDrawableListeningTarget(image, future));

    return future;
  }

  public void setThumbnailClickListener(SlideClickListener listener) {
    this.thumbnailClickListener = listener;
  }

  public void clear(GlideRequests glideRequests) {
    glideRequests.clear(image);

    slide = null;
  }

  private GlideRequest applySizing(@NonNull GlideRequest request, @NonNull BitmapTransformation fitting) {
    int[] size = new int[2];
    fillTargetDimensions(size, dimens, bounds);
    if (size[WIDTH] == 0 && size[HEIGHT] == 0) {
      size[WIDTH]  = getDefaultWidth();
      size[HEIGHT] = getDefaultHeight();
    }
    return request.override(size[WIDTH], size[HEIGHT])
                  .transforms(fitting, new RoundedCorners(radius));
  }

  private int getDefaultWidth() {
    ViewGroup.LayoutParams params = getLayoutParams();
    if (params != null) {
      return Math.max(params.width, 0);
    }
    return 0;
  }

  private int getDefaultHeight() {
    ViewGroup.LayoutParams params = getLayoutParams();
    if (params != null) {
      return Math.max(params.height, 0);
    }
    return 0;
  }

  private class ThumbnailClickDispatcher implements View.OnClickListener {
    @Override
    public void onClick(View view) {
      if (thumbnailClickListener            != null &&
          slide                             != null &&
          slide.asAttachment().getDataUri() != null &&
          slide.getTransferState()          == AttachmentDatabase.TRANSFER_PROGRESS_DONE)
      {
        thumbnailClickListener.onClick(view, slide);
      } else if (parentClickListener != null) {
        parentClickListener.onClick(view);
      }
    }
  }
}
