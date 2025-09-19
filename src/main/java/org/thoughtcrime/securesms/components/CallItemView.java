package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.R;

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

  public void setCallItem(final @NonNull DcMsg dcMsg) {
    title.setText(dcMsg.getText());
    if (dcMsg.isOutgoing()) {
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
