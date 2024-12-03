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

import androidx.appcompat.app.ActionBar;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.rpc.HttpResponse;
import com.b44t.messenger.rpc.Rpc;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.Prefs;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

public class WebxdcStoreActivity extends PassphraseRequiredActionBarActivity {
  private static final String TAG = WebxdcStoreActivity.class.getSimpleName();

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
        view.loadUrl(url);
        return true;
      }

      @TargetApi(Build.VERSION_CODES.N)
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        view.loadUrl(request.getUrl().toString());
        return true;
      }

      @Override
      public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        WebResourceResponse res = interceptRequest(request.getUrl().toString());
        if (res == null) {
          res = super.shouldInterceptRequest(view, request);
        }
        return res;
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

      String ext = MediaUtil.getFileExtensionFromUrl(Uri.parse(url).getPath());
      if ("xdc".equals(ext)) {
        Intent intent = new Intent();
        Uri uri = PersistentBlobProvider.getInstance().create(this, httpResponse.getBlob(), mimeType, "app.xdc");
        intent.setType(mimeType);
        intent.setData(uri);
        setResult(Activity.RESULT_OK, intent);
        finish();
      } else {
        ByteArrayInputStream data = new ByteArrayInputStream(httpResponse.getBlob());
        res = new WebResourceResponse(mimeType, httpResponse.getEncoding(), data);
      }
    } catch (Exception e) {
      e.printStackTrace();
      ByteArrayInputStream data = new ByteArrayInputStream(("Error: " + e.getMessage()).getBytes());
      res = new WebResourceResponse("text/plain", "UTF-8", data);
    }
    if (res != null) {
      HashMap<String, String> headers = new HashMap<>();
      headers.put("access-control-allow-origin", "*");
      res.setResponseHeaders(headers);
    }
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
