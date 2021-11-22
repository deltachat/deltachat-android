package org.thoughtcrime.securesms;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.webkit.WebViewClientCompat;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcMsg;

import org.json.JSONException;
import org.json.JSONObject;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;


public class W30Activity extends Activity implements DcEventCenter.DcEventDelegate  {
  private static final String INTERNAL_SCHEMA = "web30";
  protected WebView webView;
  //private final DynamicTheme dynamicTheme = new DynamicTheme();
  //protected final DynamicLanguage dynamicLanguage = new DynamicLanguage();
  private InternalJSApi internal_js_api;

  private DcContext dcContext;
  private DcChat dcChat;
  Integer chatId;
  private DcMsg dcAppMsg;

  // Vars we should persist?
  private String titleWEB = "W30";
  // todo message id it was started with


  @Override
  public void handleEvent(@NonNull DcEvent event) {
    int eventId = event.getId();
    if ((eventId == DcContext.DC_EVENT_INCOMING_MSG && event.getData1Int() == chatId)) {
      DcMsg msg = dcContext.getMsg(event.getData2Int());
      Log.i("WEBVIEW", "handleEvent: "+ msg.getText());
      if (msg.getText().startsWith(this.internal_js_api.appSessionId)) {
        Log.i("WEBVIEW", "call JS");
        webView.loadUrl("javascript:window.__w30update(" + msg.getId() + ");");
        webView.loadUrl("javascript:console.log(window);");
      }
    }
  }

  protected void onPreCreate() {
    //dynamicTheme.onCreate(this);
    //dynamicLanguage.onCreate(this);
  }

  protected void onCreate(Bundle state, boolean ready) {

  }

  protected void onStart() {
    DcEventCenter eventCenter = DcHelper.getEventCenter(W30Activity.this.getApplicationContext());
    eventCenter.addObserver(DcContext.DC_EVENT_INCOMING_MSG, this);
    super.onStart();
    Log.i("W30Activity", "onCreate: init start");
    setContentView(R.layout.w30_activity);
    webView = findViewById(R.id.webview);
    Bundle b = getIntent().getExtras();
    int appMessageId = b.getInt("appMessageId");

    this.dcContext = DcHelper.getContext(getApplicationContext());
    this.dcAppMsg = this.dcContext.getMsg(appMessageId);
    this.dcChat = this.dcContext.getChat(this.dcAppMsg.getChatId());
    chatId = this.dcAppMsg.getChatId();

    internal_js_api = new InternalJSApi(appMessageId);
    configureWebView();
    // TODO implement loading saved instance

    Log.i("W30Activity", "onCreate: init done");

    webView.loadUrl(INTERNAL_SCHEMA + "://app/index.html");
  }

  public void configureWebView() {
    WebSettings webSettings = webView.getSettings();
//    Boolean preferDarkMode = DynamicTheme.isDarkTheme(this);
//    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
//      WebSettingsCompat.setForceDark(webSettings,
//        preferDarkMode ? WebSettingsCompat.FORCE_DARK_ON : WebSettingsCompat.FORCE_DARK_OFF);
//    }
    // disable "safe browsing" as this has privacy issues,
    // eg. at least false positives are sent to the "Safe Browsing Lookup API".
    // as all URLs opened in the WebView are local anyway,
    // "safe browsing" will never be able to report issues, so it can be disabled.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      webView.getSettings().setSafeBrowsingEnabled(false);
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

        WebView.setWebContentsDebuggingEnabled(true);

    }

    webSettings.setJavaScriptEnabled(true);
    webSettings.setSupportZoom(true);
    webSettings.setBuiltInZoomControls(true);
    webSettings.setAllowFileAccess(false);
    webSettings.setBlockNetworkLoads(true);
    webSettings.setGeolocationEnabled(false);
    webView.addJavascriptInterface(internal_js_api, "W30");
    webView.setWebViewClient(new WebViewClientCompat() {
      // open other sites in external browser
      private boolean internalShouldOverrideUrlLoading(String schema, Uri url) {
        Log.i("W30Activity", "internalShouldOverrideUrlLoading:" + url);
        switch (schema) {
          case "http":
          case "https":
          case "mailto":
          case "openpgp4fpr":
            openOnlineUrl(url);
            // URL opened externally, returning `true` causes the WebView to abort loading
            return true;
        }
        return !schema.equals(INTERNAL_SCHEMA);
      }

      @Override
      @SuppressWarnings("deprecation") // to support API < 21
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url != null) {
          String schema = url.split(":")[0].toLowerCase();
          return internalShouldOverrideUrlLoading(schema, Uri.parse(url));
        }
        return true;
      }

      @Override
      @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
      public boolean shouldOverrideUrlLoading(WebView view,
                                              WebResourceRequest request) {
        String schema = request.getUrl().getScheme().toLowerCase();
        return internalShouldOverrideUrlLoading(schema, request.getUrl());
      }

      // remove internet access and handle custom scheme
      @Override
      @SuppressWarnings("deprecation") // to support API < 21
      public WebResourceResponse shouldInterceptRequest(WebView view,
                                                        String url) {
        return loadWeb30File(url);
      }

      @Override
      @RequiresApi(21)
      public WebResourceResponse shouldInterceptRequest(WebView view,
                                                        WebResourceRequest request) {
        return loadWeb30File(request.getUrl().toString());
      }

    });
    webView.setWebChromeClient(new WebChromeClient() {
      @Override
      public void onReceivedTitle(WebView view, String title) {
        super.onReceivedTitle(view, title);
        titleWEB = title;
        W30Activity.this.setTitle(title);
      }

    });
  }

  WebResourceResponse loadWeb30File(String rawUrl) {
    Log.i("W30Activity", "loadWebFile:" + rawUrl);
    try {
      if (rawUrl == null) {
        throw new Exception("no url specified");
      }
      Uri url = Uri.parse(rawUrl);
      String path = url.getPath();

      if (path.equals("/index.html")) {
        InputStream targetStream = new FileInputStream(this.dcAppMsg.getFileAsFile());
        return new WebResourceResponse("text/html", "UTF-8", targetStream);
      } else if (path.equalsIgnoreCase("/deltachat.js")) {
        InputStream targetStream = getResources().openRawResource(R.raw.w30_deltachat);
        return new WebResourceResponse("text/javascript", "UTF-8", targetStream);
      } else {
        // TODO call core method here to get other files from archive
        throw new Exception("\"" + path + "\" not found");
      }
    } catch (Exception e) {
      e.printStackTrace();
      InputStream targetStream = new ByteArrayInputStream(("An Error occurred during this request: " + e.getMessage()).getBytes());
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        return new WebResourceResponse("text/plain", "UTF-8", 500, "error loading", new HashMap<String, String>(), targetStream);
      } else {
        return new WebResourceResponse("text/css", "UTF-8", targetStream);
      }
    }
  }

  @Override
  public void onResume() {
    Log.i("W30Activity", "onResume");
    super.onResume();
    //dynamicTheme.onResume(this);
    //dynamicLanguage.onResume(this);
  }

  @Override
  protected void onDestroy() {
    DcHelper.getEventCenter(this.getApplicationContext()).removeObservers(this);
    super.onDestroy();
  }

  @Override
  public void onBackPressed() {
    if (webView.canGoBack()) {
      webView.goBack();
    } else {
      super.onBackPressed();
    }
  }

  protected void openOnlineUrl(Uri url) {
    try {
      startActivity(new Intent(Intent.ACTION_VIEW, url));
    } catch (ActivityNotFoundException e) {
      Toast.makeText(this, R.string.no_browser_installed, Toast.LENGTH_LONG).show();
    }
  }

  class InternalJSApi{
    String appSessionId;

    public InternalJSApi(Integer appMessageId) {
      // get session id
      String msgInfo = W30Activity.this.dcContext.getMsgInfo(appMessageId);
      appSessionId = msgInfo.split("Message-ID: ")[1].split("\n")[0];
    }

    @JavascriptInterface
    public String toString() {
      return "[internal W30 api Object]";
    }

    @JavascriptInterface
    public String getChatName() {
      return W30Activity.this.dcChat.getName();
    }

    @JavascriptInterface
    public int sendStateUpdate(String _description, String payload) {
      return W30Activity.this.dcContext.sendTextMsg(W30Activity.this.dcChat.getId(), appSessionId + "|:|" + payload);
    }

    String stateMsgToJSON(DcMsg msg) {
      String text = msg.getText();
      try {
        JSONObject json = new JSONObject();
        json.put("payload", text.substring(text.indexOf("|:|")+3));
        json.put("authorId", msg.getFromId());
        json.put("authorDisplayName", W30Activity.this.dcContext.getContact(msg.getFromId()).getDisplayName());
        return json.toString();
      } catch (JSONException e) {
        e.printStackTrace();
        return null;
      }
    }

    @JavascriptInterface
    public String getStateUpdate(int stateMsgId) {
      DcMsg msg = W30Activity.this.dcContext.getMsg(stateMsgId);

      if (msg.getText().startsWith(appSessionId)) {
        return stateMsgToJSON(msg);
      } else {
        return null;
      }
    }

    @JavascriptInterface
    public String getAllStateUpdates() {
      int[] msgs = W30Activity.this.dcContext.searchMsgs(W30Activity.this.chatId, appSessionId);

      StringBuilder result = new StringBuilder("[");

      for (int msgId: msgs) {
        DcMsg msg = W30Activity.this.dcContext.getMsg(msgId);
        if (msg.getText().startsWith(appSessionId)) {
          result.append(stateMsgToJSON(msg)+",");
        }
      }
      if(result.length() > 1) {
        result.setCharAt(result.length()-1, ']');
      } else {
        result.append("]");
      }

      return result.toString();

    }
  }
}
