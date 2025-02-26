package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

import org.thoughtcrime.securesms.R;

public class ImageDivet extends AppCompatImageView {
  private Drawable drawable;
  private int drawableIntrinsicWidth;
  private int drawableIntrinsicHeight;

  public ImageDivet(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    setDrawable();
  }

  public ImageDivet(Context context, AttributeSet attrs) {
    super(context, attrs);
    setDrawable();
  }

  public ImageDivet(Context context) {
    super(context);
    setDrawable();
  }

  private void setDrawable() {
    int[] attributes = new int[] {R.attr.lower_right_divet};

    try (TypedArray drawables = getContext().obtainStyledAttributes(attributes)) {
      drawable = drawables.getDrawable(0);
      drawableIntrinsicWidth = drawable != null ? drawable.getIntrinsicWidth() : 0;
      drawableIntrinsicHeight = drawable != null ? drawable.getIntrinsicHeight() : 0;
    }
  }

  @Override
  public void onDraw(Canvas c) {
    super.onDraw(c);
    c.save();

    final int right = getWidth();
    final int bottom = getHeight();
    drawable.setBounds(
      right - drawableIntrinsicWidth,
      bottom - drawableIntrinsicHeight,
      right,
      bottom);

    drawable.draw(c);
    c.restore();
  }
}
