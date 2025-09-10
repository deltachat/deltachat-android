package org.thoughtcrime.securesms.videochat;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;

import org.thoughtcrime.securesms.WebViewActivity;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Util;

import java.util.Objects;

public class VideochatActivity extends WebViewActivity implements DcEventCenter.DcEventDelegate {

  public static final String EXTRA_ACCOUNT_ID = "acc_id";
  public static final String EXTRA_CHAT_ID = "chat_id";
  public static final String EXTRA_CALL_ID = "call_id";
  public static final String EXTRA_HASH = "hash";

  private DcContext dcContext;
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
    chatId = bundle.getInt(EXTRA_CHAT_ID, 0);
    callId = bundle.getInt(EXTRA_CALL_ID, 0);
    int accId = bundle.getInt(EXTRA_ACCOUNT_ID, -1);
    this.dcContext = DcHelper.getAccounts(this).getAccount(accId);

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

    String url = "file:///android_asset/calls/index.html";
    webView.loadUrl(url + hash);
  }

  @Override
  protected void onDestroy() {
    DcHelper.getEventCenter(this).removeObservers(this);
    if (callId != 0 && !ended) dcContext.endCall(callId);
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
        String hash = "#answer=" + event.getData2Str();
        webView.evaluateJavascript("window.location.hash = `"+hash+"`", null);
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
    public void startCall(String payload) {
      callId = dcContext.placeOutgoingCall(chatId, payload);
    }

    @JavascriptInterface
    public void acceptCall(String payload) {
      dcContext.acceptIncomingCall(callId, payload);
    }

    @JavascriptInterface
    public void endCall() {
      finish();
    }
  }
}
