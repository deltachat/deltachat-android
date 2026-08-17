package org.thoughtcrime.securesms;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.RequiresApi;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.thoughtcrime.securesms.util.Util;

/**
 * Holds the whole per-renderer RTCPeerConnection budget in a hidden WebView, so that no webxdc can
 * use WebRTC.
 *
 * <p>WebView gives an app process a single renderer process, and Chromium's limit of 500
 * RTCPeerConnections is enforced per renderer. Allocating all of them here blocks WebRTC in every
 * WebView of this app.
 */
public final class WebRtcHolder {

  private static final String TAG = "WebRtcHolder";

  // RTCPeerConnection is only available in secure contexts
  private static final String HOLDER_URL = "https://webrtc-holder.localhost/holder.html";

  private static final long WAITER_TIMEOUT_MS = 15000;

  public enum State {
    IDLE,
    FILLING,
    CONFIRMED,
    EMPTY,
  }

  // This is the mechanism itself. Also, there is no actual leak:
  // the non-static classes only capture the singleton.
  @SuppressLint("StaticFieldLeak")
  private static WebRtcHolder instance;

  private final Context appContext;
  private final List<Runnable> waiters = new ArrayList<>();
  private volatile WebView webView;
  private volatile State state = State.IDLE;

  private WebRtcHolder(Context context) {
    this.appContext = context.getApplicationContext();
  }

  public static synchronized WebRtcHolder getInstance(Context context) {
    if (instance == null) {
      instance = new WebRtcHolder(context);
    }
    return instance;
  }

  public void start() {
    Util.assertMainThread();
    if (state != State.IDLE) {
      return;
    }
    Log.i(TAG, "Starting fill");
    state = State.FILLING;
    createWebView();
    webView.loadUrl(HOLDER_URL);
  }

  public State getState() {
    if (!Util.isMainThread()) {
      Log.w(TAG, "getState() off main thread, result may be stale");
    }
    return state;
  }

  private boolean isSettled() {
    return state == State.CONFIRMED || state == State.EMPTY;
  }

  public void awaitSettled(final Runnable callback) {
    if (!Util.isMainThread()) {
      Util.runOnMain(this::start);
      return;
    }
    if (isSettled()) {
      callback.run();
      return;
    }
    final boolean[] fired = new boolean[1];
    final Runnable once =
        () -> {
          if (!fired[0]) {
            fired[0] = true;
            callback.run();
          }
        };
    waiters.add(once);
    Util.runOnMainDelayed(
        () -> {
          if (!fired[0]) {
            Log.w(TAG, "timed out waiting for holder");
          }
          once.run();
        },
        WAITER_TIMEOUT_MS);
  }

  public void onRendererGone() {
    if (!Util.isMainThread()) {
      Util.runOnMain(this::start);
      return;
    }
    Log.w(TAG, "Renderer gone");
    destroyWebView();
    state = State.IDLE;
    start();
  }

  private void createWebView() {
    destroyWebView();

    WebView wv = new WebView(appContext);
    WebSettings settings = wv.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setBlockNetworkLoads(true);
    settings.setAllowFileAccess(false);
    settings.setAllowContentAccess(false);
    settings.setGeolocationEnabled(false);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      settings.setSafeBrowsingEnabled(false);
    }
    wv.setWebViewClient(new HolderWebViewClient());
    wv.addJavascriptInterface(new HolderJSApi(), "HolderJSApi");
    webView = wv;
  }

  private void destroyWebView() {
    if (webView != null) {
      webView.destroy();
      webView = null;
    }
  }

  private void handleFillFinished(String result, String reason) {
    if (state != State.FILLING) {
      return;
    }
    if ("confirmed".equals(result)) {
      state = State.CONFIRMED;
      Log.i(TAG, "Confirmed, holding the whole budget");
    } else {
      state = State.EMPTY;
      Log.w(TAG, "Released: " + reason);
      destroyWebView();
    }
    List<Runnable> pending = new ArrayList<>(waiters);
    waiters.clear();
    for (Runnable waiter : pending) {
      waiter.run();
    }
  }

  private WebResourceResponse serve(String url) {
    if (url != null && url.startsWith(HOLDER_URL)) {
      InputStream stream = appContext.getResources().openRawResource(R.raw.webrtc_holder);
      return new WebResourceResponse("text/html", "UTF-8", stream);
    }
    Log.w(TAG, "Blocked unexpected request from holder: " + url);
    return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
  }

  private class HolderWebViewClient extends WebViewClient {
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
      return serve(request.getUrl().toString());
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.O)
    public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
      Log.w(TAG, "Holder renderer gone, didCrash=" + detail.didCrash());
      Util.runOnMainDelayed(WebRtcHolder.this::onRendererGone, 0);
      return true;
    }
  }

  private class HolderJSApi {
    @JavascriptInterface
    public void onFillFinished(final String result, final String reason) {
      Util.runOnMain(() -> handleFillFinished(result, reason));
    }
  }
}
