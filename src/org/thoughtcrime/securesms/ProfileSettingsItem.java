package org.thoughtcrime.securesms;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ProfileSettingsItem extends LinearLayout {

  private TextView labelView;

  public ProfileSettingsItem(Context context) {
    super(context);
  }

  public ProfileSettingsItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    labelView = findViewById(R.id.label);
  }

  public void set(String label, int iconLeft) {
    labelView.setText(label==null? "" : label);
    labelView.setCompoundDrawablesWithIntrinsicBounds(iconLeft, 0,0,0);
  }
}
