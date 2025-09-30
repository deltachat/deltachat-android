package org.thoughtcrime.securesms.calls;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.util.Log;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class CallUtil {
  private static final String TAG = CallUtil.class.getSimpleName();

  public static void startCall(Activity activity, int chatId) {
    Permissions.with(activity)
      .request(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
      .ifNecessary()
      .withPermanentDenialDialog(activity.getString(R.string.perm_explain_access_to_camera_denied))
      .onAllGranted(() -> {
          int accId = DcHelper.getContext(activity).getAccountId();
          startCall(activity, accId, chatId);
        })
      .execute();
  }

  public static void startCall(Context context, int accId, int chatId) {
    Intent intent = new Intent(context, CallActivity.class);
    intent.setAction(Intent.ACTION_VIEW);
    intent.putExtra(CallActivity.EXTRA_ACCOUNT_ID, accId);
    intent.putExtra(CallActivity.EXTRA_CHAT_ID, chatId);
    intent.putExtra(CallActivity.EXTRA_HASH, "#startCall");
    context.startActivity(intent);
  }

  public static void openCall(Context context, int accId, int chatId, int callId, String payload) {
    String base64 = Base64.encodeToString(payload.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    String hash = "";
    try {
      hash = "#offerIncomingCall=" + URLEncoder.encode(base64, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      Log.e(TAG, "Error", e);
    }

    Intent intent = new Intent(context, CallActivity.class);
    intent.setAction(Intent.ACTION_VIEW);
    intent.putExtra(CallActivity.EXTRA_ACCOUNT_ID, accId);
    intent.putExtra(CallActivity.EXTRA_CHAT_ID, chatId);
    intent.putExtra(CallActivity.EXTRA_CALL_ID, callId);
    intent.putExtra(CallActivity.EXTRA_HASH, hash);
    context.startActivity(intent);
  }

}
