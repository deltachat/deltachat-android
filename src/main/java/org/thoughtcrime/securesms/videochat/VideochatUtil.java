package org.thoughtcrime.securesms.videochat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;

public class VideochatUtil {

  public static void startCall(Activity activity, int chatId) {
    Permissions.with(activity)
      .request(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
      .ifNecessary()
      .withPermanentDenialDialog(activity.getString(R.string.perm_explain_access_to_camera_denied))
      .onAllGranted(() -> {
          int accId = DcHelper.getContext(activity).getAccountId();
          Intent intent = new Intent(activity, VideochatActivity.class);
          intent.setAction(Intent.ACTION_VIEW);
          intent.putExtra(VideochatActivity.EXTRA_ACCOUNT_ID, accId);
          intent.putExtra(VideochatActivity.EXTRA_CHAT_ID, chatId);
          intent.putExtra(VideochatActivity.EXTRA_HASH, "#call");
          activity.startActivity(intent);
        })
      .execute();
  }

}
