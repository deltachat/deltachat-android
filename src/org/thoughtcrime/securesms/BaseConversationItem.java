package org.thoughtcrime.securesms;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.HashSet;
import java.util.Set;

public abstract class BaseConversationItem extends LinearLayout
    implements BindableConversationItem
{
  static long PULSE_HIGHLIGHT_MILLIS = 500;

  protected DcMsg         messageRecord;
  protected DcChat        dcChat;
  protected TextView      bodyText;

  protected final Context              context;
  protected final DcContext            dcContext;

  protected @NonNull  Set<DcMsg> batchSelected = new HashSet<>();

  protected final PassthroughClickListener passthroughClickListener = new PassthroughClickListener();

  public BaseConversationItem(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.context = context;
    this.dcContext = DcHelper.getContext(context);
  }

  protected void bind(@NonNull DcMsg            messageRecord,
                      @NonNull DcChat           dcChat,
                      @NonNull Set<DcMsg>       batchSelected,
                      boolean                   pulseHighlight)
  {
    this.messageRecord  = messageRecord;
    this.dcChat         = dcChat;
    this.batchSelected  = batchSelected;
    setInteractionState(messageRecord, pulseHighlight);
  }

  protected void setInteractionState(DcMsg messageRecord, boolean pulseHighlight) {
    final int[] attributes = new int[] {
        R.attr.conversation_item_background,
        R.attr.conversation_item_background_animated,
    };

    if (batchSelected.contains(messageRecord)) {
     final TypedArray attrs = context.obtainStyledAttributes(attributes);
      ViewUtil.setBackground(this, attrs.getDrawable(0));
      attrs.recycle();
      setSelected(true);
    } else if (pulseHighlight) {
     final TypedArray attrs = context.obtainStyledAttributes(attributes);
      ViewUtil.setBackground(this, attrs.getDrawable(1));
      attrs.recycle();
      setSelected(true);
      postDelayed(() -> setSelected(false), PULSE_HIGHLIGHT_MILLIS);
    } else {
      setSelected(false);
    }
  }

  @Override
  public void setOnClickListener(OnClickListener l) {
    super.setOnClickListener(new ClickListener(l));
  }

  protected boolean shouldInterceptClicks(DcMsg messageRecord) {
    return batchSelected.isEmpty() && (messageRecord.isFailed());
  }

  protected class PassthroughClickListener implements View.OnLongClickListener, View.OnClickListener {

    @Override
    public boolean onLongClick(View v) {
      if (bodyText.hasSelection()) {
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

  protected class ClickListener implements View.OnClickListener {
    private OnClickListener parent;

    ClickListener(@Nullable OnClickListener parent) {
      this.parent = parent;
    }

    public void onClick(View v) {
      if (!shouldInterceptClicks(messageRecord) && parent != null) {
        parent.onClick(v);
      } else if (messageRecord.isFailed()) {
        View view = View.inflate(context, R.layout.message_details_view, null);
        TextView detailsText = view.findViewById(R.id.details_text);
        detailsText.setText(messageRecord.getError());

        AlertDialog d = new AlertDialog.Builder(context)
                .setView(view)
                .setTitle(R.string.error)
                .setPositiveButton(R.string.ok, null)
                .create();
        d.show();
      }
    }
  }
}
