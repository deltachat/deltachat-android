package org.thoughtcrime.securesms.service;


import android.content.ComponentName;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import android.util.Log;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcChatlist;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.ShareActivity;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.BitmapUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RequiresApi(api = Build.VERSION_CODES.M)
public class DirectShareService extends ChooserTargetService {

  private static final String TAG = DirectShareService.class.getSimpleName();

  @Override
  public List<ChooserTarget> onGetChooserTargets(ComponentName targetActivityName,
                                                 IntentFilter matchedFilter)
  {
    List<ChooserTarget> results        = new LinkedList<>();
    ComponentName       componentName  = new ComponentName(this, ShareActivity.class);
    ApplicationDcContext dcContext = DcHelper.getContext(this);

    DcChatlist chatlist = dcContext.getChatlist(
            DcContext.DC_GCL_ADD_ALLDONE_HINT | DcContext.DC_GCL_FOR_FORWARDING | DcContext.DC_GCL_NO_SPECIALS,
            null,
            0
    );
    int max = 4;
    if (chatlist.getCnt() < max) {
      max = chatlist.getCnt();
    }
    for (int i = 0; i <= max; i++) {
      DcChat chat = chatlist.getChat(i);
      if (!chat.canSend()) {
        continue;
      }

      Bundle bundle = new Bundle();
      bundle.putInt(ShareActivity.EXTRA_CHAT_ID, chat.getId());
      bundle.setClassLoader(getClassLoader());
      Recipient recipient = new Recipient(this, chat);
      Bitmap avatar;
      try {
        avatar = GlideApp.with(this)
                .asBitmap()
                .load(recipient.getContactPhoto(this))
                .circleCrop()
                .submit(getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                        getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width))
                .get();
      } catch (InterruptedException | ExecutionException | NullPointerException e) {
        Log.w(TAG, e);
        avatar = getFallbackDrawable(recipient);
      }
      results.add(new ChooserTarget(chat.getName(), Icon.createWithBitmap(avatar), 1.0f, componentName, bundle));
    }

    return results;
  }

  private Bitmap getFallbackDrawable(@NonNull Recipient recipient) {
    return BitmapUtil.createFromDrawable(recipient.getFallbackAvatarDrawable(this),
                                         getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                                         getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height));
  }
}
