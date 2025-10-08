package org.thoughtcrime.securesms;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;

import androidx.appcompat.app.AlertDialog;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.JsonUtils;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;

import java.io.ByteArrayInputStream;
import java.lang.ref.WeakReference;

import chat.delta.rpc.Rpc;
import chat.delta.rpc.types.HttpResponse;

public class FullMsgActivity extends WebViewActivity
{
  public static final String MSG_ID_EXTRA = "msg_id";
  public static final String BLOCK_LOADING_REMOTE = "block_loading_remote";
  private int msgId;
  private DcContext dcContext;
  private Rpc rpc;
  private boolean loadRemoteContent = false;
  private boolean blockLoadingRemote;

  enum LoadRemoteContent {
    NEVER,
    ONCE,
    ALWAYS
  }

  @Override
  protected void onPreCreate() {
    super.onPreCreate();
    toggleFakeProxy(true);
  }

  @Override
  protected void onCreate(Bundle state, boolean ready) {
    super.onCreate(state, ready);

    blockLoadingRemote = getIntent().getBooleanExtra(BLOCK_LOADING_REMOTE, false);
    loadRemoteContent = !blockLoadingRemote && Prefs.getAlwaysLoadRemoteContent(this);
    webView.getSettings().setBlockNetworkLoads(!loadRemoteContent);

    // setBuiltInZoomControls() adds pinch-to-zoom as well as two on-screen zoom control buttons.
    // The latter are a bit annoying, however, they are deprecated anyway,
    // and also Android docs recommend to disable them with setDisplayZoomControls().
    webView.getSettings().setBuiltInZoomControls(true);
    webView.getSettings().setDisplayZoomControls(false);

    // disable useless and unwanted features:
    // - JavaScript and Plugins are disabled by default, however,
    //   doing it explicitly here protects against changed base classes or bugs
    // - Content- and File-access is enabled by default and disabled here
    // - the other setAllow*() functions are related to enabled JavaScript only
    // - "safe browsing" comes with privacy issues and already disabled in the base class
    webView.getSettings().setJavaScriptEnabled(false);
    webView.getSettings().setPluginState(WebSettings.PluginState.OFF);
    webView.getSettings().setAllowContentAccess(false);
    webView.getSettings().setAllowFileAccess(false);

    dcContext = DcHelper.getContext(this);
    rpc = DcHelper.getRpc(this);
    msgId = getIntent().getIntExtra(MSG_ID_EXTRA, 0);
    String title = dcContext.getMsg(msgId).getSubject();
    if (title.isEmpty()) title = getString(R.string.chat_input_placeholder);
    getSupportActionBar().setTitle(title);

    loadHtmlAsync(new WeakReference<>(this));
  }

  private static void loadHtmlAsync(final WeakReference<FullMsgActivity> activityReference) {
    Util.runOnBackground(() -> {
      try {
        FullMsgActivity activity = activityReference.get();
        String html = activity.dcContext.getMsgHtml(activity.msgId);

        activity.runOnUiThread(() -> {
          try {
            // a base URL is needed, otherwise clicking links that reference document sections will not jump to sections
            activityReference.get().webView.loadDataWithBaseURL("file://index.html", html, "text/html", null, null);
          } catch (Exception e) {
            e.printStackTrace();
          }
        });

      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (isFinishing()) {
      overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_right);
    }
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    this.getMenuInflater().inflate(R.menu.full_msg, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    if (item.getItemId() == R.id.load_remote_content) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this)
        .setTitle(R.string.load_remote_content)
        .setMessage(R.string.load_remote_content_ask);

      // we are using the buttons "[Always]  [Never][Once]" in that order.
      // 1. Checkmarks before [Always] and [Never] show the current state.
      // 2. [Once] is also shown in always-mode and disables always-mode if selected
      //    (there was the idea to hide [Once] in always mode, but that looks more like a bug in the end)
      // (maybe a usual Always-Checkbox and "[Cancel][OK]" buttons are an alternative, however, a [Once]
      // would be required as well - probably as the leftmost button which is not that usable in
      // not-always-mode where the dialog is used more often. Or [Ok] would mean "Once" as well as "Change checkbox setting",
      // which is also a bit weird. Anyway, let's give the three buttons a try :)
      final String checkmark = DynamicTheme.getCheckmarkEmoji(this) + " ";
      String alwaysCheckmark = "";
      String onceCheckmark = "";
      String neverCheckmark = "";
      if (!blockLoadingRemote && Prefs.getAlwaysLoadRemoteContent(this)) {
        alwaysCheckmark = checkmark;
      } else if (loadRemoteContent) {
        onceCheckmark = checkmark;
      } else {
        neverCheckmark = checkmark;
      }

      if (!blockLoadingRemote) {
        builder.setNeutralButton(alwaysCheckmark + getString(R.string.always), (dialog, which) -> onChangeLoadRemoteContent(LoadRemoteContent.ALWAYS));
      }
      builder.setNegativeButton(neverCheckmark + getString(blockLoadingRemote ? R.string.no : R.string.never), (dialog, which) -> onChangeLoadRemoteContent(LoadRemoteContent.NEVER));
      builder.setPositiveButton(onceCheckmark + getString(R.string.once), (dialog, which) -> onChangeLoadRemoteContent(LoadRemoteContent.ONCE));

      builder.show();
      return true;
    }
    return false;
  }

  private void onChangeLoadRemoteContent(LoadRemoteContent loadRemoteContent) {
    switch (loadRemoteContent) {
      case NEVER:
        this.loadRemoteContent = false;
        if (!blockLoadingRemote) {
          Prefs.setBooleanPreference(this, Prefs.ALWAYS_LOAD_REMOTE_CONTENT, false);
        }
        break;
      case ONCE:
        this.loadRemoteContent = true;
        if (!blockLoadingRemote) {
          Prefs.setBooleanPreference(this, Prefs.ALWAYS_LOAD_REMOTE_CONTENT, false);
        }
        break;
      case ALWAYS:
        this.loadRemoteContent = true;
        Prefs.setBooleanPreference(this, Prefs.ALWAYS_LOAD_REMOTE_CONTENT, true);
        break;
    }
    webView.getSettings().setBlockNetworkLoads(!this.loadRemoteContent);
    webView.reload();
  }

  @Override
  protected WebResourceResponse interceptRequest(String url) {
    WebResourceResponse res = null;
    try {
      if (!loadRemoteContent) {
        throw new Exception("loading remote content disabled");
      }
      if (url == null) {
        throw new Exception("no url specified");
      }
      HttpResponse httpResponse = rpc.getHttpResponse(dcContext.getAccountId(), url);
      String mimeType = httpResponse.mimetype;
      if (mimeType == null) {
        mimeType = "application/octet-stream";
      }
      byte[] blob = JsonUtils.decodeBase64(httpResponse.blob);
      res = new WebResourceResponse(mimeType, httpResponse.encoding, new ByteArrayInputStream(blob));
    } catch (Exception e) {
      e.printStackTrace();
      res = new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(("Error: " + e.getMessage()).getBytes()));
    }
    return res;
  }
}
