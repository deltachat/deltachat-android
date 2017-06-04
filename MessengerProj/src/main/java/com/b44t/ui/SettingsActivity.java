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

package com.b44t.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.R;
import com.b44t.messenger.UserConfig;
import com.b44t.messenger.browser.Browser;
import com.b44t.ui.ActionBar.ActionBar;
import com.b44t.ui.ActionBar.BaseFragment;
import com.b44t.ui.ActionBar.DrawerLayoutContainer;
import com.b44t.ui.Adapters.BaseFragmentAdapter;
import com.b44t.ui.Cells.DrawerProfileCell;
import com.b44t.ui.Cells.HeaderCell;
import com.b44t.ui.Cells.ShadowSectionCell;
import com.b44t.ui.Cells.TextCheckCell;
import com.b44t.ui.Cells.TextDetailSettingsCell;
import com.b44t.ui.Cells.TextSettingsCell;
import com.b44t.ui.Components.LayoutHelper;
import com.b44t.ui.Components.NumberPicker;


public class SettingsActivity extends BaseFragment {

    // the list
    private int profileRow, accountHeaderRow, usernameRow, accountShadowRow;
    private int settingsHeaderRow, notificationRow, backgroundRow, textSizeRow, advRow, settingsShadowRow;
    private int readReceiptsRow, blockedRow, passcodeRow;
    private int aboutHeaderRow, aboutRow, inviteRow, helpRow, aboutShadowRow;
    private int rowCount;

    private static final int ROWTYPE_SHADOW          = 0;
    private static final int ROWTYPE_TEXT_SETTINGS   = 1;
    private static final int ROWTYPE_HEADER          = 2;
    private static final int ROWTYPE_DETAIL_SETTINGS = 3;
    private static final int ROWTYPE_PROFILE         = 4;
    private static final int ROWTYPE_CHECK           = 5;
    private static final int ROWTYPE_COUNT           = 6;

    private ListView listView;

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
            accountShadowRow = rowCount++;
            settingsHeaderRow = rowCount++;
        }
        else {
            accountShadowRow = -1;
            settingsHeaderRow = -1;
        }

        if (DrawerLayoutContainer.USE_DRAWER) {
            notificationRow    = rowCount++;
            backgroundRow      = rowCount++;
            textSizeRow        = rowCount++;
        }
        else {
            notificationRow    = rowCount++;
            backgroundRow      = rowCount++;
            textSizeRow        = rowCount++;
        }
        readReceiptsRow=-1;
        passcodeRow = rowCount++;
        blockedRow = rowCount++;
        advRow = rowCount++;
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
        // (we have also used an action bar menu for less frequently stuff (advanced, backup, account settings) but this needs to be explained -
        // so, if possible, we will avoid this - form follows function)
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
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
                if (i == usernameRow) {
                    presentFragment(new SettingsNameActivity());
                }
                else if (i == blockedRow) {
                    presentFragment(new BlockedUsersActivity());
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
                    Toast.makeText(getParentActivity(), ApplicationLoader.applicationContext.getString(R.string.NotYetImplemented), Toast.LENGTH_SHORT).show();
                }
                else if (i == notificationRow) {
                    presentFragment(new SettingsNotificationsActivity());
                }
                else if (i == backgroundRow) {
                    presentFragment(new WallpapersActivity());
                }
                if (i == textSizeRow) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(ApplicationLoader.applicationContext.getString(R.string.TextSize));
                    final NumberPicker numberPicker = new NumberPicker(getParentActivity());
                    final int MIN_VAL = 12;
                    final int MAX_VAL = 30;
                    final int DEF_VAL = SettingsAdvActivity.defMsgFontSize();
                    String displayValues[] = new String[MAX_VAL-MIN_VAL+1];
                    for( int v = MIN_VAL; v <= MAX_VAL; v++ ) {
                        String cur = String.format("%d", v);
                        if( v==DEF_VAL ) {
                            cur += " (" +ApplicationLoader.applicationContext.getString(R.string.Default)+ ")";
                        }
                        displayValues[v-MIN_VAL] = cur;
                    }
                    numberPicker.setMinValue(MIN_VAL);
                    numberPicker.setMaxValue(MAX_VAL);
                    numberPicker.setDisplayedValues(displayValues);
                    numberPicker.setWrapSelectorWheel(false);
                    numberPicker.setValue(ApplicationLoader.fontSize);
                    builder.setView(numberPicker);
                    builder.setPositiveButton(ApplicationLoader.applicationContext.getString(R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt("msg_font_size", numberPicker.getValue());
                            ApplicationLoader.fontSize = numberPicker.getValue();
                            editor.apply();
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                        }
                    });
                    showDialog(builder.create());
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
            return i == textSizeRow || i == usernameRow ||
                    i == blockedRow || i==passcodeRow || i==readReceiptsRow || i == notificationRow || i == backgroundRow || i == advRow ||
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
                if (i == blockedRow) {
                    String cntStr = String.format("%d", MrMailbox.getBlockedCount());
                    textCell.setTextAndValue(ApplicationLoader.applicationContext.getString(R.string.BlockedContacts), cntStr, true);
                }
                else if (i == passcodeRow) {
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
                else if (i == textSizeRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    int size = preferences.getInt("msg_font_size", SettingsAdvActivity.defMsgFontSize());
                    textCell.setTextAndValue(mContext.getString(R.string.TextSize), String.format("%d", size), true);
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
                else if (i == usernameRow) {
                    String value = MrMailbox.getConfig("displayname", "");
                    if( value.isEmpty()) {
                        value = mContext.getString(R.string.NotSet);
                    }
                    textCell.setTextAndValue(mContext.getString(R.string.MyName), value, true);
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
                if (i == usernameRow) {
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
            else if (type == ROWTYPE_CHECK ) {
                if (view == null) {
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                TextCheckCell textCell = (TextCheckCell) view;
                if( i == readReceiptsRow ) {
                    textCell.setTextAndCheck(mContext.getString(R.string.SendNRcvReadReceipts),
                            MrMailbox.getConfigInt("read_receipts", 0)!=0, true);
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
            else if ( (DrawerLayoutContainer.USE_DRAWER && (i==usernameRow)) || i==aboutRow ) {
                return ROWTYPE_DETAIL_SETTINGS;
            }
            else if (i == settingsHeaderRow || i == aboutHeaderRow || i == accountHeaderRow) {
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
