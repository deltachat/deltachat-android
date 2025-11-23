package org.thoughtcrime.securesms;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import org.thoughtcrime.securesms.util.Util;

import java.io.InputStream;
import java.util.Locale;

public class LocalHelpActivity extends WebViewActivity
{
  public static final String SECTION_EXTRA = "section_extra";

  @Override
  protected boolean allowInLockedMode() { return true; }

  @Override
  protected void onCreate(Bundle state, boolean ready) {
    super.onCreate(state, ready);
    setForceDark();
    getSupportActionBar().setTitle(getString(R.string.menu_help));

    String section = getIntent().getStringExtra(SECTION_EXTRA);
    String helpPath = "help/LANG/help.html";
    String helpLang = "en";
    try {
      Locale locale = Util.getLocale();
      String appLang = locale.getLanguage();
      String appCountry = locale.getCountry();
      if (assetExists(helpPath.replace("LANG", appLang))) {
        helpLang = appLang;
      } else if (assetExists(helpPath.replace("LANG", appLang+"_"+appCountry))) {
        helpLang = appLang+"_"+appCountry;
      } else {
        appLang = appLang.substring(0, 2);
        if (assetExists(helpPath.replace("LANG", appLang))) {
          helpLang = appLang;
        }
      }
    } catch(Exception e) {
      e.printStackTrace();
    }

    webView.loadUrl("file:///android_asset/" + helpPath.replace("LANG", helpLang) + (section!=null? section : ""));
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    this.getMenuInflater().inflate(R.menu.local_help, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    int itemId = item.getItemId();
    if (itemId == R.id.log_scroll_up) {
      webView.scrollTo(0, 0);
      return true;
    } else if (itemId == R.id.learn_more) {
      openOnlineUrl("https://delta.chat");
      return true;
    } else if (itemId == R.id.privacy_policy) {
      openOnlineUrl("https://delta.chat/gdpr");
      return true;
    } else if (itemId == R.id.contribute) {
      openOnlineUrl("https://delta.chat/contribute");
      return true;
    } else if (itemId == R.id.report_issue) {
      openOnlineUrl("https://github.com/deltachat/deltachat-android/issues");
      return true;
    }
    return false;
  }

  @Override
  public void onBackPressed() {
    if (webView.canGoBack()) {
      webView.goBack();
    } else {
      super.onBackPressed();
    }
  }

  private boolean assetExists(String fileName) {
    // test using AssetManager.open();
    // AssetManager.list() is unreliable eg. on my Android 7 Moto G
    // and also reported to be pretty slow.
    boolean exists = false;
    try {
      AssetManager assetManager = getResources().getAssets();
      InputStream is = assetManager.open(fileName);
      exists = true;
      is.close();
    } catch(Exception e) {
      ; // a non-existent asset is no error, the function's purpose is to check exactly that.
    }
    return exists;
  }
}
