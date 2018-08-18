/*******************************************************************************
 *
 *                           Delta Chat Java Adapter
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


public class DcChatlist {

    public DcChatlist(long hChatlist) {
        m_hChatlist = hChatlist;
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        DcChatlistUnref(m_hChatlist);
        m_hChatlist = 0;
    }

    public native int getCnt();

    public DcChat getChatByIndex(int index) {
        return new DcChat(DcChatlistGetChatByIndex(m_hChatlist, index));
    }

    public DcMsg getMsgByIndex(int index) {
        return new DcMsg(DcChatlistGetMsgByIndex(m_hChatlist, index));
    }

    public DcLot getSummaryByIndex(int index, DcChat chat) {
        return new DcLot(DcChatlistGetSummaryByIndex(m_hChatlist, index, chat.getCPtr()));
    }

    // working with raw c-data
    private long m_hChatlist; // must not be renamed as referenced by JNI
    private native static void DcChatlistUnref(long hChatlist);
    private native static long DcChatlistGetChatByIndex(long hChatlist, int index); // returns hChat which must be unref'd after usage
    private native static long DcChatlistGetMsgByIndex(long hChatlist, int index); // returns hMsg which must be unref'd after usage
    private native static long DcChatlistGetSummaryByIndex(long hChatlist, int index, long hChat);
}
