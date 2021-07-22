package org.thoughtcrime.securesms;

import android.annotation.SuppressLint;
import android.app.Activity;
import java.util.Objects;
import android.os.Bundle;
import java.io.InputStream;
import java.io.IOException;
import android.net.Uri;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.JavascriptInterface;
import android.widget.SearchView;
import android.widget.Toast;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebChromeClient;

import androidx.annotation.NonNull;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;

public class ZhvActivity extends PassphraseRequiredActionBarActivity implements View.OnClickListener {
    private static final String MD_TEXT = "MD_TEXT";
    private static final String FIRST_TIME_INIT = "FirstTimeInit";
    private static final String IS_SEARCHING = "Searching";
    private static final String TITLE_WEB = "TitleWeb";
    private static final int ZHV_VERSION_CODE = 8;
    private boolean firstTimeInit = true;
    private boolean isSearching = false;
    private JsObject jsObject;
    private String titleWEB = "ZHV";
    protected WebView webView;

    private final DynamicTheme dynamicTheme = new DynamicTheme();
    private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

    @Override
    protected void onPreCreate() {
      dynamicTheme.onCreate(this);
      dynamicLanguage.onCreate(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState, boolean ready) {
        super.onCreate(savedInstanceState, ready);
        if (savedInstanceState != null) {
            firstTimeInit = savedInstanceState.getBoolean(FIRST_TIME_INIT, true);
            isSearching = savedInstanceState.getBoolean(IS_SEARCHING, false);
            titleWEB = savedInstanceState.getString(TITLE_WEB, titleWEB);
            String text = savedInstanceState.getString(MD_TEXT, "");
            if (text.equals("")) {
                jsObject = new JsObject("", false);
            } else {
                jsObject = new JsObject(text, true);
            }
        }

        setContentView(R.layout.activity_zhv);
	webView = findViewById(R.id.webview);

        if (firstTimeInit) {
            handleIntent();
        } else {
            registerForContextMenu(webView);
            webView.restoreState(savedInstanceState);
            configureWebView(webView);
            this.setTitle(titleWEB);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void configureWebView(WebView web) {
        WebSettings webSettings = web.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDomStorageEnabled(true);
        web.addJavascriptInterface(jsObject, "zhv");
        web.setWebChromeClient(new WebChromeClient() {

          @Override
          public void onReceivedTitle(WebView view, String title) {
              super.onReceivedTitle(view, title);
              ZhvActivity.this.titleWEB = title;
              ZhvActivity.this.setTitle(title);
          }
        });

	if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
	    if (DynamicTheme.isDarkTheme(this)) {
		WebSettingsCompat.setForceDark(webSettings, WebSettingsCompat.FORCE_DARK_ON);
	    } else {
		WebSettingsCompat.setForceDark(webSettings, WebSettingsCompat.FORCE_DARK_OFF);
	    }
	}
    }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.search_menu, menu);
    final SearchView searchView = (SearchView) menu.findItem(R.id.search_menu_item).getActionView();
    searchView.setImeOptions(EditorInfo.IME_FLAG_NAVIGATE_PREVIOUS | EditorInfo.IME_ACTION_NEXT | EditorInfo.IME_FLAG_NAVIGATE_NEXT);
    searchView.setOnSearchClickListener(this);
    searchView.setSubmitButtonEnabled(true);
    searchView.setOnCloseListener(() -> {
        isSearching = false;
        return false;
    });
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            webView.findNext(true);
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            webView.findAllAsync(newText);
            return true;
        }
    });
    return super.onCreateOptionsMenu(menu);
  }

    @Override
    public void onBackPressed() {
        if (isSearching) {
            SearchView searchView = findViewById(R.id.search_menu_item);
            //if text closes it completly
            searchView.setIconified(true);
            searchView.setIconified(true);
            isSearching = false;
        } else {
            super.onBackPressed();
        }
    }

    private void handleIntent() {
        Uri uri = getIntent().getData();
        if (uri == null) {
            tellUserThatCouldNotOpenFile();
            return;
        }
        String text = null;
        Boolean isMarkdown = false;
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (uri.getPath() != null) {
                String path = uri.getPath();
                if (path.endsWith(".html.zip") || path.endsWith(".htmlzip")) {
                    // zip html mode
                    text = getStringFromZip(inputStream);
                } else {
                    // load file into memory (html or markdown)
                    ByteArrayOutputStream result = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        result.write(buffer, 0, length);
                    }
                    text = result.toString();
                    if(path.endsWith(".md")){
                        // markdown mode
                        isMarkdown = true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (text == null) {
            tellUserThatCouldNotOpenFile();
            return;
        }

        jsObject = new JsObject(text, isMarkdown);
        configureWebView(webView);

        webView.loadData("", "text/html", null);
        if(isMarkdown){
            // markdown mode
            webView.loadUrl("file:///android_asset/markdown-reader.html");
        } else {
            // html mode
            webView.loadDataWithBaseURL("file://index.html", text, "text/html", null, null);
        }
        firstTimeInit = false;
    }

    private void tellUserThatCouldNotOpenFile() {
        Toast.makeText(this, getString(R.string.could_not_open_file), Toast.LENGTH_SHORT).show();
    }

    public static String getStringFromZip(InputStream stream) throws IOException {
        ByteArrayOutputStream fout = new ByteArrayOutputStream();
        if (!unpackZip(stream, fout)) {
            throw new IOException();
        }
        return fout.toString();
    }

    private static boolean unpackZip(InputStream is, ByteArrayOutputStream fout) {
        ZipInputStream zis;
        try {
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.isDirectory())
                    continue;
                //android 4.1 Compatibility fix Objects class access do a crash
                if (!ze.getName().contentEquals("index.html"))
                    continue;

                while ((count = zis.read(buffer)) != -1) {
                    fout.write(buffer, 0, count);
                }
                zis.closeEntry();
            }
            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        webView.saveState(outState);
        outState.putBoolean(IS_SEARCHING, isSearching);
        outState.putBoolean(FIRST_TIME_INIT, firstTimeInit);
        outState.putString(TITLE_WEB, titleWEB);
        outState.putString(MD_TEXT, jsObject.getMarkdown());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View v) {
        isSearching = true;
    }


    class JsObject {
        String contentText = "";
        boolean isMarkdownReader = false;

        public JsObject(String contentText, boolean isMarkdownReader) {
            this.contentText = contentText;
            this.isMarkdownReader = isMarkdownReader;
        }

        @JavascriptInterface
        public int getVersion() { return ZHV_VERSION_CODE; }

        @NonNull
        @JavascriptInterface
        public String toString() { return "[ZippedHTMLViewer Object]"; }

        @JavascriptInterface
        public String getMarkdown() {
          if (isMarkdownReader) {
              return contentText;
          } else {
              return "";
          }
        }
    }
}
