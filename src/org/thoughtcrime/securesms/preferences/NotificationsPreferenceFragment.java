package org.thoughtcrime.securesms.preferences;

import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import android.text.TextUtils;

import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.KeepAliveService;
import org.thoughtcrime.securesms.util.Prefs;

import static android.app.Activity.RESULT_OK;

public class NotificationsPreferenceFragment extends ListSummaryPreferenceFragment {

  @SuppressWarnings("unused")
  private static final String TAG = NotificationsPreferenceFragment.class.getSimpleName();
  private static final int REQUEST_CODE_NOTIFICATION_SELECTED = 1;

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);

    this.findPreference(Prefs.LED_COLOR_PREF)
        .setOnPreferenceChangeListener(new ListSummaryListener());
    this.findPreference(Prefs.LED_BLINK_PREF)
        .setOnPreferenceChangeListener(new ListSummaryListener());
    this.findPreference(Prefs.RINGTONE_PREF)
        .setOnPreferenceChangeListener(new RingtoneSummaryListener());
    this.findPreference(Prefs.NOTIFICATION_PRIVACY_PREF)
        .setOnPreferenceChangeListener(new ListSummaryListener());
    this.findPreference(Prefs.NOTIFICATION_PRIORITY_PREF)
        .setOnPreferenceChangeListener(new ListSummaryListener());

    this.findPreference(Prefs.RINGTONE_PREF)
        .setOnPreferenceClickListener(preference -> {
          Uri current = Prefs.getNotificationRingtone(getContext());

          Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
          intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
          intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
          intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
          intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI);
          intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current);

          startActivityForResult(intent, REQUEST_CODE_NOTIFICATION_SELECTED);

          return true;
        });

    initializeListSummary((ListPreference) findPreference(Prefs.LED_COLOR_PREF));
    initializeListSummary((ListPreference) findPreference(Prefs.LED_BLINK_PREF));
    initializeListSummary((ListPreference) findPreference(Prefs.NOTIFICATION_PRIVACY_PREF));
    initializeListSummary((ListPreference) findPreference(Prefs.NOTIFICATION_PRIORITY_PREF));

    initializeRingtoneSummary(findPreference(Prefs.RINGTONE_PREF));


    CheckBoxPreference reliableService =  this.findPreference("pref_reliable_service");
    reliableService.setOnPreferenceChangeListener((preference, newValue) -> {
      Context context = getContext();
      boolean enabled = (Boolean) newValue; // Prefs.reliableService() still has the old value
      if (enabled && Prefs.isNotificationsEnabled(context)) {
          KeepAliveService.startSelf(context);
      } else {
        context.stopService(new Intent(context, KeepAliveService.class));
      }
      return true;
    });

    CheckBoxPreference notificationsEnabled = this.findPreference("pref_key_enable_notifications");
    notificationsEnabled.setOnPreferenceChangeListener((preference, newValue) -> {
      Context context = getContext();
      boolean enabled = (Boolean) newValue; // Prefs.isNotificationsEnabled() still has the old value
      if (enabled && Prefs.reliableService(context)) {
        KeepAliveService.startSelf(context);
      } else {
        context.stopService(new Intent(context, KeepAliveService.class));
      }
      return true;
    });
  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.preferences_notifications);
  }

  @Override
  public void onResume() {
    super.onResume();
    ((ApplicationPreferencesActivity) getActivity()).getSupportActionBar().setTitle(R.string.pref_notifications);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_CODE_NOTIFICATION_SELECTED && resultCode == RESULT_OK && data != null) {
      Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

      if (Settings.System.DEFAULT_NOTIFICATION_URI.equals(uri)) {
        Prefs.removeNotificationRingtone(getContext());
      } else {
        Prefs.setNotificationRingtone(getContext(), uri != null ? uri : Uri.EMPTY);
      }

      initializeRingtoneSummary(findPreference(Prefs.RINGTONE_PREF));
    }
  }

  private class RingtoneSummaryListener implements Preference.OnPreferenceChangeListener {
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      Uri value = (Uri) newValue;

      if (value == null || TextUtils.isEmpty(value.toString())) {
        preference.setSummary(R.string.pref_silent);
      } else {
        Ringtone tone = RingtoneManager.getRingtone(getActivity(), value);

        if (tone != null) {
          preference.setSummary(tone.getTitle(getActivity()));
        }
      }

      return true;
    }
  }

  private void initializeRingtoneSummary(Preference pref) {
    RingtoneSummaryListener listener = (RingtoneSummaryListener) pref.getOnPreferenceChangeListener();
    Uri                     uri      = Prefs.getNotificationRingtone(getContext());

    listener.onPreferenceChange(pref, uri);
  }

  public static CharSequence getSummary(Context context) {
    boolean notificationsEnabled = Prefs.isNotificationsEnabled(context);
    String ret = context.getString(notificationsEnabled ? R.string.on : R.string.off);
    return ret;
  }
}
