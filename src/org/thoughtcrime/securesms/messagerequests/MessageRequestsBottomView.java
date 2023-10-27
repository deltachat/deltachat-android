package org.thoughtcrime.securesms.messagerequests;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.ViewUtil;

public class MessageRequestsBottomView extends ConstraintLayout {

  private AppCompatTextView question;
  private Button            accept;
  private Button block;

  public MessageRequestsBottomView(Context context) {
    super(context);
  }

  public MessageRequestsBottomView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public MessageRequestsBottomView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    inflate(getContext(), R.layout.message_request_bottom_bar, this);

    question = findViewById(R.id.message_request_question);
    accept   = findViewById(R.id.message_request_accept);
    block    = findViewById(R.id.message_request_block);
  }

  public void setAcceptOnClickListener(OnClickListener acceptOnClickListener) {
    accept.setOnClickListener(acceptOnClickListener);
  }

  public void setAcceptText(int text) {
    accept.setText(text);
  }

  public void setBlockOnClickListener(OnClickListener deleteOnClickListener) {
    block.setOnClickListener(deleteOnClickListener);
  }

  public void setBlockText(int text) {
    block.setText(text);
  }

  public void setQuestion(String text) {
    if (text == null || text.isEmpty()) {
      question.setMaxHeight(ViewUtil.dpToPx(5));
    } else {
      question.setMaxHeight(Integer.MAX_VALUE);
      question.setText(text);
    }
  }
}
