package org.thoughtcrime.securesms.proxy;

import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_PROXY_ENABLED;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_PROXY_URL;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcLot;

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
      if (proxySwitch.isChecked() && adapter.getCount() == 0) {
        showAddProxyDialog();
      } else {
        DcHelper.set(this, CONFIG_PROXY_ENABLED, proxySwitch.isChecked()? "1" : "0");
        restartIO();
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
    if (DcHelper.getContext(this).setConfigFromQr(proxyUrl)) {
      restartIO();
      adapter.changeData(DcHelper.get(this, CONFIG_PROXY_URL));
      proxySwitch.setChecked(DcHelper.getInt(this, CONFIG_PROXY_ENABLED) == 1);
    } else {
      Toast.makeText(this, R.string.proxy_invalid, Toast.LENGTH_LONG).show();
    }
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
          DcContext dcContext = DcHelper.getContext(this);
          final DcLot qrParsed = dcContext.checkQr(newProxy);
          if (qrParsed.getState() == DcContext.DC_QR_PROXY) {
            dcContext.setConfigFromQr(newProxy);
            restartIO();
            adapter.changeData(DcHelper.get(this, CONFIG_PROXY_URL));
          } else {
            Toast.makeText(this, R.string.proxy_invalid, Toast.LENGTH_LONG).show();
          }
          proxySwitch.setChecked(DcHelper.getInt(this, CONFIG_PROXY_ENABLED) == 1);
      })
      .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> {
          if (proxySwitch.isChecked() && adapter.getCount() == 0) {
            // user enabled switch without having proxies yet, revert
            proxySwitch.setChecked(false);
          }
      })
      .setCancelable(false)
      .show();
  }

  private void restartIO() {
    DcContext dcContext = DcHelper.getContext(this);
    dcContext.stopIo();
    dcContext.startIo();
  }

}
