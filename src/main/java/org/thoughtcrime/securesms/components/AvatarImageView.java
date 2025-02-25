package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.thoughtcrime.securesms.ProfileActivity;
import org.thoughtcrime.securesms.contacts.avatars.ContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.GeneratedContactPhoto;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.ThemeUtil;

public class AvatarImageView extends AppCompatImageView {

  private OnClickListener listener = null;

  public AvatarImageView(Context context) {
    super(context);
    setScaleType(ScaleType.CENTER_CROP);
  }

  public AvatarImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setScaleType(ScaleType.CENTER_CROP);
  }

  @Override
  public void setOnClickListener(OnClickListener listener) {
    this.listener = listener;
    super.setOnClickListener(listener);
  }

  public void setAvatar(@NonNull GlideRequests requestManager, @Nullable Recipient recipient, boolean quickContactEnabled) {
    if (recipient != null) {
      ContactPhoto contactPhoto = recipient.getContactPhoto(getContext());
      requestManager.load(contactPhoto)
                    .error(recipient.getFallbackAvatarDrawable(getContext()))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .circleCrop()
                    .into(this);
      if(quickContactEnabled) {
        setAvatarClickHandler(recipient);
      }
    } else {
      setImageDrawable(new GeneratedContactPhoto("+").asDrawable(getContext(), ThemeUtil.getDummyContactColor(getContext())));
      if (listener != null) super.setOnClickListener(listener);
    }
  }

  public void clear(@NonNull GlideRequests glideRequests) {
    glideRequests.clear(this);
  }

  private void setAvatarClickHandler(final Recipient recipient) {
    if (!recipient.isMultiUserRecipient()) {
      super.setOnClickListener(v -> {
        if(recipient.getAddress().isDcContact()) {
          Intent intent = new Intent(getContext(), ProfileActivity.class);
          intent.putExtra(ProfileActivity.CONTACT_ID_EXTRA, recipient.getAddress().getDcContactId());
          getContext().startActivity(intent);
        }
      });
    } else {
      super.setOnClickListener(listener);
    }
  }

}
