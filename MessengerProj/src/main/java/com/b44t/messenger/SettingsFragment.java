/*******************************************************************************
 *
 *                              Delta Chat Android
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

package com.b44t.messenger;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.b44t.messenger.Components.Browser;
import com.b44t.messenger.ActionBar.ActionBar;
import com.b44t.messenger.ActionBar.BaseFragment;
import com.b44t.messenger.Components.BaseFragmentAdapter;
import com.b44t.messenger.Cells.SettingsProfileCell;
import com.b44t.messenger.Cells.HeaderCell;
import com.b44t.messenger.Cells.ShadowSectionCell;
import com.b44t.messenger.Cells.TextCheckCell;
import com.b44t.messenger.Cells.TextDetailSettingsCell;
import com.b44t.messenger.Cells.TextSettingsCell;
import com.b44t.messenger.Components.LayoutHelper;


public class SettingsFragment extends BaseFragment {

    // the list
    private int profileRow, nameAndStatusRow;
    private int notificationRow, backgroundRow, advRow, settingsShadowRow;
    private int readReceiptsRow, passcodeRow;
    private int aboutHeaderRow, aboutRow, inviteRow, helpRow, aboutShadowRow;
    private int rowCount;

    private static final int ROWTYPE_SHADOW          = 0;
    private static final int ROWTYPE_TEXT_SETTINGS   = 1;
    private static final int ROWTYPE_HEADER          = 2;
    private static final int ROWTYPE_DETAIL_SETTINGS = 3;
    private static final int ROWTYPE_PROFILE         = 4;
    private static final int ROWTYPE_CHECK           = 5;
    private static final int ROWTYPE_COUNT           = 6;

    public final int MR_MDNS_DEFAULT_ENABLED = 1;

    private ListView listView;

    @Override
    public boolean onFragmentCreate()
    {
        super.onFragmentCreate();

        rowCount = 0;

        profileRow         = rowCount++;
        nameAndStatusRow   = rowCount++;
        notificationRow    = rowCount++;
        backgroundRow      = rowCount++;
        passcodeRow        = rowCount++;
        readReceiptsRow    = rowCount++;
        advRow             = rowCount++;
        settingsShadowRow  = rowCount++;

        aboutHeaderRow     = rowCount++;
        aboutRow           = rowCount++;
        inviteRow          = rowCount++;
        helpRow            = rowCount++;
        aboutShadowRow     = rowCount++;

        return true;
    }

    @Override
    public View createView(Context context)
    {
        // create action bar
        // (we have also used an action bar menu for less frequently stuff (advanced, backup, account settings) but this needs to be explained -
        // so, if possible, we will avoid this - form follows function)
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(context.getString(R.string.Settings));
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

        listView = new ListView(context);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setDrawSelectorOnTop(true);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> adapterView, View view, final int i, long l) {
                if (i == nameAndStatusRow) {
                    presentFragment(new SettingsNameFragment());
                }
                else if (i == passcodeRow) {
                    if (UserConfig.passcodeHash.length() > 0) {
                        presentFragment(new PasscodeActivity(PasscodeActivity.SCREEN2_ENTER_CODE2));
                    } else {
                        presentFragment(new PasscodeActivity(PasscodeActivity.SCREEN0_SETTINGS));
                    }
                }
                else if(i==readReceiptsRow )
                {
                    int oldval = MrMailbox.getConfigInt("mdns_enabled", MR_MDNS_DEFAULT_ENABLED);
                    if( oldval == 1 ) {
                        MrMailbox.setConfig("mdns_enabled", "0");
                    }
                    else {
                        MrMailbox.setConfig("mdns_enabled", "1");
                    }
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(oldval == 0);
                    }
                }
                else if (i == notificationRow) {
                    presentFragment(new SettingsNotificationsFragment());
                }
                else if (i == backgroundRow) {
                    presentFragment(new WallpapersActivity());
                }
                else if (i == advRow) {
                    presentFragment(new SettingsAdvFragment());
                    //getParentActivity().startActivity(new Intent(getParentActivity(), SettingsAdvActivity.class));
                }
                else if (i == aboutRow ) {
                    Intent intent2 = new Intent(getParentActivity(), WelcomeActivity.class);
                    intent2.putExtra("com.b44t.messenger.IntroActivity.isAbout", true);
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
            return  i == nameAndStatusRow ||
                    i==passcodeRow || i==readReceiptsRow || i == notificationRow || i == backgroundRow || i == advRow ||
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
                    view = new SettingsProfileCell(mContext);
                }
                ((SettingsProfileCell) view).updateUserName();
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
                if (i == passcodeRow) {
                    String val = UserConfig.passcodeHash.length() > 0? mContext.getString(R.string.Enabled) : mContext.getString(R.string.Disabled);
                    textCell.setTextAndValue(mContext.getString(R.string.Passcode), val, true);
                }
                else if (i == notificationRow) {
                    // a preview of the settings here would be rather complicated (all options may be overwritten in the
                    // chats) and either ambiguous or too long.
                    // We tried to display "On", "On/Off" (only groups disabled) and "..." (normal and groups disabled),
                    // but this throws more questions than answers (why do I get notifications for XYZ althoug groupis "off", why don't I get notification although "on", is ... an error)
                    // the control may look better if there is an preview value displayed, however, if this does not work: **Form follows function!**
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
                else if (i == nameAndStatusRow) {
                    textCell.setText(mContext.getString(R.string.NameAndStatus), true);
                }
            }
            else if (type == ROWTYPE_HEADER) {
                if (view == null) {
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                if (i == aboutHeaderRow) {
                    ((HeaderCell) view).setText(mContext.getString(R.string.Info));
                }
            }
            else if (type == ROWTYPE_DETAIL_SETTINGS) {
                if (view == null) {
                    view = new TextDetailSettingsCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                TextDetailSettingsCell textCell = (TextDetailSettingsCell) view;
                if (i == aboutRow) {
                    textCell.setTextAndValue(mContext.getString(R.string.AboutThisProgram), "v" + WelcomeActivity.getVersion(), true);
                }
            }
            else if (type == ROWTYPE_CHECK ) {
                if (view == null) {
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                TextCheckCell textCell = (TextCheckCell) view;
                if( i == readReceiptsRow ) {
                    textCell.setTextAndCheck(mContext.getString(R.string.SendNRcvReadReceipts),
                            MrMailbox.getConfigInt("mdns_enabled", MR_MDNS_DEFAULT_ENABLED)!=0, true);
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if( i == profileRow ) {
                return ROWTYPE_PROFILE;
            }
            else if ( i == settingsShadowRow || i == aboutShadowRow ) {
                return ROWTYPE_SHADOW;
            }
            else if ( i==aboutRow ) {
                return ROWTYPE_DETAIL_SETTINGS;
            }
            else if (i == aboutHeaderRow) {
                return ROWTYPE_HEADER;
            }
            else if( i==readReceiptsRow ) {
                return ROWTYPE_CHECK;
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
