package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;

public class AvatarView extends ConstraintLayout {

  private AvatarImageView     avatarImage;
  private ImageView           seenRecentlyIndicator;

  public AvatarView(Context context) {
    super(context);
    init();
  }

  public AvatarView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public AvatarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    inflate(getContext(), R.layout.avatar_view, this);

    avatarImage             = findViewById(R.id.avatar_image);
    seenRecentlyIndicator   = findViewById(R.id.status_indicator);
  }

  public void setAvatar(@NonNull GlideRequests requestManager, @Nullable Recipient recipient, boolean quickContactEnabled) {
    avatarImage.setAvatar(requestManager, recipient, quickContactEnabled);
  }

  public void setImageDrawable(@Nullable Drawable drawable) {
    avatarImage.setImageDrawable(drawable);
  }

  public void setAvatarClickListener(OnClickListener listener) {
    avatarImage.setOnClickListener(listener);
  }

  public void setAvatarLongClickListener(OnLongClickListener listener) {
    avatarImage.setOnLongClickListener(listener);
  }

  public void setSeenRecently(boolean enabled) {
    seenRecentlyIndicator.setVisibility(enabled? View.VISIBLE : View.GONE);
  }

  public void clear(@NonNull GlideRequests glideRequests) {
    avatarImage.clear(glideRequests);
  }

}
