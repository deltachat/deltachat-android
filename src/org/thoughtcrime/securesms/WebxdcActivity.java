package org.thoughtcrime.securesms;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcMsg;

import org.json.JSONObject;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class WebxdcActivity extends WebViewActivity implements DcEventCenter.DcEventDelegate  {
  private static final String TAG = WebxdcActivity.class.getSimpleName();
  private DcContext dcContext;
  private DcMsg dcAppMsg;
  private String baseURL;

  public static void openWebxdcActivity(Context context, DcMsg instance) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      if (Prefs.isDeveloperModeEnabled(context)) {
        WebView.setWebContentsDebuggingEnabled(true);
      }

      Intent intent =new Intent(context, WebxdcActivity.class);
      intent.putExtra("appMessageId", instance.getId());
      context.startActivity(intent);
    } else {
      Toast.makeText(context, "At least Android 5.0 (Lollipop) required for Webxdc.", Toast.LENGTH_LONG).show();
    }
  }

  @Override
  protected void onCreate(Bundle state, boolean ready) {
    super.onCreate(state, ready);
    DcEventCenter eventCenter = DcHelper.getEventCenter(WebxdcActivity.this.getApplicationContext());
    eventCenter.addObserver(DcContext.DC_EVENT_WEBXDC_STATUS_UPDATE, this);
    
    Bundle b = getIntent().getExtras();
    int appMessageId = b.getInt("appMessageId");

    this.dcContext = DcHelper.getContext(getApplicationContext());
    this.dcAppMsg = this.dcContext.getMsg(appMessageId);
    // `msg_id` in the subdomain makes sure, different apps using same files do not share the same cache entry
    // (WebView may use a global cache shared across objects).
    // (a random-id would also work, but would need maintenance and does not add benefits as we regard the file-part interceptRequest() only,
    // also a random-id is not that useful for debugging)
    this.baseURL = "https://acc" + dcContext.getAccountId() + "-msg" + appMessageId + ".localhost";

    WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setAllowFileAccess(false);
    webSettings.setBlockNetworkLoads(true);
    webSettings.setAllowContentAccess(false);
    webSettings.setGeolocationEnabled(false);
    webSettings.setAllowFileAccessFromFileURLs(false);
    webSettings.setAllowUniversalAccessFromFileURLs(false);
    webSettings.setDatabaseEnabled(true);
    webSettings.setDomStorageEnabled(true);
    webView.addJavascriptInterface(new InternalJSApi(), "InternalJSApi");

    webView.loadUrl(this.baseURL + "/index.html");

    Util.runOnAnyBackgroundThread(() -> {
      JSONObject info = this.dcAppMsg.getWebxdcInfo();
      String chatName =  WebxdcActivity.this.dcContext.getChat(WebxdcActivity.this.dcAppMsg.getChatId()).getName();
      Util.runOnMain(() -> {
        try {
          getSupportActionBar().setTitle(chatName + " – " + info.getString("name"));
        } catch (Exception e) {
          e.printStackTrace();
        }
      });
    });
  }

  @Override
  protected void onDestroy() {
    DcHelper.getEventCenter(this.getApplicationContext()).removeObservers(this);
    super.onDestroy();
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    // do not call super.onPrepareOptionsMenu() as the default "Search" menu is not needed
    return true;
  }

  @Override
  protected boolean openOnlineUrl(String url) {
    if (url.startsWith(baseURL +"/")) {
      // internal page, continue loading in the WebView
      return false;
    }
    Toast.makeText(this, "Please embed needed resources.", Toast.LENGTH_LONG).show();
    return true; // returning `true` causes the WebView to abort loading
  }

  @Override
  protected WebResourceResponse interceptRequest(String rawUrl) {
    Log.i(TAG, "interceptRequest: " + rawUrl);
    try {
      if (rawUrl == null) {
        throw new Exception("no url specified");
      }
      String path = Uri.parse(rawUrl).getPath();
      if (path.equalsIgnoreCase("/webxdc.js")) {
        InputStream targetStream = getResources().openRawResource(R.raw.webxdc);
        return new WebResourceResponse("text/javascript", "UTF-8", targetStream);
      } else {
        byte[] blob = this.dcAppMsg.getWebxdcBlob(path);
        if (blob == null) {
          throw new Exception("\"" + path + "\" not found");
        }
        String ext = MimeTypeMap.getFileExtensionFromUrl(path);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        if (mimeType == null) {
          switch (ext) {
            case "js": mimeType = "text/javascript"; break;
            default:   mimeType = "application/octet-stream"; Log.i(TAG, "unknown mime type for " + rawUrl); break;
          }
        }
        String encoding = mimeType.startsWith("text/")? "UTF-8" : null;
        InputStream targetStream = new ByteArrayInputStream(blob);
        return new WebResourceResponse(mimeType, encoding, targetStream);
      }
    } catch (Exception e) {
      e.printStackTrace();
      InputStream targetStream = new ByteArrayInputStream(("Webxdc Request Error: " + e.getMessage()).getBytes());
      return new WebResourceResponse("text/plain", "UTF-8", targetStream);
    }
  }

  @Override
  public void handleEvent(@NonNull DcEvent event) {
    int eventId = event.getId();
    if ((eventId == DcContext.DC_EVENT_WEBXDC_STATUS_UPDATE && event.getData1Int() == dcAppMsg.getId())) {
      Log.i(TAG, "handleEvent");
      webView.loadUrl("javascript:window.__webxdcUpdate();");
    }
  }

  class InternalJSApi {
    @JavascriptInterface
    public String selfAddr() {
      return WebxdcActivity.this.dcContext.getConfig("addr");
    }

    @JavascriptInterface
    public String selfName() {
      String name = WebxdcActivity.this.dcContext.getConfig("displayname");
      if (TextUtils.isEmpty(name)) {
        name = selfAddr();
      }
      return name;
    }

    @JavascriptInterface
    public boolean sendStatusUpdate(String payload, String descr) {
      Log.i(TAG, "sendStatusUpdate");
      if (!WebxdcActivity.this.dcContext.sendWebxdcStatusUpdate(WebxdcActivity.this.dcAppMsg.getId(), payload, descr)) {
        DcChat dcChat =  WebxdcActivity.this.dcContext.getChat(WebxdcActivity.this.dcAppMsg.getChatId());
        Toast.makeText(WebxdcActivity.this,
                      dcChat.isContactRequest() ?
                          WebxdcActivity.this.getString(R.string.accept_request_first) :
                          WebxdcActivity.this.dcContext.getLastError(),
                      Toast.LENGTH_LONG).show();
        return false;
      }
      return true;
    }

    @JavascriptInterface
    public String getStatusUpdates(int lastKnownSerial) {
      Log.i(TAG, "getStatusUpdates");
      return WebxdcActivity.this.dcContext.getWebxdcStatusUpdates(WebxdcActivity.this.dcAppMsg.getId(), lastKnownSerial    );
    }
  }
}
