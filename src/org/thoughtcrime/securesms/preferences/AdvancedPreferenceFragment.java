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
import android.widget.Toast;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEventCenter;

import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.LogViewActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.ScreenLockUtil;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.views.ProgressDialog;

import static android.app.Activity.RESULT_OK;


public class AdvancedPreferenceFragment extends CorrectedPreferenceFragment
                                        implements DcEventCenter.DcEventDelegate
{
  private static final String TAG = AdvancedPreferenceFragment.class.getSimpleName();

  private static final int REQUEST_CODE_CONFIRM_CREDENTIALS_BACKUP = ScreenLockUtil.REQUEST_CODE_CONFIRM_CREDENTIALS + 1;

  private static final int REQUEST_CODE_CONFIRM_CREDENTIALS_KEYS = REQUEST_CODE_CONFIRM_CREDENTIALS_BACKUP + 1;

  private ApplicationDcContext dcContext;

  CheckBoxPreference preferE2eeCheckbox;
  CheckBoxPreference inboxWatchCheckbox;
  CheckBoxPreference sentboxWatchCheckbox;
  CheckBoxPreference mvboxWatchCheckbox;
  CheckBoxPreference mvboxMoveCheckbox;

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);

    dcContext = DcHelper.getContext(getContext());
    dcContext.eventCenter.addObserver(this, DcContext.DC_EVENT_IMEX_PROGRESS);

    Preference sendAsm = this.findPreference("pref_send_autocrypt_setup_message");
    sendAsm.setOnPreferenceClickListener(new SendAsmListener());

    preferE2eeCheckbox = (CheckBoxPreference) this.findPreference("pref_prefer_e2ee");
    preferE2eeCheckbox.setOnPreferenceChangeListener(new PreferE2eeListener());

    inboxWatchCheckbox = (CheckBoxPreference) this.findPreference("pref_inbox_watch");
    inboxWatchCheckbox.setOnPreferenceChangeListener((preference, newValue) ->
      handleImapCheck(preference, newValue, "inbox_watch")
    );

    sentboxWatchCheckbox = (CheckBoxPreference) this.findPreference("pref_sentbox_watch");
    sentboxWatchCheckbox.setOnPreferenceChangeListener((preference, newValue) ->
      handleImapCheck(preference, newValue, "sentbox_watch")
    );

    mvboxWatchCheckbox = (CheckBoxPreference) this.findPreference("pref_mvbox_watch");
    mvboxWatchCheckbox.setOnPreferenceChangeListener((preference, newValue) ->
      handleImapCheck(preference, newValue, "mvbox_watch")
    );

    mvboxMoveCheckbox = (CheckBoxPreference) this.findPreference("pref_mvbox_move");
    mvboxMoveCheckbox.setOnPreferenceChangeListener((preference, newValue) -> {
      boolean enabled = (Boolean) newValue;
      dcContext.setConfigInt("mvbox_move", enabled? 1 : 0);
      return true;
    });

    Preference backup = this.findPreference("pref_backup");
    backup.setOnPreferenceClickListener(new BackupListener());

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
  public void onDestroy() {
    dcContext.eventCenter.removeObservers(this);
    super.onDestroy();
  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.preferences_advanced);
  }

  @Override
  public void onResume() {
    super.onResume();
    ((ApplicationPreferencesActivity) getActivity()).getSupportActionBar().setTitle(R.string.menu_advanced);

    preferE2eeCheckbox.setChecked(0!=dcContext.getConfigInt("e2ee_enabled", DcContext.DC_PREF_DEFAULT_E2EE_ENABLED));
    inboxWatchCheckbox.setChecked(0!=dcContext.getConfigInt("inbox_watch", 1));
    sentboxWatchCheckbox.setChecked(0!=dcContext.getConfigInt("sentbox_watch", 1));
    mvboxWatchCheckbox.setChecked(0!=dcContext.getConfigInt("mvbox_watch", 1));
    mvboxMoveCheckbox.setChecked(0!=dcContext.getConfigInt("mvbox_move", 1));
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
      if (resultCode == RESULT_OK) {
        if (requestCode == REQUEST_CODE_CONFIRM_CREDENTIALS_BACKUP) {
          performBackup();
        } else if (requestCode == REQUEST_CODE_CONFIRM_CREDENTIALS_KEYS) {
          exportKeys();
        }
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
      dcContext.setConfigInt("e2ee_enabled", enabled? 1 : 0);
      return true;
    }
  }

  /***********************************************************************************************
   * Import/Export
   **********************************************************************************************/

  private class BackupListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(Preference preference) {
      boolean result = ScreenLockUtil.applyScreenLock(getActivity(), REQUEST_CODE_CONFIRM_CREDENTIALS_BACKUP);
      if (!result) {
        performBackup();
      }
      return true;
    }
  }

  private void performBackup() {
    Permissions.with(getActivity())
        .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
        .ifNecessary()
        .withRationaleDialog(getActivity().getString(R.string.perm_explain_need_for_storage_access), R.drawable.ic_folder_white_48dp)
        .onAllGranted(() -> {
          new AlertDialog.Builder(getActivity())
              .setTitle(R.string.pref_backup)
              .setMessage(R.string.pref_backup_export_explain)
              .setNegativeButton(android.R.string.cancel, null)
              .setPositiveButton(R.string.pref_backup_export_start_button, (dialogInterface, i) -> startImex(DcContext.DC_IMEX_EXPORT_BACKUP))
              .show();
        })
        .execute();
  }

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

  private ProgressDialog progressDialog = null;
  private int            progressWhat = 0;
  private String         imexDir = "";
  private void startImex(int what)
  {
    if( progressDialog!=null ) {
      progressDialog.dismiss();
      progressDialog = null;
    }
    progressWhat = what;
    progressDialog = new ProgressDialog(getActivity());
    progressDialog.setMessage(getActivity().getString(R.string.one_moment));
    progressDialog.setCanceledOnTouchOutside(false);
    progressDialog.setCancelable(false);
    progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getActivity().getString(android.R.string.cancel), (dialog, which) -> dcContext.stopOngoingProcess());
    progressDialog.show();

    imexDir = dcContext.getImexDir().getAbsolutePath();
    dcContext.captureNextError();
    dcContext.imex(progressWhat, imexDir);
  }

  @Override
  public void handleEvent(int eventId, Object data1, Object data2) {
    if (eventId== DcContext.DC_EVENT_IMEX_PROGRESS) {
      long progress = (Long)data1;
      if (progress==0/*error/aborted*/) {
        dcContext.endCaptureNextError();
        progressDialog.dismiss();
        progressDialog = null;
        if (dcContext.hasCapturedError()) {
          new AlertDialog.Builder(getActivity())
              .setMessage(dcContext.getCapturedError())
              .setPositiveButton(android.R.string.ok, null)
              .show();
        }
      }
      else if (progress<1000/*progress in permille*/) {
        int percent = (int)progress / 10;
        progressDialog.setMessage(getResources().getString(R.string.one_moment)+String.format(" %d%%", percent));
      }
      else if (progress==1000/*done*/) {
        dcContext.endCaptureNextError();
        progressDialog.dismiss();
        progressDialog = null;
        String msg = "";
        if (progressWhat==DcContext.DC_IMEX_EXPORT_BACKUP) {
          msg = getActivity().getString(R.string.pref_backup_written_to_x, imexDir);
        }
        else if (progressWhat==DcContext.DC_IMEX_EXPORT_SELF_KEYS) {
          msg = getActivity().getString(R.string.pref_managekeys_secret_keys_exported_to_x, imexDir);
        }
        else if (progressWhat==DcContext.DC_IMEX_IMPORT_SELF_KEYS) {
          msg = getActivity().getString(R.string.pref_managekeys_secret_keys_imported_from_x, imexDir);
        }
        new AlertDialog.Builder(getActivity())
            .setMessage(msg)
            .setPositiveButton(android.R.string.ok, null)
            .show();
      }
    }
  }
}
