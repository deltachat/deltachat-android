package org.thoughtcrime.securesms;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.view.ViewCompat;
import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcMsg;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.json.JSONObject;
import org.thoughtcrime.securesms.components.DeliveryStatusView;
import org.thoughtcrime.securesms.components.MessageState;
import org.thoughtcrime.securesms.components.audioplay.AudioPlaybackViewModel;
import org.thoughtcrime.securesms.components.audioplay.AudioView;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.AccessibilityUtil;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.JsonUtils;

public class ConversationUpdateItem extends BaseConversationItem {
  private DeliveryStatusView deliveryStatusView;
  private AppCompatImageView appIcon;
  private int textColor;
  private final List<Integer> msgActionIds = new ArrayList<>();

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

    bodyText = findViewById(R.id.conversation_update_body);
    deliveryStatusView = new DeliveryStatusView(findViewById(R.id.delivery_indicator));
    appIcon = findViewById(R.id.app_icon);

    bodyText.setOnLongClickListener(passthroughClickListener);
    bodyText.setOnClickListener(passthroughClickListener);

    // info messages do not contain links but domains (eg. invalid_unencrypted_tap_to_learn_more),
    // however, they should not be linkified to not disturb eg. "Tap to learn more".
    bodyText.setAutoLinkMask(0);
  }

  @Override
  public void bind(
      @NonNull DcMsg messageRecord,
      @NonNull DcChat dcChat,
      @NonNull GlideRequests glideRequests,
      @NonNull Set<DcMsg> batchSelected,
      @NonNull Recipient conversationRecipient,
      boolean pulseUpdate,
      @Nullable AudioPlaybackViewModel playbackViewModel,
      AudioView.OnActionListener audioPlayPauseListener) {
    bindPartial(messageRecord, dcChat, batchSelected, pulseUpdate, conversationRecipient);
    setGenericInfoRecord(messageRecord);
    setAccessibility(messageRecord);
  }

  private void initializeAttributes() {
    final int[] attributes =
        new int[] {
          R.attr.conversation_item_update_text_color,
        };
    final TypedArray attrs = context.obtainStyledAttributes(attributes);

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
    int infoType = messageRecord.getInfoType();

    if (infoType == DcMsg.DC_INFO_WEBXDC_INFO_MESSAGE) {
      DcMsg parentMsg = messageRecord.getParent();
      Drawable drawable = null;
      if (parentMsg != null) {
        JSONObject info = parentMsg.getWebxdcInfo();
        byte[] blob = parentMsg.getWebxdcBlob(JsonUtils.optString(info, "icon"));
        if (blob != null) {
          drawable = Drawable.createFromStream(new ByteArrayInputStream(blob), "icon");
        }
      }
      appIcon.setImageDrawable(drawable);
      appIcon.setVisibility(drawable != null ? VISIBLE : GONE);
    } else {
      appIcon.setImageDrawable(null);
      appIcon.setVisibility(GONE);
    }

    bodyText.setText(messageRecord.getDisplayBody());
    bodyText.setVisibility(VISIBLE);

    deliveryStatusView.setState(MessageState.ofInfo(messageRecord));

    if (messageRecord.isFailed()) deliveryStatusView.setFailed();
    else if (!messageRecord.isOutgoing()) deliveryStatusView.setNone();
    else if (messageRecord.isPreparing()) deliveryStatusView.setPreparing();
    else if (messageRecord.isPending()) deliveryStatusView.setPending();
    else deliveryStatusView.setNone();
  }

  @Override
  public void unbind() {}

  private void setAccessibility(@NonNull DcMsg messageRecord) {
    ViewCompat.setScreenReaderFocusable(this, true);
    ViewCompat.setStateDescription(this, MessageState.ofInfo(messageRecord).getA11yLabel(context));

    StringBuilder desc = new StringBuilder();
    AccessibilityUtil.append(desc, messageRecord.getDisplayBody());
    AccessibilityUtil.append(
        desc, DateUtils.getExtendedRelativeTimeSpanString(context, messageRecord.getTimestamp()));
    setContentDescription(AccessibilityUtil.toDescription(desc));

    for (int id : msgActionIds) {
      ViewCompat.removeAccessibilityAction(this, id);
    }
    msgActionIds.clear();
    if (!batchSelected.isEmpty()) return;

    int infoType = messageRecord.getInfoType();
    Integer label = null;
    if (messageRecord.isFailed()) {
      label = R.string.a11y_action_show_error;
    } else if (infoType == DcMsg.DC_INFO_CHAT_E2EE
        || infoType == DcMsg.DC_INFO_PROTECTION_ENABLED
        || infoType == DcMsg.DC_INFO_INVALID_UNENCRYPTED_MAIL) {
      label = R.string.learn_more;
    }
    if (label != null) {
      msgActionIds.add(
          ViewCompat.addAccessibilityAction(
              this,
              context.getString(label),
              (v, args) -> {
                performClick();
                return true;
              }));
    }
  }
}
