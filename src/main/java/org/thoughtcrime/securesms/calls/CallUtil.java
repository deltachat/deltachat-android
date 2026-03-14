package org.thoughtcrime.securesms.calls;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.telecom.CallEndpointCompat;

import com.b44t.messenger.DcChat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.avatars.ContactPhoto;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.BitmapUtil;

public class CallUtil {
  private static final String TAG = CallUtil.class.getSimpleName();

  @RequiresApi(api = Build.VERSION_CODES.O)
  public static void startAudioCall(Context context, int chatId) {
    Log.d(TAG, "Starting audio call to " + chatId);
    startCall(context, chatId, false);
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  public static void startVideoCall(Context context, int chatId) {
    Log.d(TAG, "Starting video call to " + chatId);
    startCall(context, chatId, true);
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  private static void startCall(Context context, int chatId, boolean startsWithVideo) {
    if (chatId < 0) {
      Log.e(TAG, "Cannot start call: wrong chatId");
      return;
    }

    CallCoordinator coordinator = CallCoordinator.getInstance(context);
    int accId = DcHelper.getContext(context).getAccountId();

    coordinator.initiateOutgoingCall(accId, chatId, startsWithVideo);
  }

  @Nullable
  protected static Icon getIconFromChat(Context context, DcChat dcChat) {
    try {
      Recipient recipient = new Recipient(context, dcChat);
      ContactPhoto contactPhoto = recipient.getContactPhoto(context);

      int wh = context.getResources()
        .getDimensionPixelSize(R.dimen.contact_photo_target_size);
      Bitmap bitmap;

      if (contactPhoto != null) {
        bitmap = GlideApp.with(context)
          .asBitmap()
          .load(contactPhoto)
          .diskCacheStrategy(DiskCacheStrategy.ALL)
          .circleCrop()
          .submit(wh, wh)
          .get();
      } else {
        Drawable drawable = recipient.getFallbackContactPhoto()
          .asDrawable(context, recipient.getFallbackAvatarColor());
        bitmap = BitmapUtil.createFromDrawable(drawable, wh, wh);
      }

      if (bitmap != null) {
        return Icon.createWithBitmap(bitmap);
      }
    } catch (Exception e) {
      Log.e(TAG, "Failed to load caller icon", e);
    }

    return null;
  }

  protected static String getNameFromChat(DcChat dcChat) {
    return dcChat.getName();
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  public static int getIconResByCallEndpoint(CallEndpointCompat endpoint) {
    int iconRes;
    switch (endpoint.getType()) {
      case CallEndpointCompat.TYPE_EARPIECE:
        iconRes = R.drawable.ic_phone_in_talk;
        break;
      case CallEndpointCompat.TYPE_SPEAKER:
        iconRes = R.drawable.ic_volume_up;
        break;
      case CallEndpointCompat.TYPE_BLUETOOTH:
        iconRes = R.drawable.ic_bluetooth_audio;
        break;
      case CallEndpointCompat.TYPE_WIRED_HEADSET:
        iconRes = R.drawable.ic_headset;
        break;
      case CallEndpointCompat.TYPE_STREAMING:
        iconRes = R.drawable.ic_cast;
        break;
      default:
        iconRes = R.drawable.ic_volume_up;
        break;
    }
    return iconRes;
  }
}
