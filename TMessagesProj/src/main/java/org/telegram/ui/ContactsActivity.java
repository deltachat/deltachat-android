/*
 * This part of the Delta Chat fronted is based on Telegram which is covered by the following note:
 *
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package org.telegram.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MrMailbox;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.Adapters.BaseFragmentAdapter;
import org.telegram.ui.Adapters.ContactsAdapter;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;


public class ContactsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private BaseFragmentAdapter listViewAdapter;
    private TextView emptyTextView;
    private ListView listView;
    private EditText userSelectEditText;

    private int             do_what = 0;
    public final static int SELECT_CONTACT_FOR_NEW_CHAT   = 1;
    public final static int SELECT_CONTACTS_FOR_NEW_GROUP = 2;
    public final static int ADD_CONTACTS_TO_GROUP         = 3;
    public final static int SELECT_CONTACT_TO_BLOCK       = 4;

    boolean createNewChatOnItemClick;

    //private SearchAdapter searchListViewAdapter;
    //private boolean searchWas;
    //private boolean searching;

    private ContactsActivityDelegate delegate;

    private String title;
    private String subtitle;

    private final static int id_add_contact = 2;
    private final static int id_done_button = 3;
    private final static int id_toggle      = 4;

    public interface ContactsActivityDelegate {
        void didSelectContact(TLRPC.User user, String param);
    }

    public ContactsActivity(Bundle args) {
        super(args);
        if( args != null ) {
            do_what = args.getInt("do_what", 0);
        }
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.contactsDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.closeChats);

        if( do_what == SELECT_CONTACT_FOR_NEW_CHAT  )
        {
            title                    = LocaleController.getString("NewChat", R.string.NewChat);
            subtitle                 = LocaleController.getString("SendMessageTo", R.string.SendMessageTo);
            createNewChatOnItemClick = true;
        }
        else if( do_what == SELECT_CONTACTS_FOR_NEW_GROUP )
        {
            title = LocaleController.getString("NewGroup", R.string.NewGroup);
        }
        else if( do_what == ADD_CONTACTS_TO_GROUP )
        {
            ;
        }
        else if( do_what == SELECT_CONTACT_TO_BLOCK )
        {
            title = LocaleController.getString("BlockContact", R.string.BlockContact);
        }

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.contactsDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.closeChats);
        delegate = null;
    }

    @Override
    public View createView(Context context) {

        //searching = false;
        //searchWas = false;

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        if( title != null ) {
            actionBar.setTitle(title);
            if( subtitle != null ) {
                actionBar.setSubtitle(subtitle);
            }
        }

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1)
                {
                    finishFragment();
                }
                else if( id == id_toggle )
                {
                    Bundle args = new Bundle();
                    args.putInt("do_what", do_what==SELECT_CONTACTS_FOR_NEW_GROUP? SELECT_CONTACT_FOR_NEW_CHAT : SELECT_CONTACTS_FOR_NEW_GROUP);
                    presentFragment(new ContactsActivity(args), true);
                }
                else if( id == id_add_contact )
                {
                    Toast.makeText(getParentActivity(), LocaleController.getString("NotYetImplemented", R.string.NotYetImplemented), Toast.LENGTH_LONG).show();
                }
                else if( id == id_done_button )
                {

                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();

        if( do_what == SELECT_CONTACTS_FOR_NEW_GROUP || do_what == ADD_CONTACTS_TO_GROUP ) {
            menu.addItem(id_done_button, R.drawable.ic_done);
        }

        if( do_what != SELECT_CONTACT_TO_BLOCK ) {
            ActionBarMenuItem item = menu.addItem(10, R.drawable.ic_ab_other);
            if (do_what == SELECT_CONTACT_FOR_NEW_CHAT || do_what == SELECT_CONTACTS_FOR_NEW_GROUP) {
                item.addSubItem(id_toggle, do_what == SELECT_CONTACT_FOR_NEW_CHAT ? LocaleController.getString("NewGroup", R.string.NewGroup) : LocaleController.getString("NewChat", R.string.NewChat), 0);
            }
            item.addSubItem(id_add_contact, LocaleController.getString("AddContactTitle", R.string.AddContactTitle), 0);
        }

        listViewAdapter = new ContactsAdapter(context);

        fragmentView = new FrameLayout(context);

        LinearLayout emptyTextLayout = new LinearLayout(context);
        emptyTextLayout.setVisibility(View.INVISIBLE);
        emptyTextLayout.setOrientation(LinearLayout.VERTICAL);
        ((FrameLayout) fragmentView).addView(emptyTextLayout);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) emptyTextLayout.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        layoutParams.gravity = Gravity.TOP;
        emptyTextLayout.setLayoutParams(layoutParams);
        emptyTextLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        emptyTextView = new TextView(context);
        emptyTextView.setTextColor(0xff808080);
        emptyTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        emptyTextView.setGravity(Gravity.CENTER);
        emptyTextView.setText(LocaleController.getString("NoContacts", R.string.NoContacts));
        emptyTextLayout.addView(emptyTextView);
        LinearLayout.LayoutParams layoutParams1 = (LinearLayout.LayoutParams) emptyTextView.getLayoutParams();
        layoutParams1.width = LayoutHelper.MATCH_PARENT;
        layoutParams1.height = LayoutHelper.MATCH_PARENT;
        layoutParams1.weight = 0.5f;
        emptyTextView.setLayoutParams(layoutParams1);

        FrameLayout frameLayout = new FrameLayout(context);
        emptyTextLayout.addView(frameLayout);
        layoutParams1 = (LinearLayout.LayoutParams) frameLayout.getLayoutParams();
        layoutParams1.width = LayoutHelper.MATCH_PARENT;
        layoutParams1.height = LayoutHelper.MATCH_PARENT;
        layoutParams1.weight = 0.5f;
        frameLayout.setLayoutParams(layoutParams1);

        listView = new ListView(context);
        listView.setEmptyView(emptyTextLayout);
        listView.setVerticalScrollBarEnabled(false);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setFastScrollEnabled(true);
        listView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        listView.setAdapter(listViewAdapter);
        listView.setFastScrollAlwaysVisible(true);
        listView.setVerticalScrollbarPosition(LocaleController.isRTL ? ListView.SCROLLBAR_POSITION_LEFT : ListView.SCROLLBAR_POSITION_RIGHT);
        ((FrameLayout) fragmentView).addView(listView);
        layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        listView.setLayoutParams(layoutParams);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // a single click on an contact item
                Object item = listViewAdapter.getItem(i);
                if (item instanceof TLRPC.User) {
                    final TLRPC.User user = (TLRPC.User) item;
                    if( do_what == SELECT_CONTACT_FOR_NEW_CHAT ) {
                        int belonging_chat_id = MrMailbox.MrMailboxGetChatIdByContactId(MrMailbox.hMailbox, user.id);
                        if( belonging_chat_id!=0 ) {
                            Bundle args = new Bundle();
                            args.putInt("chat_id", belonging_chat_id);
                            presentFragment(new ChatActivity(args), true);
                            return;
                        }

                        long hContact = MrMailbox.MrMailboxGetContact(MrMailbox.hMailbox, user.id);
                            String name = MrMailbox.MrContactGetNameNAddr(hContact);
                        MrMailbox.MrContactUnref(hContact);

                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                int belonging_chat_id = MrMailbox.MrMailboxCreateChatByContactId(MrMailbox.hMailbox, user.id);
                                if( belonging_chat_id != 0 ) {
                                    Bundle args = new Bundle();
                                    args.putInt("chat_id", belonging_chat_id);
                                    presentFragment(new ChatActivity(args), true);
                                    return;
                                }
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                        builder.setMessage(AndroidUtilities.replaceTags(LocaleController.formatString("AskStartChatWith", R.string.AskStartChatWith, name)));
                        showDialog(builder.create());
                    }
                    else if (delegate != null) {
                        delegate.didSelectContact(user, null);
                        delegate = null;
                        finishFragment();
                    }
                }
            }
        });

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                /*if (i == SCROLL_STATE_TOUCH_SCROLL && searching && searchWas) {
                    AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                }*/
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (absListView.isFastScrollEnabled()) {
                    AndroidUtilities.clearDrawableAnimation(absListView);
                }
            }
        });

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listViewAdapter != null) {
            listViewAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (actionBar != null) {
            actionBar.closeSearchField();
        }
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.contactsDidLoaded) {
            if (listViewAdapter != null) {
                listViewAdapter.notifyDataSetChanged();
            }
        } else if (id == NotificationCenter.updateInterfaces) {
            int mask = (Integer)args[0];
            if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_NAME) != 0 || (mask & MessagesController.UPDATE_MASK_STATUS) != 0) {
                updateVisibleRows(mask);
            }
        } else if (id == NotificationCenter.closeChats) {
            removeSelfFromStack();
        }
    }

    private void updateVisibleRows(int mask) {
        if (listView != null) {
            int count = listView.getChildCount();
            for (int a = 0; a < count; a++) {
                View child = listView.getChildAt(a);
                if (child instanceof UserCell) {
                    ((UserCell) child).update(mask);
                }
            }
        }
    }

    public void setDelegate(ContactsActivityDelegate delegate) {
        this.delegate = delegate;
    }

}
