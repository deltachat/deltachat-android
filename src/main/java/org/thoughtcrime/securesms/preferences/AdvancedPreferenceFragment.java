package org.thoughtcrime.securesms.preferences;

import static android.app.Activity.RESULT_OK;
import static android.text.InputType.TYPE_TEXT_VARIATION_URI;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_BCC_SELF;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_MVBOX_MOVE;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_ONLY_FETCH_MVBOX;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_STATS_SENDING;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SHOW_EMAILS;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_WEBXDC_REALTIME_ENABLED;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.LogViewActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.relay.RelayListActivity;
import org.thoughtcrime.securesms.StatsSending;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.proxy.ProxySettingsActivity;
import org.thoughtcrime.securesms.util.IntentUtils;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.StreamUtil;
import org.thoughtcrime.securesms.util.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;


public class AdvancedPreferenceFragment extends ListSummaryPreferenceFragment
                                        implements DcEventCenter.DcEventDelegate
{
  private static final String TAG = AdvancedPreferenceFragment.class.getSimpleName();

  private ListPreference showEmails;
  CheckBoxPreference selfReportingCheckbox;
  CheckBoxPreference multiDeviceCheckbox;
  CheckBoxPreference mvboxMoveCheckbox;
  CheckBoxPreference onlyFetchMvboxCheckbox;
  CheckBoxPreference webxdcRealtimeCheckbox;

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);

    showEmails = (ListPreference) this.findPreference("pref_show_emails");
    if (showEmails != null) {
      showEmails.setOnPreferenceChangeListener((preference, newValue) -> {
        updateListSummary(preference, newValue);
        dcContext.setConfigInt(CONFIG_SHOW_EMAILS, Util.objectToInt(newValue));
        return true;
      });
    }

    multiDeviceCheckbox = (CheckBoxPreference) this.findPreference("pref_bcc_self");
    if (multiDeviceCheckbox != null) {
      multiDeviceCheckbox.setOnPreferenceChangeListener((preference, newValue) -> {
        boolean enabled = (Boolean) newValue;
        if (enabled) {
            dcContext.setConfigInt(CONFIG_BCC_SELF, 1);
            return true;
        } else {
          new AlertDialog.Builder(requireContext())
                  .setMessage(R.string.pref_multidevice_change_warn)
                  .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                    dcContext.setConfigInt(CONFIG_BCC_SELF, 0);
                    ((CheckBoxPreference)preference).setChecked(false);
                  })
                  .setNegativeButton(R.string.cancel, null)
                  .show();
          return false;
        }
      });
    }

    mvboxMoveCheckbox = (CheckBoxPreference) this.findPreference("pref_mvbox_move");
    if (mvboxMoveCheckbox != null) {
      mvboxMoveCheckbox.setOnPreferenceChangeListener((preference, newValue) -> {
        boolean enabled = (Boolean) newValue;
        dcContext.setConfigInt(CONFIG_MVBOX_MOVE, enabled? 1 : 0);
        return true;
      });
    }

    onlyFetchMvboxCheckbox = this.findPreference("pref_only_fetch_mvbox");
    if (onlyFetchMvboxCheckbox != null) {
      onlyFetchMvboxCheckbox.setOnPreferenceChangeListener(((preference, newValue) -> {
        final boolean enabled = (Boolean) newValue;
        if (enabled) {
          new AlertDialog.Builder(requireContext())
                  .setMessage(R.string.pref_imap_folder_warn_disable_defaults)
                  .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                    dcContext.setConfigInt(CONFIG_ONLY_FETCH_MVBOX, 1);
                    ((CheckBoxPreference)preference).setChecked(true);
                  })
                  .setNegativeButton(R.string.cancel, null)
                  .show();
          return false;
        } else {
          dcContext.setConfigInt(CONFIG_ONLY_FETCH_MVBOX, 0);
          return true;
        }
      }));
    }

    webxdcRealtimeCheckbox = (CheckBoxPreference) this.findPreference("pref_webxdc_realtime_enabled");
    if (webxdcRealtimeCheckbox != null) {
      webxdcRealtimeCheckbox.setOnPreferenceChangeListener((preference, newValue) -> {
        boolean enabled = (Boolean) newValue;
        dcContext.setConfigInt(CONFIG_WEBXDC_REALTIME_ENABLED, enabled? 1 : 0);
        return true;
      });
    }

    Preference screenSecurity = this.findPreference(Prefs.SCREEN_SECURITY_PREF);
    if (screenSecurity != null) {
      screenSecurity.setOnPreferenceChangeListener(new ScreenShotSecurityListener());
    }

    Preference submitDebugLog = this.findPreference("pref_view_log");
    if (submitDebugLog != null) {
      submitDebugLog.setOnPreferenceClickListener(new ViewLogListener());
    }

    Preference webxdcStore = this.findPreference(Prefs.WEBXDC_STORE_URL_PREF);
    if (webxdcStore != null) {
      webxdcStore.setOnPreferenceClickListener(new WebxdcStoreUrlListener());
    }
    updateWebxdcStoreSummary();

    Preference newBroadcastList = this.findPreference("pref_new_broadcast_list");
    if (newBroadcastList != null) {
      newBroadcastList.setOnPreferenceChangeListener((preference, newValue) -> {
        if ((Boolean)newValue) {
          new AlertDialog.Builder(requireActivity())
            .setTitle("Thanks for trying out \"Channels\"!")
            .setMessage("• You can now create new \"Channels\" from the \"New Chat\" dialog\n\n"
              + "• If you want to quit the experimental feature, you can disable it at \"Settings / Advanced\"")
            .setCancelable(false)
            .setPositiveButton(R.string.ok, null)
            .show();
        }
        return true;
      });
    }

    Preference locationStreamingEnabled = this.findPreference("pref_location_streaming_enabled");
    if (locationStreamingEnabled != null) {
      locationStreamingEnabled.setOnPreferenceChangeListener((preference, newValue) -> {
        if ((Boolean)newValue) {
          new AlertDialog.Builder(requireActivity())
            .setTitle("Thanks for trying out \"Location Streaming\"!")
            .setMessage("• You will find a corresponding option in the attach menu (the paper clip) of each chat now\n\n"
              + "• If you want to quit the experimental feature, you can disable it at \"Settings / Advanced\"")
            .setCancelable(false)
            .setPositiveButton(R.string.ok, null)
            .show();
        }
        return true;
      });
    }

    Preference callsEnabled = this.findPreference("pref_calls_enabled");
    if (callsEnabled != null) {
      callsEnabled.setOnPreferenceChangeListener((preference, newValue) -> {
        if ((Boolean)newValue) {
          new AlertDialog.Builder(requireActivity())
            .setTitle("Thanks for helping to debug \"Calls\"!")
            .setMessage("• You can now debug calls using the \"phone\" icon in one-to-one-chats\n\n"
              + "• The experiment is about making decentralised calls work and reliable at all, not about options or UI. We're happy about focused feedback at support.delta.chat\n\n")
            .setCancelable(false)
            .setPositiveButton(R.string.ok, null)
            .show();
        }
        return true;
      });
    }

    selfReportingCheckbox = this.findPreference("pref_stats_sending");
    if (selfReportingCheckbox != null) {
      selfReportingCheckbox.setOnPreferenceChangeListener((preference, newValue) -> {
        boolean enabled = (Boolean) newValue;
        if (enabled) {
          StatsSending.showStatsConfirmationDialog(requireActivity(), () -> {
            ((CheckBoxPreference)preference).setChecked(true);
          });
          return false;
        } else {
          dcContext.setConfigInt(CONFIG_STATS_SENDING, 0);
          return true;
        }
      });
    }

    Preference proxySettings = this.findPreference("proxy_settings_button");
    if (proxySettings != null) {
      proxySettings.setOnPreferenceClickListener((preference) -> {
        startActivity(new Intent(requireActivity(), ProxySettingsActivity.class));
        return true;
      });
    }

    Preference relayListBtn = this.findPreference("pref_relay_list_button");
    if (relayListBtn != null) {
      relayListBtn.setOnPreferenceClickListener(((preference) -> {
        openRelayListActivity();
        return true;
      }));
    }

    if (dcContext.isChatmail()) {
      findPreference("pref_category_legacy").setVisible(false);
    }
  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.preferences_advanced);
  }

  @Override
  public void onResume() {
    super.onResume();
    Objects.requireNonNull(((ApplicationPreferencesActivity) requireActivity()).getSupportActionBar()).setTitle(R.string.menu_advanced);

    String value = Integer.toString(dcContext.getConfigInt("show_emails"));
    showEmails.setValue(value);
    updateListSummary(showEmails, value);

    selfReportingCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_STATS_SENDING));
    multiDeviceCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_BCC_SELF));
    mvboxMoveCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_MVBOX_MOVE));
    onlyFetchMvboxCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_ONLY_FETCH_MVBOX));
    webxdcRealtimeCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_WEBXDC_REALTIME_ENABLED));
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_CONFIRM_CREDENTIALS_ACCOUNT) {
      openRelayListActivity();
    }
  }

  protected File copyToCacheDir(Uri uri) throws IOException {
    try (InputStream inputStream = requireActivity().getContentResolver().openInputStream(uri)) {
      File file = File.createTempFile("tmp-keys-file", ".tmp", requireActivity().getCacheDir());
      try (OutputStream outputStream = new FileOutputStream(file)) {
        StreamUtil.copy(inputStream, outputStream);
      }
      return file;
    }
  }

  public static @NonNull String getVersion(@Nullable Context context) {
    try {
      if (context == null) return "";

      String app     = context.getString(R.string.app_name);
      String version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;

      return String.format("%s %s", app, version);
    } catch (PackageManager.NameNotFoundException e) {
      Log.w(TAG, e);
      return context.getString(R.string.app_name);
    }
  }

  private class ScreenShotSecurityListener implements Preference.OnPreferenceChangeListener {
    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
      boolean enabled = (Boolean) newValue;
      Prefs.setScreenSecurityEnabled(getContext(), enabled);
      Toast.makeText(getContext(), R.string.pref_screen_security_please_restart_hint, Toast.LENGTH_LONG).show();
      return true;
    }
  }

  private class ViewLogListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
      final Intent intent = new Intent(requireActivity(), LogViewActivity.class);
      startActivity(intent);
      return true;
    }
  }

  private class WebxdcStoreUrlListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
      View gl = View.inflate(requireActivity(), R.layout.single_line_input, null);
      EditText inputField = gl.findViewById(R.id.input_field);
      inputField.setHint(Prefs.DEFAULT_WEBXDC_STORE_URL);
      inputField.setText(Prefs.getWebxdcStoreUrl(requireActivity()));
      inputField.setSelection(inputField.getText().length());
      inputField.setInputType(TYPE_TEXT_VARIATION_URI);
      new AlertDialog.Builder(requireActivity())
              .setTitle(R.string.webxdc_store_url)
              .setMessage(R.string.webxdc_store_url_explain)
              .setView(gl)
              .setNegativeButton(android.R.string.cancel, null)
              .setPositiveButton(android.R.string.ok, (dlg, btn) -> {
                Prefs.setWebxdcStoreUrl(requireActivity(), inputField.getText().toString());
                updateWebxdcStoreSummary();
              })
              .show();
      return true;
    }
  }

  private void updateWebxdcStoreSummary() {
    Preference preference = this.findPreference(Prefs.WEBXDC_STORE_URL_PREF);
    if (preference != null) {
        preference.setSummary(Prefs.getWebxdcStoreUrl(requireActivity()));
    }
  }

  private void openRelayListActivity() {
    Intent intent = new Intent(requireActivity(), RelayListActivity.class);
    startActivity(intent);
  }

}
