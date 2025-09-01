package org.thoughtcrime.securesms.preferences;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.connect.KeepAliveService;
import org.thoughtcrime.securesms.notifications.FcmReceiveService;
import org.thoughtcrime.securesms.util.Prefs;

public class NotificationsPreferenceFragment extends ListSummaryPreferenceFragment implements Preference.OnPreferenceChangeListener {

  private static final String TAG = NotificationsPreferenceFragment.class.getSimpleName();
  private static final int REQUEST_CODE_NOTIFICATION_SELECTED = 1;

  private CheckBoxPreference ignoreBattery;
  private CheckBoxPreference notificationsEnabled;
  private CheckBoxPreference mentionNotifEnabled;
  private CheckBoxPreference reliableService;

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);

    this.findPreference(Prefs.LED_COLOR_PREF)
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
          if (current.toString().isEmpty()) current = null;  // silent

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
    initializeListSummary((ListPreference) findPreference(Prefs.NOTIFICATION_PRIVACY_PREF));
    initializeListSummary((ListPreference) findPreference(Prefs.NOTIFICATION_PRIORITY_PREF));

    initializeRingtoneSummary(findPreference(Prefs.RINGTONE_PREF));

    ignoreBattery = this.findPreference("pref_ignore_battery_optimizations");
    if (ignoreBattery != null) {
      ignoreBattery.setVisible(needsIgnoreBatteryOptimizations());
      ignoreBattery.setOnPreferenceChangeListener((preference, newValue) -> {
        requestToggleIgnoreBatteryOptimizations();
        return true;
      });
    }


    // reliableService is just used for displaying the actual value
    // of the reliable service preference that is managed via
    // Prefs.setReliableService() and Prefs.reliableService()
    reliableService =  this.findPreference("pref_reliable_service2");
    if (reliableService != null) {
      reliableService.setOnPreferenceChangeListener(this);
    }

    notificationsEnabled = this.findPreference("pref_enable_notifications");
    if (notificationsEnabled != null) {
      notificationsEnabled.setOnPreferenceChangeListener((preference, newValue) -> {
        boolean enabled = (Boolean) newValue;
        dcContext.setMuted(!enabled);
        notificationsEnabled.setSummary(getSummary(getContext(), false));
        return true;
      });
    }

    mentionNotifEnabled = this.findPreference("pref_enable_mention_notifications");
    if (mentionNotifEnabled != null) {
      mentionNotifEnabled.setOnPreferenceChangeListener((preference, newValue) -> {
        boolean enabled = (Boolean) newValue;
        dcContext.setMentionsEnabled(enabled);
        return true;
      });
    }
  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.preferences_notifications);
  }

  @Override
  public void onResume() {
    super.onResume();
    ((ApplicationPreferencesActivity) getActivity()).getSupportActionBar().setTitle(R.string.pref_notifications);

    // update ignoreBattery in onResume() to reflects changes done in the system settings
    ignoreBattery.setChecked(isIgnoringBatteryOptimizations());
    notificationsEnabled.setChecked(!dcContext.isMuted());
    notificationsEnabled.setSummary(getSummary(getContext(), false));
    mentionNotifEnabled.setChecked(dcContext.isMentionsEnabled());

    // set without altering "unset" state of the preference
    reliableService.setOnPreferenceChangeListener(null);
    reliableService.setChecked(Prefs.reliableService(getActivity()));
    reliableService.setOnPreferenceChangeListener(this);
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

  @Override
  public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
    Context context = getContext();
    boolean enabled = (Boolean) newValue;
    Prefs.setReliableService(context, enabled);
    if (enabled) {
      KeepAliveService.startSelf(context);
    } else {
      context.stopService(new Intent(context, KeepAliveService.class));
    }
    notificationsEnabled.setSummary(getSummary(context, false));
    return true;
  }

  private class RingtoneSummaryListener implements Preference.OnPreferenceChangeListener {
    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
      Uri value = (Uri) newValue;

      if (value == null || TextUtils.isEmpty(value.toString())) {
        preference.setSummary(R.string.pref_silent);
      } else {
        Ringtone tone = RingtoneManager.getRingtone(getActivity(), value);

        if (tone != null) {
          String summary;
          try {
            summary = tone.getTitle(getActivity());
          } catch (SecurityException e) {
            // this could happen in some phones when user selects ringtone from
            // external storage and later removes the read from external storage permission
            // and later this method is called from initializeRingtoneSummary()
            summary = "<no access>";
            Log.w(TAG, e);
          }
          preference.setSummary(summary);
        }
      }

      return true;
    }
  }

  private boolean needsIgnoreBatteryOptimizations() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
  }

  private boolean isIgnoringBatteryOptimizations() {
    if (!needsIgnoreBatteryOptimizations()) {
      return true;
    }
    PowerManager pm = (PowerManager)getActivity().getSystemService(Context.POWER_SERVICE);
    if(pm.isIgnoringBatteryOptimizations(getActivity().getPackageName())) {
      return true;
    }
    return false;
  }

  private void requestToggleIgnoreBatteryOptimizations() {
    Context context = getActivity();
    boolean openManualSettings = true;

    try {
      if (needsIgnoreBatteryOptimizations()
              && !isIgnoringBatteryOptimizations()
              && ContextCompat.checkSelfPermission(context, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) == PackageManager.PERMISSION_GRANTED) {
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
        openManualSettings = false;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (openManualSettings && needsIgnoreBatteryOptimizations()) {
      // fire ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS if ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS fails
      // or if isIgnoringBatteryOptimizations() is already true (there is no intent to re-enable battery optimizations)
      Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
      context.startActivity(intent);
    }
  }

  private void initializeRingtoneSummary(Preference pref) {
    RingtoneSummaryListener listener = (RingtoneSummaryListener) pref.getOnPreferenceChangeListener();
    Uri                     uri      = Prefs.getNotificationRingtone(getContext());

    listener.onPreferenceChange(pref, uri);
  }

  public static CharSequence getSummary(Context context) {
    return getSummary(context, true);
  }

  public static CharSequence getSummary(Context context, boolean detailed) {
    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || notificationManager.areNotificationsEnabled()) {
      if (DcHelper.getContext(context).isMuted()) {
        return detailed? context.getString(R.string.off) : "";
      }
      if (FcmReceiveService.getToken() == null && !Prefs.reliableService(context)) {
        return "⚠️ " + context.getString(R.string.unreliable_bg_notifications);
      }
      return detailed? context.getString(R.string.on) : "";
    } else {
      return "⚠️ " + context.getString(R.string.disabled_in_system_settings);
    }
  }
}
