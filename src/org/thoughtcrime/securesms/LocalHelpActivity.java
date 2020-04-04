package org.thoughtcrime.securesms;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;

public class LocalHelpActivity extends PassphraseRequiredActionBarActivity
                               implements SearchView.OnQueryTextListener,
                                          WebView.FindListener
{
    private WebView webView;
    private final DynamicTheme    dynamicTheme    = new DynamicTheme();
    private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

    @Override
    protected void onPreCreate() {
        dynamicTheme.onCreate(this);
        dynamicLanguage.onCreate(this);
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
            ;
        }
        return exists;
    }

    @Override
    protected void onCreate(Bundle state, boolean ready) {
        setContentView(R.layout.local_help_activity);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

        webView = findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient(){
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    openOnlineUrl(url);
                    return true;
                }
                return false;
            }
        });
        webView.loadUrl("file:///android_asset/" + helpPath.replace("LANG", helpLang));
        webView.setFindListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        dynamicTheme.onResume(this);
        dynamicLanguage.onResume(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        menu.clear();

        inflater.inflate(R.menu.local_help, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_search_localhelp);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);

        super.onPrepareOptionsMenu(menu);
        return true;
    }

    private String lastQuery = "";
    private Toast lastToast = null;

    @Override
    public boolean onQueryTextSubmit(String query) {
        webView.findNext(true);
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
        if (isDoneCounting && numberOfMatches==0 && !lastQuery.isEmpty()) {
            String msg = getString(R.string.search_no_result_for_x, lastQuery);
            if (lastToast!=null) {
                lastToast.cancel();
            }
            lastToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
            lastToast.show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
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

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void openOnlineUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_browser_installed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, final Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
    }

}
