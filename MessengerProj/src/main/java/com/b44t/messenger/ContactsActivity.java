/*******************************************************************************
 *
 *                              Delta Chat Android
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


// this file also contains the GroupCreateActivity functionality

package com.b44t.messenger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
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

import com.b44t.messenger.Cells.TextSettingsCell;
import com.b44t.messenger.Components.BaseFragmentAdapter;
import com.b44t.messenger.Cells.UserCell;
import com.b44t.messenger.ActionBar.ActionBar;
import com.b44t.messenger.ActionBar.ActionBarMenu;
import com.b44t.messenger.ActionBar.ActionBarMenuItem;
import com.b44t.messenger.ActionBar.BaseFragment;
import com.b44t.messenger.Components.ChipSpan;
import com.b44t.messenger.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.HashMap;


public class ContactsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private ContactsAdapter listViewAdapter;
    private TextView emptyTextView;
    private ListView listView;

    private EditText userSelectEditText;
    private int beforeChangeIndex;
    private boolean ignoreChange;
    private CharSequence changeString;
    private boolean searchWas;
    private boolean searching;
    private HashMap<Integer, ChipSpan> selectedContacts = new HashMap<>();
    private ArrayList<ChipSpan> allSpans = new ArrayList<>();

    private int rowNewGroup = -1;
    private int rowNewVerifiedGroup = -1;
    private int rowAddContact = -1;
    private int rowInviteViaQr = -1;
    private int rowContactFirst = -1;
    private int rowContactLast = -1;
    private int rowCount = 0;

    private int              do_what = 0;
    public final static int  SELECT_CONTACT_FOR_NEW_CHAT   = 1;
    public final static int  SELECT_CONTACTS_FOR_NEW_GROUP = 2;
    public final static int  SELECT_CONTACTS_FOR_NEW_VERIFIED_GROUP = 3;
    public final static int  ADD_CONTACTS_TO_GROUP         = 4;
    public final static int  ADD_CONTACTS_TO_VERIFIED_GROUP = 5;
    public final static int  SELECT_CONTACT_TO_BLOCK       = 6;
    public final static int  SELECT_CONTACT_TO_ATTACH      = 7;
    private final static int id_add_contact                = 20; // do_what is also used as internal IDs
    private final static int id_done_button                = 30;

    private ContactsActivityDelegate delegate;

    private String title;
    private String subtitle;


    public interface ContactsActivityDelegate {
        void didSelectContact(int user_id);
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
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.chatDidCreated);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.closeChats);

        if( do_what == SELECT_CONTACT_FOR_NEW_CHAT  )
        {
            title      = ApplicationLoader.applicationContext.getString(R.string.NewChat);
            subtitle   = ApplicationLoader.applicationContext.getString(R.string.SendMessageTo);
        }
        else if( do_what == SELECT_CONTACTS_FOR_NEW_GROUP )
        {
            title      = ApplicationLoader.applicationContext.getString(R.string.NewGroup);
            subtitle   = ApplicationLoader.applicationContext.getResources().getQuantityString(R.plurals.MeAndMembers, 0, 0);
        }
        else if( do_what == SELECT_CONTACTS_FOR_NEW_VERIFIED_GROUP )
        {
            title      = ApplicationLoader.applicationContext.getString(R.string.NewVerifiedGroup);
            subtitle   = ApplicationLoader.applicationContext.getResources().getQuantityString(R.plurals.MeAndMembers, 0, 0);
        }
        else if( do_what == ADD_CONTACTS_TO_GROUP || do_what == ADD_CONTACTS_TO_VERIFIED_GROUP )
        {
            title = ApplicationLoader.applicationContext.getString(R.string.AddMember);
        }
        else if( do_what == SELECT_CONTACT_TO_BLOCK )
        {
            title = ApplicationLoader.applicationContext.getString(R.string.BlockContact);
        }
        else if( do_what == SELECT_CONTACT_TO_ATTACH )
        {
            title = ApplicationLoader.applicationContext.getString(R.string.SelectContact);
        }

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.contactsDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.chatDidCreated);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.closeChats);
        delegate = null;
    }

    private volatile boolean m_in_sync = false;

    @Override
    public View createView(final Context context) {

        // do sync?
        boolean do_sync_now = true;
        if( m_in_sync ) {
            do_sync_now = false;
        }
        else {
            m_in_sync = true;
        }

        // initialize action
        actionBar.setBackButtonImage((do_what == SELECT_CONTACTS_FOR_NEW_GROUP||do_what == SELECT_CONTACTS_FOR_NEW_VERIFIED_GROUP)? R.drawable.ic_close_white : R.drawable.ic_ab_back);
        if( title != null ) {
            actionBar.setTitle(title);
            if( subtitle != null ) {
                actionBar.setSubtitle(do_sync_now? ApplicationLoader.applicationContext.getString(R.string.OneMoment) : subtitle);
            }
        }

        // sync phone book (the globalQueue is in the main thread)
        if( do_sync_now ) {
            Utilities.searchQueue.postRunnable(new Runnable() {
                @Override
                public void run() {

                    String pbcontacts = ContactsController.readContactsFromPhoneBook();
                    if (!pbcontacts.isEmpty()) {
                        if (MrMailbox.addAddressBook(pbcontacts) > 0) {
                            AndroidUtilities.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.contactsDidLoaded, 0);
                                }
                            });
                        }
                    }

                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            if (subtitle != null) {
                                actionBar.setSubtitle(subtitle);
                            }
                        }
                    });

                    m_in_sync = false;
                }
            });
        }

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1)
                {
                    finishFragment();
                }
                else if( id == SELECT_CONTACT_FOR_NEW_CHAT || id == SELECT_CONTACTS_FOR_NEW_GROUP || id == SELECT_CONTACTS_FOR_NEW_VERIFIED_GROUP )
                {
                    Bundle args = new Bundle();
                    args.putInt("do_what", id);
                    presentFragment(new ContactsActivity(args), true , true );
                }
                else if( id == id_add_contact )
                {
                    Bundle args = new Bundle();
                    args.putInt("do_what", ContactAddActivity.CREATE_CONTACT);
                    args.putBoolean("create_chat_when_done", do_what==SELECT_CONTACT_FOR_NEW_CHAT);
                    presentFragment(new ContactAddActivity(args));
                }
                else if( id == id_done_button )
                {
                    /*it's okay if selectedContacts is empty - a group only with SELF with created then*/
                    ArrayList<Integer> result = new ArrayList<>();
                    result.addAll(selectedContacts.keySet());
                    Bundle args = new Bundle();
                    args.putIntegerArrayList("result", result);
                    args.putInt("do_what", do_what);
                    presentFragment(new GroupCreateFinalActivity(args));
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();

        if( do_what == SELECT_CONTACTS_FOR_NEW_GROUP || do_what == SELECT_CONTACTS_FOR_NEW_VERIFIED_GROUP ) {
            menu.addItem(id_done_button, R.drawable.ic_done); // should the "done" button be right or left of other buttons?  Esp. for the "more" button, it looks better if it is right left of it - beside the title describing the action and nearer to "cancel"; the other buttons has not so much to do with the group but switches to other types.  In other situations, the icon-oder-decision may be different.
        }

        /* -- instead of a menu we're testing buttons in the list

        ActionBarMenuItem item = menu.addItem(10, R.drawable.ic_ab_other);
        if (do_what == SELECT_CONTACT_FOR_NEW_CHAT || do_what == SELECT_CONTACTS_FOR_NEW_GROUP || do_what == SELECT_CONTACTS_FOR_NEW_VERIFIED_GROUP) {
            if( do_what!=SELECT_CONTACT_FOR_NEW_CHAT ) { item.addSubItem(SELECT_CONTACT_FOR_NEW_CHAT, context.getString(R.string.NewChat)); }
            if( do_what!=SELECT_CONTACTS_FOR_NEW_GROUP ) { item.addSubItem(SELECT_CONTACTS_FOR_NEW_GROUP, context.getString(R.string.NewGroup)); }

            if( MrMailbox.getConfigInt("qr_enabled", 0) != 0 ) {
                if (do_what != SELECT_CONTACTS_FOR_NEW_VERIFIED_GROUP) {
                    item.addSubItem(SELECT_CONTACTS_FOR_NEW_VERIFIED_GROUP, context.getString(R.string.NewVerifiedGroup));
                }
            }
        }
        item.addSubItem(id_add_contact, context.getString(R.string.NewContactTitle));
        */

        int listflags = 0;
        if( do_what==SELECT_CONTACTS_FOR_NEW_VERIFIED_GROUP||do_what==ADD_CONTACTS_TO_VERIFIED_GROUP) {
            listflags |= MrMailbox.MR_GCL_VERIFIED_ONLY;
        }

        if( do_what==SELECT_CONTACT_FOR_NEW_CHAT || do_what==SELECT_CONTACT_TO_ATTACH ) {
            listflags |= MrMailbox.MR_GCL_ADD_SELF;
        }

        listViewAdapter = new ContactsAdapter(context, listflags, do_what==SELECT_CONTACT_FOR_NEW_CHAT);

        if( do_what == SELECT_CONTACTS_FOR_NEW_GROUP || do_what == SELECT_CONTACTS_FOR_NEW_VERIFIED_GROUP ) {
            listViewAdapter.setCheckedMap(selectedContacts);
        }

        fragmentView = new LinearLayout(context);

        LinearLayout linearLayout = (LinearLayout) fragmentView;
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        FrameLayout frameLayout = new FrameLayout(context);
        linearLayout.addView(frameLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // START SEARCH FIELD
        userSelectEditText = new EditText(context);
        userSelectEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        userSelectEditText.setHintTextColor(0xffBBBBBB);
        userSelectEditText.setTextColor(0xff212121);
        userSelectEditText.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        userSelectEditText.setMinimumHeight(AndroidUtilities.dp(54));
        userSelectEditText.setSingleLine(false);
        userSelectEditText.setLines(2);
        userSelectEditText.setMaxLines(2);
        userSelectEditText.setVerticalScrollBarEnabled(true);
        userSelectEditText.setHorizontalScrollBarEnabled(false);
        userSelectEditText.setPadding(0, 0, 0, 0);
        userSelectEditText.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        userSelectEditText.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        frameLayout.addView(userSelectEditText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.START, 18, 0, 18, 0));

        userSelectEditText.setHint(ApplicationLoader.applicationContext.getString(R.string.Search));
        userSelectEditText.setTextIsSelectable(false);
        userSelectEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                if (!ignoreChange) {
                    beforeChangeIndex = userSelectEditText.getSelectionStart();
                    changeString = new SpannableString(charSequence);
                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!ignoreChange) {
                    boolean search = false;
                    int afterChangeIndex = userSelectEditText.getSelectionEnd();
                    if (editable.toString().length() < changeString.toString().length()) {
                        String deletedString = "";
                        try {
                            deletedString = changeString.toString().substring(afterChangeIndex, beforeChangeIndex);
                        } catch (Exception e) {

                        }
                        if (deletedString.length() > 0) {
                            if (searching && searchWas) {
                                search = true;
                            }
                            Spannable span = userSelectEditText.getText();
                            for (int a = 0; a < allSpans.size(); a++) {
                                ChipSpan sp = allSpans.get(a);
                                if (span.getSpanStart(sp) == -1) {
                                    allSpans.remove(sp);
                                    selectedContacts.remove(sp.uid);
                                }
                            }
                            listView.invalidateViews();
                        } else {
                            search = true;
                        }
                    } else {
                        search = true;
                    }
                    if (search) {
                        String text = userSelectEditText.getText().toString().replace("<", "");
                        if (text.length() != 0) {
                            searching = true;
                            searchWas = true;
                            if (emptyTextView != null) {
                                emptyTextView.setText(context.getString(R.string.NoResult));
                            }
                            listViewAdapter.search(text);
                            listViewAdapter.notifyDataSetChanged();
                        } else {
                            listViewAdapter.search(null);
                            searching = false;
                            searchWas = false;
                            listViewAdapter.notifyDataSetChanged();
                            emptyTextView.setText(context.getString(R.string.NoContacts));
                        }
                    }
                }
            }
        });
        // END SEARCH FIELD


        LinearLayout emptyTextLayout = new LinearLayout(context);
        emptyTextLayout.setVisibility(View.INVISIBLE);
        emptyTextLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(emptyTextLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        emptyTextLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        emptyTextView = new TextView(context);
        emptyTextView.setTextColor(0xff808080);
        emptyTextView.setTextSize(20);
        emptyTextView.setGravity(Gravity.CENTER);
        emptyTextView.setText(context.getString(R.string.NoContacts));
        emptyTextLayout.addView(emptyTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, 0.5f));

        FrameLayout frameLayout2 = new FrameLayout(context);
        emptyTextLayout.addView(frameLayout2, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, 0.5f));


        listView = new ListView(context);
        listView.setEmptyView(emptyTextLayout);
        listView.setVerticalScrollBarEnabled(false);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setFastScrollEnabled(true);
        listView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        listView.setAdapter(listViewAdapter);
        listView.setFastScrollAlwaysVisible(false);
        linearLayout.addView(listView);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // a single click on an contact item
                if( i == rowNewGroup || i == rowNewVerifiedGroup ) {
                    Bundle args = new Bundle();
                    args.putInt("do_what", i==rowNewGroup? SELECT_CONTACTS_FOR_NEW_GROUP : SELECT_CONTACTS_FOR_NEW_VERIFIED_GROUP);
                    presentFragment(new ContactsActivity(args), false /*removeLast*/, false /*forceWithoutAnimation*/);
                }
                else if( i == rowAddContact) {
                    Bundle args = new Bundle();
                    args.putInt("do_what", ContactAddActivity.CREATE_CONTACT);
                    args.putBoolean("create_chat_when_done", do_what==SELECT_CONTACT_FOR_NEW_CHAT);

                    String lastSearch = listViewAdapter.getSearch();
                    if( lastSearch != null ) {
                        args.putString("prefill", lastSearch);
                    }

                    presentFragment(new ContactAddActivity(args));
                }
                else if( i == rowInviteViaQr ) {
                    Intent intent2 = new Intent(getParentActivity(), QRshowActivity.class);
                    Bundle b = new Bundle();
                    b.putInt("chat_id", 0);
                    intent2.putExtras(b);
                    getParentActivity().startActivity(intent2);
                }
                else {
                    Object item = listViewAdapter.getItem(i);
                    if (item instanceof TLRPC.User) {
                        final TLRPC.User user = (TLRPC.User) item;
                        if (do_what == SELECT_CONTACT_FOR_NEW_CHAT) {
                            int belonging_chat_id = MrMailbox.getChatIdByContactId(user.id);
                            if (belonging_chat_id != 0) {
                                Bundle args = new Bundle();
                                args.putInt("chat_id", belonging_chat_id);
                                presentFragment(new ChatActivity(args), true);
                                return;
                            }

                            String name = "";
                            {
                                MrContact mrContact = MrMailbox.getContact(user.id);
                                name = mrContact.getNameNAddr();
                            }

                            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                            builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    int belonging_chat_id = MrMailbox.createChatByContactId(user.id);
                                    if (belonging_chat_id != 0) {
                                        Bundle args = new Bundle();
                                        args.putInt("chat_id", belonging_chat_id);
                                        presentFragment(new ChatActivity(args), true);
                                    }
                                }
                            });
                            builder.setNegativeButton(R.string.Cancel, null);
                            builder.setMessage(AndroidUtilities.replaceTags(String.format(context.getString(R.string.AskStartChatWith), name)));
                            showDialog(builder.create());
                        } else if (do_what == SELECT_CONTACTS_FOR_NEW_GROUP || do_what == SELECT_CONTACTS_FOR_NEW_VERIFIED_GROUP) {
                            boolean check = true;
                            if (selectedContacts.containsKey(user.id)) {
                                check = false;
                                try {
                                    ChipSpan span = selectedContacts.get(user.id);
                                    selectedContacts.remove(user.id);
                                    SpannableStringBuilder text = new SpannableStringBuilder(userSelectEditText.getText());
                                    text.delete(text.getSpanStart(span), text.getSpanEnd(span));
                                    allSpans.remove(span);
                                    ignoreChange = true;
                                    userSelectEditText.setText(text);
                                    userSelectEditText.setSelection(text.length());
                                    ignoreChange = false;
                                } catch (Exception e) {

                                }
                            } else {
                                ignoreChange = true;
                                ChipSpan span = createAndPutChipForUser(user.id);
                                span.uid = user.id;
                                ignoreChange = false;
                            }

                            updateSubtitle();

                            if (searching || searchWas) {
                                ignoreChange = true;
                                SpannableStringBuilder ssb = new SpannableStringBuilder("");
                                for (ImageSpan sp : allSpans) {
                                    ssb.append("<<");
                                    ssb.setSpan(sp, ssb.length() - 2, ssb.length(), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }
                                userSelectEditText.setText(ssb);
                                userSelectEditText.setSelection(ssb.length());
                                ignoreChange = false;

                                listViewAdapter.search(null);
                                searching = false;
                                searchWas = false;
                                listViewAdapter.notifyDataSetChanged();
                                emptyTextView.setText(context.getString(R.string.NoContacts));
                            } else {
                                if (view instanceof UserCell) {
                                    ((UserCell) view).setChecked(check, true);
                                }
                            }
                        } else if (delegate != null) {
                            delegate.didSelectContact(user.id);
                            delegate = null;
                            finishFragment();
                        }
                    }
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                Object item = listViewAdapter.getItem(i);
                if (item instanceof TLRPC.User) {
                    final TLRPC.User user = (TLRPC.User) item;
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    CharSequence[] items = new CharSequence[]{context.getString(R.string.ViewProfile),context.getString(R.string.DeleteContact)};
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if( i == 0 ) {
                                Bundle args = new Bundle();
                                args.putInt("user_id", user.id);
                                ProfileActivity fragment = new ProfileActivity(args);
                                presentFragment(fragment);
                            }
                            else if( i == 1 ) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                                builder.setMessage(context.getString(R.string.AreYouSureDeleteContact));
                                builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        if( MrMailbox.deleteContact(user.id)==0 ) {
                                            AndroidUtilities.showHint(getParentActivity(), context.getString(R.string.CannotDeleteContact));
                                        }
                                        else {
                                            AndroidUtilities.showDoneHint(getParentActivity());
                                            listViewAdapter.searchAgain();
                                            listViewAdapter.notifyDataSetChanged();                                        }
                                    }
                                });
                                builder.setNegativeButton(R.string.Cancel, null);
                                showDialog(builder.create());
                            }
                        }
                    });
                    showDialog(builder.create());
                }
                return true;
            }
        });

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                if (i == SCROLL_STATE_TOUCH_SCROLL) {
                    AndroidUtilities.hideKeyboard(userSelectEditText);
                }
                if (listViewAdapter != null) {
                    listViewAdapter.setIsScrolling(i != SCROLL_STATE_IDLE);
                }
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

    public void updateSubtitle()
    {
        int selectedContactsButMe = selectedContacts.size();
        if (selectedContacts.containsKey(MrContact.MR_CONTACT_ID_SELF)) {
            selectedContactsButMe--;
        }
        actionBar.setSubtitle(ApplicationLoader.applicationContext.getResources().getQuantityString(R.plurals.MeAndMembers, selectedContactsButMe, selectedContactsButMe));

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
        if (id == NotificationCenter.contactsDidLoaded ) {

            if (listViewAdapter != null) {
                listViewAdapter.searchAgain();
                listViewAdapter.notifyDataSetChanged();
                int sel_contact_id = (int) args[0];
                if( sel_contact_id != 0 )  {
                    createAndPutChipForUser(sel_contact_id);
                    updateSubtitle();
                }
            }
        } else if (id == NotificationCenter.updateInterfaces) {
            int mask = (Integer)args[0];
            if ((mask & MrMailbox.UPDATE_MASK_AVATAR) != 0 || (mask & MrMailbox.UPDATE_MASK_NAME) != 0 || (mask & MrMailbox.UPDATE_MASK_STATUS) != 0) {
                updateVisibleRows();
            }
        } else if (id == NotificationCenter.closeChats) {
            removeSelfFromStack();
        } else if (id == NotificationCenter.chatDidCreated) {
            removeSelfFromStack();
        }
    }

    private void updateVisibleRows() {
        if (listView != null) {
            int count = listView.getChildCount();
            for (int a = 0; a < count; a++) {
                View child = listView.getChildAt(a);
                if (child instanceof UserCell) {
                    ((UserCell) child).update();
                }
            }
        }
    }

    public void setDelegate(ContactsActivityDelegate delegate) {
        this.delegate = delegate;
    }

    private ChipSpan createAndPutChipForUser(int contact_id) {
        LayoutInflater lf = (LayoutInflater) ApplicationLoader.applicationContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View textView = lf.inflate(R.layout.group_create_bubble, null);
        TextView text = (TextView)textView.findViewById(R.id.bubble_text_view);

        String name ="";
        {
            MrContact mrContact = MrMailbox.getContact(contact_id);
            name = mrContact.getDisplayName();
        }


        text.setText(name + ", ");

        int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        textView.measure(spec, spec);
        textView.layout(0, 0, textView.getMeasuredWidth(), textView.getMeasuredHeight());
        Bitmap b = Bitmap.createBitmap(textView.getWidth(), textView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        canvas.translate(-textView.getScrollX(), -textView.getScrollY());
        textView.draw(canvas);
        textView.setDrawingCacheEnabled(true);
        Bitmap cacheBmp = textView.getDrawingCache();
        Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
        textView.destroyDrawingCache();

        final BitmapDrawable bmpDrawable = new BitmapDrawable(b);
        bmpDrawable.setBounds(0, 0, b.getWidth(), b.getHeight());

        SpannableStringBuilder ssb = new SpannableStringBuilder("");
        ChipSpan span = new ChipSpan(bmpDrawable, ImageSpan.ALIGN_BASELINE);
        allSpans.add(span);
        selectedContacts.put(contact_id, span);
        for (ImageSpan sp : allSpans) {
            ssb.append("<<");
            ssb.setSpan(sp, ssb.length() - 2, ssb.length(), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        userSelectEditText.setText(ssb);
        userSelectEditText.setSelection(ssb.length());
        return span;
    }

    private class ContactsAdapter extends BaseFragmentAdapter {

        private Context mContext;
        private HashMap<Integer, ?> checkedMap;
        private boolean scrolling;
        private String lastQuery;

        private int[] contactIds;

        private int mListflags = 0;
        private boolean mAddNewGroupRows;

        private final int ROWTYPE_TEXT_SETTINGS = 0;
        private final int ROWTYPE_CONTACT = 1;
        private final int ROWTYPE_COUNT = 2;

        public ContactsAdapter(Context context, int listflags, boolean addNewGroupRows) {
            mContext = context;
            mListflags = listflags;
            mAddNewGroupRows = addNewGroupRows;
            contactIds = MrMailbox.getContacts(mListflags, null);
            updateRowIds();
        }

        public void updateRowIds()
        {
            rowCount = 0;
            rowNewGroup = -1;
            rowNewVerifiedGroup = -1;
            rowAddContact = -1;
            rowInviteViaQr = -1;
            rowContactFirst = -1;
            rowContactLast = -1;

            if( lastQuery == null || lastQuery.isEmpty() ) {
                if (mAddNewGroupRows) {
                    rowNewGroup = rowCount++;
                    if( MrMailbox.getConfigInt("qr_enabled", 0) != 0 ) {
                        rowNewVerifiedGroup = rowCount++;
                    }
                }
            }

            if( (mListflags & MrMailbox.MR_GCL_VERIFIED_ONLY)!=0 ) {
                rowInviteViaQr = rowCount++;
            }
            else {
                rowAddContact = rowCount++;
            }

            if( contactIds.length > 0 ) {
                rowContactFirst = rowCount;
                rowCount += contactIds.length;
                rowContactLast = rowCount - 1;
            }
        }

        public void setCheckedMap(HashMap<Integer, ?> map) {
            checkedMap = map;
        }

        public void setIsScrolling(boolean value) {
            scrolling = value;
        }

        public void search(String query) {
            contactIds = MrMailbox.getContacts(mListflags, query);
            lastQuery = query;
            updateRowIds();
        }

        public void searchAgain() {
            contactIds = MrMailbox.getContacts(mListflags, lastQuery);
            updateRowIds();
        }

        public String getSearch(){
            return lastQuery;
        }

        @Override
        public Object getItem(int i) {
            int curr_user_index = i - rowContactFirst;
            if(curr_user_index>=0 && curr_user_index<contactIds.length) {
                TLRPC.User u = new TLRPC.User();
                u.id = contactIds[curr_user_index];
                return u;
            }
            return null;
        }

        @Override
        public boolean isEnabled(int i) {
            int type = getItemViewType(i);
            return (type == ROWTYPE_CONTACT || type == ROWTYPE_TEXT_SETTINGS);
        }

        @Override
        public int getCount() {
            return rowCount;
        }

        @Override
        public View getView(int i, View view, ViewGroup parent) {
            int type = getItemViewType(i);
            if( type == ROWTYPE_TEXT_SETTINGS ) {
                if (view == null) {
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                TextSettingsCell textCell = (TextSettingsCell) view;
                if( i == rowNewGroup ) {
                    textCell.setText(mContext.getString(R.string.NewGroup), true);
                }
                else if( i == rowNewVerifiedGroup ) {
                    textCell.setText(mContext.getString(R.string.NewVerifiedGroup), true);
                }
                else if( i == rowAddContact) {
                    textCell.setText(mContext.getString(R.string.NewContactTitle), false);
                }
                else if( i == rowInviteViaQr) {
                    textCell.setText(mContext.getString(R.string.QrShowInviteCode), false);
                }
            }
            else if( type == ROWTYPE_CONTACT ) {
                if (view == null) {
                    view = new UserCell(mContext, 1);
                }

                int curr_user_index = i - rowContactFirst;
                if (curr_user_index >= 0 && curr_user_index < contactIds.length) {
                    int curr_user_id = contactIds[curr_user_index];
                    MrContact mrContact = MrMailbox.getContact(curr_user_id);
                    ((UserCell) view).setData(mrContact);
                    if (checkedMap != null) {
                        boolean checked = curr_user_id == MrContact.MR_CONTACT_ID_SELF || checkedMap.containsKey(curr_user_id);
                        ((UserCell) view).setChecked(checked, !scrolling);
                    }
                }
            }

            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if(i>=rowContactFirst && i<=rowContactLast ) {
                return ROWTYPE_CONTACT;
            }
            return ROWTYPE_TEXT_SETTINGS;
        }

        @Override
        public int getViewTypeCount() {
            return ROWTYPE_COUNT;
        }
    }
}
