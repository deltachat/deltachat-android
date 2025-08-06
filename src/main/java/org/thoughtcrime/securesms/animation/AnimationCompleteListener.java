package org.thoughtcrime.securesms.animation;


import android.animation.Animator;

import androidx.annotation.NonNull;

public abstract class AnimationCompleteListener implements Animator.AnimatorListener {
  @Override
  public final void onAnimationStart(@NonNull Animator animation) {}

  @Override
  public abstract void onAnimationEnd(@NonNull Animator animation);

  @Override
  public final void onAnimationCancel(@NonNull Animator animation) {}
  @Override
  public final void onAnimationRepeat(@NonNull Animator animation) {}
}
