package org.thoughtcrime.securesms;

import android.content.Context;
import android.text.SpannableString;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.widget.AppCompatTextView;

import org.thoughtcrime.securesms.util.Linkifier;
import org.thoughtcrime.securesms.util.LongClickMovementMethod;

public class ProfileStatusItem extends LinearLayout {

  private AppCompatTextView statusTextView;
  private final PassthroughClickListener passthroughClickListener   = new PassthroughClickListener();

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
    statusTextView.setOnLongClickListener(passthroughClickListener);
    statusTextView.setOnClickListener(passthroughClickListener);
    statusTextView.setMovementMethod(LongClickMovementMethod.getInstance(getContext()));
  }

  public void set(String status) {
    statusTextView.setText(Linkifier.linkify(new SpannableString(status)));
  }

  private class PassthroughClickListener implements View.OnLongClickListener, View.OnClickListener {

    @Override
    public boolean onLongClick(View v) {
      if (statusTextView.hasSelection()) {
        return false;
      }
      performLongClick();
      return true;
    }

    @Override
    public void onClick(View v) {
      performClick();
    }
  }
}
