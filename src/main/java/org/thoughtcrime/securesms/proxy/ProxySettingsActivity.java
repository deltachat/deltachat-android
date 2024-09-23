package org.thoughtcrime.securesms.proxy;

import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_PROXY_ENABLED;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_PROXY_URL;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcLot;

import org.thoughtcrime.securesms.BaseActionBarActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.Util;

import java.util.LinkedList;

public class ProxySettingsActivity extends BaseActionBarActivity
  implements ProxyListAdapter.ItemClickListener, DcEventCenter.DcEventDelegate {

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

    ListView proxyList = findViewById(R.id.proxy_list);
    proxyList.setAdapter(adapter);
    proxyList.addHeaderView(View.inflate(this, R.layout.proxy_list_header, null), null, false);
    View footer = View.inflate(this, R.layout.proxy_list_footer, null);
    footer.setOnClickListener(l -> showAddProxyDialog());
    proxyList.addFooterView(footer);
    adapter.changeData(DcHelper.get(this, CONFIG_PROXY_URL));
    DcHelper.getEventCenter(this).addObserver(DcContext.DC_EVENT_CONNECTIVITY_CHANGED, this);
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    DcHelper.getEventCenter(this).removeObservers(this);
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

  @Override
  public void onItemShare(String proxyUrl) {
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.setType("text/plain");
    intent.putExtra(Intent.EXTRA_TEXT, proxyUrl);
    startActivity(Intent.createChooser(intent, getString(R.string.chat_share_with_title)));
  }

  @Override
  public void onItemDelete(String proxyUrl) {
    String host = DcHelper.getContext(this).checkQr(proxyUrl).getText1();
    AlertDialog dialog = new AlertDialog.Builder(this)
      .setTitle(R.string.proxy_delete)
      .setMessage(getString(R.string.proxy_delete_explain, host))
      .setPositiveButton(R.string.delete, (dlg, btn) -> deleteProxy(proxyUrl))
      .setNegativeButton(android.R.string.cancel, null)
      .show();
    Util.redPositiveButton(dialog);
  }

  private void deleteProxy(String proxyUrl) {
    final LinkedList<String> proxies = new LinkedList<>();
    for (String proxy: DcHelper.get(this, CONFIG_PROXY_URL).split("\n")) {
      if (!proxy.equals(proxyUrl)) {
        proxies.add(proxy);
      }
    }
    if (proxies.isEmpty()) {
      DcHelper.set(this, CONFIG_PROXY_ENABLED, "0");
      proxySwitch.setChecked(false);
    }
    String proxyUrls = String.join("\n", proxies);
    DcHelper.set(this, CONFIG_PROXY_URL, proxyUrls);
    restartIO();
    adapter.changeData(proxyUrls);
  }

  private void showAddProxyDialog() {
    View view = View.inflate(this, R.layout.single_line_input, null);
    EditText inputField = view.findViewById(R.id.input_field);
    inputField.setHint(R.string.proxy_add_url_hint);

    new AlertDialog.Builder(this)
      .setTitle(R.string.proxy_add)
      .setMessage(R.string.proxy_add_explain)
      .setView(view)
      .setPositiveButton(R.string.proxy_use_proxy, (dialog, whichButton) -> {
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

  @Override
  public void handleEvent(@NonNull DcEvent event) {
    if (event.getId() == DcContext.DC_EVENT_CONNECTIVITY_CHANGED) {
      adapter.refreshConnectivity();
    }
  }

}
