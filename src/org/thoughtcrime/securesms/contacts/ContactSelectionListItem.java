package org.thoughtcrime.securesms.contacts;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.b44t.messenger.DcContact;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.AvatarImageView;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientModifiedListener;
import org.thoughtcrime.securesms.util.Hash;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

public class ContactSelectionListItem extends LinearLayout implements RecipientModifiedListener {

  @SuppressWarnings("unused")
  private static final String TAG = ContactSelectionListItem.class.getSimpleName();

  private AvatarImageView contactPhotoImage;
  private View            numberContainer;
  private TextView        numberView;
  private TextView        nameView;
  private TextView        labelView;
  private CheckBox        checkBox;

  private int           specialId;
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
    this.contactPhotoImage = findViewById(R.id.contact_photo_image);
    this.numberContainer   = findViewById(R.id.number_container);
    this.numberView        = findViewById(R.id.number);
    this.labelView         = findViewById(R.id.label);
    this.nameView          = findViewById(R.id.name);
    this.checkBox          = findViewById(R.id.check_box);

    ViewUtil.setTextViewGravityStart(this.nameView, getContext());
  }

  public void set(@NonNull GlideRequests glideRequests, int specialId, DcContact contact, String name, String number, String label, int color, boolean multiSelect, boolean enabled) {
    this.glideRequests = glideRequests;
    this.specialId     = specialId;
    this.number        = number;

    if(specialId==DcContact.DC_CONTACT_ID_NEW_CONTACT || specialId==DcContact.DC_CONTACT_ID_NEW_GROUP || specialId==DcContact.DC_CONTACT_ID_NEW_VERIFIED_GROUP) {
      this.recipient = null;
      this.contactPhotoImage.setAvatar(glideRequests, Recipient.from(getContext(), Address.UNKNOWN, true), false);
    }
    else {
      this.recipient = DcHelper.getContext(getContext()).getRecipient(contact);
      String identifier = Hash.sha256(contact.getName() + contact.getAddr());
      Uri systemContactPhoto = TextSecurePreferences.getSystemContactPhoto(getContext(), identifier);
      if (systemContactPhoto != null) {
        this.recipient.setSystemContactPhoto(systemContactPhoto);
      }
      this.recipient.addListener(this);
      if (this.recipient.getName() != null) {
        name = this.recipient.getName();
      }
    }
    this.nameView.setTextColor(color);
    this.numberView.setTextColor(color);
    this.contactPhotoImage.setAvatar(glideRequests, recipient, false);

    setText(name, number, label, contact);

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

    contactPhotoImage.clear(glideRequests);
  }

  private void setText(String name, String number, String label, DcContact contact) {
    this.nameView.setEnabled(true);
    this.nameView.setText(name==null? "#" : name);

    if(number!=null) {
      this.numberView.setText(number == null ? "" : number);
      this.labelView.setText(label==null? "" : label);
      this.numberContainer.setVisibility(View.VISIBLE);
    }
    else {
      this.numberContainer.setVisibility(View.GONE);
    }
    if (contact != null && contact.isVerified()) {
      nameView.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.ic_verified,0);
    } else {
      nameView.setCompoundDrawablesWithIntrinsicBounds(0,0, 0,0);
    }
  }

  public int getSpecialId() {
    return specialId;
  }

  public String getNumber() {
    return number;
  }

  @Override
  public void onModified(final Recipient recipient) {
    if (this.recipient == recipient) {
      Util.runOnMain(() -> {
        contactPhotoImage.setAvatar(glideRequests, recipient, false);
        nameView.setText(recipient.toShortString());
      });
    }
  }
}
