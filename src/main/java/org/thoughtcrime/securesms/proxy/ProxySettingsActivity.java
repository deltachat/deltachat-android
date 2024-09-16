package org.thoughtcrime.securesms.proxy;

import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_PROXY_ENABLED;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_PROXY_URL;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.BaseActionBarActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.DynamicTheme;

public class ProxySettingsActivity extends BaseActionBarActivity implements ProxyListAdapter.ItemClickListener {

  private final DynamicTheme dynamicTheme = new DynamicTheme();
  private SwitchCompat proxySwitch;
  private ProxyListAdapter adapter;

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    dynamicTheme.onCreate(this);
    setContentView(R.layout.proxy_settings_activity);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setTitle(R.string.proxy_settings);
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    adapter = new ProxyListAdapter(this);
    adapter.setItemClickListener(this);

    proxySwitch = findViewById(R.id.proxy_switch);
    proxySwitch.setChecked(DcHelper.getInt(this, CONFIG_PROXY_ENABLED) == 1);
    proxySwitch.setOnClickListener(l -> {
      if (proxySwitch.isChecked() && adapter.isEmpty()) {
        showAddProxyDialog();
      }
    });

    findViewById(R.id.add_proxy_button).setOnClickListener(l -> showAddProxyDialog());

    ListView proxyList = findViewById(R.id.proxy_list);
    proxyList.setAdapter(adapter);
    adapter.changeData(DcHelper.get(this, CONFIG_PROXY_URL));
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
  }

  @Override
  public void onPause() {
    super.onPause();
    saveConfig();
  }

  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == android.R.id.home) {
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onItemClick(String proxyUrl) {
    adapter.selectProxy(proxyUrl);
    proxySwitch.setChecked(!adapter.isEmpty());
  }

  private void showAddProxyDialog() {
    View view = View.inflate(this, R.layout.single_line_input, null);
    EditText inputField = view.findViewById(R.id.input_field);
    inputField.setHint(R.string.proxy_add_url_hint);

    new AlertDialog.Builder(this)
      .setTitle(R.string.proxy_add)
      .setMessage(R.string.proxy_add_explain)
      .setView(view)
      .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
          String newProxy = inputField.getText().toString().trim();
          if (!TextUtils.isEmpty(newProxy)) {
            adapter.selectProxy(newProxy);
          }
          proxySwitch.setChecked(!adapter.isEmpty());
      })
      .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> {
          if (adapter.isEmpty()) proxySwitch.setChecked(false);
      })
      .setCancelable(false)
      .show();
  }

  private void saveConfig() {
    DcContext dcContext = DcHelper.getContext(this);
    String proxyUrl = adapter.getProxyUrl();
    boolean proxyEnabled = proxySwitch.isChecked() && !TextUtils.isEmpty(proxyUrl);
    dcContext.setConfigInt(CONFIG_PROXY_ENABLED, proxyEnabled? 1 : 0);
    dcContext.setConfig(CONFIG_PROXY_URL, proxyUrl);
    dcContext.stopIo();
    dcContext.startIo();
  }

}
