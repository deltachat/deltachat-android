package org.thoughtcrime.securesms.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;

import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.Prefs;

import java.util.Arrays;
import java.util.Locale;

public class AppearancePreferenceFragment extends ListSummaryPreferenceFragment {

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);

    this.findPreference(Prefs.THEME_PREF).setOnPreferenceChangeListener(new ListSummaryListener());
    this.findPreference(Prefs.LANGUAGE_PREF).setOnPreferenceChangeListener(new ListSummaryListener());
    initializeListSummary((ListPreference)findPreference(Prefs.THEME_PREF));
    initializeListSummary((ListPreference)findPreference(Prefs.LANGUAGE_PREF));
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
    String imagePath = Prefs.getBackgroundImagePath(getContext());
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

    String imagePath = Prefs.getBackgroundImagePath(context);
    String backgroundString;
    if(imagePath.isEmpty()){
      backgroundString = context.getString(R.string.def);
    }
    else{
      backgroundString = context.getString(R.string.custom);
    }

    String[] languageEntries     = context.getResources().getStringArray(R.array.language_entries);
    String[] languageEntryValues = context.getResources().getStringArray(R.array.language_values);
    int langIndex = Arrays.asList(languageEntryValues).indexOf(Prefs.getLanguage(context));
    if(langIndex==0) {
      Locale locale = DynamicLanguage.getSelectedLocale(context);
      String localeAbbr = locale.getLanguage();
      langIndex = Arrays.asList(languageEntryValues).indexOf(localeAbbr);
      if(langIndex==-1 && localeAbbr.length()>2) {
        langIndex = Arrays.asList(languageEntryValues).indexOf(localeAbbr.substring(0,2));
      }
    }
    if (langIndex == -1) langIndex = 0;

    // adding combined strings as "Read receipt: %1$s, Screen lock: %1$s, "
    // makes things inflexible on changes and/or adds lot of additional works to programmers.
    // however, if needed, we can refine this later.
    return themeEntries[themeIndex] + ", "
        + context.getString(R.string.pref_background) + " " + backgroundString + ", "
        + languageEntries[langIndex];
  }

  private class BackgroundClickListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(Preference preference) {
      Intent intent = new Intent(getContext(), ChatBackgroundActivity.class);
      getActivity().startActivity(intent);
      return true;
    }
  }
}
