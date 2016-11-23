/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *     Copyright (C) 2016 Björn Petersen Software Design and Development
 *                   Contact: r10s@b44t.com, http://b44t.com
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
 *******************************************************************************
 *
 * File:    MrMailbox.java
 * Authors: Björn Petersen
 * Purpose: Wrap around mrmsg_t
 *
 ******************************************************************************/


package com.b44t.messenger;

public class MrMsg {

    public MrMsg(long hMsg) {
        m_hMsg = hMsg;
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        MrMsgUnref(m_hMsg);
    }

    public int getId() {
        return MrMsgGetId(m_hMsg);
    }

    public String getText() {
        return MrMsgGetText(m_hMsg);
    }

    public long getTimestamp() {
        return MrMsgGetTimestamp(m_hMsg);
    }

    public int getType() {
        return MrMsgGetType(m_hMsg);
    }

    public int getState() {
        return MrMsgGetState(m_hMsg);
    }
    public int getChatId() {
        return MrMsgGetChatId(m_hMsg);
    }
    public int getFromId() {
        return MrMsgGetFromId(m_hMsg);
    }
    public int getToId() {
        return MrMsgGetToId(m_hMsg);
    }

    public String getParam (int key, String def) {
        return MrMsgGetParam(m_hMsg, key, def);
    }
    public int getParamInt(int key, int def) {
        return MrMsgGetParamInt(m_hMsg, key, def);
    }

    private long                  m_hMsg;
    private native static void    MrMsgUnref                 (long hMsg);
    private native static int     MrMsgGetId                 (long hMsg);
    private native static String  MrMsgGetText               (long hMsg);
    private native static long    MrMsgGetTimestamp          (long hMsg);
    private native static int     MrMsgGetType               (long hMsg);
    private native static int     MrMsgGetState              (long hMsg);
    private native static int     MrMsgGetChatId             (long hMsg);
    private native static int     MrMsgGetFromId             (long hMsg);
    private native static int     MrMsgGetToId               (long hMsg);
    private native static String  MrMsgGetParam              (long hMsg, int key, String def);
    private native static int     MrMsgGetParamInt           (long hMsg, int key, int def);
};
