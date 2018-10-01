package org.thoughtcrime.securesms.preferences;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.preference.Preference;
import android.widget.Toast;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.BlockedContactsActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.SwitchPreferenceCompat;
import org.thoughtcrime.securesms.dependencies.InjectableType;
import org.thoughtcrime.securesms.jobs.MultiDeviceReadReceiptUpdateJob;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import mobi.upod.timedurationpicker.TimeDurationPickerDialog;

public class AppProtectionPreferenceFragment extends CorrectedPreferenceFragment implements InjectableType {

    private static final String PREFERENCE_CATEGORY_BLOCKED = "preference_category_blocked";

    @Inject
    SignalServiceAccountManager accountManager;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ApplicationContext.getInstance(activity).injectDependencies(this);
    }

    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        this.findPreference(TextSecurePreferences.SCREEN_LOCK).setOnPreferenceChangeListener(new ScreenLockListener());
        this.findPreference(TextSecurePreferences.CHANGE_PASSPHRASE_PREF).setOnPreferenceClickListener(new ChangePassphraseClickListener());
        this.findPreference(TextSecurePreferences.SCREEN_LOCK_TIMEOUT_INTERVAL_PREF).setOnPreferenceClickListener(new LockIntervalClickListener());
        this.findPreference(TextSecurePreferences.SCREEN_SECURITY_PREF).setOnPreferenceChangeListener(new ScreenSecurityListener());
        this.findPreference(TextSecurePreferences.READ_RECEIPTS_PREF).setOnPreferenceChangeListener(new ReadReceiptToggleListener());
        this.findPreference(PREFERENCE_CATEGORY_BLOCKED).setOnPreferenceClickListener(new BlockedContactsClickListener());

        initializeVisibility();
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_app_protection);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((ApplicationPreferencesActivity) getActivity()).getSupportActionBar().setTitle(R.string.preferences__privacy);
        initializePassphraseTimeoutSummary();
    }

    private void initializePassphraseTimeoutSummary() {
        int timeoutSeconds = TextSecurePreferences.getScreenLockTimeoutInterval(getActivity());
        this.findPreference(TextSecurePreferences.SCREEN_LOCK_TIMEOUT_INTERVAL_PREF)
                .setSummary(getResources().getQuantityString(R.plurals.AppProtectionPreferenceFragment_minutes, timeoutSeconds, timeoutSeconds / 60));
    }

    private void initializeVisibility() {
        KeyguardManager keyguardManager = (KeyguardManager) getContext().getSystemService(Context.KEYGUARD_SERVICE);
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP || keyguardManager == null || !keyguardManager.isKeyguardSecure()) {
            SwitchPreferenceCompat screenLockPreference = (SwitchPreferenceCompat) findPreference(TextSecurePreferences.SCREEN_LOCK);
            screenLockPreference.setChecked(false);
            screenLockPreference.setEnabled(false);
            SwitchPreferenceCompat timeoutPreference = (SwitchPreferenceCompat) findPreference(TextSecurePreferences.PASSPHRASE_TIMEOUT_PREF);
            timeoutPreference.setChecked(false);
            timeoutPreference.setEnabled(false);
            findPreference(TextSecurePreferences.SCREEN_LOCK_TIMEOUT_INTERVAL_PREF).setEnabled(false);
        }
    }

    private class ScreenSecurityListener implements Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean enabled = (Boolean) newValue;
            TextSecurePreferences.setScreenSecurityEnabled(getContext(), enabled);
            Toast.makeText(getContext(), R.string.preferences__screen_security_restart_warning, Toast.LENGTH_LONG).show();
            return true;
        }
    }

    private class ScreenLockListener implements Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean enabled = (Boolean) newValue;
            TextSecurePreferences.setScreenLockEnabled(getContext(), enabled);
            return true;
        }
    }

    private class BlockedContactsClickListener implements Preference.OnPreferenceClickListener {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            Intent intent = new Intent(getActivity(), BlockedContactsActivity.class);
            startActivity(intent);
            return true;
        }
    }

    private class ReadReceiptToggleListener implements Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean enabled = (boolean) newValue;
            ApplicationContext.getInstance(getContext())
                    .getJobManager()
                    .add(new MultiDeviceReadReceiptUpdateJob(getContext(), enabled));

            return true;
        }
    }

    public static CharSequence getSummary(Context context) {
        final int privacySummaryResId = R.string.ApplicationPreferencesActivity_privacy_summary;
        final String onRes = context.getString(R.string.ApplicationPreferencesActivity_on);
        final String offRes = context.getString(R.string.ApplicationPreferencesActivity_off);

        if (TextSecurePreferences.isPasswordDisabled(context) && !TextSecurePreferences.isScreenLockEnabled(context)) {
            return context.getString(privacySummaryResId, offRes, offRes);
        } else {
            return context.getString(privacySummaryResId, onRes, offRes);
        }
    }

    private class ChangePassphraseClickListener implements Preference.OnPreferenceClickListener {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
            startActivity(intent);
            return true;
        }
    }


    private class LockIntervalClickListener implements Preference.OnPreferenceClickListener {

        @Override
        public boolean onPreferenceClick(Preference preference) {
            new TimeDurationPickerDialog(getContext(), (view, duration) -> {
                int timeoutSeconds = (int) Math.max(TimeUnit.MILLISECONDS.toSeconds(duration), 60);

                TextSecurePreferences.setScreenLockTimeoutInterval(getActivity(), timeoutSeconds);

                initializePassphraseTimeoutSummary();

            }, 0).show();

            return true;
        }
    }

}
