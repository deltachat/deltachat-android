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

import com.b44t.messenger.LocaleController;
import com.b44t.messenger.MrChat;
import com.b44t.messenger.MrChatlist;
import com.b44t.messenger.MrMsg;
import com.b44t.messenger.MrPoortext;
import com.b44t.messenger.support.widget.RecyclerView;
import com.b44t.messenger.MrMailbox;
import com.b44t.ui.Cells.DialogCell;
import com.b44t.ui.Cells.GreySectionCell;


public class DialogsSearchAdapter extends RecyclerView.Adapter {

    private Context mContext;

    private static final int ROWTYPE_HEADLINE = 0;
    private static final int ROWTYPE_CHAT     = 1;
    private static final int ROWTYPE_MSG      = 2;

    int rowChatsHeadline = -1;
    int rowFirstChat = -1, rowLastChat = -1;
    int rowMsgsHeadline = -1;
    int rowFirstMsg = -1, rowLastMsg = -1;
    int rowCount = 0;

    MrChatlist m_chatlist = new MrChatlist(0);
    int        m_chatlistCnt = 0;
    int[]      m_msgIds = {};

    private class Holder extends RecyclerView.ViewHolder {
        public Holder(View itemView) {
            super(itemView);
        }
    }

    public DialogsSearchAdapter(Context context) {
        mContext = context;
    }

    public void searchDialogs(String query) {
        rowCount = 0;

        m_chatlist = MrMailbox.getChatlist(query);
        m_chatlistCnt = m_chatlist.getCnt();
        if( m_chatlistCnt>0 ) {
            rowChatsHeadline = rowCount++;

            rowFirstChat = rowCount;
                rowCount += m_chatlistCnt;
            rowLastChat = rowCount-1;
        }
        else {
            rowChatsHeadline = -1;
            rowFirstChat = -1;
            rowLastChat = -1;
        }

        m_msgIds = MrMailbox.searchMsgs(0, query);
        if( m_msgIds.length>0 ) {
            rowMsgsHeadline = rowCount++;

            rowFirstMsg = rowCount;
                rowCount += m_msgIds.length;
            rowLastMsg = rowCount-1;
        }
        else {
            rowMsgsHeadline = -1;
            rowFirstMsg = -1;
            rowLastMsg = -1;
        }
    }


    @Override
    public int getItemCount() {
        return rowCount;
    }

    public Object getItem(int i) {
        if( i>=rowFirstChat && i<=rowLastChat ) {
            return m_chatlist.getChatByIndex(i-rowFirstChat);
        }
        else if( i>=rowFirstMsg && i<=rowLastMsg ) {
            return MrMailbox.getMsg(m_msgIds[i-rowFirstMsg]);
        }
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view;
        switch( viewType ) {
            case ROWTYPE_CHAT:
            case ROWTYPE_MSG:
                view = new DialogCell(mContext);
                view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                break;

            default:
                view = new GreySectionCell(mContext);
                break;
        }

        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i)
    {
        switch (viewHolder.getItemViewType() )
        {
            case ROWTYPE_CHAT: {
                    int j = i - rowFirstChat;
                    if( j >= 0 && j < m_chatlistCnt ) {
                        DialogCell cell = (DialogCell) viewHolder.itemView;
                        cell.useSeparator = (j != m_chatlistCnt - 1);
                        MrChat mrChat = m_chatlist.getChatByIndex(j);
                        MrPoortext mrSummary = m_chatlist.getSummaryByIndex(j, mrChat);
                        cell.setDialog(mrChat, mrSummary, -1, 0);
                    }
                }
                break;

            case ROWTYPE_MSG: {
                    int j = i - rowFirstMsg;
                    if( j >= 0 && j < m_msgIds.length ) {
                        DialogCell cell = (DialogCell) viewHolder.itemView;
                        cell.useSeparator = (j != m_msgIds.length - 1);
                        MrMsg mrMsg = MrMailbox.getMsg(m_msgIds[j]);
                        MrChat mrChat = MrMailbox.getChat(mrMsg.getChatId());
                        MrPoortext mrSummary = new MrPoortext(0);
                        cell.setDialog(mrChat, mrSummary, -1, 0);
                    }
                }
                break;

            case ROWTYPE_HEADLINE: {
                    GreySectionCell headlineCell = (GreySectionCell) viewHolder.itemView;
                    if (i == rowChatsHeadline) {
                        headlineCell.setText(LocaleController.formatPluralString("Chats", m_chatlistCnt));
                    } else if (i == rowMsgsHeadline) {
                        headlineCell.setText(LocaleController.formatPluralString("messages", m_msgIds.length));
                    }
                }
                break;
        }
    }

    @Override
    public int getItemViewType(int i) {
        if( i>=rowFirstChat && i<=rowLastChat ) {
            return ROWTYPE_CHAT;
        }
        else if( i>=rowFirstMsg && i<=rowLastMsg ) {
            return ROWTYPE_MSG;
        }
        return ROWTYPE_HEADLINE;
    }
}
