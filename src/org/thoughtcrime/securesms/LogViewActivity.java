package org.thoughtcrime.securesms;

import android.Manifest;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.Util;

public class LogViewActivity extends BaseActionBarActivity {

  private static final String TAG = LogViewActivity.class.getSimpleName();

  private final DynamicTheme    dynamicTheme    = new DynamicTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  LogViewFragment logViewFragment;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);

    setContentView(R.layout.log_view_activity);
    logViewFragment = new LogViewFragment(dynamicLanguage);
    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    transaction.replace(R.id.fragment_container, logViewFragment);
    transaction.commit();

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }

  @Override
  protected void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
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

    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
      case R.id.save_log:
        Permissions.with(this)
            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .ifNecessary()
            .onAllGranted(() -> {
              boolean success = logViewFragment.saveLogFile();
              new AlertDialog.Builder(this)
                  .setMessage(success? R.string.pref_saved_log : R.string.pref_save_log_failed)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
            })
            .execute();
        return true;
      case R.id.copy_log_to_clipboard:
        Util.writeTextToClipboard(this, logViewFragment.getLogText());
        Toast.makeText(getApplicationContext(), R.string.done, Toast.LENGTH_SHORT).show();
        return true;
      case R.id.log_zoom_in:
        newSize = logViewFragment.getLogTextSize() + 2.0f;
        logViewFragment.setLogTextSize(newSize);
        return false;
      case R.id.log_zoom_out:
        newSize = logViewFragment.getLogTextSize() - 2.0f;
        logViewFragment.setLogTextSize(newSize);
        return false;
      case R.id.log_scroll_down:
        logViewFragment.scrollDownLog();
        return false;
      case R.id.log_scroll_up:
        logViewFragment.scrollUpLog();
        return false;
    }

    return false;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }
}
