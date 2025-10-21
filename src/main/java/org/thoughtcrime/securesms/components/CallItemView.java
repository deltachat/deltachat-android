package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.thoughtcrime.securesms.R;

import chat.delta.rpc.types.CallInfo;
import chat.delta.rpc.types.CallState;

public class CallItemView extends FrameLayout {
  private static final String TAG = CallItemView.class.getSimpleName();

  private final @NonNull ImageView icon;
  private final @NonNull TextView title;
  private final @NonNull ConversationItemFooter footer;
  private CallInfo callInfo;
  private CallClickListener viewListener;

  public CallItemView(Context context) {
    this(context, null);
  }

  public CallItemView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public CallItemView(final Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    inflate(context, R.layout.call_item_view, this);

    this.icon = findViewById(R.id.call_icon);
    this.title = findViewById(R.id.title);
    this.footer = findViewById(R.id.footer);

    setOnClickListener(v -> {
      if (viewListener != null && callInfo != null) {
        viewListener.onClick(v, callInfo);
      }
    });
  }

  public void setCallClickListener(CallClickListener listener) {
    viewListener = listener;
  }

  public void setCallItem(boolean isOutgoing, CallInfo callInfo) {
    this.callInfo = callInfo;
    if (callInfo.state instanceof CallState.Completed) {
      footer.setCallDuration(((CallState.Completed) callInfo.state).duration);
    } else {
      footer.setCallDuration(0); // reset
    }

    if (callInfo.state instanceof CallState.Missed) {
      title.setText(R.string.missed_call);
    } else if (callInfo.state instanceof CallState.Canceled) {
      title.setText(R.string.canceled_call);
    } else if (callInfo.state instanceof CallState.Declined) {
      title.setText(R.string.declined_call);
    } else {
      title.setText(isOutgoing? R.string.outgoing_call : R.string.incoming_call);
    }

    int[] attrs;
    if (isOutgoing) {
      attrs = new int[]{
        R.attr.conversation_item_outgoing_text_primary_color,
        R.attr.conversation_item_outgoing_text_secondary_color,
      };
    } else {
      attrs = new int[]{
        R.attr.conversation_item_incoming_text_primary_color,
        R.attr.conversation_item_incoming_text_secondary_color,
      };
    }
    try (TypedArray ta = getContext().obtainStyledAttributes(attrs)) {
      icon.setColorFilter(ta.getColor(0, Color.BLACK));
      footer.setTextColor(ta.getColor(1, Color.BLACK));
    }
  }

  public ConversationItemFooter getFooter() {
    return footer;
  }

  public String getDescription() {
    return title.getText() + "\n" + footer.getDescription();
  }

  public interface CallClickListener {
    void onClick(View v, CallInfo callInfo);
  }
}
