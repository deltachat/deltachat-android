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

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.preference.Preference;
import android.widget.Toast;

import org.thoughtcrime.securesms.preferences.AdvancedPreferenceFragment;
import org.thoughtcrime.securesms.preferences.AppProtectionPreferenceFragment;
import org.thoughtcrime.securesms.preferences.AppearancePreferenceFragment;
import org.thoughtcrime.securesms.preferences.ChatsPreferenceFragment;
import org.thoughtcrime.securesms.preferences.CorrectedPreferenceFragment;
import org.thoughtcrime.securesms.preferences.NotificationsPreferenceFragment;
import org.thoughtcrime.securesms.preferences.widgets.ProfilePreference;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.Prefs;

/**
 * The Activity for application preference display and management.
 *
 * @author Moxie Marlinspike
 *
 */

public class ApplicationPreferencesActivity extends PassphraseRequiredActionBarActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
  @SuppressWarnings("unused")
  private static final String TAG = ApplicationPreferencesActivity.class.getSimpleName();

  private static final String PREFERENCE_CATEGORY_PROFILE        = "preference_category_profile";
  private static final String PREFERENCE_CATEGORY_NOTIFICATIONS  = "preference_category_notifications";
  private static final String PREFERENCE_CATEGORY_APP_PROTECTION = "preference_category_app_protection";
  private static final String PREFERENCE_CATEGORY_APPEARANCE     = "preference_category_appearance";
  private static final String PREFERENCE_CATEGORY_CHATS          = "preference_category_chats";
  private static final String PREFERENCE_CATEGORY_ADVANCED       = "preference_category_advanced";
  private static final String PREFERENCE_CATEGORY_INVITE         = "preference_category_invite";
  private static final String PREFERENCE_CATEGORY_HELP           = "preference_category_help";

  public static final int REQUEST_CODE_SET_BACKGROUND            = 11;

  private final DynamicTheme    dynamicTheme    = new DynamicTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
  }

  @Override
  protected void onCreate(Bundle icicle, boolean ready) {
    //noinspection ConstantConditions
    this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    if (icicle == null) {
      initFragment(android.R.id.content, new ApplicationPreferenceFragment());
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);
    Fragment fragment = getSupportFragmentManager().findFragmentById(android.R.id.content);
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
      recreate();
    } else if (key.equals(Prefs.LANGUAGE_PREF)) {
      recreate();
    }
  }

  public static class ApplicationPreferenceFragment extends CorrectedPreferenceFragment {

    @Override
    public void onCreate(Bundle icicle) {
      super.onCreate(icicle);

      this.findPreference(PREFERENCE_CATEGORY_PROFILE)
          .setOnPreferenceClickListener(new ProfileClickListener());
      this.findPreference(PREFERENCE_CATEGORY_NOTIFICATIONS)
        .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_NOTIFICATIONS));
      this.findPreference(PREFERENCE_CATEGORY_APP_PROTECTION)
        .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_APP_PROTECTION));
      this.findPreference(PREFERENCE_CATEGORY_APPEARANCE)
        .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_APPEARANCE));
      this.findPreference(PREFERENCE_CATEGORY_CHATS)
        .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_CHATS));
      this.findPreference(PREFERENCE_CATEGORY_ADVANCED)
        .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_ADVANCED));

      Preference invitePreference = this.findPreference(PREFERENCE_CATEGORY_INVITE);
      invitePreference.setVisible(false);
      invitePreference.setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_INVITE));

      this.findPreference(PREFERENCE_CATEGORY_HELP)
          .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_HELP));

      if (VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        tintIcons(getActivity());
      }
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

    private void setCategorySummaries() {
      ((ProfilePreference)this.findPreference(PREFERENCE_CATEGORY_PROFILE)).refresh();

      this.findPreference(PREFERENCE_CATEGORY_NOTIFICATIONS)
          .setSummary(NotificationsPreferenceFragment.getSummary(getActivity()));
      this.findPreference(PREFERENCE_CATEGORY_APP_PROTECTION)
          .setSummary(AppProtectionPreferenceFragment.getSummary(getActivity()));
      this.findPreference(PREFERENCE_CATEGORY_APPEARANCE)
          .setSummary(AppearancePreferenceFragment.getSummary(getActivity()));
      this.findPreference(PREFERENCE_CATEGORY_CHATS)
          .setSummary(ChatsPreferenceFragment.getSummary(getActivity()));
      this.findPreference(PREFERENCE_CATEGORY_HELP)
          .setSummary(AdvancedPreferenceFragment.getVersion(getActivity()));
    }

    @TargetApi(11)
    private void tintIcons(Context context) {
      Drawable notifications = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_notifications_white_24dp));
      Drawable privacy       = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_security_white_24dp));
      Drawable appearance    = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_brightness_6_white_24dp));
      Drawable chats         = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_forum_white_24dp));
      Drawable advanced      = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_advanced_white_24dp));
      Drawable invite        = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_mood_white_24dp));
      Drawable help          = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_help_white_24dp));

      int[]      tintAttr   = new int[]{R.attr.pref_icon_tint};
      TypedArray typedArray = context.obtainStyledAttributes(tintAttr);
      int        color      = typedArray.getColor(0, 0x0);
      typedArray.recycle();

      DrawableCompat.setTint(notifications, color);
      DrawableCompat.setTint(privacy, color);
      DrawableCompat.setTint(appearance, color);
      DrawableCompat.setTint(chats, color);
      DrawableCompat.setTint(advanced, color);
      DrawableCompat.setTint(invite, color);
      DrawableCompat.setTint(help, color);

      this.findPreference(PREFERENCE_CATEGORY_NOTIFICATIONS).setIcon(notifications);
      this.findPreference(PREFERENCE_CATEGORY_APP_PROTECTION).setIcon(privacy);
      this.findPreference(PREFERENCE_CATEGORY_APPEARANCE).setIcon(appearance);
      this.findPreference(PREFERENCE_CATEGORY_CHATS).setIcon(chats);
      this.findPreference(PREFERENCE_CATEGORY_ADVANCED).setIcon(advanced);
      this.findPreference(PREFERENCE_CATEGORY_INVITE).setIcon(invite);
      this.findPreference(PREFERENCE_CATEGORY_HELP).setIcon(help);
    }

    private class CategoryClickListener implements Preference.OnPreferenceClickListener {
      private String category;

      CategoryClickListener(String category) {
        this.category = category;
      }

      @Override
      public boolean onPreferenceClick(Preference preference) {
        Fragment fragment = null;

        switch (category) {
        case PREFERENCE_CATEGORY_NOTIFICATIONS:
          fragment = new NotificationsPreferenceFragment();
          break;
        case PREFERENCE_CATEGORY_APP_PROTECTION:
          fragment = new AppProtectionPreferenceFragment();
          break;
        case PREFERENCE_CATEGORY_APPEARANCE:
          fragment = new AppearancePreferenceFragment();
          break;
        case PREFERENCE_CATEGORY_CHATS:
          fragment = new ChatsPreferenceFragment();
          break;
        case PREFERENCE_CATEGORY_ADVANCED:
          fragment = new AdvancedPreferenceFragment();
          break;
        case PREFERENCE_CATEGORY_INVITE:
          startActivity(new Intent(getActivity(), InviteActivity.class));
          break;
        case PREFERENCE_CATEGORY_HELP:
          try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.pref_help_url))));
          } catch (ActivityNotFoundException e) {
            Toast.makeText(getActivity(), R.string.no_browser_installed, Toast.LENGTH_LONG).show();
          }
          break;
        default:
          throw new AssertionError();
        }

        if (fragment != null) {
          Bundle args = new Bundle();
          fragment.setArguments(args);

          FragmentManager     fragmentManager     = getActivity().getSupportFragmentManager();
          FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
          fragmentTransaction.replace(android.R.id.content, fragment);
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

}
