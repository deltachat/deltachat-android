package org.thoughtcrime.securesms;

import android.content.Context;
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
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.Util;

import java.util.HashSet;
import java.util.Set;

import chat.delta.rpc.Rpc;

public abstract class BaseConversationItem extends LinearLayout
    implements BindableConversationItem
{
  static final long PULSE_HIGHLIGHT_MILLIS = 500;

  protected DcMsg         messageRecord;
  protected DcChat        dcChat;
  protected TextView      bodyText;

  protected final Context              context;
  protected final DcContext            dcContext;
  protected final Rpc rpc;
  protected Recipient                  conversationRecipient;

  protected @NonNull  Set<DcMsg> batchSelected = new HashSet<>();

  protected final PassthroughClickListener passthroughClickListener = new PassthroughClickListener();

  public BaseConversationItem(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.context = context;
    this.dcContext = DcHelper.getContext(context);
    this.rpc = DcHelper.getRpc(context);
  }

  protected void bind(@NonNull DcMsg            messageRecord,
                      @NonNull DcChat           dcChat,
                      @NonNull Set<DcMsg>       batchSelected,
                      boolean                   pulseHighlight,
                      @NonNull Recipient        conversationRecipient)
  {
    this.messageRecord  = messageRecord;
    this.dcChat         = dcChat;
    this.batchSelected  = batchSelected;
    this.conversationRecipient  = conversationRecipient;
    setInteractionState(messageRecord, pulseHighlight);
  }

  protected void setInteractionState(DcMsg messageRecord, boolean pulseHighlight) {
    if (batchSelected.contains(messageRecord)) {
      setBackgroundResource(R.drawable.conversation_item_background);
      setSelected(true);
    } else if (pulseHighlight) {
      setBackgroundResource(R.drawable.conversation_item_background_animated);
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
    return batchSelected.isEmpty()
            && (messageRecord.isFailed()
                || messageRecord.getInfoType() == DcMsg.DC_INFO_CHAT_E2EE
                || messageRecord.getInfoType() == DcMsg.DC_INFO_PROTECTION_ENABLED
                || messageRecord.getInfoType() == DcMsg.DC_INFO_INVALID_UNENCRYPTED_MAIL);
  }

  protected void onAccessibilityClick() {}

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
    private final OnClickListener parent;

    ClickListener(@Nullable OnClickListener parent) {
      this.parent = parent;
    }

    public void onClick(View v) {
      if (!shouldInterceptClicks(messageRecord) && parent != null) {
        if (batchSelected.isEmpty() && Util.isTouchExplorationEnabled(context)) {
          BaseConversationItem.this.onAccessibilityClick();
        }
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
      } else if (messageRecord.getInfoType() == DcMsg.DC_INFO_CHAT_E2EE || messageRecord.getInfoType() == DcMsg.DC_INFO_PROTECTION_ENABLED) {
        DcHelper.showProtectionEnabledDialog(context);
      } else if (messageRecord.getInfoType() == DcMsg.DC_INFO_INVALID_UNENCRYPTED_MAIL) {
        DcHelper.showInvalidUnencryptedDialog(context);
      }
    }
  }
}
