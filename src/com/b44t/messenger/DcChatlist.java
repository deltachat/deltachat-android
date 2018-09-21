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


import android.support.annotation.NonNull;

public class DcChatlist {

    public DcChatlist(long chatlistCPtr) {
        this.chatlistCPtr = chatlistCPtr;
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        unrefChatlistCPtr();
        chatlistCPtr = 0;
    }

    public native int       getCnt    ();
    public native int       getChatId (int index);
    public @NonNull DcChat  getChat   (int index) { return new DcChat(getChatCPtr(index)); }
    public @NonNull DcMsg   getMsg    (int index) { return new DcMsg(getMsgCPtr(index)); }
    public @NonNull DcLot   getSummary(int index, DcChat chat) { return new DcLot(getSummaryCPtr(index, chat==null? null : chat.getChatCPtr())); }

    // working with raw c-data
    private long        chatlistCPtr;    // CAVE: the name is referenced in the JNI
    private native void unrefChatlistCPtr();
    private native long getChatCPtr      (int index);
    private native long getMsgCPtr       (int index);
    private native long getSummaryCPtr   (int index, long chatCPtr);
}
