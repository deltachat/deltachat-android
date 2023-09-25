package org.thoughtcrime.securesms;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.thoughtcrime.securesms.util.ResUtil;

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

  public void set(String label, int labelColor, int iconLeft) {
    labelView.setText(label==null? "" : label);
    labelView.setCompoundDrawablesWithIntrinsicBounds(iconLeft, 0,0,0);

    // we need different color getters as `labelColor` is `R.color.name` while default is `R.attr.name`
    if (labelColor != 0) {
      labelView.setTextColor(ContextCompat.getColor(getContext(), labelColor));
    } else {
      labelView.setTextColor(ResUtil.getColor(getContext(), R.attr.emoji_text_color)); //
    }
  }
}
