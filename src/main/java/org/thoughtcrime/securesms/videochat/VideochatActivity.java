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
  public static final String EXTRA_URL = "url";

  private DcContext dcContext;
  private String url = "";

  @Override
  protected void onCreate(Bundle state, boolean ready) {
    super.onCreate(state, ready);
    this.dcContext = DcHelper.getContext(getApplicationContext());

    Bundle b = getIntent().getExtras();
    url = b.getString(EXTRA_URL, "");
    int chatId = b.getInt(EXTRA_CHAT_ID, 0);

    WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36");

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

    webView.loadUrl(url);
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
