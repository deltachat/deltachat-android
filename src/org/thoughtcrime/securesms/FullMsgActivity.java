package org.thoughtcrime.securesms;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AlertDialog;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;

import java.lang.ref.WeakReference;

import static android.util.Base64.DEFAULT;
import static android.util.Base64.encodeToString;

public class FullMsgActivity extends WebViewActivity
{
  public static final String MSG_ID_EXTRA = "msg_id";
  private int msgId;
  private ApplicationDcContext dcContext;
  private boolean loadRemoteContent = false;

  enum LoadRemoteContent {
    NEVER,
    ONCE,
    ALWAYS
  }

  @Override
  protected void onCreate(Bundle state, boolean ready) {
    super.onCreate(state, ready);

    loadRemoteContent = Prefs.getAlwaysLoadRemoteContent(this);
    webView.getSettings().setBlockNetworkLoads(!loadRemoteContent);

    // setBuiltInZoomControls() adds pinch-to-zoom as well as two ugly buttons;
    // the latter are hidden with setDisplayZoomControls() again.
    webView.getSettings().setBuiltInZoomControls(true);
    webView.getSettings().setDisplayZoomControls(false);

    getSupportActionBar().setTitle(getString(R.string.chat_input_placeholder));

    dcContext = DcHelper.getContext(this);
    msgId = getIntent().getIntExtra(MSG_ID_EXTRA, 0);

    loadHtmlAsync(new WeakReference<>(this));
  }

  private static void loadHtmlAsync(final WeakReference<FullMsgActivity> activityReference) {
    Util.runOnBackground(() -> {
      // android9 seems to make problems for non-base64-encoded html,
      // see eg. https://stackoverflow.com/questions/54516798/webview-loaddata-not-working-on-android-9-0-api-29
      String html;

      try {
        FullMsgActivity activity = activityReference.get();
        html = activity.dcContext.getMsgHtml(activity.msgId);
        html = encodeToString(html.getBytes("UTF-8"), DEFAULT);
        String finalHtml = html;

        activity.runOnUiThread(() -> {
          try {
            activityReference.get().webView.loadData(finalHtml, "text/html; charset=utf-8", "base64");
          } catch (Exception e) {
            e.printStackTrace();
          }
        });

      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (isFinishing()) {
      overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_right);
    }
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    this.getMenuInflater().inflate(R.menu.full_msg, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()) {
      case R.id.load_remote_content:
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
          .setTitle(R.string.load_remote_content)
          .setMessage(R.string.load_remote_content_ask);

        // we are using the buttons "[Always]  [Never][Once]" in that order.
        // 1. Checkmarks before [Always] and [Never] show the current state.
        // 2. [Once] is also shown in always-mode and disables always-mode if selected
        //    (there was the idea to hide [Once] in always mode, but that looks more like a bug in the end)
        // (maybe a usual Always-Checkbox and "[Cancel][OK]" buttons are an alternative, however, a [Once]
        // would be required as well - probably as the leftmost button which is not that usable in
        // not-always-mode where the dialog is used more often. Or [Ok] would mean "Once" as well as "Change checkbox setting",
        // which is also a bit weird. Anyway, let's give the three buttons a try :)
        final String checkmarkPrefix = DynamicTheme.getCheckmarkEmoji(this) + " ";
        if (Prefs.getAlwaysLoadRemoteContent(this)) {
          builder.setNeutralButton(checkmarkPrefix + getString(R.string.always), (dialog, which) -> onChangeLoadRemoteContent(LoadRemoteContent.ALWAYS));
          builder.setNegativeButton(R.string.never, (dialog, which) -> onChangeLoadRemoteContent(LoadRemoteContent.NEVER));
          builder.setPositiveButton(R.string.once, (dialog, which) -> onChangeLoadRemoteContent(LoadRemoteContent.ONCE));
        } else {
          builder.setNeutralButton(R.string.always, (dialog, which) -> onChangeLoadRemoteContent(LoadRemoteContent.ALWAYS));
          builder.setNegativeButton((loadRemoteContent? "" : checkmarkPrefix) + getString(R.string.never), (dialog, which) -> onChangeLoadRemoteContent(LoadRemoteContent.NEVER));
          builder.setPositiveButton((loadRemoteContent? checkmarkPrefix : "") + getString(R.string.once), (dialog, which) -> onChangeLoadRemoteContent(LoadRemoteContent.ONCE));
        }

        builder.show();
        return true;
    }
    return false;
  }

  private void onChangeLoadRemoteContent(LoadRemoteContent loadRemoteContent) {
    switch (loadRemoteContent) {
      case NEVER:
        this.loadRemoteContent = false;
        Prefs.setBooleanPreference(this, Prefs.ALWAYS_LOAD_REMOTE_CONTENT, false);
        break;
      case ONCE:
        this.loadRemoteContent = true;
        Prefs.setBooleanPreference(this, Prefs.ALWAYS_LOAD_REMOTE_CONTENT, false);
        break;
      case ALWAYS:
        this.loadRemoteContent = true;
        Prefs.setBooleanPreference(this, Prefs.ALWAYS_LOAD_REMOTE_CONTENT, true);
        break;
    }
    webView.getSettings().setBlockNetworkLoads(!this.loadRemoteContent);
    webView.reload();
  }
}
