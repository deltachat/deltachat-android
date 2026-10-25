package com.b44t.messenger.uitests.offline;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.thoughtcrime.securesms.R;

@RunWith(AndroidJUnit4.class)
public class WebxdcLoadingTest {
  private WebView webView;

  @After
  public void cleanup() {
    getInstrumentation()
        .runOnMainSync(
            () -> {
              if (webView != null) webView.destroy();
            });
  }

  @Test
  public void testProgressHiddenWhileCertificatePending() throws Exception {
    String wrapper;
    try (InputStream input =
        getInstrumentation()
            .getTargetContext()
            .getResources()
            .openRawResource(R.raw.webxdc_wrapper)) {
      wrapper = new Scanner(input, "UTF-8").useDelimiter("\\A").next();
    }
    // Keep certificate generation pending to catch the initial progress bar flash.
    String html =
        wrapper.replace(
            "<script>",
            "<script>window.RTCPeerConnection = {"
                + "generateCertificate: () => new Promise(() => {})};</script><script>");
    CountDownLatch checked = new CountDownLatch(1);
    AtomicReference<String> hidden = new AtomicReference<>();
    getInstrumentation()
        .runOnMainSync(
            () -> {
              webView = new WebView(getInstrumentation().getTargetContext());
              webView.getSettings().setJavaScriptEnabled(true);
              webView.setWebViewClient(
                  new WebViewClient() {
                    @Override
                    public WebResourceResponse shouldInterceptRequest(
                        WebView view, WebResourceRequest request) {
                      return new WebResourceResponse(
                          "text/html", "UTF-8", new ByteArrayInputStream(new byte[0]));
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                      view.evaluateJavascript(
                          "getComputedStyle(document.getElementById('progress')).display === 'none'",
                          value -> {
                            hidden.set(value);
                            checked.countDown();
                          });
                    }
                  });
              webView.loadDataWithBaseURL(
                  "https://webxdc.localhost/?h=1", html, "text/html", "UTF-8", null);
            });
    assertTrue("Progress check timed out", checked.await(5, TimeUnit.SECONDS));
    assertEquals("true", hidden.get());
  }
}
