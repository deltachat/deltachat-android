package org.thoughtcrime.securesms;

import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;

import java.lang.ref.WeakReference;

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

    new LoadHtmlAsyncTask(this).execute();
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (isFinishing()) {
      overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_right);
    }
  }

  // helper class for loading an html-file
  private static class LoadHtmlAsyncTask extends AsyncTask<Void, Void, Void> {
    private final WeakReference<FullMsgActivity> activityReference;
    private @NonNull String html = "";

    public LoadHtmlAsyncTask(@NonNull FullMsgActivity activity) {
      this.activityReference = new WeakReference<>(activity);
    }

    @Override
    protected Void doInBackground(Void... voids) {
      // android9 seems to make problems for non-base64-encoded html,
      // see eg. https://stackoverflow.com/questions/54516798/webview-loaddata-not-working-on-android-9-0-api-29
      try {
        FullMsgActivity activity = activityReference.get();
        html = activity.dcContext.getMsgHtml(activity.msgId);
        html = encodeToString(html.getBytes("UTF-8"), DEFAULT);
      } catch(Exception e) {
        e.printStackTrace();
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      try {
        FullMsgActivity activity = activityReference.get();
        activity.webView.loadData(html, "text/html; charset=utf-8", "base64");
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
  }
}
