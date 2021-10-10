package org.thoughtcrime.securesms.accounts;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.b44t.messenger.DcContact;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.AvatarImageView;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.ViewUtil;

public class AccountSelectionListItem extends LinearLayout {

  @SuppressWarnings("unused")
  private static final String TAG = AccountSelectionListItem.class.getSimpleName();

  private AvatarImageView contactPhotoImage;
  private View            addrContainer;
  private TextView        addrView;
  private TextView        nameView;
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

    ViewUtil.setTextViewGravityStart(this.nameView, getContext());
  }

  public void bind(@NonNull GlideRequests glideRequests, int accountId, DcContact self, String name, String addr, AccountSelectionListAdapter.ItemClickListener listener) {
    this.glideRequests = glideRequests;
    this.accountId     = accountId;
    this.listener      = listener;

    Recipient recipient;
    if (accountId != DcContact.DC_CONTACT_ID_ADD_ACCOUNT) {
      deleteBtn.setVisibility(View.VISIBLE);
      recipient = new Recipient(getContext(), self);
    } else {
      deleteBtn.setVisibility(View.GONE);
      recipient = null;
    }
    this.contactPhotoImage.setAvatar(glideRequests, recipient, false);

    setText(name, addr);
  }

  public void unbind(GlideRequests glideRequests) {
    contactPhotoImage.clear(glideRequests);
  }

  private void setText(String name, String addr) {
    this.nameView.setText(name==null? "#" : name);

    if(addr != null) {
      this.addrView.setText(addr);
      this.addrContainer.setVisibility(View.VISIBLE);
    }
    else {
      this.addrContainer.setVisibility(View.GONE);
    }
  }

  public int getAccountId() {
    return accountId;
  }
}
