package org.thoughtcrime.securesms.contacts;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

public class ContactSelectionListItem extends LinearLayout {

  private AvatarView avatar;
  private View subtitleContainer;
  private TextView subtitleView;
  private TextView nameView;
  private CheckBox checkBox;

  private int specialId;
  private @Nullable Recipient recipient;
  private @Nullable RecipientModifiedListener recipientListener;

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

  public void setContact(
      @NonNull GlideRequests glideRequests, @NonNull DcContact contact, boolean multiSelect) {
    this.specialId = contact.getId();
    String name = contact.getDisplayName();

    this.recipient = new Recipient(getContext(), contact);
    this.recipientListener =
        (recipient) -> {
          if (this.recipient == recipient) {
            Util.runOnMain(
                () -> {
                  avatar.setAvatar(glideRequests, recipient, false);
                  DcContact dcContact = recipient.getDcContact();
                  avatar.setSeenRecently(dcContact != null && dcContact.wasSeenRecently());
                  nameView.setText(recipient.toShortString());
                });
          }
        };
    this.recipient.addListener(recipientListener);
    if (this.recipient.getName() != null) {
      name = this.recipient.getName();
    }

    this.avatar.setAvatar(glideRequests, recipient, false);
    this.avatar.setSeenRecently(contact.wasSeenRecently());

    String subtitle = null;
    if (!contact.isKeyContact()) {
      subtitle = contact.getAddr();
    }

    this.nameView.setTypeface(null, Typeface.NORMAL);
    setText(name, subtitle);

    if (multiSelect) this.checkBox.setVisibility(View.VISIBLE);
    else this.checkBox.setVisibility(View.GONE);
  }

  public void setSpecial(
      @NonNull GlideRequests glideRequests, int specialId, @NonNull String title) {
    this.specialId = specialId;
    this.recipientListener = null;
    this.recipient = null;

    if (specialId == DcContact.DC_CONTACT_ID_QR_INVITE) {
      this.avatar.setImageDrawable(
          new ResourceContactPhoto(R.drawable.ic_qr_code_24)
              .asDrawable(getContext(), ThemeUtil.getDummyContactColor(getContext())));
    } else {
      this.avatar.setAvatar(glideRequests, null, false);
    }
    this.avatar.setSeenRecently(false);

    this.nameView.setTypeface(null, Typeface.BOLD);
    setText(title, null);

    this.checkBox.setVisibility(View.GONE);
  }

  public void setQrInviteData(
      @NonNull QrInviteData inviteData, int specialId, @NonNull GlideRequests glideRequests) {
    this.specialId = specialId;

    if (inviteData.getContactId() > 0) {
      DcContext dcContext = DcHelper.getContext(getContext());
      DcContact dcContact = dcContext.getContact(inviteData.getContactId());
      this.recipient = new Recipient(getContext(), dcContact);
    } else {
      this.recipient = null;
    }
    this.recipientListener = null;
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
    if (recipientListener != null && recipient != null) {
      recipient.removeListener(recipientListener);
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
    return recipient == null ? null : recipient.getDcContact();
  }

  public int getContactId() {
    if (recipient != null && recipient.getAddress().isDcContact()) {
      return recipient.getAddress().getDcContactId();
    } else {
      return -1;
    }
  }
}
