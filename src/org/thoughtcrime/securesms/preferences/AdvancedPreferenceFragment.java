package org.thoughtcrime.securesms.preferences;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEventCenter;

import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.LogViewActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.ScreenLockUtil;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.views.ProgressDialog;

import static android.app.Activity.RESULT_OK;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_E2EE_ENABLED;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_INBOX_WATCH;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_MVBOX_MOVE;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_MVBOX_WATCH;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SENTBOX_WATCH;


public class AdvancedPreferenceFragment extends ListSummaryPreferenceFragment
                                        implements DcEventCenter.DcEventDelegate
{
  private static final String TAG = AdvancedPreferenceFragment.class.getSimpleName();




  CheckBoxPreference preferE2eeCheckbox;
  CheckBoxPreference inboxWatchCheckbox;
  CheckBoxPreference sentboxWatchCheckbox;
  CheckBoxPreference mvboxWatchCheckbox;
  CheckBoxPreference mvboxMoveCheckbox;

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);

    Preference sendAsm = this.findPreference("pref_send_autocrypt_setup_message");
    sendAsm.setOnPreferenceClickListener(new SendAsmListener());

    preferE2eeCheckbox = (CheckBoxPreference) this.findPreference("pref_prefer_e2ee");
    preferE2eeCheckbox.setOnPreferenceChangeListener(new PreferE2eeListener());

    inboxWatchCheckbox = (CheckBoxPreference) this.findPreference("pref_inbox_watch");
    inboxWatchCheckbox.setOnPreferenceChangeListener((preference, newValue) ->
      handleImapCheck(preference, newValue, CONFIG_INBOX_WATCH)
    );

    sentboxWatchCheckbox = (CheckBoxPreference) this.findPreference("pref_sentbox_watch");
    sentboxWatchCheckbox.setOnPreferenceChangeListener((preference, newValue) ->
      handleImapCheck(preference, newValue, CONFIG_SENTBOX_WATCH)
    );

    mvboxWatchCheckbox = (CheckBoxPreference) this.findPreference("pref_mvbox_watch");
    mvboxWatchCheckbox.setOnPreferenceChangeListener((preference, newValue) ->
      handleImapCheck(preference, newValue, CONFIG_MVBOX_WATCH)
    );

    mvboxMoveCheckbox = (CheckBoxPreference) this.findPreference("pref_mvbox_move");
    mvboxMoveCheckbox.setOnPreferenceChangeListener((preference, newValue) -> {
      boolean enabled = (Boolean) newValue;
      dcContext.setConfigInt(CONFIG_MVBOX_MOVE, enabled? 1 : 0);
      return true;
    });

    Preference emptyServerFolders = this.findPreference("pref_empty_server_folders");
    if(emptyServerFolders!=null) {
      emptyServerFolders.setOnPreferenceClickListener(new EmptyServerFoldersListener());
    }

    Preference manageKeys = this.findPreference("pref_manage_keys");
    manageKeys.setOnPreferenceClickListener(new ManageKeysListener());

    Preference submitDebugLog = this.findPreference("pref_view_log");
    submitDebugLog.setOnPreferenceClickListener(new ViewLogListener());
  }

  private boolean handleImapCheck(Preference preference, Object newValue, String dc_config_name) {
    final boolean newEnabled = (Boolean) newValue;
    if(newEnabled) {
      dcContext.setConfigInt(dc_config_name, 1);
      return true;
    }
    else {
      new AlertDialog.Builder(getContext())
        .setMessage(R.string.pref_imap_folder_warn_disable_defaults)
        .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
          dcContext.setConfigInt(dc_config_name, 0);
          ((CheckBoxPreference)preference).setChecked(false);
        })
        .setNegativeButton(R.string.cancel, null)
        .show();
      return false;
    }

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
    inboxWatchCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_INBOX_WATCH));
    sentboxWatchCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_SENTBOX_WATCH));
    mvboxWatchCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_MVBOX_WATCH));
    mvboxMoveCheckbox.setChecked(0!=dcContext.getConfigInt(CONFIG_MVBOX_MOVE));
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
      if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_CONFIRM_CREDENTIALS_KEYS) {
          exportKeys();
      } else {
        Toast.makeText(getActivity(), R.string.screenlock_authentication_failed, Toast.LENGTH_SHORT).show();
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

  private class ViewLogListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(Preference preference) {
      final Intent intent = new Intent(getActivity(), LogViewActivity.class);
      startActivity(intent);
      return true;
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
      boolean result = ScreenLockUtil.applyScreenLock(getActivity(), REQUEST_CODE_CONFIRM_CREDENTIALS_KEYS);
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
        .withRationaleDialog(getActivity().getString(R.string.pref_managekeys_ask_for_storage_permission), R.drawable.ic_folder_white_48dp)
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
                          .setMessage(getActivity().getString(R.string.pref_managekeys_export_explain, dcContext.getImexDir().getAbsolutePath()))
                          .setNegativeButton(android.R.string.cancel, null)
                          .setPositiveButton(android.R.string.ok, (dialogInterface2, i2) -> startImex(DcContext.DC_IMEX_EXPORT_SELF_KEYS))
                          .show();
                    }
                    else {
                      new AlertDialog.Builder(getActivity())
                          .setTitle(R.string.pref_managekeys_import_secret_keys)
                          .setMessage(getActivity().getString(R.string.pref_managekeys_import_explain, dcContext.getImexDir().getAbsolutePath()))
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


  /***********************************************************************************************
   * Empty server folder
   **********************************************************************************************/

  private class EmptyServerFoldersListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(Preference preference) {
      View gl = View.inflate(getActivity(), R.layout.empty_folder_options, null);
      new AlertDialog.Builder(getActivity())
          .setTitle("Empty folders on server")
          .setMessage("This will delete all messages in the given folders on the server.")
          .setView(gl)
          .setNegativeButton(R.string.cancel, null)
          .setPositiveButton(R.string.ok, (dialog, which) -> {
            int flags = 0;
            CheckBox cb = gl.findViewById(R.id.empty_inbox_chat_folder);
            if (cb!=null && cb.isChecked()) {
              flags |= DC_EMPTY_INBOX;
            }
            if (cb!=null && cb.isChecked()) {
              flags |= DC_EMPTY_MVBOX;
            }

          })
          .show();
      return true;
    }
  }

}
