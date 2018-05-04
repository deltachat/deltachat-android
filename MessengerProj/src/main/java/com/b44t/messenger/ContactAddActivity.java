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


package com.b44t.messenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.b44t.messenger.ActionBar.ActionBar;
import com.b44t.messenger.ActionBar.ActionBarMenu;
import com.b44t.messenger.ActionBar.BaseFragment;
import com.b44t.messenger.Cells.HeaderCell;
import com.b44t.messenger.Cells.TextInfoCell;
import com.b44t.messenger.Components.LayoutHelper;


public class ContactAddActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private int             do_what = 0;
    public final static int CREATE_CONTACT   = 1;
    public final static int EDIT_NAME        = 2;

    private EditText nameTextView;
    private EditText emailTextView;
    private String prefill;
    private int chat_id; // only used for EDIT_NAME in chats
    private int user_id;
    private boolean create_chat_when_done;

    private final static int done_button = 1;

    public ContactAddActivity(Bundle args) {
        super(args);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);
        do_what = getArguments().getInt("do_what", 0);
        user_id = getArguments().getInt("user_id", 0);
        chat_id = getArguments().getInt("chat_id", 0);
        create_chat_when_done = getArguments().getBoolean("create_chat_when_done", false);
        prefill = getArguments().getString("prefill", "");
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
    }

    @Override
    public View createView(final Context context) {

        String nameToSet = null;
        String emailToSet = null;

        actionBar.setBackButtonImage(R.drawable.ic_close_white);
        if (do_what==CREATE_CONTACT) {
            actionBar.setTitle(context.getString(R.string.NewContactTitle));

            if( prefill!=null && !prefill.isEmpty()) {
                if( prefill.contains("@") ) {
                    emailToSet = prefill;
                }
                else {
                    nameToSet = prefill;
                }
            }

        } else {
            actionBar.setTitle(context.getString(R.string.EditName));
            if( user_id!=0 ) {
                nameToSet = MrMailbox.getContact(user_id).getDisplayName();
            }
            else {
                nameToSet = MrMailbox.getChat(chat_id).getName();
            }
        }

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == done_button) {
                    String name = nameTextView.getText().toString();
                    if( chat_id!=0 ) {
                        MrMailbox.setChatName(chat_id, name);
                    }
                    else {
                        String addr = "";
                        if (do_what==CREATE_CONTACT) {
                            addr = emailTextView.getText().toString();
                        }
                        else {
                            MrContact mrContact = MrMailbox.getContact(user_id);
                            addr = mrContact.getAddr();
                        }

                        int new_user_id;
                        if( (new_user_id=MrMailbox.createContact(name, addr))==0 ) {
                            Toast.makeText(getParentActivity(), context.getString(R.string.BadEmailAddress), Toast.LENGTH_LONG).show();
                            return;
                        }
                        else if (do_what==CREATE_CONTACT) {
                            if(create_chat_when_done) {
                                int belonging_chat_id = MrMailbox.createChatByContactId(new_user_id);
                                if( belonging_chat_id != 0 ) {
                                    Bundle args = new Bundle();
                                    args.putInt("chat_id", belonging_chat_id);
                                    presentFragment(new ChatActivity(args), true);
                                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.chatDidCreated, belonging_chat_id); /*this will remove the contact list from stack */
                                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces, MrMailbox.UPDATE_MASK_NAME);
                                    return;
                                }
                            }
                            else {
                                Toast.makeText(getParentActivity(), context.getString(R.string.ContactCreated), Toast.LENGTH_LONG).show();
                            }
                        }
                    }

                    finishFragment();
                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces, MrMailbox.UPDATE_MASK_NAME);
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItemWithWidth(done_button, R.drawable.ic_done, AndroidUtilities.dp(56));

        fragmentView = new LinearLayout(context);
        LinearLayout linearLayout = (LinearLayout) fragmentView;
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        nameTextView = new EditText(context);

        if(do_what==CREATE_CONTACT) {
            TextView label = HeaderCell.createTextView(context, context.getString(R.string.Name));
            linearLayout.addView(label, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 18, 18, 18, 0));
        }
        nameTextView.setHint(context.getString(R.string.Name));

        if (nameToSet != null) {
            nameTextView.setText(nameToSet);
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
        linearLayout.addView(nameTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 18, do_what==CREATE_CONTACT? 1:18, 18, 0));

        if( do_what==CREATE_CONTACT ) {
            TextView label = HeaderCell.createTextView(context, context.getString(R.string.EmailAddress));
            linearLayout.addView(label, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 18, 18, 18, 0));

            emailTextView = new EditText(context);
            if (emailToSet != null) {
                emailTextView.setText(emailToSet);
            }
            emailTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            emailTextView.setHint(R.string.ExampleEmailAddr);
            emailTextView.setHintTextColor(0xffBBBBBB);
            emailTextView.setTextColor(0xff212121);
            emailTextView.setMaxLines(4);
            emailTextView.setGravity(Gravity.START);
            emailTextView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            emailTextView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            emailTextView.setPadding(0, 0, 0, AndroidUtilities.dp(8));
            linearLayout.addView(emailTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 18, 1, 18, 0));
        }

        if( do_what == EDIT_NAME && chat_id != 0 ) {
            TextView textView = TextInfoCell.createTextView(context);
            textView.setText(R.string.EditedNameWillBeShared);
            linearLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 18, 18, 18, 0));
        }

        if( nameToSet != null ) {
            nameTextView.setSelection(nameToSet.length());
        }
        if( emailToSet != null ) {
            emailTextView.setSelection(emailToSet.length());
        }

        return fragmentView;
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
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
    }
}
