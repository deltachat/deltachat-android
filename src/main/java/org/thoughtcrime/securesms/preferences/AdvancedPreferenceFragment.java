package org.thoughtcrime.securesms.preferences;

import static android.app.Activity.RESULT_OK;
import static android.text.InputType.TYPE_TEXT_VARIATION_URI;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_BCC_SELF;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_E2EE_ENABLED;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_MVBOX_MOVE;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_ONLY_FETCH_MVBOX;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SENTBOX_WATCH;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SHOW_EMAILS;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_WEBXDC_REALTIME_ENABLED;
import static org.thoughtcrime.securesms.connect.DcHelper.getRpc;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.rpc.RpcException;

import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.ConversationActivity;
import org.thoughtcrime.securesms.LogViewActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.RegistrationActivity;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.AttachmentManager;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.proxy.ProxySettingsActivity;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ScreenLockUtil;
import org.thoughtcrime.securesms.util.StorageUtil;
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
  public static final int PICK_SELF_KEYS = 29923;

  private ListPreference showEmails;
  CheckBoxPreference preferE2eeCheckbox;
  CheckBoxPreference sentboxWatchCheckbox;
  CheckBoxPreference bccSelfCheckbox;
  CheckBoxPreference mvboxMoveCheckbox;
  CheckBoxPreference onlyFetchMvboxCheckbox;
  CheckBoxPreference webxdcRealtimeCheckbox;
  CheckBoxPreference showSystemContacts;

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

    Preference sendAsm = this.findPreference("pref_send_autocrypt_setup_message");
    if (sendAsm != null) {
      sendAsm.setOnPreferenceClickListener(new SendAsmListener());
    }

    preferE2eeCheckbox = (CheckBoxPreference) this.findPreference("pref_prefer_e2ee");
    if (preferE2eeCheckbox != null) {
      preferE2eeCheckbox.setOnPreferenceChangeListener(new PreferE2eeListener());
    }

    sentboxWatchCheckbox = (CheckBoxPreference) this.findPreference("pref_sentbox_watch");
    if (sentboxWatchCheckbox != null) {
      sentboxWatchCheckbox.setOnPreferenceChangeListener((preference, newValue) -> {
        boolean enabled = (Boolean) newValue;
        dcContext.setConfigInt(CONFIG_SENTBOX_WATCH, enabled? 1 : 0);
        return true;
      });
    }

    bccSelfCheckbox = (CheckBoxPreference) this.findPreference("pref_bcc_self");
    if (bccSelfCheckbox != null) {
      bccSelfCheckbox.setOnPreferenceChangeListener((preference, newValue) -> {
        boolean enabled = (Boolean) newValue;
        dcContext.setConfigInt(CONFIG_BCC_SELF, enabled? 1 : 0);
        return true;
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

    showSystemContacts = (CheckBoxPreference) this.findPreference("pref_show_system_contacts");
    if (showSystemContacts != null) {
      showSystemContacts.setOnPreferenceChangeListener((preference, newValue) -> {
        boolean enabled = (Boolean) newValue;
        dcContext.setConfigInt("ui.android.show_system_contacts", enabled? 1 : 0);
        return true;
      });
    }

    Preference manageKeys = this.findPreference("pref_manage_keys");
    if (manageKeys != null) {
      manageKeys.setOnPreferenceClickListener(new ManageKeysListener());
    }

    Preference screenSecurity = this.findPreference(Prefs.SCREEN_SECURITY_PREF);
    if (screenSecurity != null) {
      screenSecurity.setOnPreferenceChangeListener(new ScreenShotSecurityListener());
    }

    Preference submitDebugLog = this.findPreference("pref_view_log");
    if (submitDebugLog != null) {
      submitDebugLog.setOnPreferenceClickListener(new ViewLogListener());
    }

    Preference webrtcInstance = this.findPreference("pref_webrtc_instance");
    if (webrtcInstance != null) {
      webrtcInstance.setOnPreferenceClickListener(new WebrtcInstanceListener());
    }
    updateWebrtcSummary();

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
            .setTitle("Thanks for trying out \"Broadcast Lists\"!")
            .setMessage("• You can now create new \"Broadcast Lists\" from the \"New Chat\" dialog\n\n"
              + "• In case you are using more than one device, broadcast lists are currently not synced between them\n\n"
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

    Preference developerModeEnabled = this.findPreference("pref_developer_mode_enabled");
    if (developerModeEnabled != null) {
      developerModeEnabled.setOnPreferenceChangeListener((preference, newValue) -> {
        WebView.setWebContentsDebuggingEnabled((Boolean) newValue);
        return true;
      });
    }

    Preference selfReporting = this.findPreference("pref_self_reporting");
    if (selfReporting != null) {
      selfReporting.setOnPreferenceClickListener(((preference) -> {
        try {
          int chatId = getRpc(requireActivity()).draftSelfReport(dcContext.getAccountId());

          Intent intent = new Intent(requireActivity(), ConversationActivity.class);
          intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, chatId);
          intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
          requireActivity().startActivity(intent);
        } catch (RpcException e) {
          Log.e(TAG, "Error calling rpc.draftSelfReport()", e);
        }

        return true;
      }));
    }

    Preference proxySettings = this.findPreference("proxy_settings_button");
    if (proxySettings != null) {
      proxySettings.setOnPreferenceClickListener((preference) -> {
        startActivity(new Intent(requireActivity(), ProxySettingsActivity.class));
        return true;
      });
    }

    Preference passwordAndAccount = this.findPreference("password_account_settings_button");
    if (passwordAndAccount != null) {
      passwordAndAccount.setOnPreferenceClickListener(((preference) -> {
        boolean result = ScreenLockUtil.applyScreenLock(requireActivity(), getString(R.string.pref_password_and_account_settings), getString(R.string.enter_system_secret_to_continue), REQUEST_CODE_CONFIRM_CREDENTIALS_ACCOUNT);
        if (!result) {
          openRegistrationActivity();
        }
        return true;
      }));
    }

    if (dcContext.isChatmail()) {
      this.findPreference("pref_category_encryption").setVisible(false);
      showSystemContacts.setVisible(false);
      sentboxWatchCheckbox.setVisible(false);
      bccSelfCheckbox.setVisible(false);
      mvboxMoveCheckbox.setVisible(false);
      onlyFetchMvboxCheckbox.setVisible(false);
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

    preferE2eeCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_E2EE_ENABLED));
    sentboxWatchCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_SENTBOX_WATCH));
    bccSelfCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_BCC_SELF));
    mvboxMoveCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_MVBOX_MOVE));
    onlyFetchMvboxCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_ONLY_FETCH_MVBOX));
    webxdcRealtimeCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_WEBXDC_REALTIME_ENABLED));
    showSystemContacts.setChecked(0!=dcContext.getConfigInt("ui.android.show_system_contacts"));
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode != RESULT_OK) return;
    if (requestCode == REQUEST_CODE_CONFIRM_CREDENTIALS_KEYS) {
        manageKeys();
    } else if (requestCode == PICK_SELF_KEYS) {
        Uri uri = (data != null ? data.getData() : null);
        if (uri == null) {
            Log.e(TAG, " Can't import null URI");
            return;
        }
      try {
        String name = AttachmentManager.getFileName(getContext(), uri);
        if (name == null || name.isEmpty()) name = "FILE";
        File file = copyToCacheDir(uri);
        showImportKeysDialog(file.getAbsolutePath(), name);
      } catch (IOException e) {
        Log.e(TAG, "Error calling copyToCacheDir()", e);
      }
    } else if (requestCode == REQUEST_CODE_CONFIRM_CREDENTIALS_ACCOUNT) {
      openRegistrationActivity();
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

  private class WebrtcInstanceListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
      View gl = View.inflate(requireActivity(), R.layout.single_line_input, null);
      EditText inputField = gl.findViewById(R.id.input_field);
      inputField.setHint(R.string.videochat_instance_placeholder);
      inputField.setText(dcContext.getConfig(DcHelper.CONFIG_WEBRTC_INSTANCE));
      inputField.setSelection(inputField.getText().length());
      inputField.setInputType(TYPE_TEXT_VARIATION_URI);
      new AlertDialog.Builder(requireActivity())
              .setTitle(R.string.videochat_instance)
              .setMessage(getString(R.string.videochat_instance_explain_2)+"\n\n"+getString(R.string.videochat_instance_example))
              .setView(gl)
              .setNegativeButton(android.R.string.cancel, null)
              .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                dcContext.setConfig(DcHelper.CONFIG_WEBRTC_INSTANCE, inputField.getText().toString());
                updateWebrtcSummary();
              })
              .show();
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

  private void updateWebrtcSummary() {
    Preference webrtcInstance = this.findPreference("pref_webrtc_instance");
    if (webrtcInstance != null) {
      webrtcInstance.setSummary(DcHelper.isWebrtcConfigOk(dcContext)?
              dcContext.getConfig(DcHelper.CONFIG_WEBRTC_INSTANCE) : getString(R.string.none));
    }
  }

  private void updateWebxdcStoreSummary() {
    Preference preference = this.findPreference(Prefs.WEBXDC_STORE_URL_PREF);
    if (preference != null) {
        preference.setSummary(Prefs.getWebxdcStoreUrl(requireActivity()));
    }
  }

  private void openRegistrationActivity() {
    Intent intent = new Intent(requireActivity(), RegistrationActivity.class);
    startActivity(intent);
  }

  /***********************************************************************************************
   * Autocrypt
   **********************************************************************************************/

  private class SendAsmListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
      Activity activity = requireActivity();
      new AlertDialog.Builder(activity)
        .setTitle(activity.getString(R.string.autocrypt_send_asm_title))
        .setMessage(activity.getString(R.string.autocrypt_send_asm_explain_before))
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(R.string.autocrypt_send_asm_button, (dialog, which) -> {
              final String sc = dcContext.initiateKeyTransfer();
              if( sc != null ) {
                String scFormatted = "";
                try {
                  scFormatted = sc.substring(0, 4) + "  -  " + sc.substring(5, 9) + "  -  " + sc.substring(10, 14) + "  -\n\n" +
                      sc.substring(15, 19) + "  -  " + sc.substring(20, 24) + "  -  " + sc.substring(25, 29) + "  -\n\n" +
                      sc.substring(30, 34) + "  -  " + sc.substring(35, 39) + "  -  " + sc.substring(40, 44);
                } catch (Exception e) {
                  Log.e(TAG, "Unexpected exception", e);
                }
                new AlertDialog.Builder(activity)
                  .setTitle(activity.getString(R.string.autocrypt_send_asm_title))
                  .setMessage(activity.getString(R.string.autocrypt_send_asm_explain_after) + "\n\n" + scFormatted)
                  .setPositiveButton(android.R.string.ok, null)
                  .setCancelable(false) // prevent the dialog from being dismissed accidentally (when the dialog is closed, the setup code is gone forever and the user has to create a new setup message)
                  .show();
              }
        })
        .show();
      return true;
    }
  }

  private class PreferE2eeListener implements Preference.OnPreferenceChangeListener {
    @Override
    public boolean onPreferenceChange(@NonNull final Preference preference, Object newValue) {
      boolean enabled = (Boolean) newValue;
      dcContext.setConfigInt(CONFIG_E2EE_ENABLED, enabled? 1 : 0);
      return true;
    }
  }

  /***********************************************************************************************
   * Key Import/Export
   **********************************************************************************************/
  protected void showImportKeysDialog(String imexPath, String pathAsDisplayedToUser) {
    new AlertDialog.Builder(requireActivity())
      .setTitle(R.string.pref_managekeys_import_secret_keys)
      .setMessage(requireActivity().getString(R.string.pref_managekeys_import_explain, pathAsDisplayedToUser))
      .setNegativeButton(android.R.string.cancel, null)
      .setPositiveButton(android.R.string.ok, (dialogInterface2, i2) -> startImexOne(DcContext.DC_IMEX_IMPORT_SELF_KEYS, imexPath, pathAsDisplayedToUser))
      .show();
  }

  private class ManageKeysListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
      boolean result = ScreenLockUtil.applyScreenLock(requireActivity(), getString(R.string.pref_manage_keys), getString(R.string.enter_system_secret_to_continue), REQUEST_CODE_CONFIRM_CREDENTIALS_KEYS);
      if (!result) {
        manageKeys();
      }
      return true;
    }
  }

  private void manageKeys() {
    Activity activity = requireActivity();
    Permissions.with(activity)
        .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
        .alwaysGrantOnSdk30()
        .ifNecessary()
        .withPermanentDenialDialog(getString(R.string.perm_explain_access_to_storage_denied))
        .onAllGranted(() -> {
          new AlertDialog.Builder(activity)
              .setTitle(R.string.pref_managekeys_menu_title)
              .setItems(new CharSequence[]{
                      activity.getString(R.string.pref_managekeys_export_secret_keys),
                      activity.getString(R.string.pref_managekeys_import_secret_keys)
                  },
                  (dialogInterface, i) -> {
                    if (i==0) {
                      new AlertDialog.Builder(activity)
                          .setTitle(R.string.pref_managekeys_export_secret_keys)
                          .setMessage(activity.getString(R.string.pref_managekeys_export_explain, DcHelper.getImexDir().getAbsolutePath()))
                          .setNegativeButton(android.R.string.cancel, null)
                          .setPositiveButton(android.R.string.ok, (dialogInterface2, i2) -> startImexOne(DcContext.DC_IMEX_EXPORT_SELF_KEYS))
                          .show();
                    }
                    else {
                      if (Build.VERSION.SDK_INT >= 30) {
                        AttachmentManager.selectMediaType(activity, "*/*", null, PICK_SELF_KEYS, StorageUtil.getDownloadUri());
                      } else {
                        String path = DcHelper.getImexDir().getAbsolutePath();
                        showImportKeysDialog(path, path);
                      }
                    }
                  }
              )
              .setNegativeButton(R.string.cancel, null)
              .setNeutralButton(R.string.learn_more, (d, w) -> DcHelper.openHelp(activity, "#importkey"))
              .show();
        })
        .execute();
  }
}
