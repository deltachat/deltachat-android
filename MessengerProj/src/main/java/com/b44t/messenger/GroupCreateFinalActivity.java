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


package com.b44t.messenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.b44t.messenger.Cells.HeaderCell;
import com.b44t.messenger.Components.BaseFragmentAdapter;
import com.b44t.messenger.Cells.GreySectionCell;
import com.b44t.messenger.Cells.UserCell;
import com.b44t.messenger.ActionBar.ActionBar;
import com.b44t.messenger.ActionBar.ActionBarMenu;
import com.b44t.messenger.ActionBar.BaseFragment;
import com.b44t.messenger.Components.LayoutHelper;

import java.util.ArrayList;

public class GroupCreateFinalActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private ListAdapter listAdapter;
    private ListView listView;
    private EditText nameTextView;
    private ArrayList<Integer> selectedContacts;
    private String nameToSet = null;

    private final static int done_button = 1;

    int do_what; // values from ContactsActivity

    public GroupCreateFinalActivity(Bundle args) {

        super(args);
        if( args != null ) {
            do_what = args.getInt("do_what", 0);
            selectedContacts = args.getIntegerArrayList("result"); /* may be empty - in this case a group only with SELF is created */
        }
        if( selectedContacts == null ) { selectedContacts = new ArrayList<>(); }
        if( !selectedContacts.contains(MrContact.MR_CONTACT_ID_SELF) ) {
            selectedContacts.add(MrContact.MR_CONTACT_ID_SELF);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);

        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View createView(final Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_close_white);
        actionBar.setTitle(do_what==ContactsActivity.SELECT_CONTACTS_FOR_NEW_VERIFIED_GROUP? context.getString(R.string.NewVerifiedGroup) : context.getString(R.string.NewGroup));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == done_button) {
                    String groupName = nameTextView.getText().toString().trim();
                    if (groupName.isEmpty()) {
                        Toast.makeText(getParentActivity(), context.getString(R.string.ErrGroupNameEmpty), Toast.LENGTH_LONG).show();
                        return;
                    }
                    int chat_id=MrMailbox.createGroupChat(do_what==ContactsActivity.SELECT_CONTACTS_FOR_NEW_VERIFIED_GROUP, groupName);
                    if( chat_id<=0 ) {
                        /* this should never happen, the group is created locally, there is no reason to fail here */
                        Toast.makeText(getParentActivity(), "ErrCreateGroup", Toast.LENGTH_LONG).show();
                        return;
                    }
                    int i, icnt = selectedContacts.size();
                    for( i = 0; i < icnt; i++ ) {
                        int contact_id = selectedContacts.get(i);
                        if( contact_id != MrContact.MR_CONTACT_ID_SELF ) {
                            if (0 == MrMailbox.addContactToChat(chat_id, contact_id)) {
                                Toast.makeText(getParentActivity(), "ErrAddContactToGroup", Toast.LENGTH_LONG).show();
                            }
                        }
                    }

                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.chatDidCreated, chat_id);
                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
                    Bundle args2 = new Bundle();
                    args2.putInt("chat_id", chat_id);
                    presentFragment(new ChatActivity(args2), true);
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItemWithWidth(done_button, R.drawable.ic_done, AndroidUtilities.dp(56));

        fragmentView = new LinearLayout(context);
        LinearLayout linearLayout = (LinearLayout) fragmentView;
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        TextView label = HeaderCell.createTextView(context, context.getString(R.string.Name));
        linearLayout.addView(label, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 18, 18, 18, 0));

        nameTextView = new EditText(context);
        nameTextView.setHint(context.getString(R.string.EnterGroupNamePlaceholder));
        if (nameToSet != null) {
            nameTextView.setText(nameToSet);
            nameToSet = null;
        }
        nameTextView.setMaxLines(4);
        nameTextView.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        nameTextView.setHintTextColor(0xffBBBBBB);
        nameTextView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        nameTextView.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nameTextView.setPadding(0, 0, 0, AndroidUtilities.dp(8));
        InputFilter[] inputFilters = new InputFilter[1];
        inputFilters[0] = new InputFilter.LengthFilter(100);
        nameTextView.setFilters(inputFilters);
        nameTextView.setTextColor(0xff212121);
        linearLayout.addView(nameTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 18, 1, 18, 18));

        label = HeaderCell.createTextView(context, context.getResources().getQuantityString(R.plurals.Members, selectedContacts.size(), selectedContacts.size()));
        linearLayout.addView(label, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 18, 9, 18, 4));

        listView = new ListView(context);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setAdapter(listAdapter = new ListAdapter(context));
        linearLayout.addView(listView);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) listView.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        listView.setLayoutParams(layoutParams);

        return fragmentView;
    }


    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
    }

    @Override
    public void saveSelfArgs(Bundle args) {
        if (nameTextView != null) {
            String text = nameTextView.getText().toString();
            if (text != null && text.length() != 0) {
                args.putString("nameTextView", text);
            }
        }
    }

    @Override
    public void restoreSelfArgs(Bundle args) {
        String text = args.getString("nameTextView");
        if (text != null) {
            if (nameTextView != null) {
                nameTextView.setText(text);
            } else {
                nameToSet = text;
            }
        }
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen && nameTextView!=null) {
            nameTextView.requestFocus();
            AndroidUtilities.showKeyboard(nameTextView);
        }
    }

    @Override
    public void didReceivedNotification(int id, final Object... args) {
        if (id == NotificationCenter.updateInterfaces) {
            int mask = (Integer)args[0];
            if ((mask & MrMailbox.UPDATE_MASK_AVATAR) != 0 || (mask & MrMailbox.UPDATE_MASK_NAME) != 0 || (mask & MrMailbox.UPDATE_MASK_STATUS) != 0) {
                updateVisibleRows();
            }
        }
    }

    private void updateVisibleRows() {
        if (listView == null) {
            return;
        }
        int count = listView.getChildCount();
        for (int a = 0; a < count; a++) {
            View child = listView.getChildAt(a);
            if (child instanceof UserCell) {
                ((UserCell) child).update();
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
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = new UserCell(mContext,0);
            }

            int curr_user_id = selectedContacts.get(i);

            MrContact mrContact = MrMailbox.getContact(curr_user_id);

            ((UserCell) view).setData(mrContact);

            return view;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public int getCount() {
            return selectedContacts.size();
        }
    }
}
