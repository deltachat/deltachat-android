package org.thoughtcrime.securesms;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.thoughtcrime.securesms.util.ResUtil;

public class ProfileTextItem extends LinearLayout {

  private TextView labelView;
  private @Nullable TextView valueView;

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
    valueView = findViewById(R.id.value);
  }

  public void set(String label, int iconRight) {
    labelView.setText(label);
    labelView.setCompoundDrawablesWithIntrinsicBounds(0, 0,iconRight,0);
  }

  public void setValue(String value) {
    if (valueView != null) {
      valueView.setText(value);
      valueView.setVisibility(View.VISIBLE);
    }
  }
}
