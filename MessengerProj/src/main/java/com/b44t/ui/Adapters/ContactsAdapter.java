/*
 * This part of the Delta Chat fronted is based on Telegram which is covered by the following note:
 *
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.b44t.ui.Adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.TLRPC;
import com.b44t.ui.Cells.UserCell;

import java.util.HashMap;

public class ContactsAdapter extends BaseFragmentAdapter {

    private Context mContext;
    private HashMap<Integer, ?> checkedMap;
    private boolean scrolling;
    private String lastQuery;

    private int[] contactIds;

    public ContactsAdapter(Context context) {
        mContext = context;
        contactIds = MrMailbox.MrMailboxGetKnownContacts(MrMailbox.hMailbox, null);
    }

    public void setCheckedMap(HashMap<Integer, ?> map) {
        checkedMap = map;
    }

    public void setIsScrolling(boolean value) {
        scrolling = value;
    }

    public void search(String query) {
        contactIds = MrMailbox.MrMailboxGetKnownContacts(MrMailbox.hMailbox, query);
        lastQuery = query;
    }

    public void searchAgain() {
        contactIds = MrMailbox.MrMailboxGetKnownContacts(MrMailbox.hMailbox, lastQuery);
    }

    @Override
    public Object getItem(int curr_user_index) {
        if(curr_user_index>=0 && curr_user_index<contactIds.length) {
            TLRPC.User u = new TLRPC.User();
            u.id = contactIds[curr_user_index];
            return u;
        }
        return null;
    }

    @Override
    public boolean isEnabled(int row) {
        return true;
    }

    @Override
    public int getCount() {
        return contactIds.length;
    }

    @Override
    public View getView(int curr_user_index, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = new UserCell(mContext, 1, 1);
            ((UserCell) convertView).setStatusColors(0xffa8a8a8);
        }

        if(curr_user_index>=0 && curr_user_index<contactIds.length) {
            int curr_user_id = contactIds[curr_user_index];
            long hContact = MrMailbox.MrMailboxGetContact(MrMailbox.hMailbox, curr_user_id);
            ((UserCell) convertView).setData(curr_user_id, 0, MrMailbox.MrContactGetDisplayName(hContact),
                    MrMailbox.MrContactGetAddr(hContact), 0);
            MrMailbox.MrContactUnref(hContact);
            if (checkedMap != null) {
                ((UserCell) convertView).setChecked(checkedMap.containsKey(curr_user_id), !scrolling);
            }
        }

        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }
}
