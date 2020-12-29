package org.thoughtcrime.securesms;

import android.os.Bundle;
import android.util.Log;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;

import java.nio.charset.StandardCharsets;

import static android.util.Base64.*;

public class FullMsgActivity extends WebViewActivity
{
  public static final String MSG_ID_EXTRA = "msg_id";
  private int msgId;
  private ApplicationDcContext dcContext;

  @Override
  protected void onCreate(Bundle state, boolean ready) {
    super.onCreate(state, ready);
    getSupportActionBar().setTitle(getString(R.string.chat_input_placeholder));

    dcContext = DcHelper.getContext(this);
    msgId = getIntent().getIntExtra(MSG_ID_EXTRA, 0);

    // android9 seems to make problems for non-base64-encoded html,
    // see eg. https://stackoverflow.com/questions/54516798/webview-loaddata-not-working-on-android-9-0-api-29
    String html = dcContext.getOriginalMimeHtml(msgId);
    try {
      html = encodeToString(html.getBytes("UTF-8"), android.util.Base64.DEFAULT);
    } catch(Exception e) {
      e.printStackTrace();
    }
    webView.loadData(html, "text/html; charset=utf-8", "base64");
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (isFinishing()) {
      overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_right);
    }
  }
}
