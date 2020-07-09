package org.thoughtcrime.securesms.preferences;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.BlockedAndShareContactsActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.ScreenLockUtil;
import org.thoughtcrime.securesms.util.Util;

import static android.app.Activity.RESULT_OK;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SHOW_EMAILS;

public class ChatsPreferenceFragment extends ListSummaryPreferenceFragment {
  private static final String TAG = ChatsPreferenceFragment.class.getSimpleName();


  private ListPreference showEmails;
  private ListPreference mediaQuality;
  private CheckBoxPreference readReceiptsCheckbox;

  private ListPreference autoDelDevice;
  private ListPreference autoDelServer;

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);

    mediaQuality = (ListPreference) this.findPreference("pref_compression");
    mediaQuality.setOnPreferenceChangeListener((preference, newValue) -> {
      updateListSummary(preference, newValue);
      dcContext.setConfigInt(DcHelper.CONFIG_MEDIA_QUALITY, Util.objectToInt(newValue));
      return true;
    });

    showEmails = (ListPreference) this.findPreference("pref_show_emails");
    showEmails.setOnPreferenceChangeListener((preference, newValue) -> {
      updateListSummary(preference, newValue);
      dcContext.setConfigInt(CONFIG_SHOW_EMAILS, Util.objectToInt(newValue));
      return true;
    });

    readReceiptsCheckbox = (CheckBoxPreference) this.findPreference("pref_read_receipts");
    readReceiptsCheckbox.setOnPreferenceChangeListener(new ReadReceiptToggleListener());

    this.findPreference("preference_category_blocked").setOnPreferenceClickListener(new BlockedContactsClickListener());

    Preference backup = this.findPreference("pref_backup");
    backup.setOnPreferenceClickListener(new BackupListener());

    autoDelDevice = findPreference("autodel_device");
    autoDelDevice.setOnPreferenceChangeListener(new AutodelChangeListener("delete_device_after"));

    autoDelServer = findPreference("autodel_server");
    autoDelServer.setOnPreferenceChangeListener(new AutodelChangeListener("delete_server_after"));
  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.preferences_chats);
  }

  @Override
  public void onResume() {
    super.onResume();
    ((ApplicationPreferencesActivity)getActivity()).getSupportActionBar().setTitle(R.string.pref_chats_and_media);

    String value = Integer.toString(dcContext.getConfigInt("show_emails"));
    showEmails.setValue(value);
    updateListSummary(showEmails, value);

    value = Integer.toString(dcContext.getConfigInt(DcHelper.CONFIG_MEDIA_QUALITY));
    mediaQuality.setValue(value);
    updateListSummary(mediaQuality, value);

    readReceiptsCheckbox.setChecked(0 != dcContext.getConfigInt("mdns_enabled"));

    initAutodelFromCore();
  }

  private void initAutodelFromCore() {
    String value = Integer.toString(dcContext.getConfigInt("delete_server_after"));
    autoDelServer.setValue(value);
    updateListSummary(autoDelServer, value);

    value = Integer.toString(dcContext.getConfigInt("delete_device_after"));
    autoDelDevice.setValue(value);
    updateListSummary(autoDelDevice, value);
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

  public static CharSequence getSummary(Context context) {
    DcContext dcContext = DcHelper.getContext(context);
    final String onRes = context.getString(R.string.on);
    final String offRes = context.getString(R.string.off);
    String readReceiptState = dcContext.getConfigInt("mdns_enabled")!=0? onRes : offRes;
    boolean deleteOld = (dcContext.getConfigInt("delete_device_after")!=0 || dcContext.getConfigInt("delete_server_after")!=0);

    String summary = context.getString(R.string.pref_read_receipts) + " " + readReceiptState;
    if (deleteOld) {
      summary += ", " + context.getString(R.string.delete_old_messages) + " " + onRes;
    }
    return summary;
  }

  private class BlockedContactsClickListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(Preference preference) {
      Intent intent = new Intent(getActivity(), BlockedAndShareContactsActivity.class);
      intent.putExtra(BlockedAndShareContactsActivity.SHOW_ONLY_BLOCKED_EXTRA, true);
      startActivity(intent);
      return true;
    }
  }

  private class ReadReceiptToggleListener implements Preference.OnPreferenceChangeListener {
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      boolean enabled = (boolean) newValue;
      dcContext.setConfigInt("mdns_enabled", enabled ? 1 : 0);
      return true;
    }
  }

  private class AutodelChangeListener implements Preference.OnPreferenceChangeListener {
    private String coreKey;

    AutodelChangeListener(String coreKey) {
      this.coreKey = coreKey;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      int timeout = Util.objectToInt(newValue);
      if (timeout>0) {
        Context context = preference.getContext();
        boolean fromServer = coreKey.equals("delete_server_after");
        int delCount = DcHelper.getContext(context).estimateDeletionCount(fromServer, timeout);

        View gl = View.inflate(getActivity(), R.layout.autodel_confirm, null);
        CheckBox confirmCheckbox = gl.findViewById(R.id.i_understand);
        String msg = String.format(context.getString(fromServer?
                R.string.autodel_server_ask : R.string.autodel_device_ask),
                delCount, getSelectedSummary(preference, newValue));

        new AlertDialog.Builder(context)
                .setTitle(preference.getTitle())
                .setMessage(msg)
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
                .setCancelable(false)
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
