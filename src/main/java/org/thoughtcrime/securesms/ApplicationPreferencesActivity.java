/*
 * Copyright (C) 2011 Whisper Systems
 * Copyright (C) 2013-2017 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;

import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.preferences.AdvancedPreferenceFragment;
import org.thoughtcrime.securesms.preferences.AppearancePreferenceFragment;
import org.thoughtcrime.securesms.preferences.ChatsPreferenceFragment;
import org.thoughtcrime.securesms.preferences.CorrectedPreferenceFragment;
import org.thoughtcrime.securesms.preferences.NotificationsPreferenceFragment;
import org.thoughtcrime.securesms.preferences.widgets.ProfilePreference;
import org.thoughtcrime.securesms.qr.BackupTransferActivity;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.IntentUtils;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ScreenLockUtil;
import org.thoughtcrime.securesms.util.ViewUtil;

/**
 * The Activity for application preference display and management.
 *
 * @author Moxie Marlinspike
 *
 */

public class ApplicationPreferencesActivity extends PassphraseRequiredActionBarActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
  private static final String PREFERENCE_CATEGORY_PROFILE        = "preference_category_profile";
  private static final String PREFERENCE_CATEGORY_NOTIFICATIONS  = "preference_category_notifications";
  private static final String PREFERENCE_CATEGORY_APPEARANCE     = "preference_category_appearance";
  private static final String PREFERENCE_CATEGORY_CHATS          = "preference_category_chats";
  private static final String PREFERENCE_CATEGORY_MULTIDEVICE    = "preference_category_multidevice";
  private static final String PREFERENCE_CATEGORY_ADVANCED       = "preference_category_advanced";
  private static final String PREFERENCE_CATEGORY_CONNECTIVITY   = "preference_category_connectivity";
  private static final String PREFERENCE_CATEGORY_DONATE         = "preference_category_donate";
  private static final String PREFERENCE_CATEGORY_HELP           = "preference_category_help";

  public static final int REQUEST_CODE_SET_BACKGROUND            = 11;

  @Override
  protected void onCreate(Bundle icicle, boolean ready) {
    setContentView(R.layout.activity_application_preferences);

    //noinspection ConstantConditions
    this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    // add padding to avoid content hidden behind system bars
    ViewUtil.applyWindowInsets(findViewById(R.id.fragment));

    if (icicle == null) {
      initFragment(R.id.fragment, new ApplicationPreferenceFragment());
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_OK && requestCode == ScreenLockUtil.REQUEST_CODE_CONFIRM_CREDENTIALS) {
      showBackupProvider();
      return;
    }
    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
    fragment.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public boolean onSupportNavigateUp() {
    FragmentManager fragmentManager = getSupportFragmentManager();
    if (fragmentManager.getBackStackEntryCount() > 0) {
      fragmentManager.popBackStack();
    } else {
      Intent intent = new Intent(this, ConversationListActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
      finish();
    }
    return true;
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (key.equals(Prefs.THEME_PREF)) {
      DynamicTheme.setDefaultDayNightMode(this);
      recreate();
    }
  }

  public void showBackupProvider() {
    Intent intent = new Intent(this, BackupTransferActivity.class);
    intent.putExtra(BackupTransferActivity.TRANSFER_MODE, BackupTransferActivity.TransferMode.SENDER_SHOW_QR.getInt());
    startActivity(intent);
    overridePendingTransition(0, 0); // let the activity appear in the same way as the other pages (which are mostly fragments)
    finishAffinity(); // see comment (**2) in BackupTransferActivity.doFinish()
  }

  public static class ApplicationPreferenceFragment extends CorrectedPreferenceFragment implements DcEventCenter.DcEventDelegate {

    @Override
    public void onCreate(Bundle icicle) {
      super.onCreate(icicle);

      this.findPreference(PREFERENCE_CATEGORY_PROFILE)
          .setOnPreferenceClickListener(new ProfileClickListener());
      this.findPreference(PREFERENCE_CATEGORY_NOTIFICATIONS)
        .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_NOTIFICATIONS));
      this.findPreference(PREFERENCE_CATEGORY_CONNECTIVITY)
        .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_CONNECTIVITY));
      this.findPreference(PREFERENCE_CATEGORY_APPEARANCE)
        .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_APPEARANCE));
      this.findPreference(PREFERENCE_CATEGORY_CHATS)
        .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_CHATS));
      this.findPreference(PREFERENCE_CATEGORY_MULTIDEVICE)
        .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_MULTIDEVICE));
      this.findPreference(PREFERENCE_CATEGORY_ADVANCED)
        .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_ADVANCED));

      this.findPreference(PREFERENCE_CATEGORY_DONATE)
          .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_DONATE));

      this.findPreference(PREFERENCE_CATEGORY_HELP)
          .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_HELP));

      DcHelper.getEventCenter(getActivity()).addObserver(DcContext.DC_EVENT_CONNECTIVITY_CHANGED, this);
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
      addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
      super.onResume();
      //noinspection ConstantConditions
      ((ApplicationPreferencesActivity) getActivity()).getSupportActionBar().setTitle(R.string.menu_settings);
      setCategorySummaries();
    }

    @Override
    public void onDestroy() {
      super.onDestroy();
      DcHelper.getEventCenter(getActivity()).removeObservers(this);
    }

    @Override
    public void handleEvent(@NonNull DcEvent event) {
      if (event.getId() == DcContext.DC_EVENT_CONNECTIVITY_CHANGED) {
        this.findPreference(PREFERENCE_CATEGORY_CONNECTIVITY)
                .setSummary(DcHelper.getConnectivitySummary(getActivity(), getString(R.string.connectivity_connected)));
      }
    }

    private void setCategorySummaries() {
      ((ProfilePreference)this.findPreference(PREFERENCE_CATEGORY_PROFILE)).refresh();

      this.findPreference(PREFERENCE_CATEGORY_NOTIFICATIONS)
          .setSummary(NotificationsPreferenceFragment.getSummary(getActivity()));
      this.findPreference(PREFERENCE_CATEGORY_APPEARANCE)
          .setSummary(AppearancePreferenceFragment.getSummary(getActivity()));
      this.findPreference(PREFERENCE_CATEGORY_CHATS)
          .setSummary(ChatsPreferenceFragment.getSummary(getActivity()));
      this.findPreference(PREFERENCE_CATEGORY_CONNECTIVITY)
          .setSummary(DcHelper.getConnectivitySummary(getActivity(), getString(R.string.connectivity_connected)));
      this.findPreference(PREFERENCE_CATEGORY_HELP)
          .setSummary(AdvancedPreferenceFragment.getVersion(getActivity()));
    }

    private class CategoryClickListener implements Preference.OnPreferenceClickListener {
      private final String category;

      CategoryClickListener(String category) {
        this.category = category;
      }

      @Override
      public boolean onPreferenceClick(Preference preference) {
        Fragment fragment = null;

        switch (category) {
        case PREFERENCE_CATEGORY_NOTIFICATIONS:
          NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || notificationManager.areNotificationsEnabled()) {
            fragment = new NotificationsPreferenceFragment();
          } else {
            new AlertDialog.Builder(getActivity())
              .setTitle(R.string.notifications_disabled)
              .setMessage(R.string.perm_explain_access_to_notifications_denied)
              .setPositiveButton(R.string.perm_continue, (dialog, which) -> getActivity().startActivity(Permissions.getApplicationSettingsIntent(getActivity())))
              .setNegativeButton(android.R.string.cancel, null)
              .show();
          }
          break;
        case PREFERENCE_CATEGORY_CONNECTIVITY:
          startActivity(new Intent(getActivity(), ConnectivityActivity.class));
          break;
        case PREFERENCE_CATEGORY_APPEARANCE:
          fragment = new AppearancePreferenceFragment();
          break;
        case PREFERENCE_CATEGORY_CHATS:
          fragment = new ChatsPreferenceFragment();
          break;
        case PREFERENCE_CATEGORY_MULTIDEVICE:
          if (!ScreenLockUtil.applyScreenLock(getActivity(), getString(R.string.multidevice_title),
              getString(R.string.multidevice_this_creates_a_qr_code) + "\n\n" + getString(R.string.enter_system_secret_to_continue),
              ScreenLockUtil.REQUEST_CODE_CONFIRM_CREDENTIALS)) {
            new AlertDialog.Builder(getActivity())
              .setTitle(R.string.multidevice_title)
              .setMessage(R.string.multidevice_this_creates_a_qr_code)
              .setPositiveButton(R.string.perm_continue,
                (dialog, which) -> ((ApplicationPreferencesActivity)getActivity()).showBackupProvider())
              .setNegativeButton(R.string.cancel, null)
              .show();
            ;
          }
          break;
        case PREFERENCE_CATEGORY_ADVANCED:
          fragment = new AdvancedPreferenceFragment();
          break;
        case PREFERENCE_CATEGORY_DONATE:
          IntentUtils.showInBrowser(requireActivity(), "https://delta.chat/donate");
          break;
        case PREFERENCE_CATEGORY_HELP:
          startActivity(new Intent(getActivity(), LocalHelpActivity.class));
          break;
        default:
          throw new AssertionError();
        }

        if (fragment != null) {
          Bundle args = new Bundle();
          fragment.setArguments(args);

          FragmentManager     fragmentManager     = getActivity().getSupportFragmentManager();
          FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
          fragmentTransaction.replace(R.id.fragment, fragment);
          fragmentTransaction.addToBackStack(null);
          fragmentTransaction.commit();
        }

        return true;
      }
    }

    private class ProfileClickListener implements Preference.OnPreferenceClickListener {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent intent = new Intent(preference.getContext(), CreateProfileActivity.class);
        getActivity().startActivity(intent);
        return true;
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }
}
