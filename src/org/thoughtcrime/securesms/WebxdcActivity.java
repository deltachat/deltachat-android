package org.thoughtcrime.securesms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcMsg;

import org.json.JSONObject;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.JsonUtils;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.HashMap;
import java.util.Map;

public class WebxdcActivity extends WebViewActivity implements DcEventCenter.DcEventDelegate  {
  private static final String TAG = WebxdcActivity.class.getSimpleName();
  private static final int REQUEST_CODE_FILE_PICKER = 51426;
  private ValueCallback<Uri[]> filePathCallback;
  private DcContext dcContext;
  private DcMsg dcAppMsg;
  private String baseURL;
  private String sourceCodeUrl = "";
  private boolean internetAccess = false;

  public static void openWebxdcActivity(Context context, DcMsg instance) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      if (Prefs.isDeveloperModeEnabled(context)) {
        WebView.setWebContentsDebuggingEnabled(true);
      }
      context.startActivity(getWebxdcIntent(context, instance.getId()));
    } else {
      Toast.makeText(context, "At least Android 5.0 (Lollipop) required for Webxdc.", Toast.LENGTH_LONG).show();
    }
  }

  private static Intent getWebxdcIntent(Context context, int msgId) {
    DcContext dcContext = DcHelper.getContext(context);
    Intent intent = new Intent(context, WebxdcActivity.class);
    intent.setAction(Intent.ACTION_VIEW);
    intent.putExtra("accountId", dcContext.getAccountId());
    intent.putExtra("appMessageId", msgId);
    return intent;
  }

  private static Intent[] getWebxdcIntentWithParentStack(Context context, int msgId) {
    DcContext dcContext = DcHelper.getContext(context);

    final Intent chatIntent = new Intent(context, ConversationActivity.class)
      .putExtra(ConversationActivity.CHAT_ID_EXTRA, dcContext.getMsg(msgId).getChatId())
      .setAction(Intent.ACTION_VIEW);

    final Intent webxdcIntent = getWebxdcIntent(context, msgId);

    return TaskStackBuilder.create(context)
      .addNextIntentWithParentStack(chatIntent)
      .addNextIntent(webxdcIntent)
      .getIntents();
  }

  @Override
  protected void onCreate(Bundle state, boolean ready) {
    super.onCreate(state, ready);

    webView.setWebChromeClient(new WebChromeClient() {
      @Override
      @RequiresApi(21)
      public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        if (WebxdcActivity.this.filePathCallback != null) {
          WebxdcActivity.this.filePathCallback.onReceiveValue(null);
        }
        WebxdcActivity.this.filePathCallback = filePathCallback;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
        WebxdcActivity.this.startActivityForResult(Intent.createChooser(intent, getString(R.string.select)), REQUEST_CODE_FILE_PICKER);
        return true;
      }
    });

    DcEventCenter eventCenter = DcHelper.getEventCenter(WebxdcActivity.this.getApplicationContext());
    eventCenter.addObserver(DcContext.DC_EVENT_WEBXDC_STATUS_UPDATE, this);
    
    Bundle b = getIntent().getExtras();
    int appMessageId = b.getInt("appMessageId");

    this.dcContext = DcHelper.getContext(getApplicationContext());
    if (dcContext.getAccountId() != b.getInt("accountId")) {
      Toast.makeText(this, "Switch to belonging account first.", Toast.LENGTH_LONG).show();
      finish();
      return;
    }

    this.dcAppMsg = this.dcContext.getMsg(appMessageId);
    if (!this.dcAppMsg.isOk()) {
      Toast.makeText(this, "Webxdc does no longer exist.", Toast.LENGTH_LONG).show();
      finish();
      return;
    }

    // `msg_id` in the subdomain makes sure, different apps using same files do not share the same cache entry
    // (WebView may use a global cache shared across objects).
    // (a random-id would also work, but would need maintenance and does not add benefits as we regard the file-part interceptRequest() only,
    // also a random-id is not that useful for debugging)
    this.baseURL = "https://acc" + dcContext.getAccountId() + "-msg" + appMessageId + ".localhost";

    final JSONObject info = this.dcAppMsg.getWebxdcInfo();
    internetAccess = JsonUtils.optBoolean(info, "internet_access");

    WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setAllowFileAccess(false);
    webSettings.setBlockNetworkLoads(!internetAccess);
    webSettings.setAllowContentAccess(false);
    webSettings.setGeolocationEnabled(false);
    webSettings.setAllowFileAccessFromFileURLs(false);
    webSettings.setAllowUniversalAccessFromFileURLs(false);
    webSettings.setDatabaseEnabled(true);
    webSettings.setDomStorageEnabled(true);
    webView.setNetworkAvailable(internetAccess); // this does not block network but sets `window.navigator.isOnline` in js land
    webView.addJavascriptInterface(new InternalJSApi(), "InternalJSApi");

    webView.loadUrl(this.baseURL + "/webxdc_bootstrap324567869.html");

    Util.runOnAnyBackgroundThread(() -> {
      final DcChat chat = dcContext.getChat(dcAppMsg.getChatId());
      Util.runOnMain(() -> {
        updateTitleAndMenu(info, chat);
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
    menu.clear();
    this.getMenuInflater().inflate(R.menu.webxdc, menu);
    menu.findItem(R.id.source_code).setVisible(!sourceCodeUrl.isEmpty());
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()) {
      case R.id.menu_add_to_home_screen:
        addToHomeScreen(this, dcAppMsg.getId());
        return true;
      case R.id.source_code:
        openUrlInBrowser(this, sourceCodeUrl);
        return true;
    }
    return false;
  }

  @Override
  protected boolean openOnlineUrl(String url) {
    if (url.startsWith("mailto:")) {
      return super.openOnlineUrl(url);
    }

    Toast.makeText(this, "Please embed needed resources.", Toast.LENGTH_LONG).show();
    return true; // returning `true` causes the WebView to abort loading
  }

  @Override
  protected WebResourceResponse interceptRequest(String rawUrl) {
    Log.i(TAG, "interceptRequest: " + rawUrl);
    WebResourceResponse res = null;
    try {
      if (rawUrl == null) {
        throw new Exception("no url specified");
      }
      String path = Uri.parse(rawUrl).getPath();
      if (path.equalsIgnoreCase("/webxdc.js")) {
        InputStream targetStream = getResources().openRawResource(R.raw.webxdc);
        res = new WebResourceResponse("text/javascript", "UTF-8", targetStream);
      } else if (path.equalsIgnoreCase("/webxdc_bootstrap324567869.html")) {
        InputStream targetStream = getResources().openRawResource(R.raw.webxdc_wrapper);
        res = new WebResourceResponse("text/html", "UTF-8", targetStream);
      } else if (path.equalsIgnoreCase("/sandboxed_iframe_rtcpeerconnection_check_5965668501706.html")) {
        InputStream targetStream = getResources().openRawResource(R.raw.sandboxed_iframe_rtcpeerconnection_check);
        res = new WebResourceResponse("text/html", "UTF-8", targetStream);
      } else {
        byte[] blob = this.dcAppMsg.getWebxdcBlob(path);
        if (blob == null) {
          if (internetAccess) {
            return null; // do not intercept request
          }
          throw new Exception("\"" + path + "\" not found");
        }
        String ext = MediaUtil.getFileExtensionFromUrl(path);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        if (mimeType == null) {
          switch (ext) {
            case "js": mimeType = "text/javascript"; break;
            case "wasm": mimeType = "application/wasm"; break;
            default:   mimeType = "application/octet-stream"; Log.i(TAG, "unknown mime type for " + rawUrl); break;
          }
        }
        String encoding = mimeType.startsWith("text/")? "UTF-8" : null;
        InputStream targetStream = new ByteArrayInputStream(blob);
        res = new WebResourceResponse(mimeType, encoding, targetStream);
      }
    } catch (Exception e) {
      e.printStackTrace();
      InputStream targetStream = new ByteArrayInputStream(("Webxdc Request Error: " + e.getMessage()).getBytes());
      res = new WebResourceResponse("text/plain", "UTF-8", targetStream);
    }

    if (res != null) {
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Security-Policy",
          "default-src 'self'; "
        + "style-src 'self' 'unsafe-inline' blob: ; "
        + "font-src 'self' data: blob: ; "
        + "script-src 'self' 'unsafe-inline' 'unsafe-eval' blob: ; "
        + "connect-src 'self' data: blob: ; "
        + "img-src 'self' data: blob: ; "
        + "media-src 'self' data: blob: ;"
        + "webrtc 'block' ; "
      );
      headers.put("X-DNS-Prefetch-Control", "off");
      res.setResponseHeaders(headers);
    }
    return res;
  }

  @Override
  public void handleEvent(@NonNull DcEvent event) {
    int eventId = event.getId();
    if ((eventId == DcContext.DC_EVENT_WEBXDC_STATUS_UPDATE && event.getData1Int() == dcAppMsg.getId())) {
      Log.i(TAG, "handleEvent");
      webView.loadUrl("javascript:document.getElementById('frame').contentWindow.__webxdcUpdate();");
    } else if ((eventId == DcContext.DC_EVENT_MSGS_CHANGED && event.getData2Int() == dcAppMsg.getId())) {
      Util.runOnAnyBackgroundThread(() -> {
        final JSONObject info = dcAppMsg.getWebxdcInfo();
        final DcChat chat = dcContext.getChat(dcAppMsg.getChatId());
        Util.runOnMain(() -> {
          updateTitleAndMenu(info, chat);
        });
      });
    }
  }

  private void updateTitleAndMenu(JSONObject info, DcChat chat) {
      final String docName = JsonUtils.optString(info, "document");
      final String xdcName = JsonUtils.optString(info, "name");
      final String currSourceCodeUrl = JsonUtils.optString(info, "source_code_url");
      getSupportActionBar().setTitle((docName.isEmpty() ? xdcName : docName) + " – " + chat.getName());
      if (!sourceCodeUrl.equals(currSourceCodeUrl)) {
        sourceCodeUrl = currSourceCodeUrl;
        invalidateOptionsMenu();
      }
  }

  public static void addToHomeScreen(Activity activity, int msgId) {
    Context context = activity.getApplicationContext();
    try {
      DcContext dcContext = DcHelper.getContext(context);
      DcMsg msg = dcContext.getMsg(msgId);
      final JSONObject info = msg.getWebxdcInfo();

      final String docName = JsonUtils.optString(info, "document");
      final String xdcName = JsonUtils.optString(info, "name");
      byte[] blob = msg.getWebxdcBlob(JsonUtils.optString(info, "icon"));
      ByteArrayInputStream is = new ByteArrayInputStream(blob);
      BitmapDrawable drawable = (BitmapDrawable) Drawable.createFromStream(is, "icon");
      Bitmap bitmap = drawable.getBitmap();

      ShortcutInfoCompat shortcutInfoCompat = new ShortcutInfoCompat.Builder(context, "xdc-" + dcContext.getAccountId() + "-" + msgId)
        .setShortLabel(docName.isEmpty() ? xdcName : docName)
        .setIcon(IconCompat.createWithBitmap(bitmap)) // createWithAdaptiveBitmap() removes decorations but cuts out a too small circle and defamiliarize the icon too much
        .setIntents(getWebxdcIntentWithParentStack(context, msgId))
        .build();

      if (ShortcutManagerCompat.requestPinShortcut(context, shortcutInfoCompat, null)) {
        Toast.makeText(context, R.string.done, Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(context, "ErrAddToHomescreen: requestPinShortcut() failed", Toast.LENGTH_LONG).show();
      }
    } catch(Exception e) {
      Toast.makeText(context, "ErrAddToHomescreen: " + e, Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public void onActivityResult(int reqCode, int resultCode, final Intent data) {
    if (reqCode == REQUEST_CODE_FILE_PICKER && filePathCallback != null) {
      Uri[] dataUris = null;
      if (resultCode == Activity.RESULT_OK && data != null) {
        try {
          if (data.getDataString() != null) {
            dataUris = new Uri[]{Uri.parse(data.getDataString())};
          } else if (data.getClipData() != null) {
            final int numSelectedFiles = data.getClipData().getItemCount();
            dataUris = new Uri[numSelectedFiles];
            for (int i = 0; i < numSelectedFiles; i++) {
              dataUris[i] = data.getClipData().getItemAt(i).getUri();
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      filePathCallback.onReceiveValue(dataUris);
      filePathCallback = null;
    }
    super.onActivityResult(reqCode, resultCode, data);
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

    @JavascriptInterface
    public String sendToChat(String message) {
      Log.i(TAG, "sendToChat");
      try {
        JSONObject jsonObject = new JSONObject(message);

        String text = null;
        byte[] data = null;
        String name = null;
        if (jsonObject.has("base64")) {
            data = Base64.decode(jsonObject.getString("base64"), Base64.NO_WRAP | Base64.NO_PADDING);
            name = jsonObject.getString("name");
        }
        if (jsonObject.has("text")) {
            text = jsonObject.getString("text");
        }

        if (TextUtils.isEmpty(text) && TextUtils.isEmpty(name)) {
            return "provided file is invalid, you need to set both name and base64 content";
        }

        DcHelper.share(WebxdcActivity.this, data, "application/octet-stream", name, text);
        return null;
      } catch (Exception e) {
        e.printStackTrace();
        return e.toString();
      }
    }
  }
}
