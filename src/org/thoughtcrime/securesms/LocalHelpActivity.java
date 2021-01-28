package org.thoughtcrime.securesms;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.io.InputStream;
import java.util.Locale;

public class LocalHelpActivity extends WebViewActivity
{
  @Override
  protected void onCreate(Bundle state, boolean ready) {
    super.onCreate(state, ready);
    getSupportActionBar().setTitle(getString(R.string.menu_help));

    String helpPath = "help/LANG/help.html";
    String helpLang = "en";
    try {
      Locale locale = dynamicLanguage.getCurrentLocale();
      String appLang = locale.getLanguage();
      if (assetExists(helpPath.replace("LANG", appLang))) {
        helpLang = appLang;
      } else {
        appLang = appLang.substring(0, 2);
        if (assetExists(helpPath.replace("LANG", appLang))) {
          helpLang = appLang;
        }
      }
    } catch(Exception e) {
      e.printStackTrace();
    }

    webView.loadUrl("file:///android_asset/" + helpPath.replace("LANG", helpLang));
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
    switch (item.getItemId()) {
      case R.id.log_scroll_up:
        webView.scrollTo(0, 0);
        return true;
      case R.id.learn_more:
        openOnlineUrl("https://delta.chat");
        return true;
      case R.id.contribute:
        openOnlineUrl("https://github.com/deltachat/deltachat-android");
        return true;
      case R.id.report_issue:
        openOnlineUrl("https://github.com/deltachat/deltachat-android/issues");
        return true;
    }
    return false;
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
