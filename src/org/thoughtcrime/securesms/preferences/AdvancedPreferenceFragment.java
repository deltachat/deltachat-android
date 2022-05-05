package org.thoughtcrime.securesms.preferences;

import static android.app.Activity.RESULT_OK;
import static android.text.InputType.TYPE_TEXT_VARIATION_URI;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_BCC_SELF;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_DEBUG_LOGGING;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_E2EE_ENABLED;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_MVBOX_MOVE;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_ONLY_FETCH_MVBOX;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SENTBOX_WATCH;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.preference.Preference;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.LogViewActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ScreenLockUtil;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.views.ProgressDialog;


public class AdvancedPreferenceFragment extends ListSummaryPreferenceFragment
                                        implements DcEventCenter.DcEventDelegate
{
  private static final String TAG = AdvancedPreferenceFragment.class.getSimpleName();




  CheckBoxPreference preferE2eeCheckbox;
  CheckBoxPreference sentboxWatchCheckbox;
  CheckBoxPreference bccSelfCheckbox;
  CheckBoxPreference mvboxMoveCheckbox;
  CheckBoxPreference onlyFetchMvboxCheckbox;
  CheckBoxPreference debugLoggingCheckbox;

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);

    Preference sendAsm = this.findPreference("pref_send_autocrypt_setup_message");
    sendAsm.setOnPreferenceClickListener(new SendAsmListener());

    preferE2eeCheckbox = (CheckBoxPreference) this.findPreference("pref_prefer_e2ee");
    preferE2eeCheckbox.setOnPreferenceChangeListener(new PreferE2eeListener());

    sentboxWatchCheckbox = (CheckBoxPreference) this.findPreference("pref_sentbox_watch");
    sentboxWatchCheckbox.setOnPreferenceChangeListener((preference, newValue) -> {
      boolean enabled = (Boolean) newValue;
      DcHelper.getAccounts(getContext()).stopIo();
      dcContext.setConfigInt(CONFIG_SENTBOX_WATCH, enabled? 1 : 0);
      DcHelper.getAccounts(getContext()).startIo();
      return true;
    });

    bccSelfCheckbox = (CheckBoxPreference) this.findPreference("pref_bcc_self");
    bccSelfCheckbox.setOnPreferenceChangeListener((preference, newValue) -> {
      boolean enabled = (Boolean) newValue;
      dcContext.setConfigInt(CONFIG_BCC_SELF, enabled? 1 : 0);
      return true;
    });

    mvboxMoveCheckbox = (CheckBoxPreference) this.findPreference("pref_mvbox_move");
    mvboxMoveCheckbox.setOnPreferenceChangeListener((preference, newValue) -> {
      boolean enabled = (Boolean) newValue;
      DcHelper.getAccounts(getContext()).stopIo();
      dcContext.setConfigInt(CONFIG_MVBOX_MOVE, enabled? 1 : 0);
      DcHelper.getAccounts(getContext()).startIo();
      return true;
    });

    onlyFetchMvboxCheckbox = this.findPreference("pref_only_fetch_mvbox");
    onlyFetchMvboxCheckbox.setOnPreferenceChangeListener(((preference, newValue) -> {
      final boolean enabled = (Boolean) newValue;
      if (enabled) {
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.pref_imap_folder_warn_disable_defaults)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                  DcHelper.getAccounts(getContext()).stopIo();
                  dcContext.setConfigInt(CONFIG_ONLY_FETCH_MVBOX, 1);
                  ((CheckBoxPreference)preference).setChecked(true);
                  DcHelper.getAccounts(getContext()).startIo();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        return false;
      } else {
        DcHelper.getAccounts(getContext()).stopIo();
        dcContext.setConfigInt(CONFIG_ONLY_FETCH_MVBOX, 0);
        DcHelper.getAccounts(getContext()).startIo();
        return true;
      }
    }));

    Preference manageKeys = this.findPreference("pref_manage_keys");
    manageKeys.setOnPreferenceClickListener(new ManageKeysListener());

    Preference screenSecurity = this.findPreference(Prefs.SCREEN_SECURITY_PREF);
    screenSecurity.setOnPreferenceChangeListener(new ScreenShotSecurityListener());

    Preference submitDebugLog = this.findPreference("pref_view_log");
    submitDebugLog.setOnPreferenceClickListener(new ViewLogListener());

    Preference webrtcInstance = this.findPreference("pref_webrtc_instance");
    webrtcInstance.setOnPreferenceClickListener(new WebrtcInstanceListener());
    updateWebrtcSummary();

    Preference newBroadcastList = this.findPreference("pref_new_broadcast_list");
    newBroadcastList.setOnPreferenceChangeListener((preference, newValue) -> {
      if ((Boolean)newValue) {
        new AlertDialog.Builder(getActivity())
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

    Preference locationStreamingEnabled = this.findPreference("pref_location_streaming_enabled");
    locationStreamingEnabled.setOnPreferenceChangeListener((preference, newValue) -> {
      if ((Boolean)newValue) {
        new AlertDialog.Builder(getActivity())
          .setTitle("Thanks for trying out \"Location Streaming\"!")
          .setMessage("• You will find a corresponding option in the attach menu (the paper clip) of each chat now\n\n"
            + "• If you want to quit the experimental feature, you can disable it at \"Settings / Advanced\"")
          .setCancelable(false)
          .setPositiveButton(R.string.ok, null)
          .show();
      }
      return true;
    });

    Preference developerModeEnabled = this.findPreference("pref_developer_mode_enabled");
    developerModeEnabled.setOnPreferenceChangeListener((preference, newValue) -> {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        WebView.setWebContentsDebuggingEnabled((Boolean) newValue);
      }
      return true;
    });

    debugLoggingCheckbox = (CheckBoxPreference) this.findPreference("pref_debug_logging_enabled");
    debugLoggingCheckbox.setOnPreferenceChangeListener((preference, newValue) -> {
      boolean enabled = (Boolean) newValue;
      dcContext.setConfigInt(CONFIG_DEBUG_LOGGING, enabled? 1 : 0);
      return true;
    });
  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.preferences_advanced);
  }

  @Override
  public void onResume() {
    super.onResume();
    ((ApplicationPreferencesActivity) getActivity()).getSupportActionBar().setTitle(R.string.menu_advanced);

    preferE2eeCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_E2EE_ENABLED));
    sentboxWatchCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_SENTBOX_WATCH));
    bccSelfCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_BCC_SELF));
    mvboxMoveCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_MVBOX_MOVE));
    onlyFetchMvboxCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_ONLY_FETCH_MVBOX));
    debugLoggingCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_DEBUG_LOGGING));
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
      if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_CONFIRM_CREDENTIALS_KEYS) {
          exportKeys();
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
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      boolean enabled = (Boolean) newValue;
      Prefs.setScreenSecurityEnabled(getContext(), enabled);
      Toast.makeText(getContext(), R.string.pref_screen_security_please_restart_hint, Toast.LENGTH_LONG).show();
      return true;
    }
  }

  private class ViewLogListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(Preference preference) {
      final Intent intent = new Intent(getActivity(), LogViewActivity.class);
      startActivity(intent);
      return true;
    }
  }

  private class WebrtcInstanceListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(Preference preference) {
      View gl = View.inflate(getActivity(), R.layout.single_line_input, null);
      EditText inputField = gl.findViewById(R.id.input_field);
      inputField.setHint(R.string.videochat_instance_placeholder);
      inputField.setText(dcContext.getConfig(DcHelper.CONFIG_WEBRTC_INSTANCE));
      inputField.setSelection(inputField.getText().length());
      inputField.setInputType(TYPE_TEXT_VARIATION_URI);
      new AlertDialog.Builder(getActivity())
              .setTitle(R.string.videochat_instance)
              .setMessage(R.string.videochat_instance_explain)
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

  private void updateWebrtcSummary() {
    Preference webrtcInstance = this.findPreference("pref_webrtc_instance");
    if (webrtcInstance != null) {
      webrtcInstance.setSummary(DcHelper.isWebrtcConfigOk(dcContext)?
              dcContext.getConfig(DcHelper.CONFIG_WEBRTC_INSTANCE) : getString(R.string.none));
    }
  }

  /***********************************************************************************************
   * Autocrypt
   **********************************************************************************************/

  private class SendAsmListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(Preference preference) {
      new AlertDialog.Builder(getActivity())
        .setTitle(getActivity().getString(R.string.autocrypt_send_asm_title))
        .setMessage(getActivity().getString(R.string.autocrypt_send_asm_explain_before))
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(R.string.autocrypt_send_asm_button, (dialog, which) -> {

          progressDialog = new ProgressDialog(getActivity());
          progressDialog.setMessage(getActivity().getString(R.string.one_moment));
          progressDialog.setCanceledOnTouchOutside(false);
          progressDialog.setCancelable(false);
          progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getActivity().getString(android.R.string.cancel), (dialog1, which1) -> dcContext.stopOngoingProcess());
          progressDialog.show();


          new Thread(() -> {
            final String sc = dcContext.initiateKeyTransfer();
            Util.runOnMain(() -> {
              if( progressDialog != null ) {
                progressDialog.dismiss();
                progressDialog = null;
              }

              if( sc != null ) {
                String scFormatted = "";
                try {
                  scFormatted = sc.substring(0, 4) + "  -  " + sc.substring(5, 9) + "  -  " + sc.substring(10, 14) + "  -\n\n" +
                      sc.substring(15, 19) + "  -  " + sc.substring(20, 24) + "  -  " + sc.substring(25, 29) + "  -\n\n" +
                      sc.substring(30, 34) + "  -  " + sc.substring(35, 39) + "  -  " + sc.substring(40, 44);
                } catch (Exception e) {
                  e.printStackTrace();
                }
                new AlertDialog.Builder(getActivity())
                  .setTitle(getActivity().getString(R.string.autocrypt_send_asm_title))
                  .setMessage(getActivity().getString(R.string.autocrypt_send_asm_explain_after, scFormatted))
                  .setPositiveButton(android.R.string.ok, null)
                  .setCancelable(false) // prevent the dialog from being dismissed accidentally (when the dialog is closed, the setup code is gone forever and the user has to create a new setup message)
                  .show();
              }
            });
          }).start();


        })
        .show();
      return true;
    }
  }

  private class PreferE2eeListener implements Preference.OnPreferenceChangeListener {
    @Override
    public boolean onPreferenceChange(final Preference preference, Object newValue) {
      boolean enabled = (Boolean) newValue;
      dcContext.setConfigInt(CONFIG_E2EE_ENABLED, enabled? 1 : 0);
      return true;
    }
  }

  /***********************************************************************************************
   * Key Import/Export
   **********************************************************************************************/
  private class ManageKeysListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(Preference preference) {
      boolean result = ScreenLockUtil.applyScreenLock(getActivity(), getString(R.string.pref_manage_keys), REQUEST_CODE_CONFIRM_CREDENTIALS_KEYS);
      if (!result) {
        exportKeys();
      }
      return true;
    }
  }

  private void exportKeys() {
    Permissions.with(getActivity())
        .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
        .ifNecessary()
        .withPermanentDenialDialog(getString(R.string.perm_explain_access_to_storage_denied))
        .onAllGranted(() -> {
          new AlertDialog.Builder(getActivity())
              .setTitle(R.string.pref_managekeys_menu_title)
              .setItems(new CharSequence[]{
                      getActivity().getString(R.string.pref_managekeys_export_secret_keys),
                      getActivity().getString(R.string.pref_managekeys_import_secret_keys)
                  },
                  (dialogInterface, i) -> {
                    if (i==0) {
                      new AlertDialog.Builder(getActivity())
                          .setTitle(R.string.pref_managekeys_export_secret_keys)
                          .setMessage(getActivity().getString(R.string.pref_managekeys_export_explain, DcHelper.getImexDir().getAbsolutePath()))
                          .setNegativeButton(android.R.string.cancel, null)
                          .setPositiveButton(android.R.string.ok, (dialogInterface2, i2) -> startImex(DcContext.DC_IMEX_EXPORT_SELF_KEYS))
                          .show();
                    }
                    else {
                      new AlertDialog.Builder(getActivity())
                          .setTitle(R.string.pref_managekeys_import_secret_keys)
                          .setMessage(getActivity().getString(R.string.pref_managekeys_import_explain, DcHelper.getImexDir().getAbsolutePath()))
                          .setNegativeButton(android.R.string.cancel, null)
                          .setPositiveButton(android.R.string.ok, (dialogInterface2, i2) -> startImex(DcContext.DC_IMEX_IMPORT_SELF_KEYS))
                          .show();
                    }
                  }
              )
              .show();
        })
        .execute();
  }
}
