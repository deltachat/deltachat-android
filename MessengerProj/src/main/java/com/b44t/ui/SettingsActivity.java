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
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.R;
import com.b44t.messenger.browser.Browser;
import com.b44t.ui.ActionBar.ActionBar;
import com.b44t.ui.ActionBar.ActionBarMenu;
import com.b44t.ui.ActionBar.ActionBarMenuItem;
import com.b44t.ui.ActionBar.BaseFragment;
import com.b44t.ui.ActionBar.DrawerLayoutContainer;
import com.b44t.ui.Adapters.BaseFragmentAdapter;
import com.b44t.ui.Cells.DrawerProfileCell;
import com.b44t.ui.Cells.HeaderCell;
import com.b44t.ui.Cells.ShadowSectionCell;
import com.b44t.ui.Cells.TextDetailSettingsCell;
import com.b44t.ui.Cells.TextSettingsCell;
import com.b44t.ui.Components.LayoutHelper;


public class SettingsActivity extends BaseFragment {

    // the list
    private int profileRow, accountHeaderRow, usernameRow, accountSettingsRow, accountShadowRow;
    private int settingsHeaderRow, privacyRow, notificationRow, backgroundRow, advRow, settingsShadowRow;
    private int aboutHeaderRow, aboutRow, inviteRow, helpRow, aboutShadowRow;
    private int rowCount;

    private static final int ID_ACCOUNT_SETTINGS = 10;

    private static final int ROWTYPE_SHADOW          = 0;
    private static final int ROWTYPE_TEXT_SETTINGS   = 1;
    private static final int ROWTYPE_HEADER          = 2;
    private static final int ROWTYPE_DETAIL_SETTINGS = 3;
    private static final int ROWTYPE_PROFILE         = 4;
    private static final int ROWTYPE_COUNT           = 5;

    @Override
    public boolean onFragmentCreate()
    {
        super.onFragmentCreate();

        rowCount = 0;

        if (DrawerLayoutContainer.USE_DRAWER) {
            profileRow = -1;
            accountHeaderRow = rowCount++;
        } else {
            profileRow = rowCount++;
            accountHeaderRow = -1;
        }

        usernameRow = rowCount++;
        if (DrawerLayoutContainer.USE_DRAWER) {
            accountSettingsRow = rowCount++;
            accountShadowRow = rowCount++;
            settingsHeaderRow = rowCount++;
        }
        else {
            accountSettingsRow = -1;
            accountShadowRow = -1;
            settingsHeaderRow = -1;
        }
        privacyRow         = rowCount++;
        notificationRow    = rowCount++;
        backgroundRow      = rowCount++;
        advRow             = rowCount++;
        settingsShadowRow  = rowCount++;

        aboutHeaderRow     = rowCount++;
        aboutRow           = rowCount++;
        if( DrawerLayoutContainer.USE_DRAWER ) {
            inviteRow = -1;
            helpRow = -1;
        }
        else {
            inviteRow = rowCount++;
            helpRow = rowCount++;
        }
        aboutShadowRow     = rowCount++;

        return true;
    }

    @Override
    public View createView(Context context)
    {
        // create action bar
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(context.getString(R.string.Settings));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
                else if( id == ID_ACCOUNT_SETTINGS ) {
                    presentFragment(new SettingsAccountActivity(null));
                }
            }
        });

        // create action bar menu (we use it only for the account settings that are not changed frequently)
        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem headerItem = menu.addItem(0, R.drawable.ic_ab_other);
        headerItem.addSubItem(ID_ACCOUNT_SETTINGS, context.getString(R.string.AccountSettings), 0);

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
                    intent2.putExtra("com.b44t.ui.IntroActivity.isAbout", true);
                    getParentActivity().startActivity(intent2);
                }
                else if (i == inviteRow ) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_TEXT, MrMailbox.getInviteText());
                        getParentActivity().startActivity(Intent.createChooser(intent, ApplicationLoader.applicationContext.getString(R.string.InviteMenuEntry)));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else if (i == helpRow ) {
                    String helpUrl = ApplicationLoader.applicationContext.getString(R.string.HelpUrl);
                    Browser.openUrl(getParentActivity(), helpUrl);
                }
            }
        });

        return fragmentView;
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
                    i == aboutRow || i == inviteRow || i == helpRow;
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
            if( type == ROWTYPE_PROFILE ) {
                if (view == null) {
                    view = new DrawerProfileCell(mContext);
                }
                ((DrawerProfileCell) view).updateUserName();
            }
            else if (type == ROWTYPE_SHADOW) {
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
                    textCell.setText(mContext.getString(R.string.PrivacySettings), true);
                }
                else if (i == notificationRow) {
                    textCell.setText(mContext.getString(R.string.NotificationsAndSounds), true);
                }
                else if (i == backgroundRow) {
                    textCell.setText(mContext.getString(R.string.ChatBackground), true);
                }
                else if (i == advRow) {
                    textCell.setText(mContext.getString(R.string.AdvancedSettings), false);
                }
                else if(i == inviteRow) {
                    textCell.setText(mContext.getString(R.string.InviteMenuEntry), true);
                }
                else if(i == helpRow) {
                    textCell.setText(mContext.getString(R.string.Help), false);
                }
                else if (i == accountSettingsRow) {
                    textCell.setText(mContext.getString(R.string.AccountSettings), true);
                }
                else if (i == usernameRow) {
                    textCell.setText(mContext.getString(R.string.MyName), true);
                }
            }
            else if (type == ROWTYPE_HEADER) {
                if (view == null) {
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                if (i == settingsHeaderRow) {
                    ((HeaderCell) view).setText(mContext.getString(R.string.Settings));
                }
                else if (i == aboutHeaderRow) {
                    ((HeaderCell) view).setText(mContext.getString(R.string.Info));
                }
                else if (i == accountHeaderRow) {
                    ((HeaderCell) view).setText(mContext.getString(R.string.MyAccount));
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
                        subtitle = mContext.getString(R.string.AccountNotConfigured);
                    }

                    textCell.setTextAndValue(mContext.getString(R.string.AccountSettings), subtitle, false);
                }
                else if (i == usernameRow) {
                    String subtitle = MrMailbox.getConfig("displayname", "");
                    if( subtitle.isEmpty()) {
                        subtitle = mContext.getString(R.string.NotSet);
                    }
                    textCell.setTextAndValue(mContext.getString(R.string.MyName), subtitle, true);
                }
                else if (i == aboutRow) {
                    textCell.setTextAndValue(mContext.getString(R.string.AboutThisProgram), "v" + IntroActivity.getVersion(), true);
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if( i == profileRow ) {
                return ROWTYPE_PROFILE;
            }
            else if (i == accountShadowRow || i == settingsShadowRow || i == aboutShadowRow ) {
                return ROWTYPE_SHADOW;
            }
            else if ( (DrawerLayoutContainer.USE_DRAWER && (i==accountSettingsRow || i==usernameRow)) || i==aboutRow ) {
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
