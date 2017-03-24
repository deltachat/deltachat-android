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


// this file also contains the GroupCreateActivity functionality

package com.b44t.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.ContactsController;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.MrContact;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.Utilities;
import com.b44t.messenger.TLRPC;
import com.b44t.messenger.FileLog;
import com.b44t.messenger.NotificationCenter;
import com.b44t.messenger.R;
import com.b44t.ui.Adapters.ContactsAdapter;
import com.b44t.ui.Cells.UserCell;
import com.b44t.ui.ActionBar.ActionBar;
import com.b44t.ui.ActionBar.ActionBarMenu;
import com.b44t.ui.ActionBar.ActionBarMenuItem;
import com.b44t.ui.ActionBar.BaseFragment;
import com.b44t.ui.Components.ChipSpan;
import com.b44t.ui.Components.LayoutHelper;

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

    private int             do_what = 0;
    public final static int SELECT_CONTACT_FOR_NEW_CHAT   = 1;
    public final static int SELECT_CONTACTS_FOR_NEW_GROUP = 2;
    public final static int ADD_CONTACTS_TO_GROUP         = 3;
    public final static int SELECT_CONTACT_TO_BLOCK       = 4;
    public final static int SELECT_CONTACT_TO_ATTACH      = 5;

    private ContactsActivityDelegate delegate;

    private String title;
    private String subtitle;

    private final static int id_add_contact = 2;
    private final static int id_done_button = 3;
    private final static int id_toggle      = 4;

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
            title      = LocaleController.getString("NewChat", R.string.NewChat);
            subtitle   = LocaleController.getString("SendMessageTo", R.string.SendMessageTo);
        }
        else if( do_what == SELECT_CONTACTS_FOR_NEW_GROUP )
        {
            title      = LocaleController.getString("NewGroup", R.string.NewGroup);
            subtitle   = ApplicationLoader.applicationContext.getResources().getQuantityString(R.plurals.MeAndMembers, 0, 0);
        }
        else if( do_what == ADD_CONTACTS_TO_GROUP )
        {
            title = LocaleController.getString("AddMember", R.string.AddMember);
        }
        else if( do_what == SELECT_CONTACT_TO_BLOCK )
        {
            title = LocaleController.getString("BlockContact", R.string.BlockContact);
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
        actionBar.setBackButtonImage(do_what == SELECT_CONTACTS_FOR_NEW_GROUP? R.drawable.ic_close_white : R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
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
                                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.contactsDidLoaded);
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
                else if( id == id_toggle )
                {
                    Bundle args = new Bundle();
                    args.putInt("do_what", do_what==SELECT_CONTACTS_FOR_NEW_GROUP? SELECT_CONTACT_FOR_NEW_CHAT : SELECT_CONTACTS_FOR_NEW_GROUP);
                    presentFragment(new ContactsActivity(args), true /*removeLast*/, true /*forceWithoutAnimation*/);
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
                    presentFragment(new GroupCreateFinalActivity(args));
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();

        if( do_what == SELECT_CONTACTS_FOR_NEW_GROUP ) {
            menu.addItem(id_done_button, R.drawable.ic_done); // should the "done" button be right or left of other buttons?  Esp. for the "more" button, it looks better if it is right left of it - beside the title describing the action and nearer to "cancel"; the other buttons has not so much to do with the group but switches to other types.  In other situations, the icon-oder-decision may be different.
        }

        ActionBarMenuItem item = menu.addItem(10, R.drawable.ic_ab_other);
        if (do_what == SELECT_CONTACT_FOR_NEW_CHAT || do_what == SELECT_CONTACTS_FOR_NEW_GROUP) {
            item.addSubItem(id_toggle, do_what == SELECT_CONTACT_FOR_NEW_CHAT ? LocaleController.getString("NewGroup", R.string.NewGroup) : LocaleController.getString("NewChat", R.string.NewChat), 0);
        }
        item.addSubItem(id_add_contact, LocaleController.getString("NewContactTitle", R.string.NewContactTitle), 0);


        listViewAdapter = new ContactsAdapter(context);
        listViewAdapter.setCheckedMap(selectedContacts);


        fragmentView = new LinearLayout(context);

        LinearLayout linearLayout = (LinearLayout) fragmentView;
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        FrameLayout frameLayout = new FrameLayout(context);
        linearLayout.addView(frameLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // START SEARCH FIELD
        userSelectEditText = new EditText(context);
        userSelectEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        userSelectEditText.setHintTextColor(0xff979797);
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
        userSelectEditText.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        AndroidUtilities.clearCursorDrawable(userSelectEditText);
        frameLayout.addView(userSelectEditText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 10, 0, 10, 0));

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
                            FileLog.e("messenger", e);
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
                            if (listView != null) {
                                listView.setFastScrollAlwaysVisible(false);
                                listView.setFastScrollEnabled(false);
                                listView.setVerticalScrollBarEnabled(true);
                            }
                            if (emptyTextView != null) {
                                emptyTextView.setText(LocaleController.getString("NoResult", R.string.NoResult));
                            }
                            listViewAdapter.search(text);
                            listViewAdapter.notifyDataSetChanged();
                        } else {
                            listViewAdapter.search(null);
                            searching = false;
                            searchWas = false;
                            listViewAdapter.notifyDataSetChanged();
                            listView.setFastScrollAlwaysVisible(true);
                            listView.setFastScrollEnabled(true);
                            listView.setVerticalScrollBarEnabled(false);
                            emptyTextView.setText(LocaleController.getString("NoContacts", R.string.NoContacts));
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
        emptyTextView.setText(LocaleController.getString("NoContacts", R.string.NoContacts));
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
        listView.setFastScrollAlwaysVisible(true);
        listView.setVerticalScrollbarPosition(LocaleController.isRTL ? ListView.SCROLLBAR_POSITION_LEFT : ListView.SCROLLBAR_POSITION_RIGHT);
        linearLayout.addView(listView);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // a single click on an contact item
                Object item = listViewAdapter.getItem(i);
                if (item instanceof TLRPC.User) {
                    final TLRPC.User user = (TLRPC.User) item;
                    if( do_what == SELECT_CONTACT_FOR_NEW_CHAT ) {
                        int belonging_chat_id = MrMailbox.getChatIdByContactId(user.id);
                        if( belonging_chat_id!=0 ) {
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
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                int belonging_chat_id = MrMailbox.createChatByContactId(user.id);
                                if( belonging_chat_id != 0 ) {
                                    Bundle args = new Bundle();
                                    args.putInt("chat_id", belonging_chat_id);
                                    presentFragment(new ChatActivity(args), true);
                                }
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                        builder.setMessage(AndroidUtilities.replaceTags(LocaleController.formatString("AskStartChatWith", R.string.AskStartChatWith, name)));
                        showDialog(builder.create());
                    }
                    else if( do_what == SELECT_CONTACTS_FOR_NEW_GROUP ) {
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
                                FileLog.e("messenger", e);
                            }
                        } else {
                            ignoreChange = true;
                            ChipSpan span = createAndPutChipForUser(user);
                            span.uid = user.id;
                            ignoreChange = false;
                        }
                        actionBar.setSubtitle(context.getResources().getQuantityString(R.plurals.MeAndMembers, selectedContacts.size(), selectedContacts.size()));
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
                            listView.setFastScrollAlwaysVisible(true);
                            listView.setFastScrollEnabled(true);
                            listView.setVerticalScrollBarEnabled(false);
                            emptyTextView.setText(LocaleController.getString("NoContacts", R.string.NoContacts));
                        } else {
                            if (view instanceof UserCell) {
                                ((UserCell) view).setChecked(check, true);
                            }
                        }
                    }
                    else if (delegate != null) {
                        delegate.didSelectContact(user.id);
                        delegate = null;
                        finishFragment();
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
                    CharSequence[] items = new CharSequence[]{LocaleController.getString("ViewProfile", R.string.ViewProfile)};
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Bundle args = new Bundle();
                            args.putInt("user_id", user.id);
                            ProfileActivity fragment = new ProfileActivity(args);
                            presentFragment(fragment);
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

    private ChipSpan createAndPutChipForUser(TLRPC.User user) {
        LayoutInflater lf = (LayoutInflater) ApplicationLoader.applicationContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View textView = lf.inflate(R.layout.group_create_bubble, null);
        TextView text = (TextView)textView.findViewById(R.id.bubble_text_view);

        String name ="";
        {
            MrContact mrContact = MrMailbox.getContact(user.id);
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
        selectedContacts.put(user.id, span);
        for (ImageSpan sp : allSpans) {
            ssb.append("<<");
            ssb.setSpan(sp, ssb.length() - 2, ssb.length(), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        userSelectEditText.setText(ssb);
        userSelectEditText.setSelection(ssb.length());
        return span;
    }
}
