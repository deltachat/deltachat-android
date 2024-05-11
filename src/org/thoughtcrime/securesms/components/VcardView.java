package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.b44t.messenger.DcMsg;
import com.b44t.messenger.rpc.Rpc;
import com.b44t.messenger.rpc.RpcException;
import com.b44t.messenger.rpc.VcardContact;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.mms.DocumentSlide;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.mms.SlideClickListener;
import org.thoughtcrime.securesms.recipients.Recipient;

public class VcardView extends FrameLayout {
  private static final String TAG = VcardView.class.getSimpleName();

  private final @NonNull AvatarView avatar;
  private final @NonNull TextView name;
  private final @NonNull TextView address;

  private @Nullable SlideClickListener viewListener;

  public VcardView(Context context) {
    this(context, null);
  }

  public VcardView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public VcardView(final Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    inflate(context, R.layout.vcard_view, this);

    this.avatar  = findViewById(R.id.avatar);
    this.name    = findViewById(R.id.name);
    this.address = findViewById(R.id.addr);
  }

  public void setVcardClickListener(@Nullable SlideClickListener listener) {
    this.viewListener = listener;
  }

  public void setVcardContact(@NonNull GlideRequests glideRequests, final @NonNull DcMsg dcMsg, final @NonNull Rpc rpc) {
    try {
      VcardContact vcardContact = rpc.parseVcard(dcMsg.getFile()).get(0);
      name.setText(vcardContact.getDisplayName());
      address.setText(vcardContact.getAddr());
      avatar.setAvatar(glideRequests, new Recipient(getContext(), vcardContact), false);

      setOnClickListener(new OpenClickedListener(getContext(), dcMsg));
    } catch (RpcException e) {
      Log.e(TAG, "failed to parse vCard", e);
    }
  }

  public String getDescription() {
    return name.getText() + "\n" + address.getText();
  }


  private class OpenClickedListener implements View.OnClickListener {
    private final @NonNull DocumentSlide slide;

    private OpenClickedListener(Context context, @NonNull DcMsg dcMsg) {
      this.slide = new DocumentSlide(context, dcMsg);
    }

    @Override
    public void onClick(View v) {
      if (viewListener != null) {
        viewListener.onClick(v, slide);
      }
    }
  }

}
