package org.thoughtcrime.securesms.accounts;

import android.content.Context;
import android.graphics.Color;
import androidx.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.b44t.messenger.DcContact;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.AvatarImageView;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.DynamicTheme;

public class AccountSelectionListItem extends LinearLayout {

  @SuppressWarnings("unused")
  private static final String TAG = AccountSelectionListItem.class.getSimpleName();

  private AvatarImageView contactPhotoImage;
  private View            addrContainer;
  private TextView        addrView;
  private TextView        nameView;
  private ImageView       unreadIndicator;
  private ImageButton     deleteBtn;

  private int           accountId;
  private GlideRequests glideRequests;
  private AccountSelectionListAdapter.ItemClickListener listener;

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
    this.deleteBtn         = findViewById(R.id.delete);

    setOnClickListener(view -> {
      if (listener != null) {
        listener.onItemClick(this);
      }
    });
    deleteBtn.setOnClickListener(view -> {
      if (listener != null) {
        listener.onDeleteButtonClick(accountId);
      }
    });

    deleteBtn.setColorFilter(DynamicTheme.isDarkTheme(getContext())? Color.WHITE : Color.BLACK);
    ViewUtil.setTextViewGravityStart(this.nameView, getContext());
  }

  public void bind(@NonNull GlideRequests glideRequests, int accountId, DcContact self, String name, String addr, int unreadCount, AccountSelectionListAdapter.ItemClickListener listener) {
    this.glideRequests = glideRequests;
    this.accountId     = accountId;
    this.listener      = listener;

    Recipient recipient;
    if (accountId != DcContact.DC_CONTACT_ID_ADD_ACCOUNT) {
      deleteBtn.setVisibility(View.VISIBLE);
      recipient = new Recipient(getContext(), self, name);
    } else {
      deleteBtn.setVisibility(View.GONE);
      recipient = null;
    }
    this.contactPhotoImage.setAvatar(glideRequests, recipient, false);

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
      unreadIndicator.setImageDrawable(TextDrawable.builder()
          .beginConfig()
          .width(ViewUtil.dpToPx(getContext(), 24))
          .height(ViewUtil.dpToPx(getContext(), 24))
          .textColor(Color.WHITE)
          .bold()
          .endConfig()
          .buildRound(String.valueOf(unreadCount), getResources().getColor(R.color.green_A700)));
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
