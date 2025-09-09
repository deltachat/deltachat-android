package org.thoughtcrime.securesms.videochat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;

public class VideochatUtil {

  public static void startCall(Activity activity, int chatId) {
    openCall(activity, chatId, 0, "#call");
  }

  public static void joinCall(Activity activity, int callId, String payload) {
    DcContext dcContext = DcHelper.getContext(activity);
    DcMsg dcMsg = dcContext.getMsg(callId);
    int accId = dcContext.getAccountId();
    int chatId = dcMsg.getChatId();
    String hash = "#offer=" + payload;

    //DcHelper.getNotificationCenter(activity).addCallNotification(accId, chatId, callId);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      CallIntegrationService.addNewIncomingCall(activity, accId, chatId, callId, payload);
    }
    openCall(activity, chatId, callId, hash);
  }

  private static void openCall(Activity activity, int chatId, int callId, String hash) {
    DcContext dcContext = DcHelper.getContext(activity);
    int accId = dcContext.getAccountId();

    Permissions.with(activity)
      .request(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
      .ifNecessary()
      .withPermanentDenialDialog(activity.getString(R.string.perm_explain_access_to_camera_denied))
      .onAllGranted(() -> {
          if ("#call".equals(hash) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            CallIntegrationService.registerPhoneAccount(activity, accId);
            CallIntegrationService.placeCall(activity, accId, chatId);
          }
          Intent intent = new Intent(activity, VideochatActivity.class);
          intent.setAction(Intent.ACTION_VIEW);
          intent.putExtra(VideochatActivity.EXTRA_CHAT_ID, chatId);
          intent.putExtra(VideochatActivity.EXTRA_CALL_ID, callId);
          intent.putExtra(VideochatActivity.EXTRA_HASH, hash);
          activity.startActivity(intent);
        })
      .execute();
  }

}
