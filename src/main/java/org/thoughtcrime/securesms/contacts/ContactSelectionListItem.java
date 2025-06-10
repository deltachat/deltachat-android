package org.thoughtcrime.securesms.contacts;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.rpc.Contact;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.AvatarView;
import org.thoughtcrime.securesms.contacts.avatars.ResourceContactPhoto;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.ThemeUtil;
import org.thoughtcrime.securesms.util.ViewUtil;

public class ContactSelectionListItem extends LinearLayout {

  private AvatarView      avatar;
  private View            numberContainer;
  private TextView        numberView;
  private TextView        nameView;
  private TextView        labelView;
  private CheckBox        checkBox;

  private int           contactId;
  private Contact       contact;
  private String        name;
  private String        number;

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

  public void set(@NonNull GlideRequests glideRequests, int contactId, Contact contact, String name, String number, String label, boolean multiSelect, boolean enabled) {
    this.contactId     = contactId;
    this.contact       = contact;
    this.name          = name;
    this.number        = number;

    if (contactId==DcContact.DC_CONTACT_ID_NEW_CLASSIC_CONTACT || contactId==DcContact.DC_CONTACT_ID_NEW_GROUP
     || contactId==DcContact.DC_CONTACT_ID_NEW_BROADCAST_CHANNEL
     || contactId==DcContact.DC_CONTACT_ID_ADD_MEMBER || contactId==DcContact.DC_CONTACT_ID_QR_INVITE) {
      this.nameView.setTypeface(null, Typeface.BOLD);
    }
    else {
      this.nameView.setTypeface(null, Typeface.NORMAL);
    }
    if (contactId == DcContact.DC_CONTACT_ID_QR_INVITE) {
      this.avatar.setImageDrawable(new ResourceContactPhoto(R.drawable.baseline_qr_code_24).asDrawable(getContext(), ThemeUtil.getDummyContactColor(getContext())));
    } else {
      this.avatar.setAvatar(glideRequests, contact!=null? new Recipient(getContext(), contact) : null, false);
    }
    this.avatar.setSeenRecently(contact != null && contact.wasSeenRecently);

    setText(name, number, label, contact);
    setEnabled(enabled);

    if (multiSelect) this.checkBox.setVisibility(View.VISIBLE);
    else             this.checkBox.setVisibility(View.GONE);
  }

  public void setChecked(boolean selected) {
    this.checkBox.setChecked(selected);
  }

  public void unbind(GlideRequests glideRequests) {
    avatar.clear(glideRequests);
  }

  private void setText(String name, String number, String label, Contact contact) {
    this.nameView.setEnabled(true);
    this.nameView.setText(name==null? "#" : name);

    if (contact != null && contact.isVerified) {
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
    if (contact != null && contact.isVerified) {
      nameView.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.ic_verified,0);
    } else if (contact != null && !contact.isPgpContact) {
      nameView.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.ic_outline_email,0);
      nameView.getCompoundDrawables()[2].setColorFilter(nameView.getCurrentTextColor(), PorterDuff.Mode.SRC_IN);
    } else {
      nameView.setCompoundDrawablesWithIntrinsicBounds(0,0, 0,0);
    }
  }

  public String getName() {
    return name;
  }

  public String getNumber() {
    return number;
  }

  public Contact getContact() {
    return contact;
  }

  public int getContactId() {
    return contactId;
  }

  public void setNoHeaderPadding() {
    int paddinglr = getContext().getResources().getDimensionPixelSize(R.dimen.contact_list_normal_padding);
    setPadding(paddinglr, 0, paddinglr, 0);
  }
}
