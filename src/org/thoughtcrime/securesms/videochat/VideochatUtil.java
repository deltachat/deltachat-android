package org.thoughtcrime.securesms.videochat;

import android.app.Activity;

import androidx.appcompat.app.AlertDialog;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.IntentUtils;

public class VideochatUtil {

  public void invite(Activity activity, int chatId) {
    ApplicationDcContext dcContext = DcHelper.getContext(activity);
    DcChat dcChat = dcContext.getChat(chatId);

    new AlertDialog.Builder(activity)
            .setTitle(String.format("Invite %1$s to a video chat?", dcChat.getName()))
            .setMessage("This requires a compatible app or a compatible browser on both ends.")
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok, (dialog, which) -> {
                dcContext.sendVideochatInvitation(dcChat.getId());
            })
            .show();
  }

  public void join(Activity activity, int msgId) {
    ApplicationDcContext dcContext = DcHelper.getContext(activity);
    DcMsg dcMsg = dcContext.getMsg(msgId);
    String videochatUrl = dcMsg.getVideochatUrl();
    IntentUtils.showBrowserIntent(activity, videochatUrl);
  }

}
