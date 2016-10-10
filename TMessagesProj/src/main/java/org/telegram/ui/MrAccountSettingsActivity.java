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
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MrMailbox;
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


public class MrAccountSettingsActivity extends BaseFragment {

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
    private final int   typeCount         = 4; // /no gaps here!

    MrEditTextCell      addrCell;
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
        rowMailPort = rowCount++;
        rowMailUser = rowCount++;
        rowBreak2 = rowCount++;

        rowSectionSend = rowCount++;
        rowSendServer = rowCount++;
        rowSendPort = rowCount++;
        rowSendUser = rowCount++;
        rowSendPw = rowCount++;
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
        actionBar.setTitle("Kontoeinstellungen");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    /*
                    if( addrCell.isValueModified() ) { // TODO: maybe we should also ask if the user presses the "back" button
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                        builder.setMessage("Änderungen verwerfen?");
                        builder.setPositiveButton(LocaleController.getString("Yes", R.string.Yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finishFragment();
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("No", R.string.No), null);
                        showDialog(builder.create());
                    }
                    else
                    */
                    {
                        finishFragment();
                    }
                } else if (id == done_button) {
                    saveData();
                    finishFragment();
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
        MrMailbox.MrMailboxSetConfig(MrMailbox.hMailbox, "addr", addrCell.getValue());

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
            int type = getItemViewType(i);
            if (type == typeTextEntry) {
                if (view == null) {
                    view = new MrEditTextCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                MrEditTextCell editTextCell = (MrEditTextCell) view;
                if (i == rowAddr) {
                    addrCell = editTextCell;
                    editTextCell.setValueHintAndLabel(MrMailbox.MrMailboxGetConfig(MrMailbox.hMailbox, "addr", ""),
                            "", "Meine E-Mail-Adresse", false);
                }
                else if (i == rowMailPw) {
                    mailPwCell = editTextCell;
                    editTextCell.setValueHintAndLabel(MrMailbox.MrMailboxGetConfig(MrMailbox.hMailbox, "mail_pw", ""),
                            "", "Passwort", false);
                }
                else if (i == rowMailServer) {
                    mailServerCell = editTextCell;
                    editTextCell.setValueHintAndLabel(MrMailbox.MrMailboxGetConfig(MrMailbox.hMailbox, "mail_server", ""),
                            "", "IMAP-/POP-Server", false);
                }
                else if (i == rowMailPort) {
                    mailPortCell = editTextCell;
                    editTextCell.setValueHintAndLabel(MrMailbox.MrMailboxGetConfig(MrMailbox.hMailbox, "mail_port", ""),
                            "", "IMAP-/POP-Port", false);
                }
                else if (i == rowMailUser) {
                    mailUserCell = editTextCell;
                    editTextCell.setValueHintAndLabel(MrMailbox.MrMailboxGetConfig(MrMailbox.hMailbox, "mail_user", ""),
                            "", "IMAP-/POP-Loginname", false);
                }
                else if (i == rowSendServer) {
                    sendServerCell = editTextCell;
                    editTextCell.setValueHintAndLabel(MrMailbox.MrMailboxGetConfig(MrMailbox.hMailbox, "send_server", ""),
                            "", "SMTP-Server", false);
                }
                else if (i == rowSendPort) {
                    sendPortCell = editTextCell;
                    editTextCell.setValueHintAndLabel(MrMailbox.MrMailboxGetConfig(MrMailbox.hMailbox, "send_port", ""),
                            "", "SMTP-Port", false);
                }
                else if (i == rowSendUser) {
                    sendUserCell = editTextCell;
                    editTextCell.setValueHintAndLabel(MrMailbox.MrMailboxGetConfig(MrMailbox.hMailbox, "send_user", ""),
                            "", "SMTP-Loginname", false);
                }
                else if (i == rowSendPw) {
                    sendPwCell = editTextCell;
                    editTextCell.setValueHintAndLabel(MrMailbox.MrMailboxGetConfig(MrMailbox.hMailbox, "send_pw", ""),
                            "", "SMTP-Passwort", false);
                }
            }
            else if (type == typeSection) {
                if (view == null) {
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                if (i == rowSectionBasic) {
                    ((HeaderCell) view).setText("Grundeinstellungen");
                } else if (i == rowSectionMail) {
                    ((HeaderCell) view).setText("Posteingangs-Einstellungen");
                    //view.setBackgroundResource(R.drawable.greydivider_top);
                } else if (i == rowSectionSend) {
                    ((HeaderCell) view).setText("Postausgangs-Einstellungen");
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
                    ((TextInfoPrivacyCell) view).setText("LibreChat versucht aus den eingegebenen Grundeinstellungen die restlichen Einstellungen automatisch zu ermitteln.\n\nWenn dies nicht gelingt, müssen in den folgenden Felder die notwendigen Daten eingegeben werden:");
                    view.setBackgroundResource(R.drawable.greydivider); // has shadow top+bottom
                }
                else if( i==rowInfoBelowSendPw) {
                    ((TextInfoPrivacyCell) view).setText("Bei Problemen kann vielleicht Ihr E-Mail-Provider oder ein Bekannter weiterhelfen.");
                    view.setBackgroundResource(R.drawable.greydivider_bottom);
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
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
            return typeCount;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
