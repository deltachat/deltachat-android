package org.thoughtcrime.securesms;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentTransaction;

import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.FileProviderUtil;

import java.io.File;

public class LogViewActivity extends BaseActionBarActivity {

  private static final String TAG = LogViewActivity.class.getSimpleName();

  LogViewFragment logViewFragment;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.log_view_activity);
    logViewFragment = new LogViewFragment();
    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    transaction.replace(R.id.fragment_container, logViewFragment);
    transaction.commit();

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuInflater inflater = this.getMenuInflater();
    menu.clear();

    inflater.inflate(R.menu.view_log, menu);

    super.onPrepareOptionsMenu(menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    Float newSize;

    int itemId = item.getItemId();
    if (itemId == android.R.id.home) {
      finish();
      return true;
    } else if (itemId == R.id.save_log) {
      Permissions.with(this)
        .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        .alwaysGrantOnSdk30()
        .ifNecessary()
        .onAllGranted(() -> {
          File outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
          boolean success = logViewFragment.saveLogFile(outputDir) != null;
          new AlertDialog.Builder(this)
            .setMessage(success ? R.string.pref_saved_log : R.string.pref_save_log_failed)
            .setPositiveButton(android.R.string.ok, null)
            .show();
        })
        .execute();
      return true;
    } else if (itemId == R.id.share_log) {
      shareLog();
      return true;
    } else if (itemId == R.id.log_zoom_in) {
      newSize = logViewFragment.getLogTextSize() + 2.0f;
      logViewFragment.setLogTextSize(newSize);
      return false;
    } else if (itemId == R.id.log_zoom_out) {
      newSize = logViewFragment.getLogTextSize() - 2.0f;
      logViewFragment.setLogTextSize(newSize);
      return false;
    } else if (itemId == R.id.log_scroll_down) {
      logViewFragment.scrollDownLog();
      return false;
    } else if (itemId == R.id.log_scroll_up) {
      logViewFragment.scrollUpLog();
      return false;
    }

    return false;
  }

  public void shareLog() {
    try {
      File logFile = logViewFragment.saveLogFile(getExternalCacheDir());
      Uri uri = FileProviderUtil.getUriFor(this, logFile);
      Intent intent = new Intent(Intent.ACTION_SEND);
      intent.setType("text/plain");
      intent.putExtra(Intent.EXTRA_STREAM, uri);
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      startActivity(Intent.createChooser(intent, getString(R.string.chat_share_with_title)));
    } catch (Exception e) {
      Log.e(TAG, "failed to share log", e);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }
}
