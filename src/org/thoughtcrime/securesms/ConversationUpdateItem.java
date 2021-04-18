package org.thoughtcrime.securesms;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.components.DeliveryStatusView;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;

import java.util.Locale;
import java.util.Set;

public class ConversationUpdateItem extends LinearLayout
    implements BindableConversationItem
{
  private Set<DcMsg>    batchSelected;

  private DeliveryStatusView  deliveryStatusView;
  private TextView            body;
  private DcMsg               messageRecord;

  public ConversationUpdateItem(Context context) {
    super(context);
  }

  public ConversationUpdateItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void onFinishInflate() {
    super.onFinishInflate();

    body               = findViewById(R.id.conversation_update_body);
    deliveryStatusView = new DeliveryStatusView(findViewById(R.id.delivery_indicator));
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
    body.setText(messageRecord.getDisplayBody());
    body.setVisibility(VISIBLE);

    if      (!messageRecord.isOutgoing())  deliveryStatusView.setNone();
    else if (messageRecord.isFailed())     deliveryStatusView.setFailed();
    else if (messageRecord.isPreparing())  deliveryStatusView.setPreparing();
    else if (messageRecord.isPending())    deliveryStatusView.setPending();
    else                                   deliveryStatusView.setNone();
  }

  @Override
  public void unbind() {
  }
}
