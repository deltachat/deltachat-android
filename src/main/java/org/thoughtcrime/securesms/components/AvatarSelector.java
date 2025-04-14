package org.thoughtcrime.securesms.components;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.ViewUtil;

public class AvatarSelector extends PopupWindow {

  public static final int ADD_GALLERY       = 1;
  public static final int TAKE_PHOTO        = 5;
  public static final int REMOVE_PHOTO      = 8;

  private static final int ANIMATION_DURATION = 300;

  private final @NonNull LoaderManager       loaderManager;
  private final @NonNull RecentPhotoViewRail recentRail;

  private @Nullable AttachmentClickedListener listener;

  public AvatarSelector(@NonNull Context context, @NonNull LoaderManager loaderManager, @Nullable AttachmentClickedListener listener, boolean includeClear) {
    super(context);

    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    LinearLayout   layout   = (LinearLayout) inflater.inflate(R.layout.avatar_selector, null, true);

    this.listener       = listener;
    this.loaderManager  = loaderManager;
    this.recentRail     = ViewUtil.findById(layout, R.id.recent_photos);
    ImageView imageButton = ViewUtil.findById(layout, R.id.gallery_button);
    ImageView cameraButton = ViewUtil.findById(layout, R.id.camera_button);
    ImageView closeButton = ViewUtil.findById(layout, R.id.close_button);
    ImageView removeButton = ViewUtil.findById(layout, R.id.remove_button);

    imageButton.setOnClickListener(new PropagatingClickListener(ADD_GALLERY));
    cameraButton.setOnClickListener(new PropagatingClickListener(TAKE_PHOTO));
    closeButton.setOnClickListener(new CloseClickListener());
    removeButton.setOnClickListener(new PropagatingClickListener(REMOVE_PHOTO));
    this.recentRail.setListener(new RecentPhotoSelectedListener());
    if (!includeClear) {
      removeButton.setVisibility(View.GONE);
      ViewUtil.findById(layout, R.id.remove_button_label).setVisibility(View.GONE);
    }

    setContentView(layout);
    setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
    setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
    setBackgroundDrawable(new BitmapDrawable(context.getResources(), (Bitmap) null));
    setAnimationStyle(0);
    setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
    setFocusable(true);
    setTouchable(true);

    loaderManager.initLoader(1, null, recentRail);
  }

  public void show(@NonNull Activity activity, final @NonNull View anchor) {
    if (Permissions.hasAll(activity, Permissions.galleryPermissions())) {
      recentRail.setVisibility(View.VISIBLE);
      loaderManager.restartLoader(1, null, recentRail);
    } else {
      recentRail.setVisibility(View.GONE);
    }

    showAtLocation(anchor, Gravity.BOTTOM, 0, 0);

    getContentView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        getContentView().getViewTreeObserver().removeOnGlobalLayoutListener(this);

        animateWindowInTranslate(getContentView());
      }
    });
  }

  @Override
  public void dismiss() {
      animateWindowOutTranslate(getContentView());
  }

  public void setListener(@Nullable AttachmentClickedListener listener) {
    this.listener = listener;
  }


  private void animateWindowInTranslate(@NonNull View contentView) {
    Animation animation = new TranslateAnimation(0, 0, contentView.getHeight(), 0);
    animation.setDuration(ANIMATION_DURATION);

    getContentView().startAnimation(animation);
  }

  private void animateWindowOutTranslate(@NonNull View contentView) {
    Animation animation = new TranslateAnimation(0, 0, 0, contentView.getTop() + contentView.getHeight());
    animation.setDuration(ANIMATION_DURATION);
    animation.setAnimationListener(new Animation.AnimationListener() {
      @Override
      public void onAnimationStart(Animation animation) {
      }

      @Override
      public void onAnimationEnd(Animation animation) {
        AvatarSelector.super.dismiss();
      }

      @Override
      public void onAnimationRepeat(Animation animation) {
      }
    });

    getContentView().startAnimation(animation);
  }

  private class RecentPhotoSelectedListener implements RecentPhotoViewRail.OnItemClickedListener {
    @Override
    public void onItemClicked(Uri uri) {
      animateWindowOutTranslate(getContentView());

      if (listener != null) listener.onQuickAttachment(uri);
    }
  }

  private class PropagatingClickListener implements View.OnClickListener {

    private final int type;

    private PropagatingClickListener(int type) {
      this.type = type;
    }

    @Override
    public void onClick(View v) {
      animateWindowOutTranslate(getContentView());

      if (listener != null) listener.onClick(type);
    }

  }

  private class CloseClickListener implements View.OnClickListener {
    @Override
    public void onClick(View v) {
      dismiss();
    }
  }

  public interface AttachmentClickedListener {
    void onClick(int type);
    void onQuickAttachment(Uri uri);
  }

}
