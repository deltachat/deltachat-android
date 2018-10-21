package org.thoughtcrime.securesms;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.b44t.messenger.DcChat;

import org.thoughtcrime.securesms.components.AvatarImageView;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.util.ViewUtil;

public class ConversationTitleView extends RelativeLayout {

  @SuppressWarnings("unused")
  private static final String TAG = ConversationTitleView.class.getSimpleName();

  private View            content;
  private ImageView       back;
  private AvatarImageView avatar;
  private TextView        title;
  private TextView        subtitle;
  private ImageView       verified;

  public ConversationTitleView(Context context) {
    this(context, null);
  }

  public ConversationTitleView(Context context, AttributeSet attrs) {
    super(context, attrs);

  }

  @Override
  public void onFinishInflate() {
    super.onFinishInflate();

    this.back     = ViewUtil.findById(this, R.id.up_button);
    this.content  = ViewUtil.findById(this, R.id.content);
    this.title    = ViewUtil.findById(this, R.id.title);
    this.subtitle = ViewUtil.findById(this, R.id.subtitle);
    this.verified = ViewUtil.findById(this, R.id.verified_indicator);
    this.avatar   = ViewUtil.findById(this, R.id.contact_photo_image);

    ViewUtil.setTextViewGravityStart(this.title, getContext());
    ViewUtil.setTextViewGravityStart(this.subtitle, getContext());
  }

  public void setTitle(@NonNull GlideRequests glideRequests, @Nullable DcChat dcChat) {
    if      (dcChat == null) setComposeTitle();
    else                     setRecipientTitle(dcChat);

    /*if (dcChat != null && recipient.isBlocked()) { TODO: dc: show icons when blocked or muted
      title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_block_white_18dp, 0, 0, 0);
    } else if (recipient != null && recipient.isMuted()) {
      title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_volume_off_white_18dp, 0, 0, 0);
    } else*/ {
      title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    }

    if (dcChat != null) {
      this.avatar.setAvatar(glideRequests, DcHelper.getContext(getContext()).getRecipient(dcChat), false);
    }
  }

  public void setVerified(boolean verified) {
    this.verified.setVisibility(verified ? View.VISIBLE : View.GONE);
  }

  public void hideAvatar() {
    avatar.setVisibility(View.GONE);
  }

  @Override
  public void setOnClickListener(@Nullable OnClickListener listener) {
    this.content.setOnClickListener(listener);
    this.avatar.setOnClickListener(listener);
  }

  @Override
  public void setOnLongClickListener(@Nullable OnLongClickListener listener) {
    this.content.setOnLongClickListener(listener);
    this.avatar.setOnLongClickListener(listener);
  }

  public void setOnBackClickedListener(@Nullable OnClickListener listener) {
    this.back.setOnClickListener(listener);
  }

  private void setComposeTitle() {
    this.title.setText(R.string.ConversationActivity_compose_message);
    this.subtitle.setText(null);
    this.subtitle.setVisibility(View.GONE);
  }

  private void setRecipientTitle(DcChat dcChat) {
    this.title.setText(dcChat.getName());
    this.subtitle.setText(dcChat.getSubtitle());
    this.subtitle.setVisibility(View.VISIBLE);
  }
}
