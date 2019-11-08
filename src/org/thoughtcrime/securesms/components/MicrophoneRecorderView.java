package org.thoughtcrime.securesms.components;

import android.Manifest;
import android.content.Context;
import android.graphics.PorterDuff;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.ViewUtil;

public class MicrophoneRecorderView extends FrameLayout implements View.OnTouchListener {

  public static final int ANIMATION_DURATION = 200;

  private           FloatingRecordButton floatingRecordButton;
  private @Nullable Listener             listener;
  private           boolean              actionInProgress;

  public MicrophoneRecorderView(Context context) {
    super(context);
  }

  public MicrophoneRecorderView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void onFinishInflate() {
    super.onFinishInflate();

    ImageView recordButtonFab = ViewUtil.findById(this, R.id.quick_audio_fab);
    this.floatingRecordButton = new FloatingRecordButton(getContext(), recordButtonFab);

    View recordButton = ViewUtil.findById(this, R.id.quick_audio_toggle);
    recordButton.setOnTouchListener(this);
  }

  public void cancelAction() {
    if (this.actionInProgress) {
      this.actionInProgress = false;
      this.floatingRecordButton.hide(this.floatingRecordButton.lastPositionX);

      if (listener != null) listener.onRecordCanceled(this.floatingRecordButton.lastPositionX);
    }
  }

  @Override
  public boolean onTouch(View v, final MotionEvent event) {
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        if (!Permissions.hasAll(getContext(), Manifest.permission.RECORD_AUDIO)) {
          if (listener != null) listener.onRecordPermissionRequired();
        } else {
          this.actionInProgress = true;
          this.floatingRecordButton.display(event.getX());
          if (listener != null) listener.onRecordPressed(event.getX());
        }
        break;
      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP:
        if (this.actionInProgress) {
          this.actionInProgress = false;
          this.floatingRecordButton.hide(event.getX());
          if (listener != null) listener.onRecordReleased(event.getX());
        }
        break;
      case MotionEvent.ACTION_MOVE:
        if (this.actionInProgress) {
          this.floatingRecordButton.moveTo(event.getX());
          if (listener != null) listener.onRecordMoved(event.getX(), event.getRawX());
        }
        break;
    }

    return false;
  }

  public void setListener(@Nullable Listener listener) {
    this.listener = listener;
  }

  public interface Listener {
    void onRecordPressed(float x);
    void onRecordReleased(float x);
    void onRecordCanceled(float x);
    void onRecordMoved(float x, float absoluteX);
    void onRecordPermissionRequired();
  }

  private static class FloatingRecordButton {

    private final ImageView recordButtonFab;

    private float startPositionX;
    private float lastPositionX;

    public FloatingRecordButton(Context context, ImageView recordButtonFab) {
      this.recordButtonFab = recordButtonFab;
      this.recordButtonFab.getBackground().setColorFilter(context.getResources()
                                                                 .getColor(R.color.audio_icon),
                                                          PorterDuff.Mode.SRC_IN);
    }

    public void display(float x) {
      this.startPositionX = x;
      this.lastPositionX  = x;

      recordButtonFab.setVisibility(View.VISIBLE);

      AnimationSet animation = new AnimationSet(true);
      animation.addAnimation(new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
                                                    Animation.RELATIVE_TO_SELF, 0,
                                                    Animation.RELATIVE_TO_SELF, -.25f,
                                                    Animation.RELATIVE_TO_SELF, -.25f));

      animation.addAnimation(new ScaleAnimation(.5f, 1f, .5f, 1f,
                                                Animation.RELATIVE_TO_SELF, .5f,
                                                Animation.RELATIVE_TO_SELF, .5f));

      animation.setFillBefore(true);
      animation.setFillAfter(true);
      animation.setDuration(ANIMATION_DURATION);
      animation.setInterpolator(new OvershootInterpolator());

      recordButtonFab.startAnimation(animation);
    }

    public void moveTo(float x) {
      this.lastPositionX = x;

      float offset = getOffset(x);

      Animation translateAnimation = new TranslateAnimation(Animation.ABSOLUTE, offset,
                                                            Animation.ABSOLUTE, offset,
                                                            Animation.RELATIVE_TO_SELF, -.25f,
                                                            Animation.RELATIVE_TO_SELF, -.25f);

      translateAnimation.setDuration(0);
      translateAnimation.setFillAfter(true);
      translateAnimation.setFillBefore(true);

      recordButtonFab.startAnimation(translateAnimation);
    }

    public void hide(float x) {
      this.lastPositionX = x;

      float offset = getOffset(x);

      AnimationSet animation = new AnimationSet(false);
      Animation scaleAnimation = new ScaleAnimation(1, 0.5f, 1, 0.5f,
                                                    Animation.RELATIVE_TO_SELF, 0.5f,
                                                    Animation.RELATIVE_TO_SELF, 0.5f);

      Animation translateAnimation = new TranslateAnimation(Animation.ABSOLUTE, offset,
                                                            Animation.ABSOLUTE, 0,
                                                            Animation.RELATIVE_TO_SELF, -.25f,
                                                            Animation.RELATIVE_TO_SELF, -.25f);

      scaleAnimation.setInterpolator(new AnticipateOvershootInterpolator(1.5f));
      translateAnimation.setInterpolator(new DecelerateInterpolator());
      animation.addAnimation(scaleAnimation);
      animation.addAnimation(translateAnimation);
      animation.setDuration(ANIMATION_DURATION);
      animation.setFillBefore(true);
      animation.setFillAfter(false);
      animation.setInterpolator(new AnticipateOvershootInterpolator(1.5f));

      recordButtonFab.setVisibility(View.GONE);
      recordButtonFab.clearAnimation();
      recordButtonFab.startAnimation(animation);
    }

    private float getOffset(float x) {
      return ViewCompat.getLayoutDirection(recordButtonFab) == ViewCompat.LAYOUT_DIRECTION_LTR ?
          -Math.max(0, this.startPositionX - x) : Math.max(0, x - this.startPositionX);
    }
  }

}
