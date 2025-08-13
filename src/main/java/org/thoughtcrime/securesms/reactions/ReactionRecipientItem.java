package org.thoughtcrime.securesms.reactions;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.b44t.messenger.DcContact;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.AvatarImageView;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.ViewUtil;

public class ReactionRecipientItem extends LinearLayout {

  private AvatarImageView contactPhotoImage;
  private TextView        nameView;
  private TextView        reactionView;

  private int             contactId;
  private String          reaction;

  public ReactionRecipientItem(Context context) {
    super(context);
  }

  public ReactionRecipientItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    this.contactPhotoImage = findViewById(R.id.contact_photo_image);
    this.nameView          = findViewById(R.id.name);
    this.reactionView      = findViewById(R.id.reaction);

    ViewUtil.setTextViewGravityStart(this.nameView, getContext());
  }

  public void bind(@NonNull GlideRequests glideRequests, int contactId, String reaction) {
    this.contactId      = contactId;
    this.reaction       = reaction;
    DcContact dcContact = DcHelper.getContext(getContext()).getContact(contactId);
    Recipient recipient = new Recipient(getContext(), dcContact);
    this.contactPhotoImage.setAvatar(glideRequests, recipient, false);
    this.reactionView.setText(reaction);
    this.nameView.setText(dcContact.getDisplayName());
  }

  public void unbind(GlideRequests glideRequests) {
    contactPhotoImage.clear(glideRequests);
  }

  public int getContactId() {
    return contactId;
  }

  public String getReaction() {
    return reaction;
  }

  public View getReactionView() {
    return reactionView;
  }
}
