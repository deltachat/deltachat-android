package org.thoughtcrime.securesms.accounts;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.amulyakhare.textdrawable.TextDrawable;
import com.b44t.messenger.DcContact;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.AvatarImageView;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.ThemeUtil;
import org.thoughtcrime.securesms.util.ViewUtil;

public class AccountSelectionListItem extends LinearLayout {

  private AvatarImageView contactPhotoImage;
  private View            addrContainer;
  private TextView        addrView;
  private TextView        nameView;
  private ImageView       unreadIndicator;
  private ImageView       checkbox;

  private int           accountId;

  public AccountSelectionListItem(Context context) {
    super(context);
  }

  public AccountSelectionListItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    this.contactPhotoImage = findViewById(R.id.contact_photo_image);
    this.addrContainer     = findViewById(R.id.addr_container);
    this.addrView          = findViewById(R.id.addr);
    this.nameView          = findViewById(R.id.name);
    this.unreadIndicator   = findViewById(R.id.unread_indicator);
    this.checkbox          = findViewById(R.id.checkbox);

    ViewUtil.setTextViewGravityStart(this.nameView, getContext());
  }

  public void bind(@NonNull GlideRequests glideRequests, int accountId, DcContact self, String name, String addr, int unreadCount, boolean selected, boolean isMuted, AccountSelectionListFragment fragment) {
    this.accountId     = accountId;

    Recipient recipient;
    if (accountId != DcContact.DC_CONTACT_ID_ADD_ACCOUNT) {
      recipient = new Recipient(getContext(), self, name);
    } else {
      recipient = null;
    }
    this.contactPhotoImage.setAvatar(glideRequests, recipient, false);

    nameView.setCompoundDrawablesWithIntrinsicBounds(isMuted? R.drawable.ic_volume_off_grey600_18dp : 0, 0, 0, 0);

    if (selected) {
      addrView.setTypeface(null, Typeface.BOLD);
      nameView.setTypeface(null, Typeface.BOLD);
      checkbox.setVisibility(View.VISIBLE);
    } else {
      addrView.setTypeface(null, Typeface.NORMAL);
      nameView.setTypeface(null, accountId == DcContact.DC_CONTACT_ID_ADD_ACCOUNT? Typeface.BOLD : Typeface.NORMAL);
      checkbox.setVisibility(View.GONE);
    }

    updateUnreadIndicator(unreadCount, isMuted);
    setText(name, addr);

    if (accountId != DcContact.DC_CONTACT_ID_ADD_ACCOUNT) {
      fragment.registerForContextMenu(this);
    } else {
      fragment.unregisterForContextMenu(this);
    }
  }

  public void unbind(GlideRequests glideRequests) {
    contactPhotoImage.clear(glideRequests);
  }

  private void updateUnreadIndicator(int unreadCount, boolean isMuted) {
    if(unreadCount == 0) {
      unreadIndicator.setVisibility(View.GONE);
    } else {
      final int color = getResources().getColor(isMuted ? (ThemeUtil.isDarkTheme(getContext()) ? R.color.unread_count_muted_dark : R.color.unread_count_muted) : R.color.unread_count);
      unreadIndicator.setImageDrawable(TextDrawable.builder()
              .beginConfig()
              .width(ViewUtil.dpToPx(getContext(), 24))
              .height(ViewUtil.dpToPx(getContext(), 24))
              .textColor(Color.WHITE)
              .bold()
              .endConfig()
              .buildRound(String.valueOf(unreadCount), color));
      unreadIndicator.setVisibility(View.VISIBLE);
    }
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

  public int getAccountId() {
    return accountId;
  }
}
