package org.thoughtcrime.securesms;

import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SOCKS5_ENABLED;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SOCKS5_HOST;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SOCKS5_PASSWORD;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SOCKS5_PORT;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SOCKS5_USER;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.Group;

import com.b44t.messenger.DcContext;
import com.google.android.material.textfield.TextInputEditText;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.DynamicTheme;

public class ProxySettingsActivity extends BaseActionBarActivity {

  private enum VerificationType {
    SERVER,
    PORT,
  }

 private final DynamicTheme dynamicTheme = new DynamicTheme();

  private SwitchCompat proxySwitch;
  private Group proxyGroup;

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

    proxyGroup = findViewById(R.id.proxy_group);

    proxySwitch = findViewById(R.id.proxy_switch);
    proxySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
        proxyGroup.setVisibility(isChecked? View.VISIBLE : View.GONE);
      });
    proxySwitch.setChecked(DcHelper.getInt(this, CONFIG_SOCKS5_ENABLED) == 1);

    TextInputEditText proxyHostInput = findViewById(R.id.proxy_host_text);
    proxyHostInput.setOnFocusChangeListener((view, focused) -> focusListener(view, focused, VerificationType.SERVER));
    proxyHostInput.setText(DcHelper.get(this, CONFIG_SOCKS5_HOST));

    TextInputEditText proxyPortInput = findViewById(R.id.proxy_port_text);
    proxyPortInput.setOnFocusChangeListener((view, focused) -> focusListener(view, focused, VerificationType.PORT));
    proxyPortInput.setText(DcHelper.get(this, CONFIG_SOCKS5_PORT));

    TextInputEditText proxyUserInput = findViewById(R.id.proxy_user_text);
    proxyUserInput.setText(DcHelper.get(this, CONFIG_SOCKS5_USER));

    TextInputEditText proxyPasswordInput = findViewById(R.id.proxy_password_text);
    proxyPasswordInput.setText(DcHelper.get(this, CONFIG_SOCKS5_PASSWORD));
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
  public void onDestroy() {
    super.onDestroy();
    saveConfig();
  }

  @Override
  public void onPause() {
    super.onPause();
    saveConfig();
  }

  private void focusListener(View view, boolean focused, VerificationType type) {

    if (!focused) {
      TextInputEditText inputEditText = (TextInputEditText) view;
      switch (type) {
      case SERVER:
        verifyServer(inputEditText);
        break;
      case PORT:
        verifyPort(inputEditText);
        break;
      }
    }
  }

  private void verifyServer(TextInputEditText view) {
    String server = view.getText().toString();
    if (!TextUtils.isEmpty(server) && !Patterns.DOMAIN_NAME.matcher(server).matches()
        && !Patterns.IP_ADDRESS.matcher(server).matches()
        && !Patterns.WEB_URL.matcher(server).matches()
        && !"localhost".equals(server)) {
      view.setError(getString(R.string.login_error_server));
    }
  }

  private void verifyPort(TextInputEditText view) {
    String portString = view.getText().toString();
    if (!portString.isEmpty()) {
      String error = getString(R.string.login_error_port);
      try {
        int port = Integer.valueOf(portString);
        if (port < 1 || port > 65535) {
          view.setError(error);
        }
      } catch (NumberFormatException exception) {
        view.setError(error);
      }
    }
  }

  private void saveConfig() {
    DcContext dcContext = DcHelper.getContext(this);
    dcContext.setConfigInt(CONFIG_SOCKS5_ENABLED, proxySwitch.isChecked()? 1 : 0);
    setConfig(R.id.proxy_host_text, CONFIG_SOCKS5_HOST, true);
    setConfig(R.id.proxy_port_text, CONFIG_SOCKS5_PORT, true);
    setConfig(R.id.proxy_user_text, CONFIG_SOCKS5_USER, true);
    setConfig(R.id.proxy_password_text, CONFIG_SOCKS5_PASSWORD, false);
    dcContext.stopIo();
    dcContext.startIo();
  }

  private void setConfig(@IdRes int viewId, String configTarget, boolean doTrim) {
    TextInputEditText view = findViewById(viewId);
    String value = view.getText().toString();
    if(doTrim) {
      value = value.trim();
    }
    DcHelper.getContext(this).setConfig(configTarget, value.isEmpty()? null : value);
  }

}
