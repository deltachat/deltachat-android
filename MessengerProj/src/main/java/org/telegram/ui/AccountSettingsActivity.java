/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *     Copyright (C) 2016 Björn Petersen Software Design and Development
 *                   Contact: r10s@b44t.com, http://b44t.com
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
 * File:    MrAccoutSettingsActivity.java
 * Authors: Björn Petersen
 * Purpose: Let the user configure his name
 *
 ******************************************************************************/

package org.telegram.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MrMailbox;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Adapters.BaseFragmentAdapter;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.MrEditTextCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;


public class AccountSettingsActivity extends BaseFragment {

    // the list
    private ListAdapter listAdapter;

    private int         rowSectionBasic;
    private int         rowAddr;
    private int         rowMailPw;
    private int         rowInfoBelowMailPw2;

    private int         rowSectionMail;
    private int         rowMailServer;
    private int         rowMailPort;
    private int         rowMailUser;
    private int         rowBreak2;

    private int         rowSectionSend;
    private int         rowSendServer;
    private int         rowSendPort;
    private int         rowSendUser;
    private int         rowSendPw;
    private int         rowInfoBelowSendPw;
    private int         rowCount;

    private final int   typeInfo          = 0; // no gaps here!
    private final int   typeTextEntry     = 1;
    private final int   typeShadowSection = 2;
    private final int   typeSection       = 3;

    MrEditTextCell      addrCell;  // warning all these objects may be null!
    MrEditTextCell      mailPwCell;
    MrEditTextCell      mailServerCell;
    MrEditTextCell      mailPortCell;
    MrEditTextCell      mailUserCell;
    MrEditTextCell      sendPwCell;
    MrEditTextCell      sendServerCell;
    MrEditTextCell      sendPortCell;
    MrEditTextCell      sendUserCell;

    // misc.
    private View             doneButton;
    private final static int done_button = 1;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        rowCount = 0;
        rowSectionBasic = rowCount++;
        rowAddr = rowCount++;
        rowMailPw = rowCount++;
        rowInfoBelowMailPw2 = rowCount++;

        rowSectionMail = rowCount++;
        rowMailServer = rowCount++;
        rowMailUser = rowCount++;
        rowMailPort = rowCount++;
        rowBreak2 = rowCount++;

        rowSectionSend = rowCount++;
        rowSendServer = rowCount++;
        rowSendUser = rowCount++;
        rowSendPw = rowCount++;
        rowSendPort = rowCount++;
        rowInfoBelowSendPw = rowCount++;

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
    }

    @Override
    public View createView(Context context) {

        // create action bar
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("AccountSettings", R.string.AccountSettings));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    if( isModified() ) { // TODO: maybe we should also ask if the user presses the "back" button
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setMessage(LocaleController.getString("DiscardChanges", R.string.DiscardChanges));
                        builder.setPositiveButton(LocaleController.getString("Yes", R.string.Yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finishFragment();
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("No", R.string.No), null);
                        showDialog(builder.create());
                    }
                    else {
                        finishFragment();
                    }
                } else if (id == done_button) {
                    saveData();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        doneButton = menu.addItemWithWidth(done_button, R.drawable.ic_done, AndroidUtilities.dp(56));

        // create object to hold the whole view
        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(0xfff0f0f0);

        // create the main layout list
        listAdapter = new ListAdapter(context);

        ListView listView = new ListView(context);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setVerticalScrollBarEnabled(false);
        listView.setDrawSelectorOnTop(true);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> adapterView, View view, final int i, long l) {
            }
        });

        return fragmentView;
    }

    private void saveData() {
        // Warning: the widgets are created as needed and may not be present!
        String v;

        /*
        if( !isModified() && MrMailbox.MrMailboxIsConfigured(MrMailbox.hMailbox)!=0 ) {
            finishFragment();
            return; // nothing to do
        }
        */

        if( addrCell!=null) {
            v = addrCell.getValue().trim();
            MrMailbox.MrMailboxSetConfig(MrMailbox.hMailbox, "addr", v.isEmpty() ? null : v);
        }

        if( mailPwCell!=null) {
            v = mailPwCell.getValue().trim();
            MrMailbox.MrMailboxSetConfig(MrMailbox.hMailbox, "mail_pw", v.isEmpty() ? null : v);
        }

        if( mailServerCell!=null) {
            v = mailServerCell.getValue().trim();
            MrMailbox.MrMailboxSetConfig(MrMailbox.hMailbox, "mail_server", v.isEmpty() ? null : v);
        }

        if( mailPortCell!=null ) {
            v = mailPortCell.getValue().trim();
            MrMailbox.MrMailboxSetConfig(MrMailbox.hMailbox, "mail_port", v.isEmpty() ? null : v);
        }

        if( mailUserCell!=null) {
            v = mailUserCell.getValue().trim();
            MrMailbox.MrMailboxSetConfig(MrMailbox.hMailbox, "mail_user", v.isEmpty() ? null : v);
        }

        if( sendServerCell!=null ) {
            v = sendServerCell.getValue().trim();
            MrMailbox.MrMailboxSetConfig(MrMailbox.hMailbox, "send_server", v.isEmpty() ? null : v);
        }

        if( sendPortCell!=null ) {
            v = sendPortCell.getValue().trim();
            MrMailbox.MrMailboxSetConfig(MrMailbox.hMailbox, "send_port", v.isEmpty() ? null : v);
        }

        if(sendUserCell!=null) {
            v = sendUserCell.getValue().trim();
            MrMailbox.MrMailboxSetConfig(MrMailbox.hMailbox, "send_user", v.isEmpty() ? null : v);
        }

        if( sendPwCell!=null ) {
            v = sendPwCell.getValue().trim();
            MrMailbox.MrMailboxSetConfig(MrMailbox.hMailbox, "send_pw", v.isEmpty() ? null : v);
        }

        MrMailbox.MrMailboxDisconnect(MrMailbox.hMailbox);
        MrMailbox.MrMailboxConfigure(MrMailbox.hMailbox);
        MrMailbox.MrMailboxConnect(MrMailbox.hMailbox);

        // show dialog

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setMessage("Testing the server connection, this may take a moment.");
        builder.setNeutralButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finishFragment();
            }
        });
        showDialog(builder.create());


        //finishFragment(); // disable this when a dialog is used

        NotificationCenter.getInstance().postNotificationName(NotificationCenter.mainUserInfoChanged);
    }

    private boolean isModified(){
        // Warning: the widgets are created as needed and may not be present!
        if( addrCell!=null && addrCell.isModified()) { return true; }
        if( mailPwCell!=null && mailPwCell.isModified()) { return true; }

        if( mailServerCell!=null && mailServerCell.isModified()) { return true; }
        if( mailPortCell!=null && mailPortCell.isModified()) { return true; }
        if( mailUserCell!=null && mailUserCell.isModified()) { return true; }

        if( sendServerCell!=null && sendServerCell.isModified()) { return true; }
        if( sendPortCell!=null && sendPortCell.isModified()) { return true; }
        if( sendUserCell!=null && sendUserCell.isModified()) { return true; }
        if( sendPwCell!=null && sendPwCell.isModified()) { return true; }

        return false;
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen && addrCell!=null) {
            if(addrCell.getValue().isEmpty()) {
                addrCell.getEditTextView().requestFocus();
                AndroidUtilities.showKeyboard(addrCell.getEditTextView());
            }
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
            return (i == rowAddr || i==rowMailPw || i==rowMailServer || i==rowMailPort|| i==rowMailUser
                    || i==rowSendServer || i==rowSendPort || i==rowSendUser || i== rowSendPw);
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
            int type = getItemViewType__(i);
            if (type == typeTextEntry) {
                if (i == rowAddr) {
                    if( addrCell==null) {
                        addrCell = new MrEditTextCell(mContext);
                        addrCell.setValueHintAndLabel(MrMailbox.MrMailboxGetConfig(MrMailbox.hMailbox, "addr", ""),
                                "", LocaleController.getString("MyEmailAddress", R.string.MyEmailAddress), false);
                        addrCell.getEditTextView().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                    }
                    view = addrCell;
                }
                else if (i == rowMailPw) {
                    if( mailPwCell==null) {
                        mailPwCell = new MrEditTextCell(mContext);
                        mailPwCell.setValueHintAndLabel(MrMailbox.MrMailboxGetConfig(MrMailbox.hMailbox, "mail_pw", ""),
                                "", LocaleController.getString("Password", R.string.Password), false);
                        mailPwCell.getEditTextView().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    }
                    view = mailPwCell;
                }
                else if (i == rowMailServer) {
                    if( mailServerCell==null) {
                        mailServerCell = new MrEditTextCell(mContext);
                        mailServerCell.setValueHintAndLabel(MrMailbox.MrMailboxGetConfig(MrMailbox.hMailbox, "mail_server", ""),
                                LocaleController.getString("Automatic", R.string.Automatic), LocaleController.getString("ImapServer", R.string.ImapServer), false);
                    }
                    view = mailServerCell;
                }
                else if (i == rowMailPort) {
                    if( mailPortCell==null) {
                        mailPortCell = new MrEditTextCell(mContext);
                        mailPortCell.setValueHintAndLabel(MrMailbox.MrMailboxGetConfig(MrMailbox.hMailbox, "mail_port", ""),
                                LocaleController.getString("Default", R.string.Default), LocaleController.getString("ImapPort", R.string.ImapPort), false);
                    }
                    view = mailPortCell;
                }
                else if (i == rowMailUser) {
                    if( mailUserCell==null) {
                        mailUserCell = new MrEditTextCell(mContext);
                        mailUserCell.setValueHintAndLabel(MrMailbox.MrMailboxGetConfig(MrMailbox.hMailbox, "mail_user", ""),
                                LocaleController.getString("FromAbove", R.string.FromAbove), LocaleController.getString("ImapLoginname", R.string.ImapLoginname), false);
                    }
                    view = mailUserCell;
                }
                else if (i == rowSendServer) {
                    if( sendServerCell==null) {
                        sendServerCell = new MrEditTextCell(mContext);
                        sendServerCell.setValueHintAndLabel(MrMailbox.MrMailboxGetConfig(MrMailbox.hMailbox, "send_server", ""),
                                LocaleController.getString("Automatic", R.string.Automatic), LocaleController.getString("SmtpServer", R.string.SmtpServer), false);
                    }
                    view = sendServerCell;
                }
                else if (i == rowSendPort) {
                    if( sendPortCell==null) {
                        sendPortCell = new MrEditTextCell(mContext);
                        sendPortCell.setValueHintAndLabel(MrMailbox.MrMailboxGetConfig(MrMailbox.hMailbox, "send_port", ""),
                                LocaleController.getString("Default", R.string.Default), LocaleController.getString("SmtpPort", R.string.SmtpPort), false);
                    }
                    view = sendPortCell;
                }
                else if (i == rowSendUser) {
                    if( sendUserCell==null) {
                        sendUserCell = new MrEditTextCell(mContext);
                        sendUserCell.setValueHintAndLabel(MrMailbox.MrMailboxGetConfig(MrMailbox.hMailbox, "send_user", ""),
                                LocaleController.getString("FromAbove", R.string.FromAbove), LocaleController.getString("SmtpLoginname", R.string.SmtpLoginname), false);
                    }
                    view = sendUserCell;
                }
                else if (i == rowSendPw) {
                    if( sendPwCell==null) {
                        sendPwCell = new MrEditTextCell(mContext);
                        sendPwCell.setValueHintAndLabel(MrMailbox.MrMailboxGetConfig(MrMailbox.hMailbox, "send_pw", ""),
                                LocaleController.getString("FromAbove", R.string.FromAbove), LocaleController.getString("SmtpPassword", R.string.SmtpPassword), false);
                        sendPwCell.getEditTextView().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    }
                    view = sendPwCell;
                }
            }
            else if (type == typeSection) {
                if (view == null) {
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                if (i == rowSectionBasic) {
                    ((HeaderCell) view).setText(LocaleController.getString("BasicSettings", R.string.BasicSettings));
                } else if (i == rowSectionMail) {
                    ((HeaderCell) view).setText(LocaleController.getString("InboxHeadline", R.string.InboxHeadline));
                } else if (i == rowSectionSend) {
                    ((HeaderCell) view).setText(LocaleController.getString("OutboxHeadline", R.string.OutboxHeadline));
                }
            }
            else if (type == typeShadowSection) {
                if (view == null) {
                    view = new ShadowSectionCell(mContext);
                }
            }
            else if (type == typeInfo) {
                if (view == null) {
                    view = new TextInfoPrivacyCell(mContext);
                }
                if( i==rowInfoBelowMailPw2) {
                    ((TextInfoPrivacyCell) view).setText(LocaleController.getString("MyAccoutExplain", R.string.MyAccountExplain));
                    view.setBackgroundResource(R.drawable.greydivider); // has shadow top+bottom
                }
                else if( i==rowInfoBelowSendPw) {
                    ((TextInfoPrivacyCell) view).setText(LocaleController.getString("MyAccountExplain2", R.string.MyAccountExplain2));
                    view.setBackgroundResource(R.drawable.greydivider_bottom);
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            return IGNORE_ITEM_VIEW_TYPE;
        }

        private int getItemViewType__(int i) {
            if (i == rowAddr || i==rowMailPw || i==rowMailServer || i==rowMailPort|| i==rowMailUser
                     || i==rowSendServer || i==rowSendPort || i==rowSendUser || i== rowSendPw ) {
                return typeTextEntry;
            }
            else if( i==rowSectionBasic || i==rowSectionMail || i==rowSectionSend ) {
                return typeSection;
            }
            else if( i== rowBreak2) {
                return typeShadowSection;
            }
            return typeInfo;
        }

        @Override
        public int getViewTypeCount() {
            return 1; /* SIC! internally, we ingnore the type, each row has its own type--otherwise text entry stuff would not work */
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
