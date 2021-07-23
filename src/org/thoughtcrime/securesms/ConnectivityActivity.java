package org.thoughtcrime.securesms;

import android.os.Bundle;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;

import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;

public class ConnectivityActivity extends WebViewActivity implements DcEventCenter.DcEventDelegate {
  @Override
  protected void onCreate(Bundle state, boolean ready) {
    super.onCreate(state, ready);
    refresh();

    DcHelper.getEventCenter(this).addObserver(DcContext.DC_EVENT_CONNECTIVITY_CHANGED, this);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    DcHelper.getEventCenter(this).removeObservers(this);
  }

  private void refresh() {
    getSupportActionBar().setTitle(R.string.connectivity);

    String connectivityHtml = DcHelper.getContext(this).getConnectivityHtml();
    webView.loadDataWithBaseURL(null, connectivityHtml, "text/html", "utf-8", null);
  }

  @Override
  public void handleEvent(DcEvent event) {
    refresh();
  }
}
