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
import chat.delta.rpc.types.CallInfo;
import chat.delta.rpc.types.CallState;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.DateUtils;

public class CallItemView extends FrameLayout {
  private static final String TAG = CallItemView.class.getSimpleName();

  private final @NonNull ImageView icon;
  private final @NonNull TextView title;
  private final @NonNull TextView duration;
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
    this.duration = findViewById(R.id.duration);

    setOnClickListener(
        v -> {
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
      duration.setText(
          DateUtils.getFormattedCallDuration(
              getContext(), ((CallState.Completed) callInfo.state).duration));
      duration.setVisibility(VISIBLE);
    } else {
      duration.setVisibility(GONE);
    }

    if (callInfo.state instanceof CallState.Missed) {
      title.setText(R.string.missed_call);
    } else if (callInfo.state instanceof CallState.Canceled) {
      title.setText(R.string.canceled_call);
    } else if (callInfo.state instanceof CallState.Declined) {
      title.setText(R.string.declined_call);
    } else if (callInfo.hasVideo) {
      title.setText(R.string.video_call);
    } else {
      title.setText(R.string.audio_call);
    }

    icon.setImageResource(
        callInfo.hasVideo ? R.drawable.ic_videocam_white_24dp : R.drawable.baseline_call_24);

    int[] attrs;
    if (isOutgoing) {
      attrs =
          new int[] {
            R.attr.conversation_item_outgoing_text_primary_color,
          };
    } else {
      attrs =
          new int[] {
            R.attr.conversation_item_incoming_text_primary_color,
          };
    }
    try (TypedArray ta = getContext().obtainStyledAttributes(attrs)) {
      icon.setColorFilter(ta.getColor(0, Color.BLACK));
    }
  }

  public String getDescription() {
    return title.getText()
        + (duration.getVisibility() == VISIBLE ? ("\n" + duration.getText()) : "");
  }

  public interface CallClickListener {
    void onClick(View v, CallInfo callInfo);
  }
}
