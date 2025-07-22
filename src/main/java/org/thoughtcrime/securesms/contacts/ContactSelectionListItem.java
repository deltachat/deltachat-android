package org.thoughtcrime.securesms.contacts;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.b44t.messenger.DcContact;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.AvatarView;
import org.thoughtcrime.securesms.contacts.avatars.ResourceContactPhoto;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientModifiedListener;
import org.thoughtcrime.securesms.util.ThemeUtil;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

public class ContactSelectionListItem extends LinearLayout implements RecipientModifiedListener {

  private AvatarView      avatar;
  private View            numberContainer;
  private TextView        numberView;
  private TextView        nameView;
  private TextView        labelView;
  private CheckBox        checkBox;

  private int           specialId;
  private String        name;
  private String        number;
  private Recipient     recipient;
  private GlideRequests glideRequests;

  public ContactSelectionListItem(Context context) {
    super(context);
  }

  public ContactSelectionListItem(Context context, AttributeSet attrs) {
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
    this.checkBox          = findViewById(R.id.check_box);

    ViewUtil.setTextViewGravityStart(this.nameView, getContext());
  }

  public void set(@NonNull GlideRequests glideRequests, int specialId, DcContact contact, String name, String number, String label, boolean multiSelect, boolean enabled) {
    this.glideRequests = glideRequests;
    this.specialId     = specialId;
    this.name          = name;
    this.number        = number;

    if (specialId==DcContact.DC_CONTACT_ID_NEW_CLASSIC_CONTACT
     || specialId==DcContact.DC_CONTACT_ID_NEW_GROUP
     || specialId==DcContact.DC_CONTACT_ID_NEW_UNENCRYPTED_GROUP
     || specialId==DcContact.DC_CONTACT_ID_NEW_BROADCAST
     || specialId==DcContact.DC_CONTACT_ID_ADD_MEMBER
     || specialId==DcContact.DC_CONTACT_ID_QR_INVITE) {
      this.nameView.setTypeface(null, Typeface.BOLD);
    }
    else {
      this.recipient = new Recipient(getContext(), contact);
      this.recipient.addListener(this);
      if (this.recipient.getName() != null) {
        name = this.recipient.getName();
      }
      this.nameView.setTypeface(null, Typeface.NORMAL);
    }
    if (specialId == DcContact.DC_CONTACT_ID_QR_INVITE) {
      this.avatar.setImageDrawable(new ResourceContactPhoto(R.drawable.ic_qr_code_24).asDrawable(getContext(), ThemeUtil.getDummyContactColor(getContext())));
    } else {
      this.avatar.setAvatar(glideRequests, recipient, false);
    }
    this.avatar.setSeenRecently(contact != null && contact.wasSeenRecently());

    setText(name, number, label, contact);
    setEnabled(enabled);

    if (multiSelect) this.checkBox.setVisibility(View.VISIBLE);
    else             this.checkBox.setVisibility(View.GONE);
  }

  public void setChecked(boolean selected) {
    this.checkBox.setChecked(selected);
  }

  public void unbind(GlideRequests glideRequests) {
    if (recipient != null) {
      recipient.removeListener(this);
      recipient = null;
    }

    avatar.clear(glideRequests);
  }

  private void setText(String name, String number, String label, DcContact contact) {
    this.nameView.setEnabled(true);
    this.nameView.setText(name==null? "#" : name);

    if (contact != null && contact.isKeyContact()) {
      number = null;
    }

    if(number!=null) {
      this.numberView.setText(number);
      this.labelView.setText(label==null? "" : label);
      this.numberContainer.setVisibility(View.VISIBLE);
    }
    else {
      this.numberContainer.setVisibility(View.GONE);
    }
  }

  public int getSpecialId() {
    return specialId;
  }

  public String getName() {
    return name;
  }

  public String getNumber() {
    return number;
  }

  public DcContact getDcContact() {
    return recipient.getDcContact();
  }

  public int getContactId() {
    if (recipient.getAddress().isDcContact()) {
      return recipient.getAddress().getDcContactId();
    } else {
      return -1;
    }
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
