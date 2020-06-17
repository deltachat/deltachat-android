package org.thoughtcrime.securesms;

import android.app.Activity;
import java.util.Objects;
import android.os.Bundle;
import java.io.InputStream;
import java.io.IOException;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebChromeClient;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

import org.thoughtcrime.securesms.R;

public class ZhvActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            this.getActionBar().hide();
        } catch (NullPointerException e) {
        }

        setContentView(R.layout.activity_zhv);

        handleIntent();
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
            if (!uri.getPath().contains("zip")) {
                // load file into memory (html or markdown)
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }      
                text = result.toString();
                if(uri.getPath().contains("md")){ 
                    // markdown mode
                    isMarkdown = true;
                }
            } else {
                // zip html mode
                text = getStringFromZip(inputStream);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (text == null) {
            tellUserThatCouldNotOpenFile();
            return;
        }

        WebView myWebView = findViewById(R.id.webview);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
	webSettings.setBlockNetworkLoads(true);
        webSettings.setDomStorageEnabled(true);
        
        // myWebView.setWebContentsDebuggingEnabled(true);

        final Boolean isMarkdownReader = isMarkdown;
        final String contentText = text;
        class JsObject {
            @JavascriptInterface
            public int getVersion() { return 14; }
            @JavascriptInterface
            public String toString() { return "[ZippedHTMLViewer Object]"; }
            @JavascriptInterface
            public String getMarkdown() { 
                if(isMarkdownReader){
                    return contentText;
                } else {
                    return "";
                }
            }
        }
        myWebView.addJavascriptInterface(new JsObject(), "zhv");
        myWebView.setWebChromeClient(new WebChromeClient());
        myWebView.loadData("", "text/html", null);
        if(isMarkdown){ 
            // markdown mode
            myWebView.loadUrl("file:///android_asset/markdown-reader.html");
        } else {
            // html mode
            myWebView.loadDataWithBaseURL("file://index.html", contentText, "text/html", null, null);
        }
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
                if (!Objects.equals(ze.getName(), "index.html"))
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

}
