package org.thoughtcrime.securesms;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.rpc.HttpResponse;
import com.b44t.messenger.rpc.Rpc;
import com.b44t.messenger.rpc.RpcException;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.util.IntentUtils;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

public class WebxdcStoreActivity extends PassphraseRequiredActionBarActivity {
  private static final String TAG = "DeltaChatUI." + WebxdcStoreActivity.class.getSimpleName();

  private DcContext dcContext;
  private Rpc rpc;

  @Override
  protected void onCreate(Bundle state, boolean ready) {
    setContentView(R.layout.web_view_activity);
    rpc = DcHelper.getRpc(this);
    dcContext = DcHelper.getContext(this);
    WebView webView = findViewById(R.id.webview);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setTitle(R.string.webxdc_apps);
    }

    webView.setWebViewClient(new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        String ext = MediaUtil.getFileExtensionFromUrl(Uri.parse(url).getPath());
        if ("xdc".equals(ext)) {
          Util.runOnAnyBackgroundThread(() -> {
            try {
              HttpResponse httpResponse = rpc.getHttpResponse(dcContext.getAccountId(), url);
              Uri uri = PersistentBlobProvider.getInstance().create(WebxdcStoreActivity.this, httpResponse.getBlob(), "application/octet-stream", "app.xdc");
              Intent intent = new Intent();
              intent.setData(uri);
              setResult(Activity.RESULT_OK, intent);
              finish();
            } catch (RpcException e) {
              e.printStackTrace();
              Util.runOnMain(() -> Toast.makeText(WebxdcStoreActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
          });
        } else {
          IntentUtils.showInBrowser(WebxdcStoreActivity.this, url);
        }
        return true;
      }

      @TargetApi(Build.VERSION_CODES.N)
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return shouldOverrideUrlLoading(view, request.getUrl().toString());
      }

      @Override
      public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return interceptRequest(request.getUrl().toString());
      }
    });

    WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setAllowFileAccess(false);
    webSettings.setAllowContentAccess(false);
    webSettings.setGeolocationEnabled(false);
    webSettings.setAllowFileAccessFromFileURLs(false);
    webSettings.setAllowUniversalAccessFromFileURLs(false);
    webSettings.setDatabaseEnabled(true);
    webSettings.setDomStorageEnabled(true);
    webView.setNetworkAvailable(true); // this does not block network but sets `window.navigator.isOnline` in js land

    webView.loadUrl(Prefs.getWebxdcStoreUrl(this));
  }

  private WebResourceResponse interceptRequest(String url) {
    WebResourceResponse res = null;
    try {
      if (url == null) {
        throw new Exception("no url specified");
      }
      HttpResponse httpResponse = rpc.getHttpResponse(dcContext.getAccountId(), url);
      String mimeType = httpResponse.getMimetype();
      if (mimeType == null) {
        mimeType = "application/octet-stream";
      }

      ByteArrayInputStream data = new ByteArrayInputStream(httpResponse.getBlob());
      res = new WebResourceResponse(mimeType, httpResponse.getEncoding(), data);
    } catch (Exception e) {
      e.printStackTrace();
      ByteArrayInputStream data = new ByteArrayInputStream(("Could not load apps. Are you online?\n\n" + e.getMessage()).getBytes());
      res = new WebResourceResponse("text/plain", "UTF-8", data);
    }

    HashMap<String, String> headers = new HashMap<>();
    headers.put("access-control-allow-origin", "*");
    res.setResponseHeaders(headers);
    return res;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
    }
    return false;
  }

}
