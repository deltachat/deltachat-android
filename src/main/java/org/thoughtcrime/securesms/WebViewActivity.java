package org.thoughtcrime.securesms;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import org.thoughtcrime.securesms.qr.QrCodeHandler;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.IntentUtils;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

import chat.delta.rpc.types.SecurejoinSource;

import java.net.IDN;

public class WebViewActivity extends PassphraseRequiredActionBarActivity
                               implements SearchView.OnQueryTextListener,
                                          WebView.FindListener
{
  private static final String TAG = WebViewActivity.class.getSimpleName();

  protected WebView webView;

  /** Return true the window content should display fullscreen/edge-to-edge ex. in the integrated maps app */
  protected boolean immersiveMode() { return false; }

  protected boolean shouldAskToOpenLink() { return false; }

  protected void toggleFakeProxy(boolean enable) {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
      if (enable) {
        // Set proxy to non-routable address.
        ProxyConfig proxyConfig = new ProxyConfig.Builder()
          .removeImplicitRules()
          .addProxyRule("0.0.0.0")
          .build();
        ProxyController.getInstance().setProxyOverride(proxyConfig, Runnable::run, () -> Log.i(TAG, "Set WebView proxy."));
      } else {
        ProxyController.getInstance().clearProxyOverride(Runnable::run, () -> Log.i(TAG, "Cleared WebView proxy."));
      }
    } else {
      Log.w(TAG, "Cannot " + (enable? "set": "clear") + " WebView proxy.");
    }
  }

  @Override
  protected void onCreate(Bundle state, boolean ready) {
    setContentView(R.layout.web_view_activity);
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    webView = findViewById(R.id.webview);

    if(immersiveMode()) {
      // set a shadow in the status bar to make it more readable
      findViewById(R.id.status_bar_background).setBackgroundResource(R.drawable.search_toolbar_shadow);
    } else {
      // add padding to avoid content hidden behind system bars
      ViewUtil.applyWindowInsets(findViewById(R.id.content_container));
    }

    webView.setWebViewClient(new WebViewClient() {
      // IMPORTANT: this is will likely not be called inside iframes unless target=_blank is used in the anchor/link tag.
      // `shouldOverrideUrlLoading()` is called when the user clicks a URL,
      // returning `true` causes the WebView to abort loading the URL,
      // returning `false` causes the WebView to continue loading the URL as usual.
      // the method is not called for POST request nor for on-page-links.
      //
      // nb: from API 24, `shouldOverrideUrlLoading(String)` is deprecated and
      // `shouldOverrideUrlLoading(WebResourceRequest)` shall be used.
      // the new one has the same functionality, and the old one still exist,
      // so, to support all systems, for now, using the old one seems to be the simplest way.
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url != null) {
          String schema = url.split(":")[0].toLowerCase();
          switch (schema) {
            case "http":
            case "https":
            case "gemini":
            case "tel":
            case "sms":
            case "mailto":
            case "openpgp4fpr":
            case "geo":
            case "dcaccount":
            case "dclogin":
              return openOnlineUrl(url);
          }
        }
        return true; // returning `true` aborts loading
      }

      @Override
      public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        WebResourceResponse res = interceptRequest(url);
        if (res!=null) {
          return res;
        }
        return super.shouldInterceptRequest(view, url);
      }

      @Override
      @RequiresApi(21)
      public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        WebResourceResponse res = interceptRequest(request.getUrl().toString());
        if (res!=null) {
          return res;
        }
        return super.shouldInterceptRequest(view, request);
      }
    });
    webView.setFindListener(this);

    // disable "safe browsing" as this has privacy issues,
    // eg. at least false positives are sent to the "Safe Browsing Lookup API".
    // as all URLs opened in the WebView are local anyway,
    // "safe browsing" will never be able to report issues, so it can be disabled.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      webView.getSettings().setSafeBrowsingEnabled(false);
    }
  }

  protected void setForceDark() {
    if (Build.VERSION.SDK_INT <= 32 && WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
      // needed for older API (tested on android7) that do not set `color-scheme` without the following hint
      WebSettingsCompat.setForceDark(webView.getSettings(),
        DynamicTheme.isDarkTheme(this) ? WebSettingsCompat.FORCE_DARK_ON : WebSettingsCompat.FORCE_DARK_OFF);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    webView.onPause();
  }

  @Override
  public void onResume() {
    super.onResume();
    webView.onResume();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    webView.destroy();
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuInflater inflater = this.getMenuInflater();
    menu.clear();

    inflater.inflate(R.menu.web_view, menu);

    try {
      MenuItem searchItem = menu.findItem(R.id.menu_search_web_view);
      searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
        @Override
        public boolean onMenuItemActionExpand(final MenuItem item) {
          searchMenu = menu;
          WebViewActivity.this.lastQuery = "";
          WebViewActivity.this.makeSearchMenuVisible(menu, searchItem, true);
          return true;
        }

        @Override
        public boolean onMenuItemActionCollapse(final MenuItem item) {
          WebViewActivity.this.makeSearchMenuVisible(menu, searchItem, false);
          return true;
        }
      });
      SearchView searchView = (SearchView) searchItem.getActionView();
      searchView.setOnQueryTextListener(this);
      searchView.setQueryHint(getString(R.string.search));
      searchView.setIconifiedByDefault(true);

      // hide the [X] beside the search field - this is too much noise, search can be aborted eg. by "back"
      ImageView closeBtn = searchView.findViewById(R.id.search_close_btn);
      if (closeBtn!=null) {
        closeBtn.setEnabled(false);
        closeBtn.setImageDrawable(null);
      }
    } catch (Exception e) {
      Log.e(TAG, "cannot set up web-view-search: ", e);
    }

    super.onPrepareOptionsMenu(menu);
    return true;
  }


  // search

  private Menu   searchMenu = null;
  private String lastQuery = "";
  private Toast  lastToast = null;

  private void updateResultCounter(int curr, int total) {
    if (searchMenu!=null) {
      MenuItem item = searchMenu.findItem(R.id.menu_search_counter);
      if (curr!=-1) {
        item.setTitle(String.format("%d/%d", total==0? 0 : curr+1, total));
        item.setVisible(true);
      } else {
        item.setVisible(false);
      }
    }
  }

  @Override
  public boolean onQueryTextSubmit(String query) {
    return true; // action handled by listener
  }

  @Override
  public boolean onQueryTextChange(String query) {
    String normQuery = query.trim();
    lastQuery = normQuery;
    if (lastToast!=null) {
      lastToast.cancel();
      lastToast = null;
    }
    webView.findAllAsync(normQuery);
    return true; // action handled by listener
  }

  @Override
  public void onFindResultReceived (int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting)
  {
    if (isDoneCounting) {
      if (numberOfMatches>0) {
        updateResultCounter(activeMatchOrdinal, numberOfMatches);
      } else {
        if (lastQuery.isEmpty()) {
          updateResultCounter(-1, 0); // hide
        } else {
          String msg = getString(R.string.search_no_result_for_x, lastQuery);
          if (lastToast != null) {
            lastToast.cancel();
          }
          lastToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
          lastToast.show();
          updateResultCounter(0, 0); // show as "0/0"
        }
      }
    }
  }


  // other actions

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    int itemId = item.getItemId();
    if (itemId == android.R.id.home) {
      finish();
      return true;
    } else if (itemId == R.id.menu_search_up) {
      if (lastQuery.isEmpty()) {
        webView.scrollTo(0, 0);
      } else {
        webView.findNext(false);
      }
      return true;
    } else if (itemId == R.id.menu_search_down) {
      if (lastQuery.isEmpty()) {
        webView.scrollTo(0, 1000000000);
      } else {
        webView.findNext(true);
      }
      return true;
    }
    return false;
  }

  // onBackPressed() can be overwritten by derived classes as needed.
  // the default behavior (close the activity) is just fine eg. for Webxdc, Connectivity, HTML-mails

  protected boolean openOnlineUrl(String url) {
    // invite-links should be handled directly
    String schema = url.split(":")[0].toLowerCase();
    if (schema.equals("openpgp4fpr") || url.startsWith("https://" + Util.INVITE_DOMAIN + "/")) {
      new QrCodeHandler(this).handleQrData(url, SecurejoinSource.InternalLink, null);
      return true; // abort internal loading
    }

    if (shouldAskToOpenLink()) {
      new AlertDialog.Builder(this)
        .setTitle(R.string.open_url_confirmation)
        .setMessage(IDN.toASCII(url))
        .setNeutralButton(R.string.cancel, null)
        .setPositiveButton(R.string.open, (d, w) -> IntentUtils.showInBrowser(this, url))
        .setNegativeButton(R.string.global_menu_edit_copy_desktop, (d, w) -> {
          Util.writeTextToClipboard(this, url);
          Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
        })
        .show();
    } else {
      IntentUtils.showInBrowser(this, url);
    }

    // returning `true` causes the WebView to abort loading
    return true;
  }

  protected WebResourceResponse interceptRequest(String url) {
    return null;
  }
}
