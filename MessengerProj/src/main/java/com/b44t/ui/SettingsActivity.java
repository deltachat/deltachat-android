/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *                        (C) 2013-2016 Nikolai Kudashov
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
 ******************************************************************************/


package com.b44t.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.MediaController;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.MessagesController;
import com.b44t.messenger.NotificationCenter;
import com.b44t.messenger.R;
import com.b44t.ui.Adapters.BaseFragmentAdapter;
import com.b44t.ui.Cells.EmptyCell;
import com.b44t.ui.Cells.HeaderCell;
import com.b44t.ui.Cells.ShadowSectionCell;
import com.b44t.ui.Cells.TextCheckCell;
import com.b44t.ui.Cells.TextDetailSettingsCell;
import com.b44t.ui.Cells.TextInfoPrivacyCell;
import com.b44t.ui.Cells.TextSettingsCell;
import com.b44t.ui.ActionBar.ActionBar;
import com.b44t.ui.ActionBar.BaseFragment;
import com.b44t.ui.Components.LayoutHelper;
import com.b44t.ui.Components.NumberPicker;
import com.b44t.ui.ActionBar.Theme;

import java.util.Locale;

public class SettingsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate /*, PhotoViewer.PhotoViewerProvider -- EDIT BY MR */ {

    private static final int ROWTYPE_SHADOW          = 0;
    private static final int ROWTYPE_TEXT_SETTINGS   = 1;
    private static final int ROWTYPE_CHECK           = 2;
    private static final int ROWTYPE_HEADER          = 3;
    private static final int ROWTYPE_DETAIL_SETTINGS = 4;
    private static final int ROWTYPE_COUNT           = 5;

    private ListView listView;
    private ListAdapter listAdapter;

    private int numberSectionRow;
    private int accountSettingsRow;
    private int usernameRow;
    private int settingsSectionRow;
    private int settingsSectionRow2;
    private int enableAnimationsRow;
    private int notificationRow;
    private int backgroundRow;
    private int languageRow;
    private int privacyRow;
    private int messagesSectionRow;
    private int messagesSectionRow2;
    private int directShareRow;
    private int textSizeRow;
    private int cacheRow;
    private int raiseToSpeakRow;
    private int sendByEnterRow;
    private int supportSectionRow;
    private int supportSectionRow2;
    private int aboutRow;
    private int rowCount;

    public static int defMsgFontSize() {
        return AndroidUtilities.isTablet() ? 18 : 16;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);

        rowCount = 0;
        numberSectionRow = rowCount++;
        usernameRow = rowCount++;
        accountSettingsRow = rowCount++;
        settingsSectionRow = rowCount++;
        settingsSectionRow2 = rowCount++;
        privacyRow = rowCount++;
        notificationRow = rowCount++;
        backgroundRow = rowCount++;
        languageRow = rowCount++;
        // saveToGalleryRow: for now, we do not use this option, this results in confusing folders ("AppName" and "AppName Images" etc.); instead, for now, the user can use the option to manually save a media. Moreover, we also avoid the problem to double-save each image _or_ to handle the case a user deletes an image in the gallery.
        messagesSectionRow = rowCount++;
        messagesSectionRow2 = rowCount++;
        textSizeRow = rowCount++; // incoming messages
        if (Build.VERSION.SDK_INT >= 23) {
            directShareRow = -1; // for now, seems not really to work, however, in T'gram it does
        }
        else {
            directShareRow = -1;
        }
        raiseToSpeakRow = rowCount++; // outgoing message
        sendByEnterRow = rowCount++;
        enableAnimationsRow = rowCount++;
        cacheRow = -1; // for now, the page is still reachable by the "storage settings" in the "android App Settings"
        supportSectionRow = rowCount++;
        supportSectionRow2 = rowCount++;
        aboutRow = rowCount++;

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
    }

    @Override
    public View createView(Context context) {
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

        listAdapter = new ListAdapter(context);

        fragmentView = new FrameLayout(context) {
        };
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new ListView(context);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setVerticalScrollBarEnabled(false);
        AndroidUtilities.setListViewEdgeEffectColor(listView, Theme.ACTION_BAR_COLOR);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                if (i == textSizeRow) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("TextSize", R.string.TextSize));
                    final NumberPicker numberPicker = new NumberPicker(getParentActivity());
                    final int MIN_VAL = 12;
                    final int MAX_VAL = 30;
                    final int DEF_VAL = defMsgFontSize();
                    String displayValues[] = new String[MAX_VAL-MIN_VAL+1];
                    for( int v = MIN_VAL; v <= MAX_VAL; v++ ) {
                        String cur = String.format("%d", v);
                        if( v==DEF_VAL ) {
                            cur += " (" +LocaleController.getString("Default", R.string.Default)+ ")";
                        }
                        displayValues[v-MIN_VAL] = cur;
                    }
                    numberPicker.setMinValue(MIN_VAL);
                    numberPicker.setMaxValue(MAX_VAL);
                    numberPicker.setDisplayedValues(displayValues);
                    numberPicker.setWrapSelectorWheel(false);
                    numberPicker.setValue(MessagesController.getInstance().fontSize);
                    builder.setView(numberPicker);
                    builder.setNegativeButton(LocaleController.getString("Done", R.string.Done), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt("msg_font_size", numberPicker.getValue());
                            MessagesController.getInstance().fontSize = numberPicker.getValue();
                            editor.apply();
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                        }
                    });
                    showDialog(builder.create());
                } else if (i == enableAnimationsRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    boolean animations = preferences.getBoolean("view_animations", true);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("view_animations", !animations);
                    editor.apply();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(!animations);
                    }
                } else if (i == notificationRow) {
                    presentFragment(new NotificationsSettingsActivity());
                } else if (i == backgroundRow) {
                    presentFragment(new WallpapersActivity());
                } else if (i == sendByEnterRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    boolean send = preferences.getBoolean("send_by_enter", false);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("send_by_enter", !send);
                    editor.apply();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(!send);
                    }
                } else if (i == raiseToSpeakRow) {
                    MediaController.getInstance().toogleRaiseToSpeak();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(MediaController.getInstance().canRaiseToSpeak());
                    }
                } else if(i == directShareRow) {
                    MediaController.getInstance().toggleDirectShare();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(MediaController.getInstance().canDirectShare());
                    }
                } else if (i == privacyRow) {
                    presentFragment(new PrivacySettingsActivity());
                } else if (i == languageRow) {
                    presentFragment(new LanguageSelectActivity());
                } else if (i == aboutRow) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("AppName", R.string.AppName) + " " + getVersion());
                    builder.setMessage(MrMailbox.getInfo() + "\n\n" + getAndroidInfo());
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        ;
                        }
                    });
                    showDialog(builder.create());
                } else if (i == usernameRow) {
                    presentFragment(new NameSettingsActivity());
                } else if (i == accountSettingsRow) {
                    presentFragment(new AccountSettingsActivity(null));
                } else if (i == cacheRow) {
                    presentFragment(new CacheControlActivity());
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return false;
            }
        });

        frameLayout.addView(actionBar);

        needLayout();

        return fragmentView;
    }

    private String getVersion()
    {
        try {
            PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            int code = pInfo.versionCode / 10;
            String abi = "";
            switch (pInfo.versionCode % 10) {
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
            return String.format(Locale.US, "V%s-%s", pInfo.versionName, abi);
        } catch (Exception e) {
            return "ErrVersion";
        }
    }

    private String getAndroidInfo()
    {
        return "Build.VERSION.SDK_INT=" + Build.VERSION.SDK_INT;
    }

    @Override
    protected void onDialogDismiss(Dialog dialog) {
        MediaController.getInstance().checkAutodownloadSettings();
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
    }

    @Override
    public void saveSelfArgs(Bundle args) {
    }

    @Override
    public void restoreSelfArgs(Bundle args) {
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.updateInterfaces) {
            int mask = (Integer) args[0];
            if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_NAME) != 0) {
                updateUserData();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
        updateUserData();
        fixLayout();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fixLayout();
    }

    private void needLayout() {
    }

    private void fixLayout() {
        if (fragmentView == null) {
            return;
        }
        fragmentView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (fragmentView != null) {
                    needLayout();
                    fragmentView.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                return true;
            }
        });
    }

    private void updateUserData() {
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
            return i == textSizeRow || i == enableAnimationsRow || i == notificationRow || i == backgroundRow || i == accountSettingsRow ||
                    i == sendByEnterRow || i == privacyRow ||
                    i == languageRow || i == usernameRow ||
                    i == aboutRow ||
                    i == cacheRow || i == raiseToSpeakRow || i == directShareRow ;
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
            } else if (type == ROWTYPE_TEXT_SETTINGS) {
                if (view == null) {
                    view = new TextSettingsCell(mContext);
                }
                TextSettingsCell textCell = (TextSettingsCell) view;
                if (i == textSizeRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    int size = preferences.getInt("msg_font_size", defMsgFontSize());
                    textCell.setTextAndValue(LocaleController.getString("TextSize", R.string.TextSize), String.format("%d", size), true);
                } else if (i == languageRow) {
                    textCell.setTextAndValue(LocaleController.getString("Language", R.string.Language), LocaleController.getCurrentLanguageName(), false);
                } else if (i == notificationRow) {
                    textCell.setText(LocaleController.getString("NotificationsAndSounds", R.string.NotificationsAndSounds), true);
                } else if (i == backgroundRow) {
                    textCell.setText(LocaleController.getString("ChatBackground", R.string.ChatBackground), true);
                } else if (i == privacyRow) {
                    textCell.setText(LocaleController.getString("PrivacySettings", R.string.PrivacySettings), true);
                } else if (i == aboutRow) {
                    textCell.setText(LocaleController.getString("AboutThisProgram", R.string.AboutThisProgram), true);
                } else if (i == cacheRow) {
                    textCell.setText(LocaleController.getString("CacheSettings", R.string.CacheSettings), false);
                } 
            } else if (type == ROWTYPE_CHECK) {
                if (view == null) {
                    view = new TextCheckCell(mContext);
                }
                TextCheckCell textCell = (TextCheckCell) view;

                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                if (i == enableAnimationsRow) {
                    textCell.setTextAndCheck(LocaleController.getString("EnableAnimations", R.string.EnableAnimations), preferences.getBoolean("view_animations", true), false);
                } else if (i == sendByEnterRow) {
                    textCell.setTextAndCheck(LocaleController.getString("SendByEnter", R.string.SendByEnter), preferences.getBoolean("send_by_enter", false), true);
                } else if (i == raiseToSpeakRow) {
                    textCell.setTextAndCheck(LocaleController.getString("RaiseToSpeak", R.string.RaiseToSpeak), MediaController.getInstance().canRaiseToSpeak(), true);
                } else if (i == directShareRow) {
                    textCell.setTextAndValueAndCheck(LocaleController.getString("DirectShare", R.string.DirectShare), LocaleController.getString("DirectShareInfo", R.string.DirectShareInfo), MediaController.getInstance().canDirectShare(), false, true);
                }
            } else if (type == ROWTYPE_HEADER) {
                if (view == null) {
                    view = new HeaderCell(mContext);
                }
                if (i == settingsSectionRow2) {
                    ((HeaderCell) view).setText(LocaleController.getString("SETTINGS", R.string.SETTINGS));
                } else if (i == supportSectionRow2) {
                    ((HeaderCell) view).setText(LocaleController.getString("Info", R.string.Info));
                } else if (i == messagesSectionRow2) {
                    ((HeaderCell) view).setText(LocaleController.getString("AdvancedSettings", R.string.AdvancedSettings));
                } else if (i == numberSectionRow) {
                    ((HeaderCell) view).setText(LocaleController.getString("MyAccount", R.string.MyAccount));
                }
            } else if (type == ROWTYPE_DETAIL_SETTINGS) {
                if (view == null) {
                    view = new TextDetailSettingsCell(mContext);
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
                } else if (i == usernameRow) {
                    String subtitle = MrMailbox.getConfig("displayname", "");
                    if( subtitle.isEmpty()) {
                        subtitle = LocaleController.getString("NotSet", R.string.NotSet);
                    }
                    textCell.setTextAndValue(LocaleController.getString("MyName", R.string.MyName), subtitle, true);
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == settingsSectionRow || i == supportSectionRow || i == messagesSectionRow ) {
                return ROWTYPE_SHADOW;
            } else if (i == enableAnimationsRow || i == sendByEnterRow || i == raiseToSpeakRow || i == directShareRow) {
                return ROWTYPE_CHECK;
            } else if ( i == accountSettingsRow || i == usernameRow) {
                return ROWTYPE_DETAIL_SETTINGS;
            } else if (i == settingsSectionRow2 || i == messagesSectionRow2 || i == supportSectionRow2 || i == numberSectionRow ) {
                return ROWTYPE_HEADER;
            } else {
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
