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


package com.b44t.ui.Adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.R;
import com.b44t.ui.Cells.DrawerActionCell;
import com.b44t.ui.Cells.DividerCell;
import com.b44t.ui.Cells.EmptyCell;
import com.b44t.ui.Cells.DrawerProfileCell;

public class DrawerLayoutAdapter extends BaseAdapter {

    private Context mContext;

    public final static int ROW_PROFILE             = 0;
    public final static int ROW_EMPTY_BELOW_PROFILE = 1;
    public final static int ROW_NEW_CHAT            = 2;
    public final static int ROW_NEW_GROUP           = 3;
    public final static int ROW_DIVIDER             = 4;
    public final static int ROW_SETTINGS            = 5;
    public final static int ROW_INVITE              = 6;
    public final static int ROW_DEADDROP            = 7;
    public final static int ROW_FAQ                 = 8;
    public final static int ROW_COUNT               = 9;

    private final static int TYPE_PROFILE = 0;
    private final static int TYPE_EMPTY   = 1;
    private final static int TYPE_DIVIDER = 2;
    private final static int TYPE_BUTTON  = 3;
    private final static int TYPE_COUNT   = 4;

    public DrawerLayoutAdapter(Context context) {
        mContext = context;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int i) {
        return !(i == ROW_PROFILE || i == ROW_EMPTY_BELOW_PROFILE || i == ROW_DIVIDER);
    }

    @Override
    public int getCount() {
        return ROW_COUNT;
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
        return true;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        int type = getItemViewType(i);
        if (type == TYPE_PROFILE) {
            if (view == null) {
                view = new DrawerProfileCell(mContext);
            }
            ((DrawerProfileCell) view).updateUserName();
        } else if (type == TYPE_EMPTY) {
            if (view == null) {
                view = new EmptyCell(mContext, AndroidUtilities.dp(8));
            }
        } else if (type == TYPE_DIVIDER) {
            if (view == null) {
                view = new DividerCell(mContext);
            }
        } else if (type == TYPE_BUTTON) {
            if (view == null) {
                view = new DrawerActionCell(mContext);
            }
            DrawerActionCell actionCell = (DrawerActionCell) view;
            if (i == ROW_NEW_CHAT) {
                actionCell.setTextAndIcon(mContext.getString(R.string.NewChat), R.drawable.menu_newchat);
            } else if (i == ROW_NEW_GROUP) {
                actionCell.setTextAndIcon(mContext.getString(R.string.NewGroup), R.drawable.menu_empty);
            } else if (i == ROW_INVITE) {
                actionCell.setTextAndIcon(mContext.getString(R.string.InviteMenuEntry), R.drawable.menu_empty);
            } else if (i == ROW_DEADDROP) {
                actionCell.setTextAndIcon(mContext.getString(R.string.Deaddrop), R.drawable.menu_empty);
                // we do not want an icon beside the mailbox:
                // 1. We do not want to give it much attention,
                // 2. If the mailbox is shown in the chatlist, it gets the chat icon (KISS), but we should not use this icon in the drawer for the mailbox.
                // 3. Simplicity - we have two sections, "Add" and "Tools" - Mailbox belongs to the latter
            } else if (i == ROW_SETTINGS) {
                actionCell.setTextAndIcon(mContext.getString(R.string.Settings), R.drawable.menu_settings);
            } else if (i == ROW_FAQ) {
                actionCell.setTextAndIcon(mContext.getString(R.string.Help), R.drawable.menu_empty);
            }
        }

        return view;
    }

    @Override
    public int getItemViewType(int i) {
        if (i == ROW_PROFILE) {
            return TYPE_PROFILE;
        } else if (i == ROW_EMPTY_BELOW_PROFILE) {
            return TYPE_EMPTY;
        } else if (i == ROW_DIVIDER) {
            return TYPE_DIVIDER;
        }
        return TYPE_BUTTON;
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
