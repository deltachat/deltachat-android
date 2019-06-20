package org.thoughtcrime.securesms.preferences;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.widget.Toast;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.preferences.widgets.ListPreferenceWithSummary;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ScreenLockUtil;
import org.thoughtcrime.securesms.util.Util;

import static android.app.Activity.RESULT_OK;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SHOW_EMAILS;

public class ChatsPreferenceFragment extends ListSummaryPreferenceFragment {
  private static final String TAG = ChatsPreferenceFragment.class.getSimpleName();


  ListPreferenceWithSummary showEmails;

//  CheckBoxPreference trimEnabledCheckbox;

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);

    findPreference(Prefs.MESSAGE_BODY_TEXT_SIZE_PREF)
            .setOnPreferenceChangeListener(new ListSummaryListener());

    findPreference("pref_compression")
            .setOnPreferenceChangeListener(new ListSummaryListener());

    showEmails = (ListPreferenceWithSummary) this.findPreference("pref_show_emails");
    showEmails.setOnPreferenceChangeListener((preference, newValue) -> {
      updateListSummary(preference, newValue);
      dcContext.setConfigInt(CONFIG_SHOW_EMAILS, Util.objectToInt(newValue));
      return true;
    });

    Preference backup = this.findPreference("pref_backup");
    backup.setOnPreferenceClickListener(new BackupListener());

//    trimEnabledCheckbox = (CheckBoxPreference) findPreference("pref_trim_threads");
//    trimEnabledCheckbox.setOnPreferenceChangeListener(new TrimEnabledListener());
//
//    findPreference("pref_trim_length")
//        .setOnPreferenceChangeListener(new TrimLengthValidationListener());
//    findPreference("pref_trim_now")
//        .setOnPreferenceClickListener(new TrimNowClickListener());

    initializeListSummary((ListPreference) findPreference(Prefs.MESSAGE_BODY_TEXT_SIZE_PREF));

  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.preferences_chats);
  }

  @Override
  public void onResume() {
    super.onResume();
    ((ApplicationPreferencesActivity)getActivity()).getSupportActionBar().setTitle(R.string.pref_chats_and_media);

    initializeListSummary((ListPreferenceWithSummary) findPreference("pref_compression"));

    String value = Integer.toString(dcContext.getConfigInt("pref_show_emails"));
    showEmails.setValue(value);
    updateListSummary(showEmails, value);

//    trimEnabledCheckbox.setChecked(0!=dcContext.getConfigInt("trim_enabled", DcContext.DC_PREF_DEFAULT_TRIM_ENABLED));
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_CONFIRM_CREDENTIALS_BACKUP) {
      performBackup();
    } else {
      Toast.makeText(getActivity(), R.string.screenlock_authentication_failed, Toast.LENGTH_SHORT).show();
    }
  }

  //  private class TrimEnabledListener implements Preference.OnPreferenceChangeListener {
//    @Override
//    public boolean onPreferenceChange(final Preference preference, Object newValue) {
//      boolean enabled = (Boolean) newValue;
//      dcContext.setConfigInt("trim_enabled", enabled? 1 : 0);
//      Toast.makeText(getActivity(), "Not yet implemented.", Toast.LENGTH_LONG).show();
//      return true;
//    }
//  }
//
//  private class TrimLengthValidationListener implements Preference.OnPreferenceChangeListener {
//
//    public TrimLengthValidationListener() {
//      EditTextPreference preference = (EditTextPreference)findPreference("pref_trim_length");
//      onPreferenceChange(preference, dcContext.getConfig("trim_length", ""+DcContext.DC_PREF_DEFAULT_TRIM_LENGTH));
//    }
//
//    @Override
//    public boolean onPreferenceChange(Preference preference, Object newValue) {
//      if (newValue == null || ((String)newValue).trim().length() == 0) {
//        return false;
//      }
//
//      int value;
//      try {
//        value = Integer.parseInt((String)newValue);
//      } catch (NumberFormatException nfe) {
//        Log.w(TAG, nfe);
//        return false;
//      }
//
//      if (value < 1) {
//        return false;
//      }
//
//      dcContext.setConfigInt("trim_length", value);
//      preference.setSummary(getResources().getString(R.string.pref_trim_length_limit_summary, value));
//      return true;
//    }
//  }
//
//  private class TrimNowClickListener implements Preference.OnPreferenceClickListener {
//    @Override
//    public boolean onPreferenceClick(Preference preference) {
//      final int threadLengthLimit = dcContext.getConfigInt("trim_length", DcContext.DC_PREF_DEFAULT_TRIM_LENGTH);
//      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//      builder.setMessage(getResources().getString(R.string.pref_trim_now_ask,
//          threadLengthLimit));
//      builder.setPositiveButton(R.string.ok,
//          (dialog, which) -> Toast.makeText(getActivity(), "Not yet implemented.", Toast.LENGTH_LONG).show());
//
//      builder.setNegativeButton(android.R.string.cancel, null);
//      builder.show();
//
//      return true;
//    }
//  }

  public static CharSequence getSummary(Context context) {
    return null;
  }

  /***********************************************************************************************
   * Backup
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
}
