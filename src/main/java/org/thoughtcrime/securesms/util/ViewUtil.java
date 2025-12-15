/**
 * Copyright (C) 2015 Open Whisper Systems
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
package org.thoughtcrime.securesms.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AbsSpinner;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.views.Stub;

import chat.delta.util.ListenableFuture;
import chat.delta.util.SettableFuture;

public class ViewUtil {
  private final static String TAG = ViewUtil.class.getSimpleName();

  @SuppressWarnings("deprecation")
  public static void setBackground(final @NonNull View v, final @Nullable Drawable drawable) {
    v.setBackground(drawable);
  }

  public static float getY(final @NonNull View v) {
    return ViewCompat.getY(v);
  }

  public static void setX(final @NonNull View v, final int x) {
    ViewCompat.setX(v, x);
  }

  public static float getX(final @NonNull View v) {
    return ViewCompat.getX(v);
  }

  public static void swapChildInPlace(ViewGroup parent, View toRemove, View toAdd, int defaultIndex) {
    int childIndex = parent.indexOfChild(toRemove);
    if (childIndex > -1) parent.removeView(toRemove);
    parent.addView(toAdd, childIndex > -1 ? childIndex : defaultIndex);
  }

  @SuppressWarnings("unchecked")
  public static <T extends View> T inflateStub(@NonNull View parent, @IdRes int stubId) {
    return (T)((ViewStub)parent.findViewById(stubId)).inflate();
  }

  @SuppressWarnings("unchecked")
  public static <T extends View> T findById(@NonNull View parent, @IdRes int resId) {
    return (T) parent.findViewById(resId);
  }

  @SuppressWarnings("unchecked")
  public static <T extends View> T findById(@NonNull Activity parent, @IdRes int resId) {
    return (T) parent.findViewById(resId);
  }

  public static <T extends View> Stub<T> findStubById(@NonNull Activity parent, @IdRes int resId) {
    return new Stub<T>((ViewStub)parent.findViewById(resId));
  }

  private static Animation getAlphaAnimation(float from, float to, int duration) {
    final Animation anim = new AlphaAnimation(from, to);
    anim.setInterpolator(new FastOutSlowInInterpolator());
    anim.setDuration(duration);
    return anim;
  }

  public static void fadeIn(final @NonNull View view, final int duration) {
    animateIn(view, getAlphaAnimation(0f, 1f, duration));
  }

  public static ListenableFuture<Boolean> fadeOut(final @NonNull View view, final int duration) {
    return fadeOut(view, duration, View.GONE);
  }

  public static ListenableFuture<Boolean> fadeOut(@NonNull View view, int duration, int visibility) {
    return animateOut(view, getAlphaAnimation(1f, 0f, duration), visibility);
  }

  public static ListenableFuture<Boolean> animateOut(final @NonNull View view, final @NonNull Animation animation, final int visibility) {
    final SettableFuture future = new SettableFuture();
    if (view.getVisibility() == visibility) {
      future.set(true);
    } else if (AccessibilityUtil.areAnimationsDisabled(view.getContext())) {
      view.setVisibility(visibility);
      future.set(true);
    } else {
      view.clearAnimation();
      animation.reset();
      animation.setStartTime(0);
      animation.setAnimationListener(new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {}

        @Override
        public void onAnimationRepeat(Animation animation) {}

        @Override
        public void onAnimationEnd(Animation animation) {
          view.setVisibility(visibility);
          future.set(true);
        }
      });
      view.startAnimation(animation);
    }
    return future;
  }

  public static void animateIn(final @NonNull View view, final @NonNull Animation animation) {
    if (view.getVisibility() == View.VISIBLE) return;

    if (AccessibilityUtil.areAnimationsDisabled(view.getContext())) {
      view.setVisibility(View.VISIBLE);
      return;
    }

    view.clearAnimation();
    animation.reset();
    animation.setStartTime(0);
    view.setVisibility(View.VISIBLE);
    view.startAnimation(animation);
  }

  @SuppressWarnings("unchecked")
  public static <T extends View> T inflate(@NonNull   LayoutInflater inflater,
                                           @NonNull   ViewGroup      parent,
                                           @LayoutRes int            layoutResId)
  {
    return (T)(inflater.inflate(layoutResId, parent, false));
  }

  @SuppressLint("RtlHardcoded")
  public static void setTextViewGravityStart(final @NonNull TextView textView, @NonNull Context context) {
    if (Util.getLayoutDirection(context) == View.LAYOUT_DIRECTION_RTL) {
      textView.setGravity(Gravity.RIGHT);
    } else {
      textView.setGravity(Gravity.LEFT);
    }
  }

  public static void mirrorIfRtl(View view, Context context) {
    if (Util.getLayoutDirection(context) == View.LAYOUT_DIRECTION_RTL) {
      view.setScaleX(-1.0f);
    }
  }

  public static boolean isLtr(@NonNull View view) {
    return isLtr(view.getContext());
  }

  public static boolean isLtr(@NonNull Context context) {
    return Util.getLayoutDirection(context) == ViewCompat.LAYOUT_DIRECTION_LTR;
  }

  public static boolean isRtl(@NonNull View view) {
    return isRtl(view.getContext());
  }

  public static boolean isRtl(@NonNull Context context) {
    return Util.getLayoutDirection(context) == ViewCompat.LAYOUT_DIRECTION_RTL;
  }

  public static int dpToPx(Context context, int dp) {
    return (int)((dp * context.getResources().getDisplayMetrics().density) + 0.5);
  }

  public static float pxToSp(Context context, int px) {
    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
    if (VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE) {
      return TypedValue.deriveDimension(TypedValue.COMPLEX_UNIT_SP, px, metrics);
    } else {
      if (metrics.scaledDensity == 0) {
        return 0;
      }
      return px / metrics.scaledDensity;
    }
  }

  public static void updateLayoutParams(@NonNull View view, int width, int height) {
    view.getLayoutParams().width  = width;
    view.getLayoutParams().height = height;
    view.requestLayout();
  }

  public static int getLeftMargin(@NonNull View view) {
    if (ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_LTR) {
      return ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).leftMargin;
    }
    return ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).rightMargin;
  }

  public static int getRightMargin(@NonNull View view) {
    if (ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_LTR) {
      return ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).rightMargin;
    }
    return ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).leftMargin;
  }

  public static void setLeftMargin(@NonNull View view, int margin) {
    if (ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_LTR) {
      ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).leftMargin = margin;
    } else {
      ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).rightMargin = margin;
    }
    view.forceLayout();
    view.requestLayout();
  }

  public static void setRightMargin(@NonNull View view, int margin) {
    if (ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_LTR) {
      ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).rightMargin = margin;
    } else {
      ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).leftMargin = margin;
    }
    view.forceLayout();
    view.requestLayout();
  }

  public static void setTopMargin(@NonNull View view, int margin) {
    ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).topMargin = margin;
    view.requestLayout();
  }

  public static void setPaddingTop(@NonNull View view, int padding) {
    view.setPadding(view.getPaddingLeft(), padding, view.getPaddingRight(), view.getPaddingBottom());
  }

  public static void setPaddingBottom(@NonNull View view, int padding) {
    view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), padding);
  }

  public static int dpToPx(int dp) {
    return Math.round(dp * Resources.getSystem().getDisplayMetrics().density);
  }

  public static int getStatusBarHeight(@NonNull View view) {
    final WindowInsetsCompat rootWindowInsets = ViewCompat.getRootWindowInsets(view);
    if (Build.VERSION.SDK_INT > 29 && rootWindowInsets != null) {
      return rootWindowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
    } else {
      int result     = 0;
      int resourceId = view.getResources().getIdentifier("status_bar_height", "dimen", "android");
      if (resourceId > 0) {
        result = view.getResources().getDimensionPixelSize(resourceId);
      }
      return result;
    }
  }

  // Checks if a selection is valid for a given Spinner view.
  // Returns given selection if valid.
  // Otherwise, to avoid ArrayIndexOutOfBoundsException, 0 is returned, assuming to refer to a good default.
  public static int checkBounds(int selection, AbsSpinner view) {
    if (selection < 0 || selection >= view.getCount()) {
      Log.w(TAG, "index " + selection + " out of bounds of " + view.toString());
      return 0;
    }
    return selection;
  }

  /** Return true if the system supports edge-to-edge properly */
  public static boolean isEdgeToEdgeSupported() {
    return Build.VERSION.SDK_INT >= VERSION_CODES.R;
  }

  /**
   * Get combined insets from status bar, navigation bar and display cutout areas.
   * 
   * @param windowInsets The window insets to extract from
   * @return Combined insets using the maximum values from system bars and display cutout
   */
  private static Insets getCombinedInsets(@NonNull WindowInsetsCompat windowInsets) {
    Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
    Insets displayCutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());
    return Insets.max(systemBars, displayCutout);
  }

  /**
   * Apply window insets to a view by adding margin to avoid drawing it behind system bars.
   * Convenience method that applies insets to all sides.
   * 
   * @param view The view to apply insets to
   */
  public static void applyWindowInsetsAsMargin(@NonNull View view) {
    applyWindowInsetsAsMargin(view, true, true, true, true);
  }

  /**
   * Apply window insets to a view by adding margin to avoid drawing it behind system bars.
   * 
   * This method stores the original margin values in view tags to ensure that
   * margin doesn't accumulate on multiple inset applications.
   * 
   * @param view The view to apply insets to
   * @param left Whether to apply left inset
   * @param top Whether to apply top inset
   * @param right Whether to apply right inset
   * @param bottom Whether to apply bottom inset
   */
  public static void applyWindowInsetsAsMargin(@NonNull View view, boolean left, boolean top, boolean right, boolean bottom) {
    // Only enable on API 30+ where WindowInsets APIs work correctly
    if (!isEdgeToEdgeSupported()) return;

    // Store the original margin as a tag only if not already stored
    // This prevents losing the true original margin on subsequent calls
    if (view.getTag(R.id.tag_window_insets_margin_left) == null) {
      ViewGroup.LayoutParams params = view.getLayoutParams();
      if (params instanceof ViewGroup.MarginLayoutParams) {
        ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) params;
        view.setTag(R.id.tag_window_insets_margin_left, marginParams.leftMargin);
        view.setTag(R.id.tag_window_insets_margin_top, marginParams.topMargin);
        view.setTag(R.id.tag_window_insets_margin_right, marginParams.rightMargin);
        view.setTag(R.id.tag_window_insets_margin_bottom, marginParams.bottomMargin);
      }
    }

    ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
      Insets insets = getCombinedInsets(windowInsets);

      // Retrieve the original margin values from tags with null checks
      Integer leftTag = (Integer) v.getTag(R.id.tag_window_insets_margin_left);
      Integer topTag = (Integer) v.getTag(R.id.tag_window_insets_margin_top);
      Integer rightTag = (Integer) v.getTag(R.id.tag_window_insets_margin_right);
      Integer bottomTag = (Integer) v.getTag(R.id.tag_window_insets_margin_bottom);
      int baseMarginLeft = leftTag != null ? leftTag : 0;
      int baseMarginTop = topTag != null ? topTag : 0;
      int baseMarginRight = rightTag != null ? rightTag : 0;
      int baseMarginBottom = bottomTag != null ? bottomTag : 0;

      ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
      if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
        ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) layoutParams;
        marginParams.leftMargin = baseMarginLeft + insets.left;
        marginParams.topMargin = baseMarginTop + insets.top;
        marginParams.rightMargin = baseMarginRight + insets.right;
        marginParams.bottomMargin = baseMarginBottom + insets.bottom;
        v.setLayoutParams(marginParams);
      }

      return windowInsets;
    });

    // Request the initial insets to be dispatched if the view is attached
    if (view.isAttachedToWindow()) {
      ViewCompat.requestApplyInsets(view);
    }
  }

  /**
   * Apply window insets to a view by adding padding to avoid  drawing elements behind system bars.
   * Convenience method that applies insets to all sides.
   * 
   * @param view The view to apply insets to
   */
  public static void applyWindowInsets(@NonNull View view) {
    applyWindowInsets(view, true, true, true, true);
  }

  /**
   * Apply window insets to a view by adding padding to avoid drawing elements behind system bars.
   * 
   * This method stores the original padding values in view tags to ensure that
   * padding doesn't accumulate on multiple inset applications.
   * 
   * @param view The view to apply insets to
   * @param left Whether to apply left inset
   * @param top Whether to apply top inset
   * @param right Whether to apply right inset
   * @param bottom Whether to apply bottom inset
   */
  public static void applyWindowInsets(@NonNull View view, boolean left, boolean top, boolean right, boolean bottom) {
    // Only enable on API 30+ where WindowInsets APIs work correctly
    if (!isEdgeToEdgeSupported()) return;

    // Store the original padding as a tag only if not already stored
    // This prevents losing the true original padding on subsequent calls
    if (view.getTag(R.id.tag_window_insets_padding_left) == null) {
      view.setTag(R.id.tag_window_insets_padding_left, view.getPaddingLeft());
      view.setTag(R.id.tag_window_insets_padding_top, view.getPaddingTop());
      view.setTag(R.id.tag_window_insets_padding_right, view.getPaddingRight());
      view.setTag(R.id.tag_window_insets_padding_bottom, view.getPaddingBottom());
    }

    ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
      Insets insets = getCombinedInsets(windowInsets);

      // Retrieve the original padding values from tags with null checks
      Integer leftTag = (Integer) v.getTag(R.id.tag_window_insets_padding_left);
      Integer topTag = (Integer) v.getTag(R.id.tag_window_insets_padding_top);
      Integer rightTag = (Integer) v.getTag(R.id.tag_window_insets_padding_right);
      Integer bottomTag = (Integer) v.getTag(R.id.tag_window_insets_padding_bottom);
      int basePaddingLeft = leftTag != null ? leftTag : 0;
      int basePaddingTop = topTag != null ? topTag : 0;
      int basePaddingRight = rightTag != null ? rightTag : 0;
      int basePaddingBottom = bottomTag != null ? bottomTag : 0;

      v.setPadding(
          left ? basePaddingLeft + insets.left : basePaddingLeft,
          top ? basePaddingTop + insets.top : basePaddingTop,
          right ? basePaddingRight + insets.right : basePaddingRight,
          bottom ? basePaddingBottom + insets.bottom : basePaddingBottom
      );

      return windowInsets;
    });

    // Request the initial insets to be dispatched if the view is attached
    if (view.isAttachedToWindow()) {
      ViewCompat.requestApplyInsets(view);
    }
  }

  /**
   * Apply the top status bar inset as the height of a view.
   */
  private static void applyTopInsetAsHeight(@NonNull View view) {
    ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
      Insets insets = getCombinedInsets(windowInsets);

      android.view.ViewGroup.LayoutParams params = v.getLayoutParams();
      if (params != null) {
        params.height = insets.top;
        v.setLayoutParams(params);
      }

      return windowInsets;
    });

    // Request the initial insets to be dispatched if the view is attached
    if (view.isAttachedToWindow()) {
      ViewCompat.requestApplyInsets(view);
    }
  }

  /**
   * Apply adjustments to the activity's custom toolbar or set height of R.id.status_bar_background for proper Edge-to-Edge display.
   * 
   * @param activity The activity to apply the adjustments to
   */
  public static void adjustToolbarForE2E(@NonNull AppCompatActivity activity) {
    // Only enable on API 30+ where WindowInsets APIs work correctly
    if (!isEdgeToEdgeSupported()) return;

    // The toolbar/app bar should extend behind the status bar with padding applied
    View toolbar = activity.findViewById(R.id.toolbar);
    if (toolbar != null) {
      // Check if toolbar is inside an AppBarLayout
      View parent = (View) toolbar.getParent();
      if (parent instanceof com.google.android.material.appbar.AppBarLayout) {
        ViewUtil.applyWindowInsets(parent, true, true, true, false);
      } else {
        ViewUtil.applyWindowInsets(toolbar, true, true, true, false);
      }
    }

    // For activities without a custom toolbar, apply insets to status_bar_background view
    View statusBarBackground = activity.findViewById(R.id.status_bar_background);
    if (statusBarBackground != null) {
      ViewUtil.applyTopInsetAsHeight(statusBarBackground);
      ActionBar actionBar = activity.getSupportActionBar();
      if (actionBar != null) {
        // elevation is set via status_bar_background view
        // otherwise there is a drop-shadow at the top
        actionBar.setElevation(0);
      }
    }
  }

}
