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


package com.b44t.ui.Adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.b44t.messenger.MrContact;
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
        contactIds = MrMailbox.getKnownContacts(null);
    }

    public void setCheckedMap(HashMap<Integer, ?> map) {
        checkedMap = map;
    }

    public void setIsScrolling(boolean value) {
        scrolling = value;
    }

    public void search(String query) {
        contactIds = MrMailbox.getKnownContacts(query);
        lastQuery = query;
    }

    public void searchAgain() {
        contactIds = MrMailbox.getKnownContacts(lastQuery);
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
            MrContact mrContact = MrMailbox.getContact(curr_user_id);
            ((UserCell) convertView).setData(mrContact, 0);
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
