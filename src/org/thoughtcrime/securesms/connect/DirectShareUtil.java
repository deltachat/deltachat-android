package org.thoughtcrime.securesms.connect;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcChatlist;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.ShareActivity;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.BitmapUtil;
import org.thoughtcrime.securesms.util.Util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DirectShareUtil {

  private static final String TAG = DirectShareUtil.class.getSimpleName();
  private static final String SHORTCUT_CATEGORY = "android.shortcut.conversation";
  static void triggerRefreshDirectShare(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Util.runOnBackgroundDelayed(() -> {
        ShortcutManagerCompat.addDynamicShortcuts(context, getChooserTargets(context));
        // Delay 50ms because we run this when a new message appears and we want to make sure that
        // more important things like showing a notification are not delayed
      }, 50);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  private static List<ShortcutInfoCompat> getChooserTargets(Context context) {
    List<ShortcutInfoCompat> results = new LinkedList<>();
    ApplicationDcContext dcContext = DcHelper.getContext(context);

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

      Intent intent = new Intent(context, ShareActivity.class);
      intent.setAction(Intent.ACTION_SEND);
      intent.putExtra(ShareActivity.EXTRA_CHAT_ID, chat.getId());

      Recipient recipient = DcHelper.getContext(context).getRecipient(chat);
      Bitmap avatar;
      try {
        avatar = GlideApp.with(context)
                .asBitmap()
                .load(recipient.getContactPhoto(context))
                .submit(context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                        context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width))
                .get();
      } catch (InterruptedException | ExecutionException | NullPointerException e) {
        avatar = getFallbackDrawable(context, recipient);
      }
      results.add(new ShortcutInfoCompat.Builder(context, Integer.toString(chat.getId()))
              .setShortLabel(chat.getName())
              .setLongLived(true)
              .setRank(i+1)
              .setIcon(IconCompat.createWithAdaptiveBitmap(avatar))
              .setCategories(Collections.singleton(SHORTCUT_CATEGORY))
              .setIntent(intent)
              .setActivity(new ComponentName(context, "org.thoughtcrime.securesms.RoutingActivity"))
              .build());
    }

    return results;
  }

  private static Bitmap getFallbackDrawable(Context context, @NonNull Recipient recipient) {
    return BitmapUtil.createFromDrawable(recipient.getFallbackAvatarDrawable(context),
            context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
            context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height));
  }

  public static void clearShortcut(@NonNull Context context, int chatId) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Util.runOnBackgroundDelayed(() -> ShortcutManagerCompat.removeDynamicShortcuts(context, Collections.singletonList(Integer.toString(chatId))), 50);
    }
  }


//  // TODO these are gotten from Signal:
//
//
//  public static @NonNull Bitmap wrapBitmapForShortcutInfo(@NonNull Bitmap toWrap, Context context) {
//    int SHORTCUT_INFO_BITMAP_SIZE = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
//    int SHORTCUT_INFO_WRAPPED_SIZE = SHORTCUT_INFO_BITMAP_SIZE;
//    int SHORTCUT_INFO_PADDING = 0;
//    Bitmap bitmap = Bitmap.createBitmap(SHORTCUT_INFO_BITMAP_SIZE, SHORTCUT_INFO_BITMAP_SIZE, Bitmap.Config.ARGB_8888);
//    Bitmap scaled = Bitmap.createScaledBitmap(toWrap, SHORTCUT_INFO_WRAPPED_SIZE, SHORTCUT_INFO_WRAPPED_SIZE, true);
//
//    Canvas canvas = new Canvas(bitmap);
//    canvas.drawBitmap(scaled, SHORTCUT_INFO_PADDING, SHORTCUT_INFO_PADDING, null);
//
//    return bitmap;
//  }
//
//
//  private static @NonNull Bitmap getShortcutInfoBitmap(@NonNull Context context, @NonNull Recipient recipient) throws ExecutionException, InterruptedException {
//    return wrapBitmapForShortcutInfo(request(GlideApp.with(context).asBitmap(), context, recipient, false).circleCrop().submit().get(), context);
//  }
//
//
//  private static <T> GlideRequest<T> request(@NonNull GlideRequest<T> glideRequest, @NonNull Context context, @NonNull Recipient recipient) {
//    return request(glideRequest, context, recipient, true);
//  }
//
//
//  private static <T> GlideRequest<T> request(@NonNull GlideRequest<T> glideRequest, @NonNull Context context, @NonNull Recipient recipient, boolean loadSelf) {
//    final ContactPhoto photo;
//    photo = recipient.getContactPhoto(context);
//
//    return glideRequest.load(photo)
//            .error(getFallbackDrawable(context, recipient))
//            .diskCacheStrategy(DiskCacheStrategy.ALL);
//  }
//
//  @RequiresApi(api = Build.VERSION_CODES.O)
//  @WorkerThread
//  public static Icon getIconForShortcut(@NonNull Context context, @NonNull Recipient recipient) {
//    try {
//      return Icon.createWithAdaptiveBitmap(getShortcutInfoBitmap(context, recipient));
//    } catch (ExecutionException | InterruptedException e) {
//      return null; // TODO
//    }
//  }

}
