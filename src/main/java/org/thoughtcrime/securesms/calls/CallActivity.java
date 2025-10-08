package org.thoughtcrime.securesms.calls;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.WebViewActivity;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.AvatarUtil;
import org.thoughtcrime.securesms.util.Util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;

public class CallActivity extends WebViewActivity implements DcEventCenter.DcEventDelegate {
  private static final String TAG = CallActivity.class.getSimpleName();

  public static final String EXTRA_ACCOUNT_ID = "acc_id";
  public static final String EXTRA_CHAT_ID = "chat_id";
  public static final String EXTRA_CALL_ID = "call_id";
  public static final String EXTRA_HASH = "hash";

  private DcContext dcContext;
  private Rpc rpc;
  private int accId;
  private int chatId;
  private int callId;
  private boolean ended = false;

  @SuppressLint("SetJavaScriptEnabled")
  @Override
  protected void onCreate(Bundle state, boolean ready) {
    super.onCreate(state, ready);

    Bundle bundle = getIntent().getExtras();
    assert bundle != null;
    String hash = bundle.getString(EXTRA_HASH, "");
    accId = bundle.getInt(EXTRA_ACCOUNT_ID, -1);
    chatId = bundle.getInt(EXTRA_CHAT_ID, 0);
    callId = bundle.getInt(EXTRA_CALL_ID, 0);
    rpc = DcHelper.getRpc(this);
    dcContext = DcHelper.getAccounts(this).getAccount(accId);

    DcHelper.getNotificationCenter(this).removeCallNotification(accId, callId);

    WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setMediaPlaybackRequiresUserGesture(false);
    webView.addJavascriptInterface(new InternalJSApi(), "calls");

    webView.setWebChromeClient(new WebChromeClient() {
      @Override
      public void onPermissionRequest(PermissionRequest request) {
      Util.runOnMain(() -> request.grant(request.getResources()));
      }
    });

    DcEventCenter eventCenter = DcHelper.getEventCenter(getApplicationContext());
    eventCenter.addObserver(DcContext.DC_EVENT_OUTGOING_CALL_ACCEPTED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_CALL_ENDED, this);

    Util.runOnAnyBackgroundThread(() -> {
      final DcChat chat = dcContext.getChat(chatId);
      Util.runOnMain(() -> Objects.requireNonNull(getSupportActionBar()).setTitle(chat.getName()));
    });

    Permissions.with(this)
      .request(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
      .ifNecessary()
      .withPermanentDenialDialog(getString(R.string.perm_explain_access_to_camera_denied))
      .onAllGranted(() -> {
        String url = "file:///android_asset/calls/index.html";
        webView.loadUrl(url + hash);
      }).onAnyDenied(this::finish)
      .execute();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }

  @Override
  protected void onDestroy() {
    DcHelper.getEventCenter(this).removeObservers(this);
    if (callId != 0 && !ended) {
      try {
        rpc.endCall(accId, callId);
      } catch (RpcException e) {
        Log.e(TAG, "Error", e);
      }
    }
    super.onDestroy();
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    // do not call super.onPrepareOptionsMenu() as the default "Search" menu is not needed
    return true;
  }

  @Override
  protected boolean openOnlineUrl(String url) {
    finish();
    return true;
  }

  @Override
  public void handleEvent(@NonNull DcEvent event) {
    switch (event.getId()) {
    case DcContext.DC_EVENT_OUTGOING_CALL_ACCEPTED:
      if (event.getData1Int() == callId) {
        try {
          String base64 = Base64.encodeToString(event.getData2Str().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
          String hash = "#onAnswer=" + URLEncoder.encode(base64, "UTF-8");
          webView.evaluateJavascript("window.location.hash = `"+hash+"`", null);
        } catch (UnsupportedEncodingException e) {
          Log.e(TAG, "Error", e);
        }
      }
      break;
    case DcContext.DC_EVENT_CALL_ENDED:
      if (event.getData1Int() == callId) {
        ended = true;
        finish();
      }
      break;
    }
  }


  class InternalJSApi {
    @JavascriptInterface
    public String getIceServers() {
      try {
        return rpc.iceServers(accId);
      } catch (RpcException e) {
        Log.e(TAG, "Error", e);
        return null;
      }
    }

    @JavascriptInterface
    public void startCall(String payload) {
      try {
        callId = rpc.placeOutgoingCall(accId, chatId, payload);
      } catch (RpcException e) {
        Log.e(TAG, "Error", e);
      }
    }

    @JavascriptInterface
    public void acceptCall(String payload) {
      try {
        rpc.acceptIncomingCall(accId, callId, payload);
      } catch (RpcException e) {
        Log.e(TAG, "Error", e);
      }
    }

    @JavascriptInterface
    public void endCall() {
      finish();
    }

    @JavascriptInterface
    public String getAvatar() {
      final Context context = CallActivity.this;
      final DcChat dcChat = dcContext.getChat(chatId);
      if (!TextUtils.isEmpty(dcChat.getProfileImage())) {
        return AvatarUtil.asDataUri(dcChat.getProfileImage());
      } else {
        final Recipient recipient = new Recipient(context, dcChat);
        return AvatarUtil.asDataUri(recipient.getFallbackAvatarDrawable(context, false));
      }
    }
  }
}
