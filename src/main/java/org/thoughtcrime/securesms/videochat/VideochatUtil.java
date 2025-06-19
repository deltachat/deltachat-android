package org.thoughtcrime.securesms.videochat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class VideochatUtil {

  public static void startMeeting(Activity activity, int chatId) {
    DcContext dcContext = DcHelper.getContext(activity);
    DcChat dcChat = dcContext.getChat(chatId);

    new AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.videochat_invite_user_to_videochat, dcChat.getName()))
            .setMessage(R.string.videochat_invite_user_hint)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok, (dialog, which) -> {
                int msgId = dcContext.sendVideochatInvitation(dcChat.getId());
                if (msgId != 0) {
                  join(activity, msgId);
                }
            })
            .show();
  }

  public static void joinMeeting(Activity activity, int msgId) {
    DcContext dcContext = DcHelper.getContext(activity);
    DcMsg dcMsg = dcContext.getMsg(msgId);
    String videochatUrl = dcMsg.getVideochatUrl();
    IntentUtils.showInBrowser(activity, videochatUrl);
  }

  public static void startCall(Activity activity, int chatId) {
    DcContext dcContext = DcHelper.getContext(activity);
    DcChat dcChat = dcContext.getChat(chatId);

    new AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.videochat_invite_user_to_videochat, dcChat.getName()))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok, (dialog, which) -> {
                joinCall(activity, chatId, "#call");
            })
            .show();
  }

  public static void joinCall(Activity activity, int chatId) {
    joinCall(activity, chatId, "");
  }

  private static void joinCall(Activity activity, int chatId, String hash) {
    Permissions.with(activity)
      .request(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
      .ifNecessary()
      .withPermanentDenialDialog(activity.getString(R.string.perm_explain_access_to_camera_denied))
      .onAllGranted(() -> {
          Intent intent = new Intent(activity, VideochatActivity.class);
          intent.setAction(Intent.ACTION_VIEW);
          intent.putExtra(VideochatActivity.EXTRA_CHAT_ID, chatId);
          intent.putExtra(VideochatActivity.EXTRA_HASH, hash);
          activity.startActivity(intent);
        })
      .execute();
  }

}
