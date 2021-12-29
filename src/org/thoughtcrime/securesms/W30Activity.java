package org.thoughtcrime.securesms;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;

public class W30Activity extends WebViewActivity implements DcEventCenter.DcEventDelegate  {
  private static final String TAG = W30Activity.class.getSimpleName();
  private static final String INTERNAL_SCHEMA = "web30";
  private static final String INTERNAL_DOMAIN = "local.app";
  private DcContext dcContext;
  private DcMsg dcAppMsg;

  @Override
  protected void onCreate(Bundle state, boolean ready) {
    super.onCreate(state, ready);
    DcEventCenter eventCenter = DcHelper.getEventCenter(W30Activity.this.getApplicationContext());
    eventCenter.addObserver(DcContext.DC_EVENT_INCOMING_MSG, this);
    
    Bundle b = getIntent().getExtras();
    int appMessageId = b.getInt("appMessageId");

    this.dcContext = DcHelper.getContext(getApplicationContext());
    this.dcAppMsg = this.dcContext.getMsg(appMessageId);

    WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setAllowFileAccess(false);
    webSettings.setBlockNetworkLoads(true);
    webView.addJavascriptInterface(new InternalJSApi(), "InternalJSApi");

    webView.loadUrl(INTERNAL_SCHEMA + "://" + INTERNAL_DOMAIN + "/index.html");
  }

  @Override
  protected void onDestroy() {
    DcHelper.getEventCenter(this.getApplicationContext()).removeObservers(this);
    super.onDestroy();
  }

  @Override
  protected void openOnlineUrl(String url) {
    Toast.makeText(this, "Please embed needed resources.", Toast.LENGTH_LONG).show();
  }

  @Override
  protected WebResourceResponse interceptRequest(String rawUrl) {
    Log.i(TAG, "interceptRequest: " + rawUrl);
    try {
      if (rawUrl == null) {
        throw new Exception("no url specified");
      }
      String path = Uri.parse(rawUrl).getPath();
      if (path.equalsIgnoreCase("/deltachat.js")) {
        InputStream targetStream = getResources().openRawResource(R.raw.w30_deltachat);
        return new WebResourceResponse("text/javascript", "UTF-8", targetStream);
      } else {
        byte[] blob = this.dcAppMsg.getBlobFromArchive(path);
        if (blob == null) {
          throw new Exception("\"" + path + "\" not found");
        }
        String ext = MimeTypeMap.getFileExtensionFromUrl(path);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        String encoding = mimeType.startsWith("text/")? "UTF-8" : null;
        InputStream targetStream = new ByteArrayInputStream(blob);
        return new WebResourceResponse(mimeType, encoding, targetStream);
      }
    } catch (Exception e) {
      e.printStackTrace();
      InputStream targetStream = new ByteArrayInputStream(("W30 Request Error: " + e.getMessage()).getBytes());
      return new WebResourceResponse("text/plain", "UTF-8", targetStream);
    }
  }

  @Override
  public void handleEvent(@NonNull DcEvent event) {
    int eventId = event.getId();
    if ((eventId == DcContext.DC_EVENT_W30_STATUS_UPDATE && event.getData1Int() == dcAppMsg.getId())) {
      Log.i(TAG, "handleEvent");
      webView.loadUrl("javascript:window.__w30update(" + event.getData2Int() + ");");
    }
  }

  class InternalJSApi {
    @JavascriptInterface
    public String selfAddr() {
      return W30Activity.this.dcContext.getConfig("addr");
    }

    @JavascriptInterface
    public boolean sendStatusUpdate(String descr, String payload) {
      Log.i(TAG, "sendStatusUpdate");
      return W30Activity.this.dcContext.sendW30StatusUpdate(W30Activity.this.dcAppMsg.getId(), descr, payload);
    }

    @JavascriptInterface
    public String getStatusUpdates(int statusUpdateId) {
      Log.i(TAG, "getStatusUpdates");
      return W30Activity.this.dcContext.getW30StatusUpdates(W30Activity.this.dcAppMsg.getId(), statusUpdateId);
    }
  }
}
