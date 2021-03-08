package org.thoughtcrime.securesms;

import android.content.Context;
import android.text.SpannableString;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import org.thoughtcrime.securesms.components.emoji.EmojiTextView;

public class ProfileStatusItem extends LinearLayout {

  private EmojiTextView statusTextView;

  public ProfileStatusItem(Context context) {
    super(context);
  }

  public ProfileStatusItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    statusTextView = findViewById(R.id.status_text);
  }

  public void set(String status) {
    statusTextView.setText(EmojiTextView.linkify(new SpannableString(status)));
  }
}
