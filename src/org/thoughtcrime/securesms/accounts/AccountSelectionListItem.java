package org.thoughtcrime.securesms.accounts;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
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
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.ViewUtil;

public class AccountSelectionListItem extends LinearLayout {

  @SuppressWarnings("unused")
  private static final String TAG = AccountSelectionListItem.class.getSimpleName();

  private AvatarImageView contactPhotoImage;
  private View            addrContainer;
  private TextView        addrView;
  private TextView        nameView;
  private ImageView       unreadIndicator;
  private ImageView       checkbox;
  private ImageButton     deleteBtn;

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
    this.deleteBtn         = findViewById(R.id.delete);

    deleteBtn.setColorFilter(DynamicTheme.isDarkTheme(getContext())? Color.WHITE : Color.BLACK);
    ViewUtil.setTextViewGravityStart(this.nameView, getContext());
  }

  public void bind(@NonNull GlideRequests glideRequests, int accountId, DcContact self, String name, String addr, int unreadCount, boolean selected) {
    this.accountId     = accountId;

    Recipient recipient;
    if (accountId != DcContact.DC_CONTACT_ID_ADD_ACCOUNT) {
      deleteBtn.setVisibility(selected? View.INVISIBLE : View.VISIBLE);
      recipient = new Recipient(getContext(), self, name);
    } else {
      deleteBtn.setVisibility(View.GONE);
      recipient = null;
    }
    this.contactPhotoImage.setAvatar(glideRequests, recipient, false);

    if (selected) {
      addrView.setTypeface(null, Typeface.BOLD);
      nameView.setTypeface(null, Typeface.BOLD);
      checkbox.setVisibility(View.VISIBLE);
    } else {
      addrView.setTypeface(null, 0);
      nameView.setTypeface(null, 0);
      checkbox.setVisibility(View.GONE);
    }

    updateUnreadIndicator(unreadCount);
    setText(name, addr);
  }

  public void unbind(GlideRequests glideRequests) {
    contactPhotoImage.clear(glideRequests);
  }

  private void updateUnreadIndicator(int unreadCount) {
    if(unreadCount == 0) {
      unreadIndicator.setVisibility(View.GONE);
    } else {
      final TypedArray attrs = getContext().obtainStyledAttributes(new int[] {
               R.attr.conversation_list_item_unreadcount_color,
      });
      unreadIndicator.setImageDrawable(TextDrawable.builder()
              .beginConfig()
              .width(ViewUtil.dpToPx(getContext(), 24))
              .height(ViewUtil.dpToPx(getContext(), 24))
              .textColor(Color.WHITE)
              .bold()
              .endConfig()
              .buildRound(String.valueOf(unreadCount), attrs.getColor(0, Color.BLACK)));
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

  public ImageButton getDeleteBtn() {
    return deleteBtn;
  }

  public int getAccountId() {
    return accountId;
  }
}
