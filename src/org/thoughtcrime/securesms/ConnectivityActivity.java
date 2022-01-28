package org.thoughtcrime.securesms;

import android.os.Bundle;
import android.view.Menu;

import androidx.annotation.NonNull;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;

import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;

public class ConnectivityActivity extends WebViewActivity implements DcEventCenter.DcEventDelegate {
  @Override
  protected void onCreate(Bundle state, boolean ready) {
    super.onCreate(state, ready);
    setForceDark();
    getSupportActionBar().setTitle(R.string.connectivity);
    refresh();

    DcHelper.getEventCenter(this).addObserver(DcContext.DC_EVENT_CONNECTIVITY_CHANGED, this);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    DcHelper.getEventCenter(this).removeObservers(this);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    // do not call super.onPrepareOptionsMenu() as the default "Search" menu is not needed
    return true;
  }

  private void refresh() {
    String connectivityHtml = DcHelper.getContext(this).getConnectivityHtml();
    webView.loadDataWithBaseURL(null, connectivityHtml, "text/html", "utf-8", null);
  }

  @Override
  public void handleEvent(@NonNull DcEvent event) {
    refresh();
  }

  @Override
  public void onBackPressed() {
    // If we did not override this function, the back button in the connectivity view would sometimes
    // not work, probably because webView.canGoBack() in WebViewActivity.onBackPressed() wrongly returns true
    finish();
  }
}
