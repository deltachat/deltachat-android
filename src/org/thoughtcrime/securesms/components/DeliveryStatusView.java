package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.AccessibilityUtil;

public class DeliveryStatusView {

  private final ImageView deliveryIndicator;
  private final Context   context;
  private static RotateAnimation prepareAnimation;
  private static RotateAnimation sendingAnimation;
  private boolean animated;

  public DeliveryStatusView(ImageView deliveryIndicator) {
    this.deliveryIndicator = deliveryIndicator;
    this.context = deliveryIndicator.getContext();
  }

  private void animatePrepare()
  {
    if (AccessibilityUtil.areAnimationsDisabled(context)) return;
    
    if(prepareAnimation ==null) {
      prepareAnimation = new RotateAnimation(360f, 0f,
          Animation.RELATIVE_TO_SELF, 0.5f,
          Animation.RELATIVE_TO_SELF, 0.5f);
      prepareAnimation.setInterpolator(new LinearInterpolator());
      prepareAnimation.setDuration(2500);
      prepareAnimation.setRepeatCount(Animation.INFINITE);
    }

    deliveryIndicator.startAnimation(prepareAnimation);
    animated = true;
  }

  private void animateSending()
  {
    if (AccessibilityUtil.areAnimationsDisabled(context)) return;

    if(sendingAnimation ==null) {
      sendingAnimation = new RotateAnimation(0, 360f,
          Animation.RELATIVE_TO_SELF, 0.5f,
          Animation.RELATIVE_TO_SELF, 0.5f);
      sendingAnimation.setInterpolator(new LinearInterpolator());
      sendingAnimation.setDuration(1500);
      sendingAnimation.setRepeatCount(Animation.INFINITE);
    }

    deliveryIndicator.startAnimation(sendingAnimation);
    animated = true;
  }

  private void clearAnimation()
  {
    if(animated) {
      deliveryIndicator.clearAnimation();
      animated = false;
    }
  }

  public void setNone() {
    deliveryIndicator.setVisibility(View.GONE);
    deliveryIndicator.clearAnimation();
  }

  public void setDownloading() {
    deliveryIndicator.setVisibility(View.VISIBLE);
    deliveryIndicator.setImageResource(R.drawable.ic_delivery_status_sending);
    deliveryIndicator.setContentDescription(context.getString(R.string.one_moment));
    animatePrepare();
  }

  public void setPreparing() {
    deliveryIndicator.setVisibility(View.VISIBLE);
    deliveryIndicator.setImageResource(R.drawable.ic_delivery_status_sending);
    deliveryIndicator.setContentDescription(context.getString(R.string.a11y_delivery_status_sending));
    animatePrepare();
  }

  public void setPending() {
    deliveryIndicator.setVisibility(View.VISIBLE);
    deliveryIndicator.setImageResource(R.drawable.ic_delivery_status_sending);
    deliveryIndicator.setContentDescription(context.getString(R.string.a11y_delivery_status_sending));
    animateSending();
  }

  public void setSent() {
    deliveryIndicator.setVisibility(View.VISIBLE);
    deliveryIndicator.setImageResource(R.drawable.ic_delivery_status_sent);
    deliveryIndicator.setContentDescription(context.getString(R.string.a11y_delivery_status_delivered));
    clearAnimation();
  }

  public void setRead() {
    deliveryIndicator.setVisibility(View.VISIBLE);
    deliveryIndicator.setImageResource(R.drawable.ic_delivery_status_read);
    deliveryIndicator.setContentDescription(context.getString(R.string.a11y_delivery_status_read));
    clearAnimation();
  }

  public void setFailed() {
    deliveryIndicator.setVisibility(View.VISIBLE);
    deliveryIndicator.setImageResource(R.drawable.ic_delivery_status_failed);
    deliveryIndicator.setContentDescription(context.getString(R.string.a11y_delivery_status_invalid));
    clearAnimation();
  }

  public void setTint(Integer color) {
    if (color != null) {
      deliveryIndicator.setColorFilter(color);
    } else {
      resetTint();
    }
  }

  public void resetTint() {
    deliveryIndicator.setColorFilter(null);
  }

  public String getDescription() {
    if (deliveryIndicator.getVisibility() == View.VISIBLE) {
      return deliveryIndicator.getContentDescription().toString();
    }
    return "";
  }
}
