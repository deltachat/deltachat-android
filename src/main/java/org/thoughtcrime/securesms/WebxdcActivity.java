package org.thoughtcrime.securesms;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;
import chat.delta.rpc.types.WebxdcMessageInfo;
import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcMsg;
import com.google.common.base.Charsets;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONObject;
import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.LocalNetworkPermission;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.IntentUtils;
import org.thoughtcrime.securesms.util.JsonUtils;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.webxdc.WebxdcPermissions;

public class WebxdcActivity extends WebViewActivity implements DcEventCenter.DcEventDelegate {
  private static final String TAG = "WebxdcActivity";
  private static final String EXTRA_ACCOUNT_ID = "accountId";
  private static final String EXTRA_CHAT_ID = "chatId";
  private static final String EXTRA_APP_MSG_ID = "appMessageId";
  private static final String EXTRA_HIDE_ACTION_BAR = "hideActionBar";
  private static final String EXTRA_HREF = "href";
  private static final int REQUEST_CODE_FILE_PICKER = 51426;

  private ValueCallback<Uri[]> filePathCallback;
  private DcContext dcContext;
  private Rpc rpc;
  private int chatId;
  private DcMsg dcAppMsg;
  private String baseURL;
  private String sourceCodeUrl = "";
  private String selfAddr;
  private boolean isAppSender;
  private boolean isBroadcast;
  private int sendUpdateMaxSize;
  private int sendUpdateInterval;
  private boolean internetAccess = false;
  private boolean hideActionBar = false;
  private String appName = "";
  private boolean sensorsAllowed = false;
  private final ArrayDeque<PermissionRequest> permissionRequestQueue = new ArrayDeque<>();
  private PermissionRequest currentPermissionRequest;
  private GeolocationPermissions.Callback pendingGeoCallback;
  private String pendingGeoOrigin;
  private AlertDialog permissionDialog;

  private TextToSpeech tts;

  public static void openMaps(Context context, int chatId) {
    DcContext dcContext = DcHelper.getContext(context);
    int msgId = dcContext.initWebxdcIntegration(chatId);
    if (msgId == 0) {
      try {
        InputStream inputStream = context.getResources().getAssets().open("webxdc/maps.xdc");
        String outputFile = DcHelper.getBlobdirFile(dcContext, "maps", ".xdc");
        Util.copy(inputStream, new FileOutputStream(outputFile));
        dcContext.setWebxdcIntegration(outputFile);
        msgId = dcContext.initWebxdcIntegration(chatId);
      } catch (IOException e) {
        e.printStackTrace();
      }
      if (msgId == 0) {
        Toast.makeText(context, "Cannot get maps.xdc, see log for details.", Toast.LENGTH_LONG)
            .show();
        return;
      }
    }
    openWebxdcActivity(context, msgId, chatId, true, "");
  }

  public static void openWebxdcActivity(Context context, DcMsg instance) {
    openWebxdcActivity(context, instance, "");
  }

  public static void openWebxdcActivity(Context context, @NonNull DcMsg instance, String href) {
    openWebxdcActivity(context, instance.getId(), instance.getChatId(), false, href);
  }

  public static void openWebxdcActivity(
      Context context, int msgId, int chatId, boolean hideActionBar, String href) {
    if (!Util.isClickedRecently()) {
      context.startActivity(getWebxdcIntent(context, msgId, chatId, hideActionBar, href));
    }
  }

  private static Intent getWebxdcIntent(
      Context context, int msgId, int chatId, boolean hideActionBar, String href) {
    DcContext dcContext = DcHelper.getContext(context);
    Intent intent = new Intent(context, WebxdcActivity.class);
    intent.setAction(Intent.ACTION_VIEW);
    intent.putExtra(EXTRA_ACCOUNT_ID, dcContext.getAccountId());
    intent.putExtra(EXTRA_CHAT_ID, chatId);
    intent.putExtra(EXTRA_APP_MSG_ID, msgId);
    intent.putExtra(EXTRA_HIDE_ACTION_BAR, hideActionBar);
    intent.putExtra(EXTRA_HREF, href);
    return intent;
  }

  private static Intent[] getWebxdcIntentWithParentStack(Context context, int msgId) {
    DcContext dcContext = DcHelper.getContext(context);

    int chatId = dcContext.getMsg(msgId).getChatId();
    final Intent chatIntent =
        new Intent(context, ConversationActivity.class)
            .putExtra(ConversationActivity.CHAT_ID_EXTRA, chatId)
            .setAction(Intent.ACTION_VIEW);

    final Intent webxdcIntent = getWebxdcIntent(context, msgId, chatId, false, "");

    return TaskStackBuilder.create(context)
        .addNextIntentWithParentStack(chatIntent)
        .addNextIntent(webxdcIntent)
        .getIntents();
  }

  private String buildBootstrapUrl(boolean blockedByHolder, String encodedHref) {
    return this.baseURL
        + "/webxdc_bootstrap324567869.html?i="
        + (internetAccess ? "1" : "0")
        + "&h="
        + (blockedByHolder ? "1" : "0")
        + "&href="
        + encodedHref;
  }

  @Override
  protected boolean immersiveMode() {
    return hideActionBar;
  }

  @Override
  protected void onCreate(Bundle state, boolean ready) {
    Bundle b = getIntent().getExtras();
    hideActionBar = b.getBoolean(EXTRA_HIDE_ACTION_BAR, false);

    super.onCreate(state, ready);
    rpc = DcHelper.getRpc(this);
    initTTS();

    getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

    // enter fullscreen mode if necessary,
    // this is needed here because if the app is opened while already in landscape mode,
    // onConfigurationChanged() is not triggered
    setScreenMode(getResources().getConfiguration());

    webView.setWebChromeClient(
        new WebChromeClient() {
          @Override
          @RequiresApi(21)
          public boolean onShowFileChooser(
              WebView webView,
              ValueCallback<Uri[]> filePathCallback,
              WebChromeClient.FileChooserParams fileChooserParams) {
            if (WebxdcActivity.this.filePathCallback != null) {
              WebxdcActivity.this.filePathCallback.onReceiveValue(null);
            }
            WebxdcActivity.this.filePathCallback = filePathCallback;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(
                Intent.EXTRA_ALLOW_MULTIPLE,
                fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
            WebxdcActivity.this.startActivityForResult(
                Intent.createChooser(intent, getString(R.string.select)), REQUEST_CODE_FILE_PICKER);
            return true;
          }

          @Override
          public void onPermissionRequest(PermissionRequest request) {
            WebxdcActivity.this.onWebPermissionRequest(request);
          }

          @Override
          public void onPermissionRequestCanceled(PermissionRequest request) {
            WebxdcActivity.this.onWebPermissionRequestCanceled(request);
          }

          @Override
          public void onGeolocationPermissionsShowPrompt(
              String origin, GeolocationPermissions.Callback callback) {
            WebxdcActivity.this.onGeolocationShowPrompt(origin, callback);
          }

          @Override
          public void onGeolocationPermissionsHidePrompt() {
            WebxdcActivity.this.onGeolocationHidePrompt();
          }
        });

    int appMessageId = b.getInt(EXTRA_APP_MSG_ID);
    int accountId = b.getInt(EXTRA_ACCOUNT_ID);
    this.dcContext = DcHelper.getContext(getApplicationContext());
    if (accountId != dcContext.getAccountId()) {
      AccountManager.getInstance().switchAccount(getApplicationContext(), accountId);
      this.dcContext = DcHelper.getContext(getApplicationContext());
    }

    this.dcAppMsg = this.dcContext.getMsg(appMessageId);
    if (!this.dcAppMsg.isOk()) {
      Toast.makeText(this, "Webxdc does no longer exist.", Toast.LENGTH_LONG).show();
      finish();
      return;
    }
    chatId = b.getInt(EXTRA_CHAT_ID, dcAppMsg.getChatId());

    DcEventCenter eventCenter =
        DcHelper.getEventCenter(WebxdcActivity.this.getApplicationContext());
    eventCenter.addObserver(DcContext.DC_EVENT_WEBXDC_STATUS_UPDATE, this);
    eventCenter.addObserver(DcContext.DC_EVENT_MSGS_CHANGED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_MSG_DELETED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_CHAT_DELETED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_WEBXDC_REALTIME_DATA, this);

    // `msg_id` in the subdomain makes sure, different apps using same files do not share the same
    // cache entry
    // (WebView may use a global cache shared across objects).
    // (a random-id would also work, but would need maintenance and does not add benefits as we
    // regard the file-part interceptRequest() only,
    // also a random-id is not that useful for debugging)
    this.baseURL = "https://acc" + accountId + "-msg" + appMessageId + ".localhost";

    WebxdcMessageInfo info;
    try {
      info = rpc.getWebxdcInfo(accountId, appMessageId);
      internetAccess = info.internetAccess;
      selfAddr = info.selfAddr;
      isAppSender = info.isAppSender;
      isBroadcast = info.isBroadcast;
      sendUpdateMaxSize = info.sendUpdateMaxSize;
      sendUpdateInterval = info.sendUpdateInterval;
      appName = info.name;
      sensorsAllowed =
          WebxdcPermissions.get(
                  this, accountId, appMessageId, WebxdcPermissions.Capability.MOTION_SENSORS)
              == WebxdcPermissions.State.ALLOWED;
    } catch (RpcException e) { // unexpected error, log it and finish
      Log.e(TAG, "RPC Error", e);
      finish();
      return;
    }

    toggleFakeProxy(!internetAccess);

    WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setMediaPlaybackRequiresUserGesture(false);
    webSettings.setAllowFileAccess(false);
    webSettings.setBlockNetworkLoads(!internetAccess);
    webSettings.setAllowContentAccess(false);
    webSettings.setGeolocationEnabled(true);
    webSettings.setAllowFileAccessFromFileURLs(false);
    webSettings.setAllowUniversalAccessFromFileURLs(false);
    webSettings.setDatabaseEnabled(true);
    webSettings.setDomStorageEnabled(true);
    webView.setNetworkAvailable(
        internetAccess); // this does not block network but sets `window.navigator.isOnline` in js
    // land
    webView.addJavascriptInterface(new InternalJSApi(), "InternalJSApi");

    String extraHref = b.getString(EXTRA_HREF, "");
    if (TextUtils.isEmpty(extraHref)) {
      extraHref = "index.html";
    }

    String href = baseURL + "/" + extraHref;
    String encodedHref = "";
    try {
      encodedHref = URLEncoder.encode(href, Charsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    final String finalEncodedHref = encodedHref;

    if (internetAccess) {
      webView.loadUrl(buildBootstrapUrl(false, finalEncodedHref));
    } else {
      // Wait until the state of the RTCPeerConnection budget is settled.
      final WebRtcHolder holder = WebRtcHolder.getInstance(this);
      holder.awaitSettled(
          () -> {
            if (isFinishing() || isDestroyed() || webView == null) {
              return;
            }
            if (!holder.isSettled()) {
              Log.e(TAG, "Cannot block WebRTC (" + holder.getState() + "), refusing to load");
              finish();
              return;
            }
            webView.loadUrl(
                buildBootstrapUrl(
                    holder.getState() == WebRtcHolder.State.CONFIRMED, finalEncodedHref));
          });
    }

    Util.runOnAnyBackgroundThread(
        () -> {
          final DcChat chat = dcContext.getChat(dcAppMsg.getChatId());
          Util.runOnMain(
              () -> {
                updateTitleAndMenu(info, chat);
              });
          WebxdcPermissions.pruneStaleOnce(getApplicationContext(), rpc);
        });
  }

  @Override
  public void onResume() {
    super.onResume();
    DcHelper.getNotificationCenter(this)
        .updateVisibleWebxdc(dcContext.getAccountId(), dcAppMsg.getId());
  }

  @Override
  protected void onPause() {
    super.onPause();
    DcHelper.getNotificationCenter(this).clearVisibleWebxdc();
  }

  @Override
  protected void onDestroy() {
    DcHelper.getEventCenter(this.getApplicationContext()).removeObservers(this);
    dismissPermissionDialog();
    for (PermissionRequest request : permissionRequestQueue) {
      request.deny();
    }
    permissionRequestQueue.clear();
    currentPermissionRequest = null;
    answerGeoCallback(false);
    leaveRealtimeChannel();
    tts.shutdown();
    super.onDestroy();
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    // do not call super.onPrepareOptionsMenu() as the default "Search" menu is not needed
    menu.clear();
    this.getMenuInflater().inflate(R.menu.webxdc, menu);
    menu.findItem(R.id.source_code).setVisible(!sourceCodeUrl.isEmpty());
    boolean isDraft = dcAppMsg.getState() == DcMsg.DC_STATE_OUT_DRAFT;
    menu.findItem(R.id.menu_add_to_home_screen).setVisible(!isDraft);
    menu.findItem(R.id.show_in_chat).setVisible(!isDraft);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    int itemId = item.getItemId();
    if (itemId == R.id.menu_add_to_home_screen) {
      addToHomeScreen(this, dcAppMsg.getId());
      return true;
    } else if (itemId == R.id.webxdc_help) {
      DcHelper.openHelp(this, "#webxdc");
    } else if (itemId == R.id.source_code) {
      IntentUtils.showInBrowser(this, sourceCodeUrl);
      return true;
    } else if (itemId == R.id.show_in_chat) {
      showInChat();
      return true;
    } else if (itemId == R.id.webxdc_reset_permissions) {
      WebxdcPermissions.clear(this, dcContext.getAccountId(), dcAppMsg.getId());
      sensorsAllowed = false;
      Toast.makeText(this, R.string.done, Toast.LENGTH_SHORT).show();
      webView.reload();
      return true;
    }
    return false;
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    Log.i(TAG, "onConfigurationChanged(" + newConfig.orientation + ")");
    super.onConfigurationChanged(newConfig);
    // orientation might have changed, enter/exit fullscreen mode if needed
    setScreenMode(newConfig);
  }

  private void initTTS() {
    tts =
        new TextToSpeech(
            this,
            new TextToSpeech.OnInitListener() {
              @Override
              public void onInit(int status) {
                Log.i(TAG, "TTS Init Status: " + status);
              }
            });
  }

  private void setScreenMode(Configuration config) {
    // enter/exit fullscreen mode depending on orientation (landscape/portrait),
    // on tablets there is enough height so fullscreen mode is never enabled there
    boolean enable =
        config.orientation == Configuration.ORIENTATION_LANDSCAPE
            && !getResources().getBoolean(R.bool.isBigScreen);
    getWindow().getDecorView().setSystemUiVisibility(enable ? View.SYSTEM_UI_FLAG_FULLSCREEN : 0);
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      if (hideActionBar || enable) {
        actionBar.hide();
      } else {
        actionBar.show();
      }
    }
  }

  @Override
  protected boolean shouldAskToOpenLink() {
    return true;
  }

  // This is usually only called when internetAccess == true or for mailto/openpgp4fpr scheme,
  // because when internetAccess == false, the page is loaded inside an iframe,
  // and WebViewClient.shouldOverrideUrlLoading is not called for HTTP(S) links inside the iframe
  // unless target=_blank is used
  @Override
  protected boolean openOnlineUrl(String url) {
    Log.i(TAG, "openOnlineUrl: " + url);

    // if there is internet access, allow internal loading of http
    if (internetAccess && url.startsWith("http")) {
      // returning `false` continues loading in WebView; returning `true` let WebView abort loading
      return false;
    }

    return super.openOnlineUrl(url);
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
      } else if (path.equalsIgnoreCase(
          "/sandboxed_iframe_rtcpeerconnection_check_5965668501706.html")) {
        InputStream targetStream =
            getResources().openRawResource(R.raw.sandboxed_iframe_rtcpeerconnection_check);
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
            case "js":
              mimeType = "text/javascript";
              break;
            case "wasm":
              mimeType = "application/wasm";
              break;
            default:
              mimeType = "application/octet-stream";
              Log.i(TAG, "unknown mime type for " + rawUrl);
              break;
          }
        }
        String encoding = mimeType.startsWith("text/") ? "UTF-8" : null;
        InputStream targetStream = new ByteArrayInputStream(blob);
        res = new WebResourceResponse(mimeType, encoding, targetStream);
      }
    } catch (Exception e) {
      e.printStackTrace();
      InputStream targetStream =
          new ByteArrayInputStream(("Webxdc Request Error: " + e.getMessage()).getBytes());
      res = new WebResourceResponse("text/plain", "UTF-8", targetStream);
    }

    Map<String, String> headers = createHeaders();
    if (!headers.isEmpty()) {
      res.setResponseHeaders(headers);
    }
    return res;
  }

  @NonNull
  private Map<String, String> createHeaders() {
    Map<String, String> headers = new HashMap<>();
    if (!internetAccess) {
      headers.put(
          "Content-Security-Policy",
          "default-src 'self'; "
              + "style-src 'self' 'unsafe-inline' blob: ; "
              + "font-src 'self' data: blob: ; "
              + "script-src 'self' 'unsafe-inline' 'unsafe-eval' blob: ; "
              + "connect-src 'self' data: blob: ; "
              + "img-src 'self' data: blob: ; "
              + "media-src 'self' data: blob: ;"
              + "webrtc 'block' ; ");
      headers.put("X-DNS-Prefetch-Control", "off");
    }
    if (!sensorsAllowed) {
      headers.put(
          "Permissions-Policy",
          "accelerometer=(), gyroscope=(), magnetometer=(), ambient-light-sensor=()");
    }
    return headers;
  }

  private void callJavaScriptFunction(String func) {
    webView.evaluateJavascript(
        "document.getElementById('frame').contentWindow." + func + ";", null);
  }

  private void dismissPermissionDialog() {
    if (permissionDialog != null) {
      permissionDialog.dismiss();
      permissionDialog = null;
    }
  }

  private void showPermissionDialog(
      int messageRes, Runnable onAllow, Runnable onBlock, Runnable onCancel) {
    permissionDialog =
        new AlertDialog.Builder(this)
            .setMessage(getString(messageRes, appName))
            .setPositiveButton(R.string.allow, (dialog, which) -> onAllow.run())
            .setNegativeButton(R.string.block, (dialog, which) -> onBlock.run())
            .setOnCancelListener(dialog -> onCancel.run())
            .show();
  }

  private boolean isAppOrigin(String origin) {
    if (origin == null) {
      return false;
    }
    String normalized = origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
    return baseURL.equals(normalized);
  }

  private void onWebPermissionRequest(PermissionRequest request) {
    permissionRequestQueue.add(request);
    if (currentPermissionRequest == null) {
      processNextPermissionRequest();
    }
  }

  private void onWebPermissionRequestCanceled(PermissionRequest request) {
    if (currentPermissionRequest == request) {
      dismissPermissionDialog();
      finishCurrentPermissionRequest();
    } else {
      permissionRequestQueue.remove(request);
    }
  }

  private void finishCurrentPermissionRequest() {
    permissionRequestQueue.poll();
    currentPermissionRequest = null;
    if (!permissionRequestQueue.isEmpty()) {
      processNextPermissionRequest();
    }
  }

  private void processNextPermissionRequest() {
    final PermissionRequest request = permissionRequestQueue.peek();
    currentPermissionRequest = request;
    if (request == null) {
      return;
    }
    final int accountId = dcContext.getAccountId();
    final int msgId = dcAppMsg.getId();

    Uri origin = request.getOrigin();
    if (!isAppOrigin(origin == null ? null : origin.toString())) {
      Log.w(TAG, "Denying permission request from unexpected origin: " + origin);
      request.deny();
      finishCurrentPermissionRequest();
      return;
    }

    boolean wantsAudio = false;
    boolean wantsVideo = false;
    for (String resource : request.getResources()) {
      if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
        wantsAudio = true;
      } else if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
        wantsVideo = true;
      } else {
        // DRM identifiers, MIDI, WebXR are denied by default
        Log.w(TAG, "Denying unsupported permission resource: " + resource);
        request.deny();
        finishCurrentPermissionRequest();
        return;
      }
    }

    final boolean audio = wantsAudio;
    final boolean video = wantsVideo;
    WebxdcPermissions.State micState =
        WebxdcPermissions.get(this, accountId, msgId, WebxdcPermissions.Capability.MICROPHONE);
    WebxdcPermissions.State camState =
        WebxdcPermissions.get(this, accountId, msgId, WebxdcPermissions.Capability.CAMERA);
    boolean micDenied = audio && micState == WebxdcPermissions.State.DENIED;
    boolean camDenied = video && camState == WebxdcPermissions.State.DENIED;
    if (micDenied || camDenied) {
      request.deny();
      finishCurrentPermissionRequest();
      return;
    }
    boolean micOk = !audio || micState == WebxdcPermissions.State.ALLOWED;
    boolean camOk = !video || camState == WebxdcPermissions.State.ALLOWED;
    if (micOk && camOk) {
      grantCaptureRequest(request, audio, video);
      return;
    }

    int messageRes;
    if (audio && video) {
      messageRes = R.string.webxdc_perm_ask_camera_microphone;
    } else if (audio) {
      messageRes = R.string.webxdc_perm_ask_microphone;
    } else {
      messageRes = R.string.webxdc_perm_ask_camera;
    }
    showPermissionDialog(
        messageRes,
        () -> {
          if (audio) {
            WebxdcPermissions.set(
                this,
                accountId,
                msgId,
                WebxdcPermissions.Capability.MICROPHONE,
                WebxdcPermissions.State.ALLOWED);
          }
          if (video) {
            WebxdcPermissions.set(
                this,
                accountId,
                msgId,
                WebxdcPermissions.Capability.CAMERA,
                WebxdcPermissions.State.ALLOWED);
          }
          grantCaptureRequest(request, audio, video);
        },
        () -> {
          if (audio) {
            WebxdcPermissions.set(
                this,
                accountId,
                msgId,
                WebxdcPermissions.Capability.MICROPHONE,
                WebxdcPermissions.State.DENIED);
          }
          if (video) {
            WebxdcPermissions.set(
                this,
                accountId,
                msgId,
                WebxdcPermissions.Capability.CAMERA,
                WebxdcPermissions.State.DENIED);
          }
          request.deny();
          finishCurrentPermissionRequest();
        },
        () -> {
          request.deny();
          finishCurrentPermissionRequest();
        });
  }

  private void grantCaptureRequest(
      final PermissionRequest request, final boolean audio, final boolean video) {
    List<String> permissions = new ArrayList<>();
    if (audio) {
      permissions.add(Manifest.permission.RECORD_AUDIO);
    }
    if (video) {
      permissions.add(Manifest.permission.CAMERA);
    }
    Permissions.with(this)
        .request(permissions.toArray(new String[0]))
        .ifNecessary()
        .withPermanentDenialDialog(getString(R.string.webxdc_perm_denied_settings))
        .onAnyResult(
            () -> {
              if (isFinishing() || isDestroyed()) {
                request.deny();
                finishCurrentPermissionRequest();
                return;
              }
              int accountId = dcContext.getAccountId();
              int msgId = dcAppMsg.getId();
              List<String> granted = new ArrayList<>();
              if (audio) {
                if (Permissions.hasAll(this, Manifest.permission.RECORD_AUDIO)) {
                  granted.add(PermissionRequest.RESOURCE_AUDIO_CAPTURE);
                } else {
                  WebxdcPermissions.set(
                      this,
                      accountId,
                      msgId,
                      WebxdcPermissions.Capability.MICROPHONE,
                      WebxdcPermissions.State.ASK);
                }
              }
              if (video) {
                if (Permissions.hasAll(this, Manifest.permission.CAMERA)) {
                  granted.add(PermissionRequest.RESOURCE_VIDEO_CAPTURE);
                } else {
                  WebxdcPermissions.set(
                      this,
                      accountId,
                      msgId,
                      WebxdcPermissions.Capability.CAMERA,
                      WebxdcPermissions.State.ASK);
                }
              }
              if (granted.isEmpty()) {
                request.deny();
              } else {
                request.grant(granted.toArray(new String[0]));
              }
              finishCurrentPermissionRequest();
            })
        .execute();
  }

  private void onGeolocationShowPrompt(
      final String origin, final GeolocationPermissions.Callback callback) {
    if (!isAppOrigin(origin)) {
      Log.w(TAG, "Denying geolocation request from unexpected origin: " + origin);
      callback.invoke(origin, false, false);
      return;
    }
    if (pendingGeoCallback != null) {
      callback.invoke(origin, false, false);
      return;
    }
    final int accountId = dcContext.getAccountId();
    final int msgId = dcAppMsg.getId();
    WebxdcPermissions.State state =
        WebxdcPermissions.get(this, accountId, msgId, WebxdcPermissions.Capability.LOCATION);
    if (state == WebxdcPermissions.State.DENIED) {
      callback.invoke(origin, false, false);
      return;
    }
    if (state == WebxdcPermissions.State.ALLOWED) {
      requestLocationPermission(origin, callback);
      return;
    }
    pendingGeoCallback = callback;
    pendingGeoOrigin = origin;
    showPermissionDialog(
        R.string.webxdc_perm_ask_location,
        () -> {
          WebxdcPermissions.set(
              this,
              accountId,
              msgId,
              WebxdcPermissions.Capability.LOCATION,
              WebxdcPermissions.State.ALLOWED);
          requestLocationPermission(origin, callback);
        },
        () -> {
          WebxdcPermissions.set(
              this,
              accountId,
              msgId,
              WebxdcPermissions.Capability.LOCATION,
              WebxdcPermissions.State.DENIED);
          answerGeoCallback(false);
        },
        () -> answerGeoCallback(false));
  }

  private void requestLocationPermission(
      final String origin, final GeolocationPermissions.Callback callback) {
    pendingGeoCallback = callback;
    pendingGeoOrigin = origin;
    if (Permissions.hasAny(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION)) {
      answerGeoCallback(true);
      return;
    }
    Permissions.with(this)
        .request(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .ifNecessary()
        .withPermanentDenialDialog(getString(R.string.webxdc_perm_denied_settings))
        .onAnyResult(
            () -> {
              boolean granted =
                  Permissions.hasAny(
                      this,
                      Manifest.permission.ACCESS_FINE_LOCATION,
                      Manifest.permission.ACCESS_COARSE_LOCATION);
              if (!granted) {
                WebxdcPermissions.set(
                    this,
                    dcContext.getAccountId(),
                    dcAppMsg.getId(),
                    WebxdcPermissions.Capability.LOCATION,
                    WebxdcPermissions.State.ASK);
              }
              answerGeoCallback(granted);
            })
        .execute();
  }

  private void answerGeoCallback(boolean granted) {
    if (pendingGeoCallback != null) {
      final GeolocationPermissions.Callback callback = pendingGeoCallback;
      final String origin = pendingGeoOrigin;
      pendingGeoCallback = null;
      pendingGeoOrigin = null;
      webView.post(() -> callback.invoke(origin, granted, false));
    }
  }

  private void onGeolocationHidePrompt() {
    dismissPermissionDialog();
    answerGeoCallback(false);
  }

  private void onSensorPermissionRequested() {
    final int accountId = dcContext.getAccountId();
    final int msgId = dcAppMsg.getId();
    WebxdcPermissions.State state =
        WebxdcPermissions.get(this, accountId, msgId, WebxdcPermissions.Capability.MOTION_SENSORS);
    if (state == WebxdcPermissions.State.ALLOWED) {
      sensorsAllowed = true;
      callJavaScriptFunction("__webxdcSensorPermission(true)");
      return;
    }
    if (state == WebxdcPermissions.State.DENIED) {
      callJavaScriptFunction("__webxdcSensorPermission(false)");
      return;
    }
    showPermissionDialog(
        R.string.webxdc_perm_ask_sensors,
        () -> {
          WebxdcPermissions.set(
              this,
              accountId,
              msgId,
              WebxdcPermissions.Capability.MOTION_SENSORS,
              WebxdcPermissions.State.ALLOWED);
          sensorsAllowed = true;
          callJavaScriptFunction("__webxdcSensorPermission(true)");
        },
        () -> {
          WebxdcPermissions.set(
              this,
              accountId,
              msgId,
              WebxdcPermissions.Capability.MOTION_SENSORS,
              WebxdcPermissions.State.DENIED);
          callJavaScriptFunction("__webxdcSensorPermission(false)");
        },
        () -> callJavaScriptFunction("__webxdcSensorPermission(false)"));
  }

  @Override
  public void handleEvent(@NonNull DcEvent event) {
    int eventId = event.getId();
    if ((eventId == DcContext.DC_EVENT_WEBXDC_STATUS_UPDATE
        && event.getData1Int() == dcAppMsg.getId())) {
      Log.i(TAG, "handling status update event");
      callJavaScriptFunction("__webxdcUpdate()");
    } else if ((eventId == DcContext.DC_EVENT_WEBXDC_REALTIME_DATA
        && event.getData1Int() == dcAppMsg.getId())) {
      Log.i(TAG, "handling realtime data event");
      StringBuilder data = new StringBuilder();
      for (byte b : event.getData2Blob()) {
        data.append(((int) b) + ",");
      }
      callJavaScriptFunction("__webxdcRealtimeData([" + data + "])");
    } else if ((eventId == DcContext.DC_EVENT_MSGS_CHANGED
        && event.getData2Int() == dcAppMsg.getId())) {
      this.dcAppMsg =
          this.dcContext.getMsg(event.getData2Int()); // msg changed, reload data from db
      Util.runOnAnyBackgroundThread(
          () -> {
            try {
              final WebxdcMessageInfo info =
                  rpc.getWebxdcInfo(dcContext.getAccountId(), dcAppMsg.getId());
              final DcChat chat = dcContext.getChat(dcAppMsg.getChatId());
              Util.runOnMain(() -> updateTitleAndMenu(info, chat));
            } catch (RpcException e) {
              Log.e(TAG, "RPC Error", e);
            }
          });
    } else if ((eventId == DcContext.DC_EVENT_MSG_DELETED
            && event.getData2Int() == dcAppMsg.getId())
        || (eventId == DcContext.DC_EVENT_CHAT_DELETED && event.getData1Int() == chatId)) {
      WebxdcPermissions.clear(this, dcContext.getAccountId(), dcAppMsg.getId());
      finish();
    }
  }

  private void updateTitleAndMenu(WebxdcMessageInfo info, DcChat chat) {
    final String title;
    if (dcAppMsg.getState() == DcMsg.DC_STATE_OUT_DRAFT) {
      title = getString(R.string.draft);
    } else {
      final String docName = TextUtils.isEmpty(info.document) ? info.name : info.document;
      title = docName + " – " + chat.getName();
    }
    getSupportActionBar().setTitle(title);

    String currSourceCodeUrl = info.sourceCodeUrl != null ? info.sourceCodeUrl : "";
    if (!sourceCodeUrl.equals(currSourceCodeUrl)) {
      sourceCodeUrl = currSourceCodeUrl;
      invalidateOptionsMenu();
    }
  }

  private void showInChat() {
    Intent intent = new Intent(this, ConversationActivity.class);
    intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, dcAppMsg.getChatId());
    intent.putExtra(
        ConversationActivity.STARTING_POSITION_EXTRA,
        DcMsg.getMessagePosition(dcAppMsg, dcContext));
    startActivity(intent);
  }

  public static void addToHomeScreen(Activity activity, int msgId) {
    Context context = activity.getApplicationContext();
    try {
      DcContext dcContext = DcHelper.getContext(context);
      Rpc rpc = DcHelper.getRpc(context);
      int accountId = dcContext.getAccountId();
      DcMsg msg = dcContext.getMsg(msgId);
      WebxdcMessageInfo info = rpc.getWebxdcInfo(accountId, msgId);

      final String docName = TextUtils.isEmpty(info.document) ? info.name : info.document;
      byte[] blob = msg.getWebxdcBlob(info.icon);
      ByteArrayInputStream is = new ByteArrayInputStream(blob);
      BitmapDrawable drawable = (BitmapDrawable) Drawable.createFromStream(is, "icon");
      Bitmap bitmap = drawable.getBitmap();

      ShortcutInfoCompat shortcutInfoCompat =
          new ShortcutInfoCompat.Builder(context, "xdc-" + accountId + "-" + msgId)
              .setShortLabel(docName)
              .setIcon(
                  IconCompat.createWithBitmap(
                      bitmap)) // createWithAdaptiveBitmap() removes decorations but cuts out a too
              // small circle and defamiliarize the icon too much
              .setIntents(getWebxdcIntentWithParentStack(context, msgId))
              .build();

      Toast.makeText(context, R.string.one_moment, Toast.LENGTH_SHORT).show();
      Util.runOnAnyBackgroundThread(
          () -> {
            boolean success;
            try {
              success = ShortcutManagerCompat.requestPinShortcut(context, shortcutInfoCompat, null);
            } catch (Exception e) {
              Log.e(TAG, "ErrAddToHomescreen: requestPinShortcut() failed", e);
              success = false;
            }
            if (!success) {
              Util.runOnMain(
                  () ->
                      Toast.makeText(
                              context,
                              "ErrAddToHomescreen: requestPinShortcut() failed",
                              Toast.LENGTH_LONG)
                          .show());
            }
          });
    } catch (Exception e) {
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
            dataUris = new Uri[] {Uri.parse(data.getDataString())};
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

  private void leaveRealtimeChannel() {
    final Rpc rpc = this.rpc;
    final int accountId = dcContext.getAccountId();
    final int msgId = dcAppMsg.getId();
    Util.runOnAnyBackgroundThread(
        () -> {
          try {
            rpc.leaveWebxdcRealtime(accountId, msgId);
          } catch (RpcException e) {
            e.printStackTrace();
          }
        });
  }

  @NonNull
  private Runnable getAdvertise() {
    final int accountId = WebxdcActivity.this.dcContext.getAccountId();
    final int msgId = WebxdcActivity.this.dcAppMsg.getId();
    return () ->
        Util.runOnAnyBackgroundThread(
            () -> {
              try {
                this.rpc.sendWebxdcRealtimeAdvertisement(accountId, msgId);
              } catch (RpcException e) {
                e.printStackTrace();
              }
            });
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }

  class InternalJSApi {
    @JavascriptInterface
    public int sendUpdateMaxSize() {
      return WebxdcActivity.this.sendUpdateMaxSize;
    }

    @JavascriptInterface
    public int sendUpdateInterval() {
      return WebxdcActivity.this.sendUpdateInterval;
    }

    @JavascriptInterface
    public String selfAddr() {
      return WebxdcActivity.this.selfAddr;
    }

    /**
     * @noinspection unused
     */
    @JavascriptInterface
    public String selfName() {
      String name = dcContext.getConfig(DcHelper.CONFIG_DISPLAY_NAME);
      return name.isEmpty() ? getString(R.string.unnamed) : name;
    }

    @JavascriptInterface
    public boolean isAppSender() {
      return WebxdcActivity.this.isAppSender;
    }

    @JavascriptInterface
    public boolean isBroadcast() {
      return WebxdcActivity.this.isBroadcast;
    }

    /**
     * @noinspection unused
     */
    @JavascriptInterface
    public boolean sendStatusUpdate(String payload) {
      Log.i(TAG, "sendStatusUpdate");
      if (!WebxdcActivity.this.dcContext.sendWebxdcStatusUpdate(
          WebxdcActivity.this.dcAppMsg.getId(), payload)) {
        DcChat dcChat =
            WebxdcActivity.this.dcContext.getChat(WebxdcActivity.this.dcAppMsg.getChatId());
        Toast.makeText(
                WebxdcActivity.this,
                dcChat.isContactRequest()
                    ? WebxdcActivity.this.getString(R.string.accept_request_first)
                    : WebxdcActivity.this.dcContext.getLastError(),
                Toast.LENGTH_LONG)
            .show();
        return false;
      }
      return true;
    }

    /**
     * @noinspection unused
     */
    @JavascriptInterface
    public String getStatusUpdates(int lastKnownSerial) {
      Log.i(TAG, "getStatusUpdates");
      return WebxdcActivity.this.dcContext.getWebxdcStatusUpdates(
          WebxdcActivity.this.dcAppMsg.getId(), lastKnownSerial);
    }

    /**
     * @noinspection unused
     */
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

        DcHelper.sendToChat(WebxdcActivity.this, data, "application/octet-stream", name, text);
        return null;
      } catch (Exception e) {
        e.printStackTrace();
        return e.toString();
      }
    }

    @JavascriptInterface
    public void sendRealtimeAdvertisement() {
      final Runnable doAdvertise = getAdvertise();

      Util.runOnMain(
          () -> {
            if (isFinishing() || isDestroyed()) {
              return;
            }
            if (!LocalNetworkPermission.isNeeded()
                || LocalNetworkPermission.hasPermission(WebxdcActivity.this)
                || Prefs.getBooleanPreference(
                    WebxdcActivity.this, Prefs.ASKED_FOR_LOCAL_NETWORK_PERMISSION, false)) {
              doAdvertise.run();
              return;
            }
            Prefs.setBooleanPreference(
                WebxdcActivity.this, Prefs.ASKED_FOR_LOCAL_NETWORK_PERMISSION, true);
            new AlertDialog.Builder(WebxdcActivity.this)
                .setMessage(R.string.perm_explain_local_network_denied)
                .setPositiveButton(R.string.perm_continue, null)
                .setOnDismissListener(
                    d ->
                        Permissions.with(WebxdcActivity.this)
                            .request(Manifest.permission.ACCESS_LOCAL_NETWORK)
                            .ifNecessary()
                            .onAllGranted(doAdvertise)
                            .onAnyDenied(doAdvertise)
                            .execute())
                .show();
          });
    }

    /**
     * @noinspection unused
     */
    @JavascriptInterface
    public void leaveRealtimeChannel() {
      WebxdcActivity.this.leaveRealtimeChannel();
    }

    /**
     * @noinspection unused
     */
    @JavascriptInterface
    public void sendRealtimeData(String jsonData) {
      int accountId = WebxdcActivity.this.dcContext.getAccountId();
      int msgId = WebxdcActivity.this.dcAppMsg.getId();
      try {
        Integer[] data = JsonUtils.fromJson(jsonData, Integer[].class);
        WebxdcActivity.this.rpc.sendWebxdcRealtimeData(accountId, msgId, Arrays.asList(data));
      } catch (IOException | RpcException e) {
        e.printStackTrace();
      }
    }

    @JavascriptInterface
    public void ttsSpeak(String text, String lang) {
      if (lang != null && !lang.isEmpty()) tts.setLanguage(Locale.forLanguageTag(lang));
      tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @JavascriptInterface
    public String getSensorPermissionState() {
      return WebxdcPermissions.get(
              WebxdcActivity.this,
              WebxdcActivity.this.dcContext.getAccountId(),
              WebxdcActivity.this.dcAppMsg.getId(),
              WebxdcPermissions.Capability.MOTION_SENSORS)
          .name()
          .toLowerCase(Locale.US);
    }

    @JavascriptInterface
    public void requestSensorPermission() {
      Util.runOnMain(WebxdcActivity.this::onSensorPermissionRequested);
    }

    @JavascriptInterface
    public void log(String text) {
      Log.d(TAG, text);
    }
  }
}
