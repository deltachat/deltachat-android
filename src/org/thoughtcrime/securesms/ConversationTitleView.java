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
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ViewUtil;

public class ConversationTitleView extends RelativeLayout {

  @SuppressWarnings("unused")
  private static final String TAG = ConversationTitleView.class.getSimpleName();

  private View            content;
  private ImageView       back;
  private AvatarImageView avatar;
  private TextView        title;
  private TextView        subtitle;

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
    this.avatar   = ViewUtil.findById(this, R.id.contact_photo_image);

    ViewUtil.setTextViewGravityStart(this.title, getContext());
    ViewUtil.setTextViewGravityStart(this.subtitle, getContext());
  }

  public void setTitle(@NonNull GlideRequests glideRequests, @Nullable DcChat dcChat) {

    int imgLeft = 0;
    int imgRight = 0;

    if (dcChat == null) {
      setComposeTitle();
    } else {
      setRecipientTitle(dcChat);
      if (Prefs.isChatMuted(getContext(), dcChat.getId())) {
        imgLeft = R.drawable.ic_volume_off_white_18dp;
      }
      if (dcChat.isVerified()) {
        imgRight = R.drawable.ic_verified;
      }
      this.avatar.setAvatar(glideRequests, DcHelper.getContext(getContext()).getRecipient(dcChat), false);
    }

    title.setCompoundDrawablesWithIntrinsicBounds(imgLeft, 0, imgRight, 0);
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
    this.title.setText(null);
    this.subtitle.setText(null);
    this.subtitle.setVisibility(View.GONE);
  }

  private void setRecipientTitle(DcChat dcChat) {
    this.title.setText(dcChat.getName());
    this.subtitle.setText(dcChat.getSubtitle());
    this.subtitle.setVisibility(View.VISIBLE);
  }
}
