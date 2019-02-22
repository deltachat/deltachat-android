package org.thoughtcrime.securesms.preferences;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.widget.Toast;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.BlockedAndShareContactsActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.SwitchPreferenceCompat;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ScreenLockUtil;

import java.util.concurrent.TimeUnit;

import mobi.upod.timedurationpicker.TimeDurationPickerDialog;

public class AppProtectionPreferenceFragment extends CorrectedPreferenceFragment {

    private static final String PREFERENCE_CATEGORY_BLOCKED = "preference_category_blocked";

    private ApplicationDcContext dcContext;

    CheckBoxPreference readReceiptsCheckbox;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);

        dcContext = DcHelper.getContext(getContext());

        this.findPreference(Prefs.SCREEN_LOCK).setOnPreferenceChangeListener(new ScreenLockListener());
        this.findPreference(Prefs.CHANGE_PASSPHRASE_PREF).setOnPreferenceClickListener(new ChangePassphraseClickListener());
        this.findPreference(Prefs.SCREEN_LOCK_TIMEOUT_INTERVAL_PREF).setOnPreferenceClickListener(new LockIntervalClickListener());
        this.findPreference(Prefs.SCREEN_SECURITY_PREF).setOnPreferenceChangeListener(new ScreenShotSecurityListener());

        readReceiptsCheckbox = (CheckBoxPreference) this.findPreference("pref_read_receipts");
        readReceiptsCheckbox.setOnPreferenceChangeListener(new ReadReceiptToggleListener());

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
        ((ApplicationPreferencesActivity) getActivity()).getSupportActionBar().setTitle(R.string.pref_privacy);
        initializePassphraseTimeoutSummary();

        readReceiptsCheckbox.setChecked(0 != dcContext.getConfigInt("mdns_enabled", DcContext.DC_PREF_DEFAULT_MDNS_ENABLED));
    }

    private void initializePassphraseTimeoutSummary() {
        int timeoutSeconds = Prefs.getScreenLockTimeoutInterval(getActivity());
        this.findPreference(Prefs.SCREEN_LOCK_TIMEOUT_INTERVAL_PREF)
                .setSummary(getResources().getQuantityString(R.plurals.n_minutes, timeoutSeconds, timeoutSeconds / 60));
    }

    private void initializeVisibility() {
        KeyguardManager keyguardManager = (KeyguardManager) getContext().getSystemService(Context.KEYGUARD_SERVICE);
        SwitchPreferenceCompat screenLockPreference = (SwitchPreferenceCompat) findPreference(Prefs.SCREEN_LOCK);
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP || keyguardManager == null || !keyguardManager.isKeyguardSecure()) {
            screenLockPreference.setChecked(false);
            screenLockPreference.setEnabled(false);
        }
        if (!screenLockPreference.isChecked()) {
            manageScreenLockChildren(false);
        }
    }

    private void manageScreenLockChildren(boolean enable) {
        SwitchPreferenceCompat timeoutPreference = (SwitchPreferenceCompat) findPreference(Prefs.SCREEN_LOCK_TIMEOUT_PREF);
        timeoutPreference.setEnabled(enable);
        findPreference(Prefs.SCREEN_LOCK_TIMEOUT_INTERVAL_PREF).setEnabled(enable);
        if (!enable) {
            timeoutPreference.setChecked(false);
        }
    }

    private class ScreenShotSecurityListener implements Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean enabled = (Boolean) newValue;
            Prefs.setScreenSecurityEnabled(getContext(), enabled);
            Toast.makeText(getContext(), R.string.pref_screen_security_please_restart_hint, Toast.LENGTH_LONG).show();
            return true;
        }
    }

    private class ScreenLockListener implements Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean enabled = (Boolean) newValue;
            manageScreenLockChildren(enabled);
            Prefs.setScreenLockEnabled(getContext(), enabled);
            ScreenLockUtil.setShouldLockApp(false);
            return true;
        }
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

    public static CharSequence getSummary(Context context) {
        final String onRes = context.getString(R.string.on);
        final String offRes = context.getString(R.string.off);
        String screenLockState = Prefs.isScreenLockEnabled(context) ? onRes : offRes;
        String readReceiptState = DcHelper.getContext(context).getConfigInt("mdns_enabled", DcContext.DC_PREF_DEFAULT_MDNS_ENABLED)!=0? onRes : offRes;

        // adding combined strings as "Read receipt: %1$s, Screen lock: %1$s, "
        // makes things inflexible on changes and/or adds lot of additional works to programmers.
        // however, if needed, we can refine this later.
        return context.getString(R.string.pref_read_receipts) + " " + readReceiptState + ", "
            + context.getString(R.string.screenlock_title) + " " + screenLockState;
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

                Prefs.setScreenLockTimeoutInterval(getActivity(), timeoutSeconds);

                initializePassphraseTimeoutSummary();

            }, 0).show();

            return true;
        }
    }

}
