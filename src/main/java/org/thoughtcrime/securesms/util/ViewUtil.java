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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

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
}
