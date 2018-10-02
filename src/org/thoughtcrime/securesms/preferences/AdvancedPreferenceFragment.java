package org.thoughtcrime.securesms.preferences;

import android.Manifest;
import android.app.ProgressDialog;
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
import org.thoughtcrime.securesms.LogSubmitActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.ScreenLockUtil;
import org.thoughtcrime.securesms.util.Util;

import static android.app.Activity.RESULT_OK;


public class AdvancedPreferenceFragment extends CorrectedPreferenceFragment
                                        implements DcEventCenter.DcEventDelegate
{
  private static final String TAG = AdvancedPreferenceFragment.class.getSimpleName();

  private static final int REQUEST_CODE_CONFIRM_CREDENTIALS_BACKUP = ScreenLockUtil.REQUEST_CODE_CONFIRM_CREDENTIALS + 1;

  private static final int REQUEST_CODE_CONFIRM_CREDENTIALS_KEYS = REQUEST_CODE_CONFIRM_CREDENTIALS_BACKUP + 1;

  private ApplicationDcContext dcContext;

  CheckBoxPreference preferE2eeCheckbox;

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);

    dcContext = DcHelper.getContext(getContext());
    dcContext.eventCenter.addObserver(this, DcContext.DC_EVENT_IMEX_PROGRESS);

    Preference sendAsm = this.findPreference("pref_send_autocrypt_setup_message");
    sendAsm.setOnPreferenceClickListener(new SendAsmListener());

    preferE2eeCheckbox = (CheckBoxPreference) this.findPreference("pref_prefer_e2ee");
    preferE2eeCheckbox.setOnPreferenceChangeListener(new PreferE2eeListener());

    Preference backup = this.findPreference("pref_backup");
    backup.setOnPreferenceClickListener(new BackupListener());

    Preference manageKeys = this.findPreference("pref_manage_keys");
    manageKeys.setOnPreferenceClickListener(new ManageKeysListener());

    Preference submitDebugLog = this.findPreference("pref_submit_debug_logs");
    submitDebugLog.setOnPreferenceClickListener(new SubmitDebugLogListener());
    submitDebugLog.setSummary(getVersion(getActivity()));
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
    ((ApplicationPreferencesActivity) getActivity()).getSupportActionBar().setTitle(R.string.preferences__advanced);

    preferE2eeCheckbox.setChecked(0!=dcContext.getConfigInt("e2ee_enabled", DcContext.DC_PREF_DEFAULT_E2EE_ENABLED));

  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (resultCode == RESULT_OK) {
        if (requestCode == REQUEST_CODE_CONFIRM_CREDENTIALS_BACKUP) {
          performBackup();
        } else if (requestCode == REQUEST_CODE_CONFIRM_CREDENTIALS_KEYS) {
          exportKeys();
        }
      } else {
        Toast.makeText(getActivity(), R.string.security_authentication_failed, Toast.LENGTH_SHORT).show();
      }
  }

  private @NonNull String getVersion(@Nullable Context context) {
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

  private class SubmitDebugLogListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(Preference preference) {
      final Intent intent = new Intent(getActivity(), LogSubmitActivity.class);
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
        .setTitle(getActivity().getString(R.string.preferences_autocrypt__send_asm))
        .setMessage(getActivity().getString(R.string.autocrypt__msg_before_sending_asm))
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(R.string.autocrypt__button_send_asm, (dialog, which) -> {

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
                  .setTitle(getActivity().getString(R.string.preferences_autocrypt__send_asm))
                  .setMessage(getActivity().getString(R.string.autocrypt__msg_after_sending_asm, scFormatted))
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
      ScreenLockUtil.applyScreenLock(getActivity(), REQUEST_CODE_CONFIRM_CREDENTIALS_BACKUP);
      return true;
    }
  }

  private void performBackup() {
    Permissions.with(getActivity())
        .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
        .ifNecessary()
        .withRationaleDialog(getActivity().getString(R.string.preferences_backup__ask_for_storage_permission), R.drawable.ic_folder_white_48dp)
        .onAllGranted(() -> {
          new AlertDialog.Builder(getActivity())
              .setTitle(R.string.preferences__backup)
              .setMessage(R.string.preferences_backup__export_explain)
              .setNegativeButton(android.R.string.cancel, null)
              .setPositiveButton(R.string.preferences_backup__export_start_button, (dialogInterface, i) -> startImex(DcContext.DC_IMEX_EXPORT_BACKUP))
              .show();
        })
        .execute();
  }

  private class ManageKeysListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(Preference preference) {
      ScreenLockUtil.applyScreenLock(getActivity(), REQUEST_CODE_CONFIRM_CREDENTIALS_KEYS);
      return true;
    }
  }

  private void exportKeys() {
    Permissions.with(getActivity())
        .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
        .ifNecessary()
        .withRationaleDialog(getActivity().getString(R.string.preferences_managekeys__ask_for_storage_permission), R.drawable.ic_folder_white_48dp)
        .onAllGranted(() -> {
          new android.app.AlertDialog.Builder(getActivity())
              .setTitle(R.string.preferences_managekeys__menu_title)
              .setItems(new CharSequence[]{
                      getActivity().getString(R.string.preferences_managekeys__export_secret_keys),
                      getActivity().getString(R.string.preferences_managekeys__import_secret_keys)
                  },
                  (dialogInterface, i) -> {
                    if (i==0) {
                      new AlertDialog.Builder(getActivity())
                          .setTitle(R.string.preferences_managekeys__export_secret_keys)
                          .setMessage(getActivity().getString(R.string.preferences_managekeys__export_explain, dcContext.getImexDir().getAbsolutePath()))
                          .setNegativeButton(android.R.string.cancel, null)
                          .setPositiveButton(android.R.string.ok, (dialogInterface2, i2) -> startImex(DcContext.DC_IMEX_EXPORT_SELF_KEYS))
                          .show();
                    }
                    else {
                      new AlertDialog.Builder(getActivity())
                          .setTitle(R.string.preferences_managekeys__import_secret_keys)
                          .setMessage(getActivity().getString(R.string.preferences_managekeys__import_explain, dcContext.getImexDir().getAbsolutePath()))
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
          new android.app.AlertDialog.Builder(getActivity())
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
          msg = getActivity().getString(R.string.preferences_backup__backup_written_to_x, imexDir);
        }
        else if (progressWhat==DcContext.DC_IMEX_EXPORT_SELF_KEYS) {
          msg = getActivity().getString(R.string.preferences_managekeys__secret_keys_exported_to_x, imexDir);
        }
        else if (progressWhat==DcContext.DC_IMEX_IMPORT_SELF_KEYS) {
          msg = getActivity().getString(R.string.preferences_managekeys__secret_keys_imported_from_x, imexDir);
        }
        new android.app.AlertDialog.Builder(getActivity())
            .setMessage(msg)
            .setPositiveButton(android.R.string.ok, null)
            .show();
      }
    }
  }
}
