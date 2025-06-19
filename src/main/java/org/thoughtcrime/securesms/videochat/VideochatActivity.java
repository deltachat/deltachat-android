package org.thoughtcrime.securesms.videochat;

import android.os.Bundle;
import android.view.Menu;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.WebViewActivity;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Util;

public class VideochatActivity extends WebViewActivity {
  private static final String TAG = VideochatActivity.class.getSimpleName();

  public static final String EXTRA_CHAT_ID = "chat_id";
  public static final String EXTRA_HASH = "hash";

  private DcContext dcContext;
  private String url = "file:///android_asset/call.html";

  @Override
  protected void onCreate(Bundle state, boolean ready) {
    super.onCreate(state, ready);
    this.dcContext = DcHelper.getContext(getApplicationContext());

    Bundle b = getIntent().getExtras();
    String hash = b.getString(EXTRA_HASH, "");
    int chatId = b.getInt(EXTRA_CHAT_ID, 0);

    WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setMediaPlaybackRequiresUserGesture(false);

    webView.setWebChromeClient(new WebChromeClient() {
      @Override
      public void onPermissionRequest(PermissionRequest request) {
      Util.runOnMain(() -> {
        request.grant(request.getResources());
      });
      }
    });

    Util.runOnAnyBackgroundThread(() -> {
      final DcChat chat = dcContext.getChat(chatId);
      Util.runOnMain(() -> {
        getSupportActionBar().setTitle(chat.getName());
      });
    });

    webView.loadUrl(url+hash);
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
}
