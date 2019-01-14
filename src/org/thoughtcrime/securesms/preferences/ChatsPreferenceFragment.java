package org.thoughtcrime.securesms.preferences;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.util.Log;
import android.widget.Toast;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.Prefs;

public class ChatsPreferenceFragment extends ListSummaryPreferenceFragment {
  private static final String TAG = ChatsPreferenceFragment.class.getSimpleName();

  private ApplicationDcContext dcContext;

//  CheckBoxPreference trimEnabledCheckbox;

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);
    dcContext = DcHelper.getContext(getContext());

    findPreference(Prefs.MESSAGE_BODY_TEXT_SIZE_PREF)
        .setOnPreferenceChangeListener(new ListSummaryListener());

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
}
