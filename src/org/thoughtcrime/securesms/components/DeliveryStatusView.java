package org.thoughtcrime.securesms.components;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import org.thoughtcrime.securesms.R;

public class DeliveryStatusView {

  private final ImageView deliveryIndicator;
  private static RotateAnimation animation;
  private boolean animated;

  public DeliveryStatusView(ImageView deliveryIndicator) {
    this.deliveryIndicator = deliveryIndicator;
  }

  private void animate()
  {
    if(animation==null) {
      animation = new RotateAnimation(0, 360f,
          Animation.RELATIVE_TO_SELF, 0.5f,
          Animation.RELATIVE_TO_SELF, 0.5f);
      animation.setInterpolator(new LinearInterpolator());
      animation.setDuration(1500);
      animation.setRepeatCount(Animation.INFINITE);
    }

    deliveryIndicator.startAnimation(animation);
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

  public void setPending() {
    deliveryIndicator.setVisibility(View.VISIBLE);
    deliveryIndicator.setImageResource(R.drawable.ic_delivery_status_sending);
    animate();
  }

  public void setSent() {
    deliveryIndicator.setVisibility(View.VISIBLE);
    deliveryIndicator.setImageResource(R.drawable.ic_delivery_status_sent);
    clearAnimation();
  }

  public void setRead() {
    deliveryIndicator.setVisibility(View.VISIBLE);
    deliveryIndicator.setImageResource(R.drawable.ic_delivery_status_read);
    clearAnimation();
  }

  public void setFailed() {
    deliveryIndicator.setVisibility(View.VISIBLE);
    deliveryIndicator.setImageResource(R.drawable.ic_delivery_status_failed);
    clearAnimation();
  }

  public void setTint(int color) {
    deliveryIndicator.setColorFilter(color);
  }
}
