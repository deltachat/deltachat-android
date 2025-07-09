package org.thoughtcrime.securesms;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;

import org.thoughtcrime.securesms.components.AvatarView;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientModifiedListener;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

public class ProfileAvatarItem extends LinearLayout implements RecipientModifiedListener {

  private AvatarView      avatarView;
  private TextView        nameView;
  private TextView        subtitleView;

  private Recipient       recipient;
  private GlideRequests   glideRequests;

  public ProfileAvatarItem(Context context) {
    super(context);
  }

  public ProfileAvatarItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    avatarView        = findViewById(R.id.avatar);
    nameView          = findViewById(R.id.name);
    subtitleView      = findViewById(R.id.subtitle);

    ViewUtil.setTextViewGravityStart(nameView, getContext());
  }

  public void set(@NonNull GlideRequests glideRequests, @Nullable DcChat dcChat, @Nullable DcContact dcContact, int memberCount) {
    this.glideRequests = glideRequests;

    String name = "";
    boolean greenCheckmark = false;
    String subtitle = null;
    if (dcChat != null) {
      recipient = new Recipient(getContext(), dcChat);
      name = dcChat.getName();
      greenCheckmark = dcChat.isProtected();

      if (dcChat.isMailingList()) {
        subtitle = dcChat.getMailinglistAddr();
      } else if (dcChat.isBroadcast()) {
        subtitle = getContext().getResources().getQuantityString(R.plurals.n_recipients, memberCount, memberCount);
      } else if (dcChat.getType() == DcChat.DC_CHAT_TYPE_GROUP) {
        subtitle = getContext().getResources().getQuantityString(R.plurals.n_members, memberCount, memberCount);
      }
    } else if (dcContact != null) {
      recipient = new Recipient(getContext(), dcContact);
      name = dcContact.getDisplayName();
      greenCheckmark = dcContact.isVerified();
    }

    recipient.addListener(this);
    avatarView.setAvatar(glideRequests, recipient, false);
    avatarView.setSeenRecently(dcContact != null && dcContact.wasSeenRecently());

    nameView.setText(name);
    nameView.setCompoundDrawablesWithIntrinsicBounds(0,0, greenCheckmark ? R.drawable.ic_verified : 0, 0);

    if (subtitle != null) {
      subtitleView.setVisibility(View.VISIBLE);
      subtitleView.setText(subtitle);
    } else {
      subtitleView.setVisibility(View.GONE);
    }
  }

  public void setAvatarClickListener(OnClickListener listener) {
    avatarView.setAvatarClickListener(listener);
  }

  public void unbind(GlideRequests glideRequests) {
    if (recipient != null) {
      recipient.removeListener(this);
      recipient = null;
    }

    avatarView.clear(glideRequests);
  }

  @Override
  public void onModified(final Recipient recipient) {
    if (this.recipient == recipient) {
      Util.runOnMain(() -> {
        avatarView.setAvatar(glideRequests, recipient, false);
        DcContact contact = recipient.getDcContact();
        avatarView.setSeenRecently(contact != null && contact.wasSeenRecently());
        nameView.setText(recipient.toShortString());
      });
    }
  }
}
