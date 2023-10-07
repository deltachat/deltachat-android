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
  private View            addrContainer;
  private TextView        addrView;
  private TextView        nameView;
  private TextView        reactionView;

  private int             contactId;

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
    this.addrContainer     = findViewById(R.id.addr_container);
    this.addrView          = findViewById(R.id.addr);
    this.nameView          = findViewById(R.id.name);
    this.reactionView      = findViewById(R.id.reaction);

    ViewUtil.setTextViewGravityStart(this.nameView, getContext());
  }

  public void bind(@NonNull GlideRequests glideRequests, int contactId, String reaction) {
    this.contactId      = contactId;
    DcContact dcContact = DcHelper.getContext(getContext()).getContact(contactId);
    Recipient recipient = new Recipient(getContext(), dcContact);
    this.contactPhotoImage.setAvatar(glideRequests, recipient, false);
    this.reactionView.setText(reaction);
    setText(dcContact.getDisplayName(), dcContact.getAddr());
  }

  public void unbind(GlideRequests glideRequests) {
    contactPhotoImage.clear(glideRequests);
  }

  private void setText(String name, String addr) {
    this.nameView.setText(name==null? "#" : name);

    if(addr != null) {
      this.addrView.setText(addr);
      this.addrContainer.setVisibility(View.VISIBLE);
    } else {
      this.addrContainer.setVisibility(View.GONE);
    }
  }

  public int getContactId() {
    return contactId;
  }
}
