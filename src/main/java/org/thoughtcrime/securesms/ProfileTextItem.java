package org.thoughtcrime.securesms;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import org.thoughtcrime.securesms.util.ResUtil;

public class ProfileTextItem extends LinearLayout {

  private TextView labelView;
  private @Nullable TextView valueView;
  private final ProfileTextItem.PassthroughClickListener passthroughClickListener   = new ProfileTextItem.PassthroughClickListener();

  public ProfileTextItem(Context context) {
    super(context);
  }

  public ProfileTextItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    labelView = findViewById(R.id.label);
    labelView.setOnLongClickListener(passthroughClickListener);
    labelView.setOnClickListener(passthroughClickListener);
    valueView = findViewById(R.id.value);
  }

  public void set(String label, int icon) {
    labelView.setText(label);

    if (icon != 0) {
      Drawable orgDrawable = ContextCompat.getDrawable(getContext(), icon);
      if (orgDrawable != null) {
        Drawable drawable = orgDrawable.mutate(); // avoid global state modification and showing eg. app-icon tinted also elsewhere
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, getResources().getColor(R.color.delta_accent));
        labelView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
      }
    }
  }

  public void setValue(String value) {
    if (valueView != null) {
      valueView.setText(value);
      valueView.setVisibility(View.VISIBLE);
    }
  }

  private class PassthroughClickListener implements View.OnLongClickListener, View.OnClickListener {

    @Override
    public boolean onLongClick(View v) {
      performLongClick();
      return true;
    }

    @Override
    public void onClick(View v) {
      performClick();
    }
  }
}
