package org.thoughtcrime.securesms;

import android.content.Context;
import android.text.TextUtils;
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

import org.thoughtcrime.securesms.components.AvatarView;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.Locale;

public class ConversationTitleView extends RelativeLayout {

  private View            content;
  private ImageView       back;
  private AvatarView      avatar;
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
    this.avatar        = ViewUtil.findById(this, R.id.avatar);
    this.ephemeralIcon = ViewUtil.findById(this, R.id.ephemeral_icon);

    ViewUtil.setTextViewGravityStart(this.title, getContext());
    ViewUtil.setTextViewGravityStart(this.subtitle, getContext());
  }

  public void setTitle(@NonNull GlideRequests glideRequests, @NonNull DcChat dcChat) {
    setTitle(glideRequests, dcChat, false);
  }

  public void setTitle(@NonNull GlideRequests glideRequests, @NonNull DcChat dcChat, boolean profileView) {
    final int chatId = dcChat.getId();
    final Context context = getContext();
    final DcContext dcContext = DcHelper.getContext(context);

    // set title and subtitle texts
    title.setText(dcChat.getName());
    String subtitleStr = null;

    // set icons etc.
    int imgLeft = 0;
    int imgRight = 0;

    if (dcChat.isMuted()) {
      imgLeft = R.drawable.ic_volume_off_white_18dp;
    }
    if (dcChat.isProtected()) {
      imgRight = R.drawable.ic_verified;
    }

    boolean isOnline = false;
    int[] chatContacts = dcContext.getChatContacts(chatId);
    if (dcChat.isMailingList()) {
      if (profileView) {
        subtitleStr = dcChat.getMailinglistAddr();
      } else {
        subtitleStr = context.getString(R.string.mailing_list);
      }
    } else if (dcChat.isBroadcast()) {
      if (!profileView) {
        subtitleStr = context.getResources().getQuantityString(R.plurals.n_recipients, chatContacts.length, chatContacts.length);
      }
    } else if( dcChat.isMultiUser() ) {
      if (!profileView) {
        subtitleStr = context.getResources().getQuantityString(R.plurals.n_members, chatContacts.length, chatContacts.length);
      }
    } else if( chatContacts.length>=1 ) {
      if( dcChat.isSelfTalk() ) {
        subtitleStr = context.getString(R.string.chat_self_talk_subtitle);
      }
      else if( dcChat.isDeviceTalk() ) {
        subtitleStr = context.getString(R.string.device_talk_subtitle);
      }
      else {
        DcContact dcContact = dcContext.getContact(chatContacts[0]);
        if (profileView || !dcChat.isProtected()) {
          subtitleStr = dcContact.getAddr();
        } else {
          long timestamp = dcContact.getLastSeen();
          if (timestamp != 0) {
            Locale locale = DynamicLanguage.getSelectedLocale(context);
            subtitleStr = context.getString(R.string.last_seen_at, DateUtils.getExtendedTimeSpanString(context, locale, timestamp));
          }
        }
        isOnline = dcContact.wasSeenRecently();
      }
    }

    avatar.setAvatar(glideRequests, new Recipient(getContext(), dcChat), false);
    avatar.setSeenRecently(isOnline);
    title.setCompoundDrawablesWithIntrinsicBounds(imgLeft, 0, imgRight, 0);
    if (!TextUtils.isEmpty(subtitleStr)) {
      subtitle.setText(subtitleStr);
      subtitle.setVisibility(View.VISIBLE);
    } else {
      subtitle.setVisibility(View.GONE);
    }
    boolean isEphemeral = dcContext.getChatEphemeralTimer(chatId) != 0;
    ephemeralIcon.setVisibility(isEphemeral? View.VISIBLE : View.GONE);
  }

  public void setTitle(@NonNull GlideRequests glideRequests, @NonNull DcContact contact) {
    // This function is only called for contacts without a corresponding 1:1 chat.
    // If there is a 1:1 chat, then the overloaded function
    // setTitle(GlideRequests, DcChat, boolean) is called.
    avatar.setAvatar(glideRequests, new Recipient(getContext(), contact), false);
    avatar.setSeenRecently(contact.wasSeenRecently());

    int imgRight = 0;
    if (contact.isVerified()) {
      imgRight = R.drawable.ic_verified;
    }

    title.setText(contact.getDisplayName());
    title.setCompoundDrawablesWithIntrinsicBounds(0, 0, imgRight, 0);
    subtitle.setText(contact.getAddr());
    subtitle.setVisibility(View.VISIBLE);
  }

  public void setSeenRecently(boolean seenRecently) {
    avatar.setSeenRecently(seenRecently);
  }

  public void hideAvatar() {
    avatar.setVisibility(View.GONE);
  }

  @Override
  public void setOnClickListener(@Nullable OnClickListener listener) {
    this.content.setOnClickListener(listener);
    this.avatar.setAvatarClickListener(listener);
  }

  @Override
  public void setOnLongClickListener(@Nullable OnLongClickListener listener) {
    this.content.setOnLongClickListener(listener);
    this.avatar.setAvatarLongClickListener(listener);
  }

  public void setOnBackClickedListener(@Nullable OnClickListener listener) {
    this.back.setOnClickListener(listener);
  }
}
