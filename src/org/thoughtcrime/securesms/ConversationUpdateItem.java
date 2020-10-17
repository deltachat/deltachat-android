package org.thoughtcrime.securesms;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;

import java.util.Locale;
import java.util.Set;

public class ConversationUpdateItem extends LinearLayout
    implements BindableConversationItem
{
  private Set<DcMsg>    batchSelected;

  private TextView      body;
  private FrameLayout   bigIconContainer;
  private ImageView     bigIconImage;
  private DcMsg         messageRecord;

  public ConversationUpdateItem(Context context) {
    super(context);
  }

  public ConversationUpdateItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void onFinishInflate() {
    super.onFinishInflate();

    this.body             = findViewById(R.id.conversation_update_body);
    this.bigIconContainer = findViewById(R.id.big_icon_container);
    this.bigIconImage     = findViewById(R.id.big_icon);
  }

  @Override
  public void bind(@NonNull DcMsg                   messageRecord,
                   @NonNull DcChat                  dcChat,
                   @NonNull GlideRequests           glideRequests,
                   @NonNull Locale                  locale,
                   @NonNull Set<DcMsg>              batchSelected,
                   @NonNull Recipient               conversationRecipient,
                            boolean                 pulseUpdate)
  {
    this.batchSelected = batchSelected;

    bind(messageRecord);
  }

  @Override
  public void setEventListener(@Nullable EventListener listener) {
    // No events to report yet
  }

  @Override
  public DcMsg getMessageRecord() {
    return messageRecord;
  }

  private void bind(@NonNull DcMsg messageRecord) {
    this.messageRecord = messageRecord;
    setGenericInfoRecord(messageRecord);
    setSelected(batchSelected.contains(messageRecord));
  }

  private void setGenericInfoRecord(DcMsg messageRecord) {
    String text = messageRecord.getText();
    int    bigIcon = 0;

    if (messageRecord.getInfoType()==DcMsg.DC_INFO_PROTECTION_ENABLED) {
      text += "\n\n" + getContext().getString(R.string.systemmsg_chat_protection_enabled_explain);
      bigIcon = R.drawable.ic_protected_24;
    } else if (messageRecord.getInfoType()==DcMsg.DC_INFO_PROTECTION_DISABLED) {
      bigIcon = R.drawable.ic_unprotected_24;
    }

    body.setText(text);
    if (bigIcon!=0) {
      bigIconContainer.setVisibility(VISIBLE);
      bigIconImage.setImageDrawable(getContext().getResources().getDrawable(bigIcon));
    } else {
      bigIconContainer.setVisibility(GONE);
    }
  }

  @Override
  public void unbind() {
  }
}
