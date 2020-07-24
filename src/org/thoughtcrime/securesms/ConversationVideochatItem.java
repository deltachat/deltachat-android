package org.thoughtcrime.securesms;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;

import java.util.Locale;
import java.util.Set;

public class ConversationVideochatItem extends LinearLayout
    implements BindableConversationItem
{
  private TextView      body;
  private DcMsg         dcMsg;

  public ConversationVideochatItem(Context context) {
    super(context);
  }

  public ConversationVideochatItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void onFinishInflate() {
    super.onFinishInflate();
    this.body  = findViewById(R.id.conversation_update_body);
  }

  @Override
  public void bind(@NonNull DcMsg                   dcMsg,
                   @NonNull DcChat                  dcChat,
                   @NonNull GlideRequests           glideRequests,
                   @NonNull Locale                  locale,
                   @NonNull Set<DcMsg>              batchSelected,
                   @NonNull Recipient               conversationRecipient,
                            boolean                 pulseUpdate)
  {
    this.dcMsg = dcMsg;
    DcContext dcContext = DcHelper.getContext(getContext());
    DcContact dcContact = dcContext.getContact(dcMsg.getFromId());
    body.setText(dcMsg.isOutgoing()?
            getContext().getString(R.string.videochat_you_invited_hint) :
            getContext().getString(R.string.videochat_contact_invited_hint, dcContact.getFirstName()));
    setSelected(batchSelected.contains(dcMsg));
  }

  @Override
  public void setEventListener(@Nullable EventListener listener) {
    // No events to report yet
  }

  @Override
  public DcMsg getMessageRecord() {
    return dcMsg;
  }

  @Override
  public void unbind() {
  }
}
