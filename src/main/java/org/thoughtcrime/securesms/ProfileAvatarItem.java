package org.thoughtcrime.securesms;

import android.content.Context;
import android.graphics.Typeface;
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

  private AvatarView      avatar;
  private View            numberContainer;
  private TextView        numberView;
  private TextView        nameView;
  private TextView        labelView;

  private Recipient     recipient;
  private GlideRequests glideRequests;

  public ProfileAvatarItem(Context context) {
    super(context);
  }

  public ProfileAvatarItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    this.avatar            = findViewById(R.id.avatar);
    this.numberContainer   = findViewById(R.id.number_container);
    this.numberView        = findViewById(R.id.number);
    this.labelView         = findViewById(R.id.label);
    this.nameView          = findViewById(R.id.name);

    ViewUtil.setTextViewGravityStart(this.nameView, getContext());
  }

  public void set(@NonNull GlideRequests glideRequests, @Nullable DcChat dcChat, @Nullable DcContact dcContact) {
    this.glideRequests = glideRequests;

    String name = "";
    boolean greenCheckmark = false;
    if (dcChat != null) {
      this.recipient = new Recipient(getContext(), dcChat);
      name = dcChat.getName();
      greenCheckmark = dcChat.isProtected();
    } else if (dcContact != null) {
      this.recipient = new Recipient(getContext(), dcContact);
      name = dcContact.getDisplayName();
      greenCheckmark = dcContact.isVerified();
    }

    this.recipient.addListener(this);
    this.avatar.setAvatar(glideRequests, recipient, false);
    this.avatar.setSeenRecently(dcContact != null && dcContact.wasSeenRecently());

    this.nameView.setText(name);
    this.nameView.setCompoundDrawablesWithIntrinsicBounds(0,0, greenCheckmark ? R.drawable.ic_verified : 0, 0);

    this.numberView.setText("number");
    this.labelView.setText("label");
  }

  public void unbind(GlideRequests glideRequests) {
    if (recipient != null) {
      recipient.removeListener(this);
      recipient = null;
    }

    avatar.clear(glideRequests);
  }

  @Override
  public void onModified(final Recipient recipient) {
    if (this.recipient == recipient) {
      Util.runOnMain(() -> {
        avatar.setAvatar(glideRequests, recipient, false);
        DcContact contact = recipient.getDcContact();
        avatar.setSeenRecently(contact != null && contact.wasSeenRecently());
        nameView.setText(recipient.toShortString());
      });
    }
  }
}
