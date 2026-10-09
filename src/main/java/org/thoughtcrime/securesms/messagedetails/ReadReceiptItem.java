package org.thoughtcrime.securesms.messagedetails;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.b44t.messenger.DcContact;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.AvatarImageView;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.ViewUtil;

public class ReadReceiptItem extends LinearLayout {

  private AvatarImageView contactPhotoImage;
  private TextView nameView;
  private TextView timeView;
  private int contactId;

  public ReadReceiptItem(Context context) {
    super(context);
  }

  public ReadReceiptItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    contactPhotoImage = findViewById(R.id.contact_photo_image);
    nameView = findViewById(R.id.name);
    timeView = findViewById(R.id.read_time);

    ViewUtil.setTextViewGravityStart(nameView, getContext());
  }

  public void bind(@NonNull GlideRequests glideRequests, int contactId, long timestamp) {
    this.contactId = contactId;
    DcContact dcContact = DcHelper.getContext(getContext()).getContact(contactId);
    Recipient recipient = new Recipient(getContext(), dcContact);
    contactPhotoImage.setAvatar(glideRequests, recipient, false);
    nameView.setText(dcContact.getDisplayName());
    timeView.setText(DateUtils.getAbsoluteDateTime(getContext(), timestamp * 1000L));
  }

  public void unbind(@NonNull GlideRequests glideRequests) {
    contactPhotoImage.clear(glideRequests);
  }

  public int getContactId() {
    return contactId;
  }
}
