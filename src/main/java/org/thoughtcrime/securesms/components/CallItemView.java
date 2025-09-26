package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
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
  }

  public void setCallItem(boolean isOutgoing, CallInfo callInfo) {
    if (callInfo.state instanceof CallState.Missed) {
      title.setText(R.string.missed_call);
    } else if (callInfo.state instanceof CallState.Cancelled) {
      title.setText(R.string.canceled_call);
    } else if (callInfo.state instanceof CallState.Declined) {
      title.setText(R.string.declined_call);
    } else {
      title.setText(isOutgoing? R.string.outgoing_call : R.string.incoming_call);
    }

    if (isOutgoing) {
      int[] attrs = new int[]{
        R.attr.conversation_item_outgoing_text_secondary_color,
      };
      try (TypedArray ta = getContext().obtainStyledAttributes(attrs)) {
        footer.setTextColor(ta.getColor(0, Color.BLACK));
        icon.setColorFilter(ta.getColor(0, Color.BLACK));
      }
    } else {
      int[] attrs = new int[]{
        R.attr.conversation_item_incoming_text_secondary_color,
      };
      try (TypedArray ta = getContext().obtainStyledAttributes(attrs)) {
        footer.setTextColor(ta.getColor(0, Color.BLACK));
      }
      icon.setColorFilter(getResources().getColor(R.color.delta_accent_darker));
    }
  }

  public ConversationItemFooter getFooter() {
    return footer;
  }

  public String getDescription() {
    return title.getText() + "\n" + footer.getDescription();
  }
}
