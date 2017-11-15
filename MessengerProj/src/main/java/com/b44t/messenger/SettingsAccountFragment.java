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
 * File:    MrAccoutSettingsActivity.java
 * Purpose: Let the user configure his name
 *
 ******************************************************************************/

package com.b44t.messenger;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.b44t.messenger.ActionBar.ActionBar;
import com.b44t.messenger.ActionBar.ActionBarMenu;
import com.b44t.messenger.ActionBar.BaseFragment;
import com.b44t.messenger.Components.BaseFragmentAdapter;
import com.b44t.messenger.Cells.HeaderCell;
import com.b44t.messenger.Cells.EditTextCell;
import com.b44t.messenger.Cells.ShadowSectionCell;
import com.b44t.messenger.Cells.TextInfoCell;
import com.b44t.messenger.Cells.TextSettingsCell;
import com.b44t.messenger.Components.LayoutHelper;


public class SettingsAccountFragment extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    // the list
    private ListView    listView;
    private ListAdapter listAdapter;

    private int         rowAddrHeadline;
    private int         rowAddr;
    private int         rowMailPwHeadline;
    private int         rowMailPw;
    private int         rowOpenAdvOpions;

    private int         rowMailHeadline;
    private int         rowMailServer;
    private int         rowMailPort;
    private int         rowMailUser;
    private int         rowMailSecurity;
    private int         rowBreak1;

    private int         rowSendHeadline;
    private int         rowSendServer;
    private int         rowSendPort;
    private int         rowSendUser;
    private int         rowSendPw;
    private int         rowSendSecurity;

    private int         rowInfoBelowSendPw;
    private int         rowCount;

    private final int ROWTYPE_INFO         = 0; // no gaps here!
    private final int ROWTYPE_TEXT_ENTRY   = 1;
    private final int ROWTYPE_SHADOW_BREAK = 2;
    private final int ROWTYPE_HEADLINE     = 3;
    private final int ROWTYPE_TEXT_FLAGS   = 4;

    private EditTextCell addrCell;  // warning all these objects may be null!
    private EditTextCell mailPwCell;
    private EditTextCell mailServerCell;
    private EditTextCell mailPortCell;
    private EditTextCell mailUserCell;
    private EditTextCell sendPwCell;
    private EditTextCell sendServerCell;
    private EditTextCell sendPortCell;
    private EditTextCell sendUserCell;

    private final int MR_IMAP_SOCKET_STARTTLS =   0x100;
    private final int MR_IMAP_SOCKET_SSL      =   0x200;
    private final int MR_IMAP_SOCKET_PLAIN    =   0x400;
    private final int MR_SMTP_SOCKET_STARTTLS = 0x10000;
    private final int MR_SMTP_SOCKET_SSL      = 0x20000;
    private final int MR_SMTP_SOCKET_PLAIN    = 0x40000;
    private int m_serverFlags;

    // misc.
    private final int        ID_DONE_BUTTON = 1;
    private ProgressDialog   progressDialog = null;
    private boolean          fromIntro;
    private boolean          m_expanded = false;

    public SettingsAccountFragment(Bundle args) {
        super();
        if( args!=null ) {
            fromIntro = args.getBoolean("fromIntro", false);
        }
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.configureEnded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.configureProgress);

        m_serverFlags = MrMailbox.getConfigInt("server_flags", 0);

        m_expanded = false;
        if( !MrMailbox.getConfig("mail_user", "").isEmpty()
         || !MrMailbox.getConfig("mail_server", "").isEmpty()
         || !MrMailbox.getConfig("mail_port", "").isEmpty()
         || !MrMailbox.getConfig("send_user", "").isEmpty()
         || !MrMailbox.getConfig("send_pw", "").isEmpty()
         || !MrMailbox.getConfig("send_server", "").isEmpty()
         || !MrMailbox.getConfig("send_port", "").isEmpty()
         || (m_serverFlags!=0) ) {
            m_expanded = true;
        }

        calculateRows();

        return true;
    }

    private void calculateRows()
    {
        rowCount = 0;

        rowAddrHeadline  = rowCount++;
        rowAddr          = rowCount++;
        rowMailPwHeadline= rowCount++;
        rowMailPw        = rowCount++;
        rowOpenAdvOpions = rowCount++;

        if( m_expanded ) {
            rowMailHeadline  = rowCount++;
            rowMailUser      = rowCount++; // should be the first additional option, the loginname is the component, that cannot be configured automatically (if not derivable from the address)
            rowMailServer    = rowCount++;
            rowMailPort      = rowCount++;
            rowMailSecurity  = rowCount++;
            rowBreak1        = rowCount++;

            rowSendHeadline  = rowCount++;
            rowSendUser      = rowCount++;
            rowSendPw        = rowCount++;
            rowSendServer    = rowCount++;
            rowSendPort      = rowCount++;
            rowSendSecurity  = rowCount++;
        }
        else {
            rowMailHeadline  = -1;
            rowMailUser      = -1;
            rowMailServer    = -1;
            rowMailPort      = -1;
            rowMailSecurity  = -1;
            rowBreak1        = -1;

            rowSendHeadline  = -1;
            rowSendUser      = -1;
            rowSendPw        = -1;
            rowSendServer    = -1;
            rowSendPort      = -1;
            rowSendSecurity  = -1;
        }

        rowInfoBelowSendPw = rowCount++;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.configureEnded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.configureProgress);
    }

    @Override
    public View createView(final Context context) {

        // create action bar
        if( !fromIntro ) {
            actionBar.setBackButtonImage(R.drawable.ic_close_white);
        }

        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(context.getString(R.string.AccountSettings));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1 && !fromIntro ) { // no "is modified" check: as we use "close/ok" buttons instead of a "back" button it is more clear what happens. moreover, the user may have done a failed "OK" in between, so a question "discard changes?" would be ambiguously
                    finishFragment();
                } else if (id == ID_DONE_BUTTON) {
                    saveData();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItemWithWidth(ID_DONE_BUTTON, R.drawable.ic_done, AndroidUtilities.dp(56));

        // create object to hold the whole view
        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(0xfff0f0f0);

        // create the main layout list
        listAdapter = new ListAdapter(context);

        listView = new ListView(context);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setDrawSelectorOnTop(true);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> adapterView, View view, final int i, long l) {
                if( i==rowOpenAdvOpions )
                {
                    m_expanded = !m_expanded;
                    calculateRows();
                    listAdapter.notifyDataSetChanged();
                }
                else if( i==rowMailSecurity || i==rowSendSecurity )
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(R.string.SecurityTitle);
                    builder.setItems(new CharSequence[]{
                            ApplicationLoader.applicationContext.getString(R.string.Automatic),
                            "SSL/TLS", /*1*/
                            "STARTTLS", /*2*/
                            ApplicationLoader.applicationContext.getString(R.string.Disabled) /*3*/
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if( i==rowMailSecurity ) {
                                m_serverFlags &= ~(MR_IMAP_SOCKET_SSL | MR_IMAP_SOCKET_STARTTLS | MR_IMAP_SOCKET_PLAIN);
                                switch( which ) {
                                    case 1: m_serverFlags |= MR_IMAP_SOCKET_SSL; break;
                                    case 2: m_serverFlags |= MR_IMAP_SOCKET_STARTTLS; break;
                                    case 3: m_serverFlags |= MR_IMAP_SOCKET_PLAIN; break;
                                }
                            }
                            else if( i==rowSendSecurity ) {
                                m_serverFlags &= ~(MR_SMTP_SOCKET_SSL | MR_SMTP_SOCKET_STARTTLS | MR_SMTP_SOCKET_PLAIN);
                                switch( which ) {
                                    case 1: m_serverFlags |= MR_SMTP_SOCKET_SSL; break;
                                    case 2: m_serverFlags |= MR_SMTP_SOCKET_STARTTLS; break;
                                    case 3: m_serverFlags |= MR_SMTP_SOCKET_PLAIN; break;
                                }
                            }
                            listView.invalidateViews();
                        }
                    });
                    builder.setNegativeButton(R.string.Cancel, null);
                    showDialog(builder.create());

                }
            }
        });

        return fragmentView;
    }

    private void saveData() {
        // Warning: the widgets are created as needed and may not be present!
        String v;

        if( addrCell!=null) {
            v = addrCell.getValue().trim();
            MrMailbox.setConfig("addr", v.isEmpty() ? null : v);
        }

        if( mailPwCell!=null) {
            v = mailPwCell.getValue().trim();
            MrMailbox.setConfig("mail_pw", v.isEmpty() ? null : v);
        }

        if( mailServerCell!=null) {
            v = mailServerCell.getValue().trim();
            MrMailbox.setConfig("mail_server", v.isEmpty() ? null : v);
        }

        if( mailPortCell!=null ) {
            v = mailPortCell.getValue().trim();
            MrMailbox.setConfig("mail_port", v.isEmpty() ? null : v);
        }

        if( mailUserCell!=null) {
            v = mailUserCell.getValue().trim();
            MrMailbox.setConfig("mail_user", v.isEmpty() ? null : v);
        }

        if( sendServerCell!=null ) {
            v = sendServerCell.getValue().trim();
            MrMailbox.setConfig("send_server", v.isEmpty() ? null : v);
        }

        if( sendPortCell!=null ) {
            v = sendPortCell.getValue().trim();
            MrMailbox.setConfig("send_port", v.isEmpty() ? null : v);
        }

        if(sendUserCell!=null) {
            v = sendUserCell.getValue().trim();
            MrMailbox.setConfig("send_user", v.isEmpty() ? null : v);
        }

        if( sendPwCell!=null ) {
            v = sendPwCell.getValue().trim();
            MrMailbox.setConfig("send_pw", v.isEmpty() ? null : v);
        }

        MrMailbox.setConfigInt("server_flags", m_serverFlags);

        // show dialog
        if( progressDialog!=null ) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        progressDialog = new ProgressDialog(getParentActivity());
        progressDialog.setMessage(ApplicationLoader.applicationContext.getString(R.string.OneMoment));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, ApplicationLoader.applicationContext.getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MrMailbox.configureCancel();
                    }
	        });
        progressDialog.show();

        synchronized (MrMailbox.m_lastErrorLock) {
            MrMailbox.m_showNextErrorAsToast = false;
            MrMailbox.m_lastErrorString = "";
        }

        // try to connect, this results in an MR_EVENT_CONFIGURE_ENDED resp. NotificationCenter.configureEnded event
        Utilities.searchQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                final int res = MrMailbox.configureAndConnect();
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.configureEnded, (int)res);
                    }
                });
            }
        });

    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if( id == NotificationCenter.configureProgress )
        {
            if( progressDialog!=null ) {
                // we want the spinner together with a progress info
                int percent = (Integer)args[0] / 10;
                progressDialog.setMessage(ApplicationLoader.applicationContext.getString(R.string.OneMoment)+String.format(" %d%%", percent));
            }
        }
        else if (id == NotificationCenter.configureEnded )
        {
            final String errorString;

            synchronized (MrMailbox.m_lastErrorLock) {
                MrMailbox.m_showNextErrorAsToast = true;
                errorString = MrMailbox.m_lastErrorString;
            }

            if( progressDialog!=null ) {
                progressDialog.dismiss();
                progressDialog = null;
            }

            if( (int)args[0]==1 ) {
                if (fromIntro) {
                    presentFragment(new ChatlistActivity(null), true);
                } else {
                    finishFragment();
                }
                AndroidUtilities.showDoneHint(ApplicationLoader.applicationContext);
                NotificationCenter.getInstance().postNotificationName(NotificationCenter.mainUserInfoChanged);
            }
            else if( ! MrMailbox.m_lastErrorString.isEmpty() ){
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setMessage(errorString);
                builder.setPositiveButton(R.string.OK, null);
                showDialog(builder.create());
            }
        }
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        // if the address is empty, automatically show the keyboard
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
            return !(i==rowAddrHeadline || i==rowMailPwHeadline || i== rowMailHeadline || i== rowBreak1 || i== rowSendHeadline || i==rowInfoBelowSendPw);
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
            if (type == ROWTYPE_TEXT_ENTRY) {
                if (i == rowAddr) {
                    if( addrCell==null) {
                        addrCell = new EditTextCell(mContext, false);
                        addrCell.setValueHintAndLabel(MrMailbox.getConfig("addr", ""),
                                "", "", false);
                        addrCell.getEditTextView().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                    }
                    view = addrCell;
                }
                else if (i == rowMailPw) {
                    if( mailPwCell==null) {
                        mailPwCell = new EditTextCell(mContext, false);
                        mailPwCell.setValueHintAndLabel(MrMailbox.getConfig("mail_pw", ""),
                                "", "", false);
                        mailPwCell.getEditTextView().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    }
                    view = mailPwCell;
                }
                else if (i == rowMailServer) {
                    if( mailServerCell==null) {
                        mailServerCell = new EditTextCell(mContext);
                        mailServerCell.setValueHintAndLabel(MrMailbox.getConfig("mail_server", ""),
                                ApplicationLoader.applicationContext.getString(R.string.Automatic), ApplicationLoader.applicationContext.getString(R.string.ImapServer), false);
                        mailServerCell.getEditTextView().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
                    }
                    view = mailServerCell;
                }
                else if (i == rowMailPort) {
                    if( mailPortCell==null) {
                        mailPortCell = new EditTextCell(mContext);
                        mailPortCell.setValueHintAndLabel(MrMailbox.getConfig("mail_port", ""),
                                ApplicationLoader.applicationContext.getString(R.string.Automatic), ApplicationLoader.applicationContext.getString(R.string.ImapPort), false);
                        mailPortCell.getEditTextView().setInputType(InputType.TYPE_CLASS_NUMBER);
                    }
                    view = mailPortCell;
                }
                else if (i == rowMailUser) {
                    if( mailUserCell==null) {
                        mailUserCell = new EditTextCell(mContext);
                        mailUserCell.setValueHintAndLabel(MrMailbox.getConfig("mail_user", ""),
                                ApplicationLoader.applicationContext.getString(R.string.Automatic), ApplicationLoader.applicationContext.getString(R.string.ImapLoginname), false);
                    }
                    view = mailUserCell;
                }
                else if (i == rowSendServer) {
                    if( sendServerCell==null) {
                        sendServerCell = new EditTextCell(mContext);
                        sendServerCell.setValueHintAndLabel(MrMailbox.getConfig("send_server", ""),
                                ApplicationLoader.applicationContext.getString(R.string.Automatic), ApplicationLoader.applicationContext.getString(R.string.SmtpServer), false);
                        sendServerCell.getEditTextView().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
                    }
                    view = sendServerCell;
                }
                else if (i == rowSendPort) {
                    if( sendPortCell==null) {
                        sendPortCell = new EditTextCell(mContext);
                        sendPortCell.setValueHintAndLabel(MrMailbox.getConfig("send_port", ""),
                                ApplicationLoader.applicationContext.getString(R.string.Automatic), ApplicationLoader.applicationContext.getString(R.string.SmtpPort), false);
                        sendPortCell.getEditTextView().setInputType(InputType.TYPE_CLASS_NUMBER);
                    }
                    view = sendPortCell;
                }
                else if (i == rowSendUser) {
                    if( sendUserCell==null) {
                        sendUserCell = new EditTextCell(mContext);
                        sendUserCell.setValueHintAndLabel(MrMailbox.getConfig("send_user", ""),
                                ApplicationLoader.applicationContext.getString(R.string.Automatic), ApplicationLoader.applicationContext.getString(R.string.SmtpLoginname), false);
                    }
                    view = sendUserCell;
                }
                else if (i == rowSendPw) {
                    if( sendPwCell==null) {
                        sendPwCell = new EditTextCell(mContext);
                        sendPwCell.setValueHintAndLabel(MrMailbox.getConfig("send_pw", ""),
                                ApplicationLoader.applicationContext.getString(R.string.FromAbove), ApplicationLoader.applicationContext.getString(R.string.SmtpPassword), false);
                        sendPwCell.getEditTextView().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    }
                    view = sendPwCell;
                }
            }
            else if (type ==ROWTYPE_TEXT_FLAGS) {
                if (view == null) {
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                TextSettingsCell textCell = (TextSettingsCell) view;
                String value = ApplicationLoader.applicationContext.getString(R.string.Automatic);
                if( i == rowMailSecurity ) {
                    if( (m_serverFlags&MR_IMAP_SOCKET_SSL)!=0  ) { value = "SSL/TLS"; }
                    if( (m_serverFlags&MR_IMAP_SOCKET_STARTTLS)!=0 ) { value = "STARTTLS"; }
                    if( (m_serverFlags&MR_IMAP_SOCKET_PLAIN)!=0 ) { value = ApplicationLoader.applicationContext.getString(R.string.Disabled); }
                }
                else if( i == rowSendSecurity ) {
                    if( (m_serverFlags&MR_SMTP_SOCKET_SSL)!=0  ) { value = "SSL/TLS"; }
                    if( (m_serverFlags&MR_SMTP_SOCKET_STARTTLS)!=0 ) { value = "STARTTLS"; }
                    if( (m_serverFlags&MR_SMTP_SOCKET_PLAIN)!=0 ) { value = ApplicationLoader.applicationContext.getString(R.string.Disabled); }
                }
                textCell.setTextAndValue(ApplicationLoader.applicationContext.getString(R.string.SecurityTitle), value, false);
            }
            else if (type == ROWTYPE_HEADLINE) {
                if (view == null) {
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                if (i == rowAddrHeadline) {
                    ((HeaderCell) view).setText(ApplicationLoader.applicationContext.getString(R.string.EmailAddress));
                } else if (i == rowMailPwHeadline) {
                    ((HeaderCell) view).setText(ApplicationLoader.applicationContext.getString(R.string.Password));
                } else if (i == rowMailHeadline) {
                    ((HeaderCell) view).setText(ApplicationLoader.applicationContext.getString(R.string.InboxHeadline));
                } else if (i == rowSendHeadline) {
                    ((HeaderCell) view).setText(ApplicationLoader.applicationContext.getString(R.string.OutboxHeadline));
                }
            }
            else if (type == ROWTYPE_SHADOW_BREAK) {
                if (view == null) {
                    view = new ShadowSectionCell(mContext);
                }
            }
            else if (type == ROWTYPE_INFO) {
                if (view == null) {
                    view = new TextInfoCell(mContext);
                }
                if( i== rowOpenAdvOpions) {
                    ((TextInfoCell) view).setText(ApplicationLoader.applicationContext.getString(R.string.MyAccountExplain),
                            m_expanded? " \u2212" /*minus-sign*/ : "+", m_expanded /*draw bottom border?*/);
                    view.setBackgroundResource(m_expanded? R.drawable.greydivider : R.drawable.greydivider_bottom); // has shadow top+bottom
                }
                else if( i==rowInfoBelowSendPw) {
                    ((TextInfoCell) view).setText(AndroidUtilities.replaceTags(ApplicationLoader.applicationContext.getString(R.string.MyAccountExplain2)));
                    if( m_expanded ) {
                        view.setBackgroundResource(R.drawable.greydivider_bottom);
                    }
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
                return ROWTYPE_TEXT_ENTRY;
            }
            else if( i==rowAddrHeadline || i==rowMailPwHeadline || i== rowMailHeadline || i== rowSendHeadline ) {
                return ROWTYPE_HEADLINE;
            }
            else if( i== rowBreak1 ) {
                return ROWTYPE_SHADOW_BREAK;
            }
            else if( i==rowMailSecurity || i==rowSendSecurity) {
                return ROWTYPE_TEXT_FLAGS;
            }
            return ROWTYPE_INFO;
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
