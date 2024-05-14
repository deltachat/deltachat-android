package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.b44t.messenger.rpc.Rpc;
import com.b44t.messenger.rpc.RpcException;
import com.b44t.messenger.rpc.VcardContact;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.mms.SlideClickListener;
import org.thoughtcrime.securesms.mms.VcardSlide;
import org.thoughtcrime.securesms.recipients.Recipient;

public class VcardView extends FrameLayout {
  private static final String TAG = VcardView.class.getSimpleName();

  private final @NonNull AvatarView avatar;
  private final @NonNull TextView name;
  private final @NonNull TextView address;

  private @Nullable SlideClickListener viewListener;
  private @Nullable VcardSlide slide;

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

    setOnClickListener(v -> {
      if (viewListener != null && slide != null) {
        viewListener.onClick(v, slide);
      }
    });
  }

  public void setVcardClickListener(@Nullable SlideClickListener listener) {
    this.viewListener = listener;
  }

  public void setVcard(@NonNull GlideRequests glideRequests, final @NonNull VcardSlide slide, final @NonNull Rpc rpc) {
    try {
      VcardContact vcardContact = rpc.parseVcard(slide.asAttachment().getRealPath(getContext())).get(0);
      name.setText(vcardContact.getDisplayName());
      address.setText(vcardContact.getAddr());
      avatar.setAvatar(glideRequests, new Recipient(getContext(), vcardContact), false);
      this.slide = slide;
    } catch (RpcException e) {
      Log.e(TAG, "failed to parse vCard", e);
    }
  }

  public String getDescription() {
    return name.getText() + "\n" + address.getText();
  }
}
