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
 ******************************************************************************/


package com.b44t.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.MediaController;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.MrChat;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.R;
import com.b44t.ui.Adapters.BaseFragmentAdapter;
import com.b44t.ui.Cells.ShadowSectionCell;
import com.b44t.ui.Cells.TextCheckCell;
import com.b44t.ui.Cells.TextSettingsCell;
import com.b44t.ui.ActionBar.ActionBar;
import com.b44t.ui.ActionBar.BaseFragment;
import com.b44t.ui.Components.LayoutHelper;

import java.io.File;


public class SettingsAdvActivity extends BaseFragment {

    // the list
    private int directShareRow, cacheRow, raiseToSpeakRow, sendByEnterRow, autoplayGifsRow, showUnknownSendersRow, finalShadowRow;
    private int e2eEncryptionRow;
    private int manageKeysRow;
    private int rowCount;

    private static final int ROWTYPE_SHADOW          = 0;
    private static final int ROWTYPE_TEXT_SETTINGS   = 1;
    private static final int ROWTYPE_CHECK           = 2;
    private static final int ROWTYPE_COUNT           = 3;

    private ListView listView;

    public final int MR_E2EE_DEFAULT_ENABLED = 1; // when changing this constant, also change it in the C-part

    public static int defMsgFontSize() {
        return AndroidUtilities.isTablet() ? 18 : 16;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        rowCount = 0;
        if (Build.VERSION.SDK_INT >= 23) {
            directShareRow = -1; // for now, seems not really to work, however, in T'gram it does
        }
        else {
            directShareRow = -1;
        }
        autoplayGifsRow = rowCount++;
        showUnknownSendersRow   = rowCount++;
        sendByEnterRow = rowCount++;
        raiseToSpeakRow = rowCount++; // outgoing message
        cacheRow = -1;// for now, the - non-functional - page is reachable by the "storage settings" in the "android App Settings" only
        e2eEncryptionRow        = rowCount++;
        manageKeysRow           = rowCount++;
        finalShadowRow = rowCount++;

        return true;
    }

    @Override
    public View createView(Context context)
    {
        // create action bar
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(ApplicationLoader.applicationContext.getString(R.string.AdvancedSettings));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        // create object to hold the whole view
        fragmentView = new FrameLayout(context) {};
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(0xfff0f0f0);

        // create the main layout list
        ListAdapter listAdapter = new ListAdapter(context);

        listView = new ListView(context);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setDrawSelectorOnTop(true);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.START));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                if (i == sendByEnterRow) {
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
                } else if (i == autoplayGifsRow) {
                    MediaController.getInstance().toggleAutoplayGifs();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(MediaController.getInstance().canAutoplayGifs());
                    }
                } else if(i == directShareRow) {
                    MediaController.getInstance().toggleDirectShare();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(MediaController.getInstance().canDirectShare());
                    }
                } else if (i == cacheRow) {
                    presentFragment(new CacheControlActivity());
                }
                else if( i==showUnknownSendersRow) {
                    int oldval = MrMailbox.getConfigInt("show_deaddrop", 0);
                    if( oldval == 1 ) {
                        MrMailbox.setConfig("show_deaddrop", "0");
                    }
                    else {
                        MrMailbox.setConfig("show_deaddrop", "1");
                    }
                    MrMailbox.MrCallback(MrMailbox.MR_EVENT_MSGS_CHANGED, 0, 0);
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(oldval == 0);
                    }

                    // if showing deaddrop is disabled, also disable notifications for this chat (cannot be displayed otherwise)
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt("notify2_" + MrChat.MR_CHAT_ID_DEADDROP, oldval==1? 2 /*always muted*/ : 0);
                    editor.apply();
                }
                else if(i==e2eEncryptionRow )
                {
                    int oldval = MrMailbox.getConfigInt("e2ee_enabled", MR_E2EE_DEFAULT_ENABLED);
                    if( oldval == 1 ) {
                        MrMailbox.setConfig("e2ee_enabled", "0");
                    }
                    else {
                        MrMailbox.setConfig("e2ee_enabled", "1");
                    }
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(oldval == 0);
                    }
                }
                else if(i==manageKeysRow )
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity()); // was: BottomSheet.Builder
                    builder.setTitle(ApplicationLoader.applicationContext.getString(R.string.E2EManagePrivateKeys));
                    CharSequence[] items = new CharSequence[]{
                            ApplicationLoader.applicationContext.getString(R.string.ImportPrivateKeys),
                            ApplicationLoader.applicationContext.getString(R.string.ExportPrivateKeys),
                    };
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if( i== 0/*import*/ ) {
                                        if (Build.VERSION.SDK_INT >= 23 && getParentActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                            getParentActivity().requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 4);
                                            return;
                                        }
                                        AlertDialog.Builder builder2 = new AlertDialog.Builder(getParentActivity());
                                        builder2.setPositiveButton(ApplicationLoader.applicationContext.getString(R.string.OK), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                                                int cnt = MrMailbox.importStuff(MrMailbox.MR_IMEX_SELF_KEYS, downloadsDir.getAbsolutePath());
                                                String cntStr = ApplicationLoader.applicationContext.getResources().getQuantityString(R.plurals.ImportKeysDone, cnt, cnt);
                                                AndroidUtilities.showHint(ApplicationLoader.applicationContext, cntStr);
                                            }
                                        });
                                        builder2.setNegativeButton(ApplicationLoader.applicationContext.getString(R.string.Cancel), null);
                                        builder2.setMessage(ApplicationLoader.applicationContext.getString(R.string.ImportPrivateKeysAsk));
                                        showDialog(builder2.create());
                                    }
                                    else /*export*/ {
                                        if (Build.VERSION.SDK_INT >= 23 && getParentActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                            getParentActivity().requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4);
                                            return;
                                        }
                                        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                                        downloadsDir.mkdirs();
                                        MrMailbox.exportStuff(MrMailbox.MR_IMEX_SELF_KEYS, downloadsDir.getAbsolutePath());
                                        AndroidUtilities.showDoneHint(ApplicationLoader.applicationContext);
                                    }
                                }
                            }
                    );
                    showDialog(builder.create());
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
            int type = getItemViewType(i);
            return type!=ROWTYPE_SHADOW;
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
                if( i==finalShadowRow ) {
                    view.setBackgroundResource(R.drawable.greydivider_bottom);
                }
            } else if (type == ROWTYPE_TEXT_SETTINGS) {
                if (view == null) {
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                TextSettingsCell textCell = (TextSettingsCell) view;
                if (i == cacheRow) {
                    textCell.setText(mContext.getString(R.string.CacheSettings), true);
                }
                else if( i==manageKeysRow ) {
                    textCell.setText(mContext.getString(R.string.E2EManagePrivateKeys), false);
                }
            } else if (type == ROWTYPE_CHECK) {
                if (view == null) {
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                TextCheckCell textCell = (TextCheckCell) view;

                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                if (i == sendByEnterRow) {
                    textCell.setTextAndCheck(mContext.getString(R.string.SendByEnter), preferences.getBoolean("send_by_enter", false), true);
                } else if (i == raiseToSpeakRow) {
                    textCell.setTextAndCheck(mContext.getString(R.string.RaiseToSpeak), MediaController.getInstance().canRaiseToSpeak(), true);
                } else if (i == autoplayGifsRow) {
                    textCell.setTextAndCheck(mContext.getString(R.string.AutoplayGifs), MediaController.getInstance().canAutoplayGifs(), true);
                } else if (i == directShareRow) {
                    textCell.setTextAndValueAndCheck(mContext.getString(R.string.DirectShare), mContext.getString(R.string.DirectShareInfo), MediaController.getInstance().canDirectShare(), false, true);
                }
                else if( i==showUnknownSendersRow) {
                    textCell.setTextAndCheck(mContext.getString(R.string.DeaddropInChatlist),
                            MrMailbox.getConfigInt("show_deaddrop", 0)!=0, true);
                }
                else if( i == e2eEncryptionRow ) {
                    textCell.setTextAndCheck(mContext.getString(R.string.E2EEncryption),
                            MrMailbox.getConfigInt("e2ee_enabled", MR_E2EE_DEFAULT_ENABLED)!=0, true);
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == finalShadowRow ) {
                return ROWTYPE_SHADOW;
            } else if ( i == sendByEnterRow || i == raiseToSpeakRow || i == autoplayGifsRow || i==showUnknownSendersRow || i == directShareRow || i==e2eEncryptionRow) {
                return ROWTYPE_CHECK;
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
