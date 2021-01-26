package org.thoughtcrime.securesms;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.components.AvatarImageView;
import org.thoughtcrime.securesms.components.ConversationItemFooter;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.Locale;
import java.util.Set;

public class ConversationVideochatItem extends LinearLayout
    implements BindableConversationItem
{
  private TextView               body;
  private ConversationItemFooter footer;
  private AvatarImageView        contactPhoto;
  private ViewGroup              contactPhotoHolder;
  private DcMsg                  dcMsg;

  public ConversationVideochatItem(Context context) {
    super(context);
  }

  public ConversationVideochatItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void onFinishInflate() {
    super.onFinishInflate();
    this.body               = findViewById(R.id.conversation_update_body);
    this.footer             = findViewById(R.id.conversation_item_footer);
    this.contactPhoto       = findViewById(R.id.contact_photo);
    this.contactPhotoHolder = findViewById(R.id.contact_photo_container);
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
    ApplicationDcContext dcContext = DcHelper.getContext(getContext());
    DcContact dcContact = dcContext.getContact(dcMsg.getFromId());

    CharSequence line1 = dcMsg.isOutgoing()? getContext().getString(R.string.videochat_you_invited_hint) :
            getContext().getString(R.string.videochat_contact_invited_hint, dcContact.getDisplayName());
    CharSequence line2 = Util.getBoldedString(dcMsg.isOutgoing()? getContext().getString(R.string.videochat_tap_to_open) :
            getContext().getString(R.string.videochat_tap_to_join));

    body.setText(TextUtils.concat(line1, "\n", line2));

    contactPhoto.setAvatar(glideRequests, dcContext.getRecipient(dcContact), true);

    setSelected(batchSelected.contains(dcMsg));
    setFooter(dcMsg, locale);
  }

  private void setFooter(@NonNull DcMsg dcMsg, @NonNull Locale locale) {
    ViewUtil.updateLayoutParams(footer, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

    footer.setVisibility(VISIBLE);
    footer.setMessageRecord(dcMsg, locale);
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
