package org.thoughtcrime.securesms;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;

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
  private ImageView       ephemeralIcon;

  public ConversationTitleView(Context context) {
    this(context, null);
  }

  public ConversationTitleView(Context context, AttributeSet attrs) {
    super(context, attrs);

  }

  @Override
  public void onFinishInflate() {
    super.onFinishInflate();

    this.back          = ViewUtil.findById(this, R.id.up_button);
    this.content       = ViewUtil.findById(this, R.id.content);
    this.title         = ViewUtil.findById(this, R.id.title);
    this.subtitle      = ViewUtil.findById(this, R.id.subtitle);
    this.avatar        = ViewUtil.findById(this, R.id.contact_photo_image);
    this.ephemeralIcon = ViewUtil.findById(this, R.id.ephemeral_icon);

    ViewUtil.setTextViewGravityStart(this.title, getContext());
    ViewUtil.setTextViewGravityStart(this.subtitle, getContext());
  }

  public void setTitle(@NonNull GlideRequests glideRequests, @NonNull DcChat dcChat) {
    setTitle(glideRequests, dcChat, true);
  }

  public void setTitle(@NonNull GlideRequests glideRequests, @NonNull DcChat dcChat, boolean showAddInfo) {
    final int chatId = dcChat.getId();
    final Context context = getContext();
    final DcContext dcContext = DcHelper.getContext(context);

    // set title and subtitle texts
    if( chatId == DcChat.DC_CHAT_ID_DEADDROP ) {
      title.setText(R.string.menu_deaddrop);
      subtitle.setText(R.string.menu_deaddrop_subtitle);
    } else {
      title.setText(dcChat.getName());
      String subtitleStr = "ErrSubtitle";

      int[] chatContacts = dcContext.getChatContacts(chatId);
      if (dcChat.isMailingList()) {
        subtitleStr = context.getString(R.string.mailing_list);
      } else if( dcChat.isGroup() ) {
        subtitleStr = context.getResources().getQuantityString(R.plurals.n_members, chatContacts.length, chatContacts.length);
      } else if( chatContacts.length>=1 ) {
        if( dcChat.isSelfTalk() ) {
          subtitleStr = context.getString(R.string.chat_self_talk_subtitle);
        }
        else if( dcChat.isDeviceTalk() ) {
          subtitleStr = context.getString(R.string.device_talk_subtitle);
        }
        else {
          subtitleStr = dcContext.getContact(chatContacts[0]).getAddr();
        }
      }

      subtitle.setText(subtitleStr);
    }

    // set icons etc.
    int imgLeft = 0;
    int imgRight = 0;

    if (Prefs.isChatMuted(dcChat)) {
      imgLeft = R.drawable.ic_volume_off_white_18dp;
    }
    if (dcChat.isProtected()) {
      imgRight = R.drawable.ic_verified;
    }

    avatar.setAvatar(glideRequests, DcHelper.getContext(getContext()).getRecipient(dcChat), false);
    title.setCompoundDrawablesWithIntrinsicBounds(imgLeft, 0, imgRight, 0);
    subtitle.setVisibility(showAddInfo? View.VISIBLE : View.GONE);

    boolean isEphemeral = dcContext.getChatEphemeralTimer(chatId) != 0;
    ephemeralIcon.setVisibility((showAddInfo && isEphemeral)? View.VISIBLE : View.GONE);
  }

  public void setTitle(@NonNull GlideRequests glideRequests, @NonNull DcContact contact) {
    // the verified state is _not_ shown in the title. this will be confusing as in the one-to-one-ChatViews, the verified
    // icon is also not shown as these chats are always opportunistic chats
    avatar.setAvatar(glideRequests, DcHelper.getContext(getContext()).getRecipient(contact), false);
    title.setText(contact.getDisplayName());
    title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    subtitle.setText(contact.getAddr());
    subtitle.setVisibility(View.VISIBLE);
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
}
