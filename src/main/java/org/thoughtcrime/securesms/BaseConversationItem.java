package org.thoughtcrime.securesms;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;
import com.b44t.messenger.rpc.Rpc;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.recipients.Recipient;

import java.util.HashSet;
import java.util.Set;

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
                || messageRecord.getInfoType() == DcMsg.DC_INFO_PROTECTION_DISABLED
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
        } else if (Util.equals(messageRecord.getText(), "about:config") && dcChat.isSelfTalk()) {
          showAboutConfig();
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
      } else if (messageRecord.getInfoType() == DcMsg.DC_INFO_PROTECTION_DISABLED) {
        DcHelper.showVerificationBrokenDialog(context, conversationRecipient.getName());
      } else if (messageRecord.getInfoType() == DcMsg.DC_INFO_PROTECTION_ENABLED) {
        DcHelper.showProtectionEnabledDialog(context);
      } else if (messageRecord.getInfoType() == DcMsg.DC_INFO_INVALID_UNENCRYPTED_MAIL) {
        DcHelper.showInvalidUnencryptedDialog(context);
      }
    }
  }

  private void showAboutConfig() {
    final EditText input = new EditText(context);
    input.setInputType(InputType.TYPE_CLASS_TEXT);

    new AlertDialog.Builder(context)
            .setView(input)
            .setTitle("Which config do you want to see?")
            .setPositiveButton(R.string.ok, (d, i) -> {
              String config = input.getText().toString();
              if (Util.equals(config, DcHelper.CONFIG_MAIL_PASSWORD)
                      || Util.equals(config, DcHelper.CONFIG_SEND_PASSWORD)
                      || Util.equals(config, "configured_mail_pw")
                      || Util.equals(config, "configured_send_pw")
                      || Util.equals(config, "proxy_url")
                      || Util.equals(config, "socks5_password"))
              {
                Toast.makeText(context, "Not showing possibly sensitive config", Toast.LENGTH_SHORT).show();
              } else {
                aboutConfig_ViewConfig(config);
              }
            })
            .setNeutralButton(R.string.cancel, (d, i) -> {})
            .setCancelable(true)
            .show();
    input.requestFocus();
  }

  private void aboutConfig_ViewConfig(String config) {
    new AlertDialog.Builder(context)
            .setTitle(config)
            .setMessage("The current value is: '" + dcContext.getConfig(config) + "'.\n\nWarning: Only change it if you know what you are doing!")
            .setPositiveButton("Change", (d, i) -> aboutConfig_ChangeConfig(config))
            .setNeutralButton(R.string.cancel, (d, i) -> {})
            .setCancelable(true)
            .show();
  }

  private void aboutConfig_ChangeConfig(String config) {
    final EditText input = new EditText(context);
    input.setInputType(InputType.TYPE_CLASS_TEXT);

    new AlertDialog.Builder(context)
            .setTitle("Change " + config + " (current: '" + dcContext.getConfig(config) + "')")
            .setView(input)
            .setPositiveButton("Confirm", (d, i) -> {
              String oldConfig = dcContext.getConfig(config);
              dcContext.setConfig(config, input.getText().toString());

              DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
              msg.setText("You manually changed config " + config + " from '" + oldConfig + "' to '" + dcContext.getConfig(config) + "'.");
              dcContext.addDeviceMsg(null, msg);
            })
            .setNeutralButton(R.string.cancel, (d, i) -> {})
            .setCancelable(true)
            .show();
    input.requestFocus();
  }
}
