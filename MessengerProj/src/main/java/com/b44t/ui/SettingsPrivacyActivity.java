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
 ******************************************************************************/


package com.b44t.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.MrChat;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.R;
import com.b44t.messenger.UserConfig;
import com.b44t.ui.ActionBar.ActionBar;
import com.b44t.ui.ActionBar.BaseFragment;
import com.b44t.ui.Adapters.BaseFragmentAdapter;
import com.b44t.ui.Cells.TextCheckCell;
import com.b44t.ui.Cells.TextInfoCell;
import com.b44t.ui.Cells.TextSettingsCell;
import com.b44t.ui.Components.LayoutHelper;

public class SettingsPrivacyActivity extends BaseFragment {

    private ListAdapter listAdapter;

    private int e2eEncryptionRow;
    private int readReceiptsRow;
    private int blockedRow;
    private int showUnknownSendersRow;
    private int passcodeRow;
    private int secretDetailRow;
    private int manageKeysRow;
    private int rowCount;

    private final int TYPE_TEXTSETTING = 0;
    private final int TYPE_TEXT_INFO   = 1;
    private final int TYPE_CHECK_CELL  = 2;
    private final int TYPE_COUNT       = 3;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        rowCount = 0;
        passcodeRow             = rowCount++;
        blockedRow              = rowCount++;
        showUnknownSendersRow   = rowCount++;
        readReceiptsRow         = rowCount++;
        e2eEncryptionRow        = rowCount++;
        manageKeysRow           = rowCount++;
        secretDetailRow         = rowCount++;

        return true;
    }

    @Override
    public View createView(final Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(context.getString(R.string.PrivacySettings));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        listAdapter = new ListAdapter(context);

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(0xfff0f0f0);

        ListView listView = new ListView(context);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setDrawSelectorOnTop(true);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                if (i == blockedRow) {
                    presentFragment(new BlockedUsersActivity());
                } else if (i == passcodeRow) {
                    if (UserConfig.passcodeHash.length() > 0) {
                        presentFragment(new PasscodeActivity(PasscodeActivity.SCREEN2_ENTER_CODE2));
                    } else {
                        presentFragment(new PasscodeActivity(PasscodeActivity.SCREEN0_SETTINGS));
                    }
                }
                else if(i==e2eEncryptionRow )
                {
                    Toast.makeText(getParentActivity(), context.getString(R.string.NotYetImplemented), Toast.LENGTH_SHORT).show();
                }
                else if(i==manageKeysRow )
                {
                    Toast.makeText(getParentActivity(), context.getString(R.string.NotYetImplemented), Toast.LENGTH_SHORT).show();
                }
                else if(i==readReceiptsRow )
                {
                    Toast.makeText(getParentActivity(), context.getString(R.string.NotYetImplemented), Toast.LENGTH_SHORT).show();
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
            }
        });

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
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
            return type!=TYPE_TEXT_INFO;
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
            if (type == TYPE_TEXTSETTING) {
                if (view == null) {
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                TextSettingsCell textCell = (TextSettingsCell) view;
                if (i == blockedRow) {
                    String cntStr = String.format("%d", MrMailbox.getBlockedCount());
                    textCell.setTextAndValue(ApplicationLoader.applicationContext.getString(R.string.BlockedContacts), cntStr, true);
                } else if (i == passcodeRow) {
                    String val = UserConfig.passcodeHash.length() > 0? mContext.getString(R.string.Enabled) : mContext.getString(R.string.Disabled);
                    textCell.setTextAndValue(mContext.getString(R.string.Passcode), val, true);
                }
                else if( i==manageKeysRow ) {
                    textCell.setText(mContext.getString(R.string.E2EManagePrivateKeys), true);
                }
            } else if (type == TYPE_TEXT_INFO) {
                if (view == null) {
                    view = new TextInfoCell(mContext);
                }
                if (i == secretDetailRow) {
                    ((TextInfoCell) view).setText("");
                    view.setBackgroundResource(R.drawable.greydivider_bottom);
                }
            } else if (type == TYPE_CHECK_CELL ) {
                if (view == null) {
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                TextCheckCell textCell = (TextCheckCell) view;
                if( i == e2eEncryptionRow ) {
                    textCell.setTextAndCheck(mContext.getString(R.string.E2EEncryption),
                            MrMailbox.getConfigInt("e2e_encrypt", 0)!=0, true);
                }
                else if( i == readReceiptsRow ) {
                    textCell.setTextAndCheck(mContext.getString(R.string.SendNRcvReadReceipts),
                            MrMailbox.getConfigInt("read_receipts", 0)!=0, true);
                }
                else if( i==showUnknownSendersRow) {
                    textCell.setTextAndValueAndCheck(mContext.getString(R.string.DeaddropInChatlist),
                            mContext.getString(R.string.DeaddropSubtitle),
                            MrMailbox.getConfigInt("show_deaddrop", 0)!=0, true, true);
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == secretDetailRow) {
                return TYPE_TEXT_INFO;
            } else if (i==showUnknownSendersRow || i==e2eEncryptionRow || i==readReceiptsRow ) {
                return TYPE_CHECK_CELL;
            }
            return TYPE_TEXTSETTING;
        }

        @Override
        public int getViewTypeCount() {
            return TYPE_COUNT;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
