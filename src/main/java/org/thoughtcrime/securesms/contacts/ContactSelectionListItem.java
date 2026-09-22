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
import com.b44t.messenger.DcContext;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.AvatarView;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.avatars.ResourceContactPhoto;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientModifiedListener;
import org.thoughtcrime.securesms.search.QrInviteData;
import org.thoughtcrime.securesms.util.ThemeUtil;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

public class ContactSelectionListItem extends LinearLayout implements RecipientModifiedListener {

  private AvatarView avatar;
  private View subtitleContainer;
  private TextView subtitleView;
  private TextView nameView;
  private CheckBox checkBox;

  private int specialId;
  private Recipient recipient;
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
    this.avatar = findViewById(R.id.avatar);
    this.subtitleContainer = findViewById(R.id.subtitle_container);
    this.subtitleView = findViewById(R.id.subtitle);
    this.nameView = findViewById(R.id.name);
    this.checkBox = findViewById(R.id.check_box);

    ViewUtil.setTextViewGravityStart(this.nameView, getContext());
  }

  public void set(
      @NonNull GlideRequests glideRequests,
      int specialId,
      DcContact contact,
      String name,
      String subtitle,
      boolean multiSelect,
      boolean enabled) {
    this.glideRequests = glideRequests;
    this.specialId = specialId;

    if (specialId == DcContact.DC_CONTACT_ID_NEW_CLASSIC_CONTACT
        || specialId == DcContact.DC_CONTACT_ID_NEW_GROUP
        || specialId == DcContact.DC_CONTACT_ID_NEW_UNENCRYPTED_GROUP
        || specialId == DcContact.DC_CONTACT_ID_NEW_BROADCAST
        || specialId == DcContact.DC_CONTACT_ID_ADD_MEMBER
        || specialId == DcContact.DC_CONTACT_ID_QR_INVITE) {
      this.nameView.setTypeface(null, Typeface.BOLD);
    } else {
      this.recipient = new Recipient(getContext(), contact);
      this.recipient.addListener(this);
      if (this.recipient.getName() != null) {
        name = this.recipient.getName();
      }
      this.nameView.setTypeface(null, Typeface.NORMAL);
    }
    if (specialId == DcContact.DC_CONTACT_ID_QR_INVITE) {
      this.avatar.setImageDrawable(
          new ResourceContactPhoto(R.drawable.ic_qr_code_24)
              .asDrawable(getContext(), ThemeUtil.getDummyContactColor(getContext())));
    } else {
      this.avatar.setAvatar(glideRequests, recipient, false);
    }
    this.avatar.setSeenRecently(contact != null && contact.wasSeenRecently());

    setText(name, subtitle);
    setEnabled(enabled);

    if (multiSelect) this.checkBox.setVisibility(View.VISIBLE);
    else this.checkBox.setVisibility(View.GONE);
  }

  public void setQrInviteData(
      @NonNull QrInviteData inviteData, int specialId, @NonNull GlideRequests glideRequests) {
    this.glideRequests = glideRequests;
    this.specialId = specialId;

    if (inviteData.getContactId() > 0) {
      DcContext dcContext = DcHelper.getContext(getContext());
      DcContact dcContact = dcContext.getContact(inviteData.getContactId());
      this.recipient = new Recipient(getContext(), dcContact);
    } else {
      this.recipient = null;
    }
    this.avatar.setAvatar(glideRequests, recipient, false);
    this.avatar.setSeenRecently(false);

    this.nameView.setTypeface(null, Typeface.NORMAL);
    setText(inviteData.getDisplayTitle(), inviteData.getDisplaySubtitle());
    this.checkBox.setVisibility(View.GONE);
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

  private void setText(String name, String subtitle) {
    this.nameView.setEnabled(true);
    this.nameView.setText(name == null ? "#" : name);

    if (subtitle != null) {
      this.subtitleView.setText(subtitle);
      this.subtitleContainer.setVisibility(View.VISIBLE);
    } else {
      this.subtitleContainer.setVisibility(View.GONE);
    }
  }

  public int getSpecialId() {
    return specialId;
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
      Util.runOnMain(
          () -> {
            avatar.setAvatar(glideRequests, recipient, false);
            DcContact contact = recipient.getDcContact();
            avatar.setSeenRecently(contact != null && contact.wasSeenRecently());
            nameView.setText(recipient.toShortString());
          });
    }
  }
}
