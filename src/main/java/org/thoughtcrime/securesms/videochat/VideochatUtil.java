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

  public void invite(Activity activity, int chatId) {
    DcContext dcContext = DcHelper.getContext(activity);
    DcChat dcChat = dcContext.getChat(chatId);

    new AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.videochat_invite_user_to_videochat, dcChat.getName()))
            .setMessage(R.string.videochat_invite_user_hint)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok, (dialog, which) -> {
                String instance = dcContext.getConfig(DcHelper.CONFIG_WEBRTC_INSTANCE);
                boolean unset = instance == null || instance.isEmpty();
                if (unset) {
                  dcContext.setConfig(DcHelper.CONFIG_WEBRTC_INSTANCE, DcHelper.DEFAULT_VIDEOCHAT_URL);
                }

                int msgId = dcContext.sendVideochatInvitation(dcChat.getId());

                if (unset) {
                  dcContext.setConfig(DcHelper.CONFIG_WEBRTC_INSTANCE, null);
                }

                if (msgId != 0) {
                  join(activity, msgId);
                }
            })
            .show();
  }

  public void join(Activity activity, int msgId) {
    Permissions.with(activity)
      .request(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
      .ifNecessary()
      .withPermanentDenialDialog(activity.getString(R.string.perm_explain_access_to_camera_denied))
      .onAllGranted(() -> {
          DcContext dcContext = DcHelper.getContext(activity);
          DcMsg dcMsg = dcContext.getMsg(msgId);
          String url = dcMsg.getVideochatUrl();
          if (url.startsWith(DcHelper.DEFAULT_VIDEOCHAT_URL_PREFIX) && url.contains("#")) {
            String name = dcContext.getName();
            try {
              name = URLEncoder.encode(dcContext.getName(), StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException ignored) {}
            url += "&userInfo.displayName=%22" + name +"%22";
          }

          Intent intent = new Intent(activity, VideochatActivity.class);
          intent.setAction(Intent.ACTION_VIEW);
          intent.putExtra(VideochatActivity.EXTRA_CHAT_ID, dcMsg.getChatId());
          intent.putExtra(VideochatActivity.EXTRA_URL, url);
          activity.startActivity(intent);
        })
      .execute();
  }

}
