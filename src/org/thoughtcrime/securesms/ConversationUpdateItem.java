package org.thoughtcrime.securesms;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientModifiedListener;
import org.thoughtcrime.securesms.util.ExpirationUtil;
import org.thoughtcrime.securesms.util.GroupUtil;
import org.thoughtcrime.securesms.util.Util;

import java.util.Locale;
import java.util.Set;

public class ConversationUpdateItem extends LinearLayout
    implements BindableConversationItem
{
  private Set<DcMsg>    batchSelected;

  private TextView      body;
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

    this.body  = findViewById(R.id.conversation_update_body);
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

    bind(messageRecord, locale);
  }

  @Override
  public void setEventListener(@Nullable EventListener listener) {
    // No events to report yet
  }

  @Override
  public DcMsg getMessageRecord() {
    return messageRecord;
  }

  private void bind(@NonNull DcMsg messageRecord, @NonNull Locale locale) {
    this.messageRecord = messageRecord;
    setGenericInfoRecord(messageRecord);
    setSelected(batchSelected.contains(messageRecord));
  }

  private void setGenericInfoRecord(DcMsg messageRecord) {
    body.setText(messageRecord.getDisplayBody());
    body.setVisibility(VISIBLE);
  }

  @Override
  public void unbind() {
  }
}
