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

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.MrChat;
import com.b44t.messenger.MrPoortext;
import com.b44t.messenger.support.widget.RecyclerView;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.TLRPC;
import com.b44t.ui.Cells.DialogCell;


public class DialogsAdapter extends RecyclerView.Adapter {

    private Context mContext;
    private long openedDialogId;

    private class Holder extends RecyclerView.ViewHolder {
        public Holder(View itemView) {
            super(itemView);
        }
    }

    public DialogsAdapter(Context context) {
        mContext = context;

        MrMailbox.reloadMainChatlist();
    }

    public void setOpenedDialogId(long id) {
        openedDialogId = id;
    }

    @Override
    public int getItemCount() {
        return MrMailbox.m_currChatlist.getCnt();
    }

    public MrChat getItem(int i) {
        return MrMailbox.m_currChatlist.getChatByIndex(i);
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        if (holder.itemView instanceof DialogCell) {
            ((DialogCell) holder.itemView).checkCurrentDialogIndex();
        }
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = new DialogCell(mContext);
        view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        if (viewHolder.getItemViewType() == 0) {
            DialogCell cell = (DialogCell) viewHolder.itemView;
            cell.useSeparator = (i != getItemCount() - 1);
            MrChat mrChat = getItem(i);
            if (AndroidUtilities.isTablet()) {
                cell.setDialogSelected(mrChat.getId() == openedDialogId);
            }

            MrPoortext mrSummary = MrMailbox.m_currChatlist.getSummaryByIndex(i, mrChat);
            cell.setDialog(mrChat, mrSummary, i, 0);
        }
    }

    @Override
    public int getItemViewType(int i) {
        return 0;
    }
}
