package org.thoughtcrime.securesms;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;

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

public class W30Activity extends WebViewActivity implements DcEventCenter.DcEventDelegate  {
  private static final String INTERNAL_SCHEMA = "web30";
  private static final String INTERNAL_DOMAIN = "local.app";
  private InternalJSApi internalJSApi;
  private DcContext dcContext;
  private DcChat dcChat;
  private Integer chatId;
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
    this.dcChat = this.dcContext.getChat(this.dcAppMsg.getChatId());
    chatId = this.dcAppMsg.getChatId();

    internalJSApi = new InternalJSApi(appMessageId);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        WebView.setWebContentsDebuggingEnabled(true);
    }

    WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setAllowFileAccess(false);
    webSettings.setBlockNetworkLoads(true);
    webView.addJavascriptInterface(internalJSApi, "W30");

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
    Log.i("W30Activity", "interceptRequest: " + rawUrl);
    try {
      if (rawUrl == null) {
        throw new Exception("no url specified");
      }
      String path = Uri.parse(rawUrl).getPath();
      if (path.equals("/index.html")) {
        InputStream targetStream = new FileInputStream(this.dcAppMsg.getFileAsFile());
        return new WebResourceResponse("text/html", "UTF-8", targetStream);
      } else if (path.equalsIgnoreCase("/deltachat.js")) {
        InputStream targetStream = getResources().openRawResource(R.raw.w30_deltachat);
        return new WebResourceResponse("text/javascript", "UTF-8", targetStream);
      } else {
        throw new Exception("\"" + path + "\" not found");
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
    if ((eventId == DcContext.DC_EVENT_INCOMING_MSG && event.getData1Int() == chatId)) {
      DcMsg msg = dcContext.getMsg(event.getData2Int());
      Log.i("WEBVIEW", "handleEvent: "+ msg.getText());
      if (msg.getText().startsWith(this.internalJSApi.appSessionId)) {
        Log.i("WEBVIEW", "call JS");
        webView.loadUrl("javascript:window.__w30update(" + msg.getId() + ");");
        webView.loadUrl("javascript:console.log(window);");
      }
    }
  }

  class InternalJSApi {
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
