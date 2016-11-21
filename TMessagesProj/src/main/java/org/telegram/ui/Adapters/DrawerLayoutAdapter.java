/*
 * This part of the Delta Chat fronted is based on Telegram which is covered by the following note:
 *
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package org.telegram.ui.Adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.Cells.DrawerActionCell;
import org.telegram.ui.Cells.DividerCell;
import org.telegram.ui.Cells.EmptyCell;
import org.telegram.ui.Cells.DrawerProfileCell;

public class DrawerLayoutAdapter extends BaseAdapter {

    private Context mContext;

    public final static int iProfile           = 0;
    public final static int iEmptyBelowProfile = 1;
    public final static int iNewChat           = 2;
    public final static int iNewGroup          = 3;
    public final static int iDivider           = 4;
    public final static int iSettings          = 5;
    public final static int iDeaddrop          = 6;
    public final static int iFaq               = 7;
    public final static int iCount             = 8;

    public final static int typeProfile = 0;
    public final static int typeEmpty = 1;
    public final static int typeDivider = 2;
    public final static int typeButton = 3;

    public DrawerLayoutAdapter(Context context) {
        mContext = context;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int i) {
        return !(i == iProfile || i == iEmptyBelowProfile || i == iDivider);
    }

    @Override
    public int getCount() {
        return iCount;
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
        if (type == typeProfile) {
            if (view == null) {
                view = new DrawerProfileCell(mContext);
            }
            ((DrawerProfileCell) view).setUser(MessagesController.getInstance().getUser(UserConfig.getClientUserId()));
        } else if (type == typeEmpty) {
            if (view == null) {
                view = new EmptyCell(mContext, AndroidUtilities.dp(8));
            }
        } else if (type == typeDivider) {
            if (view == null) {
                view = new DividerCell(mContext);
            }
        } else if (type == typeButton) {
            if (view == null) {
                view = new DrawerActionCell(mContext);
            }
            DrawerActionCell actionCell = (DrawerActionCell) view;
            if (i == iNewChat) {
                actionCell.setTextAndIcon(LocaleController.getString("NewChat", R.string.NewChat), R.drawable.menu_newchat);
            } else if (i == iNewGroup) {
                actionCell.setTextAndIcon(LocaleController.getString("NewGroup", R.string.NewGroup), R.drawable.menu_empty);
            } else if (i == iDeaddrop) {
                actionCell.setTextAndIcon(LocaleController.getString("Deaddrop", R.string.Deaddrop), R.drawable.menu_empty); // we do not want an icon beside the mailbox: 1. We do not want to give it much attention, 2. if the mailbox is show in the chatlist, it gets the chat icon (KISS), but we should not use this icon in the drawer for the mailbox. 3. Simplicity
            } else if (i == iSettings) {
                actionCell.setTextAndIcon(LocaleController.getString("Settings", R.string.Settings), R.drawable.menu_settings);
            } else if (i == iFaq) {
                actionCell.setTextAndIcon(LocaleController.getString("Help", R.string.Help), R.drawable.menu_empty);
            }
        }

        return view;
    }

    @Override
    public int getItemViewType(int i) {
        if (i == iProfile) {
            return typeProfile;
        } else if (i == iEmptyBelowProfile) {
            return typeEmpty;
        } else if (i == iDivider) {
            return typeDivider;
        }
        return typeButton;
    }

    @Override
    public int getViewTypeCount() {
        return 4;
    }

    @Override
    public boolean isEmpty() {
        return !UserConfig.isClientActivated();
    }
}
