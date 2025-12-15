package org.thoughtcrime.securesms.preferences;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.BlockedContactsActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.ScreenLockUtil;
import org.thoughtcrime.securesms.util.Util;

public class ChatsPreferenceFragment extends ListSummaryPreferenceFragment {
  private ListPreference mediaQuality;
  private ListPreference autoDownload;
  private CheckBoxPreference readReceiptsCheckbox;

  private ListPreference autoDelDevice;
  private ListPreference autoDelServer;

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);

    mediaQuality = (ListPreference) this.findPreference("pref_compression");
    if (mediaQuality != null) {
      mediaQuality.setOnPreferenceChangeListener((preference, newValue) -> {
        updateListSummary(preference, newValue);
        dcContext.setConfigInt(DcHelper.CONFIG_MEDIA_QUALITY, Util.objectToInt(newValue));
        return true;
      });
    }


    autoDownload = findPreference("auto_download");
    if (autoDownload != null) {
      autoDownload.setOnPreferenceChangeListener((preference, newValue) -> {
        updateListSummary(preference, newValue);
        dcContext.setConfigInt("download_limit", Util.objectToInt(newValue));
        return true;
      });
    }
    nicerAutoDownloadNames();

    readReceiptsCheckbox = (CheckBoxPreference) this.findPreference("pref_read_receipts");
    if (readReceiptsCheckbox != null) {
      readReceiptsCheckbox.setOnPreferenceChangeListener(new ReadReceiptToggleListener());
    }

    this.findPreference("preference_category_blocked").setOnPreferenceClickListener(new BlockedContactsClickListener());

    Preference backup = this.findPreference("pref_backup");
    if (backup != null) {
      backup.setOnPreferenceClickListener(new BackupListener());
    }

    autoDelDevice = findPreference("autodel_device");
    if (autoDelDevice != null) {
      autoDelDevice.setOnPreferenceChangeListener(new AutodelChangeListener("delete_device_after"));
    }

    autoDelServer = findPreference("autodel_server");
    if (autoDelServer != null) {
      autoDelServer.setOnPreferenceChangeListener(new AutodelChangeListener("delete_server_after"));
    }
    if (dcContext.isChatmail()) {
      autoDelServer.setVisible(false);
    }
  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.preferences_chats);
  }

  @Override
  public void onResume() {
    super.onResume();
    ((ApplicationPreferencesActivity)getActivity()).getSupportActionBar().setTitle(R.string.pref_chats);

    String value = Integer.toString(dcContext.getConfigInt(DcHelper.CONFIG_MEDIA_QUALITY));
    mediaQuality.setValue(value);
    updateListSummary(mediaQuality, value);

    value = Integer.toString(dcContext.getConfigInt("download_limit"));
    value = alignToMaxEntry(value, autoDownload.getEntryValues());
    autoDownload.setValue(value);
    updateListSummary(autoDownload, value);

    readReceiptsCheckbox.setChecked(0 != dcContext.getConfigInt("mdns_enabled"));

    initAutodelFromCore();
  }

  private void initAutodelFromCore() {
    String value = Integer.toString(dcContext.getConfigInt("delete_server_after"));
    autoDelServer.setValue(value);
    updateListSummary(autoDelServer, value, (value.equals("0") || dcContext.isChatmail())? null : getString(R.string.autodel_server_enabled_hint));

    value = Integer.toString(dcContext.getConfigInt("delete_device_after"));
    autoDelDevice.setValue(value);
    updateListSummary(autoDelDevice, value);
  }

  // prefixes "Up to ..." to all entry names but the first one.
  private void nicerAutoDownloadNames() {
    CharSequence[] entries = autoDownload.getEntries();
    for (int i = 1 /*skip first*/; i < entries.length; i++) {
      if (entries[i].equals("160 KiB")) {
        entries[i] = getString(R.string.up_to_x_most_worse_quality_images, entries[i]);
      } else if (entries[i].equals("640 KiB")) {
        entries[i] = getString(R.string.up_to_x_most_balanced_quality_images, entries[i]);
      } else {
        entries[i] = getString(R.string.up_to_x, entries[i]);
      }
    }
    autoDownload.setEntries(entries);
  }

  // Assumes `entryValues` are sorted smallest (index 0) to largest (last index)
  // and returns the an item close to `selectedValue`.
  private String alignToMaxEntry(@NonNull String selectedValue, @NonNull CharSequence[] entryValues) {
    try {
      int selectedValueInt = Integer.parseInt(selectedValue);
      for (int i = entryValues.length - 1; i >= 1 /*first is returned below*/; i--) {
        int entryValueMin = i == 1 ? (Integer.parseInt(entryValues[i - 1].toString()) + 1) : Integer.parseInt(entryValues[i].toString());
        if (selectedValueInt >= entryValueMin) {
          return entryValues[i].toString();
        }
      }
      return entryValues[0].toString();
    } catch(Exception e) {
      return selectedValue;
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_CONFIRM_CREDENTIALS_BACKUP) {
      performBackup();
    }
  }

  public static CharSequence getSummary(Context context) {
    DcContext dcContext = DcHelper.getContext(context);
    final String onRes = context.getString(R.string.on);
    final String offRes = context.getString(R.string.off);
    String readReceiptState = dcContext.getConfigInt("mdns_enabled")!=0? onRes : offRes;
    return context.getString(R.string.pref_read_receipts) + " " + readReceiptState;
  }

  private class BlockedContactsClickListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
      Intent intent = new Intent(getActivity(), BlockedContactsActivity.class);
      startActivity(intent);
      return true;
    }
  }

  private class ReadReceiptToggleListener implements Preference.OnPreferenceChangeListener {
    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
      boolean enabled = (boolean) newValue;
      dcContext.setConfigInt("mdns_enabled", enabled ? 1 : 0);
      return true;
    }
  }

  private class AutodelChangeListener implements Preference.OnPreferenceChangeListener {
    private final String coreKey;

    AutodelChangeListener(String coreKey) {
      this.coreKey = coreKey;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      int timeout = Util.objectToInt(newValue);
      Context context = preference.getContext();
      boolean fromServer = coreKey.equals("delete_server_after");
      if (timeout>0 && !(fromServer && dcContext.isChatmail())) {
        int delCount = DcHelper.getContext(context).estimateDeletionCount(fromServer, timeout);

        View gl = View.inflate(getActivity(), R.layout.dialog_with_checkbox, null);
        CheckBox confirmCheckbox = gl.findViewById(R.id.dialog_checkbox);
        TextView msg = gl.findViewById(R.id.dialog_message);

        // If we'd use both `setMessage()` and `setView()` on the same AlertDialog, on small screens the
        // "OK" and "Cancel" buttons would not be show. So, put the message into our custom view:
        msg.setText(String.format(context.getString(fromServer?
                R.string.autodel_server_ask : R.string.autodel_device_ask),
                delCount, getSelectedSummary(preference, newValue)));
        confirmCheckbox.setText(R.string.autodel_confirm);

        new AlertDialog.Builder(context)
                .setTitle(preference.getTitle())
                .setView(gl)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                  if (confirmCheckbox.isChecked()) {
                    dcContext.setConfigInt(coreKey, timeout);
                    initAutodelFromCore();
                  } else {
                    onPreferenceChange(preference, newValue);
                  }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> initAutodelFromCore())
                .setCancelable(true) // Enable the user to quickly cancel if they are intimidated by the warnings :)
                .setOnCancelListener(dialog -> initAutodelFromCore())
                .show();
      } else if (fromServer && timeout == 1 /*at once, using a constant that cannot be used in .xml would weaken grep ability*/) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.autodel_server_warn_multi_device_title)
                .setMessage(R.string.autodel_server_warn_multi_device)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                  dcContext.setConfigInt(coreKey, timeout);
                  initAutodelFromCore();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> initAutodelFromCore())
                .setCancelable(true)
                .setOnCancelListener(dialog -> initAutodelFromCore())
                .show();
      } else {
        updateListSummary(preference, newValue);
        dcContext.setConfigInt(coreKey, timeout);
      }
      return true;
    }
  }

  /***********************************************************************************************
   * Backup
   **********************************************************************************************/

  private class BackupListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
      boolean result = ScreenLockUtil.applyScreenLock(requireActivity(), getString(R.string.pref_backup), getString(R.string.enter_system_secret_to_continue), REQUEST_CODE_CONFIRM_CREDENTIALS_BACKUP);
      if (!result) {
        performBackup();
      }
      return true;
    }
  }

  private void performBackup() {
    Permissions.with(requireActivity())
            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE) // READ_EXTERNAL_STORAGE required to read folder contents and to generate backup names
            .alwaysGrantOnSdk30()
            .ifNecessary()
            .withPermanentDenialDialog(getString(R.string.perm_explain_access_to_storage_denied))
            .onAllGranted(() -> {
              AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity())
                      .setTitle(R.string.pref_backup)
                      .setMessage(R.string.pref_backup_export_explain)
                      .setNeutralButton(android.R.string.cancel, null)
                      .setPositiveButton(R.string.pref_backup_export_this, (dialogInterface, i) -> startImexOne(DcContext.DC_IMEX_EXPORT_BACKUP));
              int[] allAccounts = DcHelper.getAccounts(requireActivity()).getAll();
              if (allAccounts.length > 1) {
                String exportAllString = requireActivity().getString(R.string.pref_backup_export_all, allAccounts.length);
                builder.setNegativeButton(exportAllString, (dialogInterface, i) -> startImexAll(DcContext.DC_IMEX_EXPORT_BACKUP));
              }
              builder.show();
            })
            .execute();
  }
}
