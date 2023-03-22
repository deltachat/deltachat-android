package org.thoughtcrime.securesms;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.widget.TextView;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcMsg;

import org.json.JSONObject;
import org.thoughtcrime.securesms.components.DeliveryStatusView;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.JsonUtils;

import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.Set;

public class ConversationUpdateItem extends BaseConversationItem
{
  private DeliveryStatusView  deliveryStatusView;
  private AppCompatImageView  appIcon;
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
    appIcon            = findViewById(R.id.app_icon);


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
    if (messageRecord.getInfoType() == DcMsg.DC_INFO_WEBXDC_INFO_MESSAGE) {
      DcMsg parentMsg = messageRecord.getParent();
      JSONObject info = parentMsg.getWebxdcInfo();
      byte[] blob = parentMsg.getWebxdcBlob(JsonUtils.optString(info, "icon"));
      if (blob != null) {
        ByteArrayInputStream is = new ByteArrayInputStream(blob);
        Drawable drawable = Drawable.createFromStream(is, "icon");
        appIcon.setImageDrawable(drawable);
        appIcon.setVisibility(VISIBLE);
      } else {
        appIcon.setVisibility(GONE);
      }
    } else {
      appIcon.setVisibility(GONE);
    }

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
