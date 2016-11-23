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
 * File:    MrChat.java
 * Authors: Björn Petersen
 * Purpose: Wrap around mrchat_t
 *
 ******************************************************************************/

package com.b44t.messenger;

public class MrChat {

    public final static int      MR_CHAT_UNDEFINED          =   0;
    public final static int      MR_CHAT_NORMAL             = 100;
    public final static int      MR_CHAT_GROUP              = 120;

    public final static int      MR_CHAT_ID_DEADDROP        = 1;

    public MrChat(long hChat) {
        m_hChat = hChat;
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        MrChatUnref(m_hChat);
    }

    public int getId() {
        return MrChatGetId(m_hChat);
    }
    public int getType() {
        return MrChatGetType(m_hChat);
    }

    public String getName() {
        return MrChatGetName(m_hChat);
    }

    public String getSubtitle() {
        return MrChatGetSubtitle(m_hChat);
    }

    public String getDraft() {
        return MrChatGetDraft(m_hChat);
    }

    public long getDraftTimestamp() {
        return MrChatGetDraftTimestamp(m_hChat);
    }

    public int getDraftReplyToMsgId() {
        return MrChatGetDraftReplyToMsgId(m_hChat);
    }

    public int setDraft(String draft/*NULL=delete*/, long replyToMsgId) {
        return MrChatSetDraft(m_hChat, draft, replyToMsgId);
    }
    public int getUnseenCount() {
        return MrChatGetUnseenCount(m_hChat);
    }

    public int getTotalMsgCount() {
        return MrChatGetTotalMsgCount(m_hChat);
    }

    public MrPoortext getSummary() {
        return new MrPoortext(MrChatGetSummary(m_hChat));
    }

    public int sendText(String text) {
        return MrChatSendText(m_hChat, text);
    }

    public int sendMedia(int type, String file, String mime, int w, int h, int time_ms) {
        return MrChatSendMedia(m_hChat, type, file, mime, w, h, time_ms);
    }

    private long                  m_hChat;
    private native static void    MrChatUnref                (long hChat);
    private native static int     MrChatGetId                (long hChat);
    private native static int     MrChatGetType              (long hChat);
    private native static String  MrChatGetName              (long hChat);
    private native static String  MrChatGetSubtitle          (long hChat);
    private native static String  MrChatGetDraft             (long hChat); // returns null for "no draft"
    private native static long    MrChatGetDraftTimestamp    (long hChat); // returns 0 for "no draft"
    private native static int     MrChatGetDraftReplyToMsgId (long hChat); // returns 0 for "no draft"
    private native static int     MrChatSetDraft             (long hChat, String draft/*NULL=delete*/, long replyToMsgId);
    private native static int     MrChatGetUnseenCount       (long hChat);
    private native static int     MrChatGetTotalMsgCount     (long hChat);
    private native static long    MrChatGetSummary           (long hChat); // returns hPoortext
    private native static int     MrChatSendText             (long hChat, String text); // returns message id
    private native static int     MrChatSendMedia            (long hChat, int type, String file, String mime, int w, int h, int time_ms);

    /* additional functions that are not 1:1 available in the backend
     **********************************************************************************************/

    public int isEncrypted() {
        return 0;
    }

    public static TLRPC.TL_dialog MrChat2dialog(MrChat mrChat)
    {
        TLRPC.TL_dialog ret = new TLRPC.TL_dialog();
        ret.id = mrChat.getId();
        return ret;
    }
}
