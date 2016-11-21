/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

/* heavily modified by Björn Petersen for Delta Chat */


package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MrMailbox;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Adapters.BaseFragmentAdapter;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;

public class PrivacySettingsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private ListAdapter listAdapter;

    private int privacySectionRow;
    private int blockedRow;
    private int showUnknownSendersRow;
    private int securitySectionRow;
    private int passcodeRow;
    private int secretDetailRow;
    private int rowCount;

    private int typeTextCheck = 3;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        //ContactsController.getInstance().loadPrivacySettings();

        rowCount = 0;
        privacySectionRow       = -1;
        blockedRow              = rowCount++;
        showUnknownSendersRow   = rowCount++;
        passcodeRow             = rowCount++;
        securitySectionRow      = -1;
        secretDetailRow         = rowCount++;

        //NotificationCenter.getInstance().addObserver(this, NotificationCenter.privacyRulesUpdated);

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        //NotificationCenter.getInstance().removeObserver(this, NotificationCenter.privacyRulesUpdated);
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("PrivacySettings", R.string.PrivacySettings));
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
        listView.setVerticalScrollBarEnabled(false);
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
                        presentFragment(new PasscodeActivity(2));
                    } else {
                        presentFragment(new PasscodeActivity(0));
                    }
                } else if( i==showUnknownSendersRow) {
                    int oldval = MrMailbox.MrMailboxGetConfigInt(MrMailbox.hMailbox, "show_deaddrop", 0);
                    if( oldval == 1 ) {
                        MrMailbox.MrMailboxSetConfig(MrMailbox.hMailbox, "show_deaddrop", "0");
                    }
                    else {
                        MrMailbox.MrMailboxSetConfig(MrMailbox.hMailbox, "show_deaddrop", "1");
                    }
                    MrMailbox.MrCallback(MrMailbox.MR_EVENT_MSGS_UPDATED, 0, 0);
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(oldval == 0);
                    }

                    // if showing deaddrop is disabled, also disable notifications for this chat (cannot be displayed otherwise)
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt("notify2_" + MrMailbox.MR_CHAT_ID_DEADDROP, oldval==1? 2 /*always muted*/ : 0);
                    editor.commit();
                }
            }
        });

        return fragmentView;
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        /*if (id == NotificationCenter.privacyRulesUpdated) {
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
        }*/
    }

    /*
    private String formatRulesString(boolean isGroup) {
        ArrayList<TLRPC.PrivacyRule> privacyRules = ContactsController.getInstance().getPrivacyRules(isGroup);
        if (privacyRules.size() == 0) {
            return LocaleController.getString("LastSeenNobody", R.string.LastSeenNobody);
        }
        int type = -1;
        int plus = 0;
        int minus = 0;
        for (int a = 0; a < privacyRules.size(); a++) {
            TLRPC.PrivacyRule rule = privacyRules.get(a);
            if (rule instanceof TLRPC.TL_privacyValueAllowUsers) {
                plus += rule.users.size();
            } else if (rule instanceof TLRPC.TL_privacyValueDisallowUsers) {
                minus += rule.users.size();
            } else if (rule instanceof TLRPC.TL_privacyValueAllowAll) {
                type = 0;
            } else if (rule instanceof TLRPC.TL_privacyValueDisallowAll) {
                type = 1;
            } else {
                type = 2;
            }
        }
        if (type == 0 || type == -1 && minus > 0) {
            if (minus == 0) {
                return LocaleController.getString("LastSeenEverybody", R.string.LastSeenEverybody);
            } else {
                return LocaleController.formatString("LastSeenEverybodyMinus", R.string.LastSeenEverybodyMinus, minus);
            }
        } else if (type == 2 || type == -1 && minus > 0 && plus > 0) {
            if (plus == 0 && minus == 0) {
                return LocaleController.getString("LastSeenContacts", R.string.LastSeenContacts);
            } else {
                if (plus != 0 && minus != 0) {
                    return LocaleController.formatString("LastSeenContactsMinusPlus", R.string.LastSeenContactsMinusPlus, minus, plus);
                } else if (minus != 0) {
                    return LocaleController.formatString("LastSeenContactsMinus", R.string.LastSeenContactsMinus, minus);
                } else {
                    return LocaleController.formatString("LastSeenContactsPlus", R.string.LastSeenContactsPlus, plus);
                }
            }
        } else if (type == 1 || plus > 0) {
            if (plus == 0) {
                return LocaleController.getString("LastSeenNobody", R.string.LastSeenNobody);
            } else {
                return LocaleController.formatString("LastSeenNobodyPlus", R.string.LastSeenNobodyPlus, plus);
            }
        }
        return "unknown";
    }
    */

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
            return (i == passcodeRow) || (i == blockedRow) ||
                    (i == showUnknownSendersRow);
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
            if (type == 0) {
                if (view == null) {
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                TextSettingsCell textCell = (TextSettingsCell) view;
                if (i == blockedRow) {
                    textCell.setText(LocaleController.getString("BlockedUsers", R.string.BlockedUsers), true);
                } else if (i == passcodeRow) {
                    textCell.setText(LocaleController.getString("Passcode", R.string.Passcode), true);
                }
            } else if (type == 1) {
                if (view == null) {
                    view = new TextInfoPrivacyCell(mContext);
                }
                if (i == secretDetailRow) {
                    ((TextInfoPrivacyCell) view).setText("");
                    view.setBackgroundResource(R.drawable.greydivider_bottom);
                }
            } else if (type == 2) {
                if (view == null) {
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                if (i == privacySectionRow) {
                    ((HeaderCell) view).setText(LocaleController.getString("Settings", R.string.Settings));
                } else if (i == securitySectionRow) {
                    ((HeaderCell) view).setText(LocaleController.getString("SecurityTitle", R.string.SecurityTitle));
                }
            } else if (type == typeTextCheck) {
                if (view == null) {
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                TextCheckCell textCell = (TextCheckCell) view;
                if( i==showUnknownSendersRow) {
                    textCell.setTextAndCheck(LocaleController.getString("DeaddropInChatlist", R.string.DeaddropInChatlist),
                            MrMailbox.MrMailboxGetConfigInt(MrMailbox.hMailbox, "show_deaddrop", 0)!=0, true);
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == blockedRow || i == passcodeRow ) {
                return 0;
            } else if (i == secretDetailRow) {
                return 1;
            } else if (i == securitySectionRow || i == privacySectionRow ) {
                return 2;
            } else if (i==showUnknownSendersRow) {
                return typeTextCheck;
            }
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 4;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
