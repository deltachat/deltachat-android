/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *                           (C) 2017 Björn Petersen
 *                    Contact: r10s@b44t.com, http://b44t.com
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see http://www.gnu.org/licenses/ .
 *
 *******************************************************************************
 *
 * File:    SettingsActivity.java
 * Purpose: Basic settings dialog, has mainly items to open other settings pages
 *
 ******************************************************************************/

package com.b44t.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.PowerManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.BuildConfig;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.R;
import com.b44t.ui.ActionBar.ActionBar;
import com.b44t.ui.ActionBar.BaseFragment;
import com.b44t.ui.Adapters.BaseFragmentAdapter;
import com.b44t.ui.Cells.HeaderCell;
import com.b44t.ui.Cells.ShadowSectionCell;
import com.b44t.ui.Cells.TextDetailSettingsCell;
import com.b44t.ui.Cells.TextSettingsCell;
import com.b44t.ui.Components.LayoutHelper;


public class SettingsActivity extends BaseFragment {

    // the list
    private int accountHeaderRow, usernameRow, accountSettingsRow, accountShadowRow;
    private int settingsHeaderRow, privacyRow, notificationRow, backgroundRow, advRow, settingsShadowRow;
    private int aboutHeaderRow, aboutRow, infoRow, aboutShadowRow;
    private int rowCount;

    private static final int ROWTYPE_SHADOW          = 0;
    private static final int ROWTYPE_TEXT_SETTINGS   = 1;
    private static final int ROWTYPE_HEADER          = 2;
    private static final int ROWTYPE_DETAIL_SETTINGS = 3;
    private static final int ROWTYPE_COUNT           = 5;

    @Override
    public boolean onFragmentCreate()
    {
        super.onFragmentCreate();

        rowCount = 0;

        accountHeaderRow   = rowCount++;
        usernameRow        = rowCount++;
        accountSettingsRow = rowCount++;
        accountShadowRow   = rowCount++;

        settingsHeaderRow  = rowCount++;
        privacyRow         = rowCount++;
        notificationRow    = rowCount++;
        backgroundRow      = rowCount++;
        advRow             = rowCount++;
        settingsShadowRow  = rowCount++;

        aboutHeaderRow     = rowCount++;
        aboutRow           = rowCount++;
        infoRow            = rowCount++;
        aboutShadowRow     = rowCount++;

        return true;
    }

    @Override
    public View createView(Context context)
    {
        // create action bar
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("Settings", R.string.Settings));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        // create object to hold the whole view
        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(0xfff0f0f0);

        // create the main layout list
        ListAdapter listAdapter = new ListAdapter(context);

        ListView listView = new ListView(context);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setDrawSelectorOnTop(true);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> adapterView, View view, final int i, long l) {
                if (i == usernameRow) {
                    presentFragment(new SettingsNameActivity());
                }
                else if (i == accountSettingsRow) {
                    presentFragment(new SettingsAccountActivity(null));
                }
                else if (i == privacyRow) {
                    presentFragment(new SettingsPrivacyActivity());
                }
                else if (i == notificationRow) {
                    presentFragment(new SettingsNotificationsActivity());
                }
                else if (i == backgroundRow) {
                    presentFragment(new WallpapersActivity());
                }
                else if (i == advRow) {
                    presentFragment(new SettingsAdvActivity());
                }
                else if (i == aboutRow ) {
                    Intent intent2 = new Intent(getParentActivity(), IntroActivity.class);
                    intent2.putExtra("buttonTitle", ApplicationLoader.applicationContext.getString(R.string.OK));
                    getParentActivity().startActivity(intent2);
                }
                else if( i== infoRow ) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(ApplicationLoader.applicationContext.getString(R.string.AppName) + " v" + getVersion());
                    builder.setMessage(MrMailbox.getInfo() + "\n\n" + getAndroidInfo());
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ;
                        }
                    });
                    showDialog(builder.create());
                }
            }
        });

        return fragmentView;
    }

    private String getVersion()
    {
        try {
            PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            return pInfo.versionName;
        }
        catch(Exception e) {
            return "ErrVersion";
        }
    }

    private String getAndroidInfo()
    {
        String abi = "ErrAbi";
        int versionCode = 0, ignoreBatteryOptimizations = -1;
        try {
            PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            versionCode = pInfo.versionCode;
            switch (versionCode % 10) {
                case 0:
                    abi = "arm";
                    break;
                case 1:
                    abi = "arm-v7a";
                    break;
                case 2:
                    abi = "x86";
                    break;
                case 3:
                    abi = "universal";
                    break;
            }
            if( Build.VERSION.SDK_INT >= 23 ) {
                PowerManager pm = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
                ignoreBatteryOptimizations = pm.isIgnoringBatteryOptimizations(ApplicationLoader.applicationContext.getPackageName())? 1 : 0;
            }
        } catch (Exception e) {}

        return      "SDK_INT="                    + Build.VERSION.SDK_INT
                + "\nMANUFACTURER="               + Build.MANUFACTURER
                + "\nMODEL="                      + Build.MODEL
                + "\nAPPLICATION_ID="             + BuildConfig.APPLICATION_ID
                + "\nBUILD_TYPE="                 + BuildConfig.BUILD_TYPE
                + "\nABI="                        + abi // ABI = Application Binary Interface
                + "\nignoreBatteryOptimizations=" + ignoreBatteryOptimizations
                + "\nversionCode="                + versionCode;
    }

    private class ListAdapter extends BaseFragmentAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int i) {
            return i == usernameRow || i == accountSettingsRow ||
                    i == privacyRow || i == notificationRow || i == backgroundRow || i == advRow ||
                    i == aboutRow || i == infoRow;
        }

        @Override
        public int getCount() {
            return rowCount;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            int type = getItemViewType(i);
            if (type == ROWTYPE_SHADOW) {
                if (view == null) {
                    view = new ShadowSectionCell(mContext);
                }
                view.setBackgroundResource(i == aboutShadowRow? R.drawable.greydivider_bottom : R.drawable.greydivider);
            }
            else if (type == ROWTYPE_TEXT_SETTINGS) {
                if (view == null) {
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                TextSettingsCell textCell = (TextSettingsCell) view;
                if (i == privacyRow) {
                    textCell.setText(LocaleController.getString("PrivacySettings", R.string.PrivacySettings), true);
                }
                else if (i == notificationRow) {
                    textCell.setText(LocaleController.getString("NotificationsAndSounds", R.string.NotificationsAndSounds), true);
                }
                else if (i == backgroundRow) {
                    textCell.setText(LocaleController.getString("ChatBackground", R.string.ChatBackground), true);
                }
                else if (i == advRow) {
                    textCell.setText(ApplicationLoader.applicationContext.getString(R.string.AdvancedSettings), false);
                }
                else if (i == aboutRow) {
                    textCell.setText(ApplicationLoader.applicationContext.getString(R.string.AboutThisProgram), true);
                }
            }
            else if (type == ROWTYPE_HEADER) {
                if (view == null) {
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                if (i == settingsHeaderRow) {
                    ((HeaderCell) view).setText(ApplicationLoader.applicationContext.getString(R.string.Settings));
                }
                else if (i == aboutHeaderRow) {
                    ((HeaderCell) view).setText(LocaleController.getString("Info", R.string.Info));
                }
                else if (i == accountHeaderRow) {
                    ((HeaderCell) view).setText(LocaleController.getString("MyAccount", R.string.MyAccount));
                }
            }
            else if (type == ROWTYPE_DETAIL_SETTINGS) {
                if (view == null) {
                    view = new TextDetailSettingsCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                TextDetailSettingsCell textCell = (TextDetailSettingsCell) view;
                if (i == accountSettingsRow) {
                    String subtitle;
                    if( MrMailbox.isConfigured()!=0) {
                        subtitle = MrMailbox.getConfig("addr", "");
                    }
                    else {
                        subtitle = LocaleController.getString("AccountNotConfigured", R.string.AccountNotConfigured);
                    }

                    textCell.setTextAndValue(LocaleController.getString("AccountSettings", R.string.AccountSettings), subtitle, false);
                }
                else if (i == usernameRow) {
                    String subtitle = MrMailbox.getConfig("displayname", "");
                    if( subtitle.isEmpty()) {
                        subtitle = LocaleController.getString("NotSet", R.string.NotSet);
                    }
                    textCell.setTextAndValue(LocaleController.getString("MyName", R.string.MyName), subtitle, true);
                }
                else if (i == infoRow) {
                    textCell.setTextAndValue(ApplicationLoader.applicationContext.getString(R.string.Info), "v" + getVersion(), false);
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == accountShadowRow || i == settingsShadowRow || i == aboutShadowRow ) {
                return ROWTYPE_SHADOW;
            }
            else if ( i == accountSettingsRow || i == usernameRow || i==infoRow ) {
                return ROWTYPE_DETAIL_SETTINGS;
            }
            else if (i == settingsHeaderRow || i == aboutHeaderRow || i == accountHeaderRow) {
                return ROWTYPE_HEADER;
            }
            else {
                return ROWTYPE_TEXT_SETTINGS;
            }
        }

        @Override
        public int getViewTypeCount() {
            return ROWTYPE_COUNT;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
