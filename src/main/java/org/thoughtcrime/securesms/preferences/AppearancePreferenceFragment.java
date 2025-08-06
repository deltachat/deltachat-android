package org.thoughtcrime.securesms.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Prefs;

import java.util.Arrays;

public class AppearancePreferenceFragment extends ListSummaryPreferenceFragment {

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);

    this.findPreference(Prefs.THEME_PREF).setOnPreferenceChangeListener(new ListSummaryListener());
    initializeListSummary((ListPreference)findPreference(Prefs.THEME_PREF));
    this.findPreference(Prefs.BACKGROUND_PREF).setOnPreferenceClickListener(new BackgroundClickListener());
  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.preferences_appearance);
  }

  @Override
  public void onStart() {
    super.onStart();
    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener((ApplicationPreferencesActivity)getActivity());
  }

  @Override
  public void onResume() {
    super.onResume();
    ((ApplicationPreferencesActivity) getActivity()).getSupportActionBar().setTitle(R.string.pref_appearance);
    String imagePath = Prefs.getBackgroundImagePath(getContext(), dcContext.getAccountId());
    String backgroundString;
    if(imagePath.isEmpty()){
      backgroundString = this.getString(R.string.def);
    }
    else{
      backgroundString = this.getString(R.string.custom);
    }
    this.findPreference(Prefs.BACKGROUND_PREF).setSummary(backgroundString);
  }

  @Override
  public void onStop() {
    super.onStop();
    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener((ApplicationPreferencesActivity) getActivity());
  }

  public static CharSequence getSummary(Context context) {
    String[] themeEntries        = context.getResources().getStringArray(R.array.pref_theme_entries);
    String[] themeEntryValues    = context.getResources().getStringArray(R.array.pref_theme_values);
    int themeIndex = Arrays.asList(themeEntryValues).indexOf(Prefs.getTheme(context));
    if (themeIndex == -1) themeIndex = 0;

    String imagePath = Prefs.getBackgroundImagePath(context, DcHelper.getContext(context).getAccountId());
    String backgroundString;
    if(imagePath.isEmpty()){
      backgroundString = context.getString(R.string.def);
    }
    else{
      backgroundString = context.getString(R.string.custom);
    }

    // adding combined strings as "Read receipt: %1$s, Screen lock: %1$s, "
    // makes things inflexible on changes and/or adds lot of additional works to programmers.
    // however, if needed, we can refine this later.
    return themeEntries[themeIndex] + ", "
        + context.getString(R.string.pref_background) + " " + backgroundString;
  }

  private class BackgroundClickListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
      Intent intent = new Intent(getContext(), ChatBackgroundActivity.class);
      requireActivity().startActivity(intent);
      return true;
    }
  }
}
