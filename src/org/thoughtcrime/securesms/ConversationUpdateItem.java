package org.thoughtcrime.securesms;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.TextView;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.components.DeliveryStatusView;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;

import java.util.Locale;
import java.util.Set;

public class ConversationUpdateItem extends BaseConversationItem
{
  private DeliveryStatusView  deliveryStatusView;
  private int                 textColor;

  public ConversationUpdateItem(Context context) {
    this(context, null);
  }

  public ConversationUpdateItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void onFinishInflate() {
    super.onFinishInflate();

    initializeAttributes();

    bodyText           = findViewById(R.id.conversation_update_body);
    deliveryStatusView = new DeliveryStatusView(findViewById(R.id.delivery_indicator));

    bodyText.setOnLongClickListener(passthroughClickListener);
    bodyText.setOnClickListener(passthroughClickListener);

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
    bind(messageRecord, dcChat, batchSelected, pulseUpdate);
    setGenericInfoRecord(messageRecord);
  }

  private void initializeAttributes() {
    final int[]      attributes = new int[] {
        R.attr.conversation_item_update_text_color,
    };
    final TypedArray attrs      = context.obtainStyledAttributes(attributes);

    textColor = attrs.getColor(0, Color.WHITE);
    attrs.recycle();
  }

  @Override
  public void setEventListener(@Nullable EventListener listener) {
    // No events to report yet
  }

  @Override
  public DcMsg getMessageRecord() {
    return messageRecord;
  }

  private void setGenericInfoRecord(DcMsg messageRecord) {
    bodyText.setText(messageRecord.getDisplayBody());
    bodyText.setVisibility(VISIBLE);

    if      (messageRecord.isFailed())     deliveryStatusView.setFailed();
    else if (!messageRecord.isOutgoing())  deliveryStatusView.setNone();
    else if (messageRecord.isPreparing())  deliveryStatusView.setPreparing();
    else if (messageRecord.isPending())    deliveryStatusView.setPending();
    else                                   deliveryStatusView.setNone();

    if (messageRecord.isFailed()) {
      deliveryStatusView.setTint(Color.RED);
    } else {
      deliveryStatusView.setTint(textColor);
    }
  }

  @Override
  public void unbind() {
  }
}
