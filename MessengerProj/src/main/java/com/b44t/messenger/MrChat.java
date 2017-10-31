/*******************************************************************************
 *
 *                              Delta Chat Android
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
 *******************************************************************************
 *
 * File:    MrChat.java
 * Purpose: Wrap around mrchat_t
 *
 ******************************************************************************/

package com.b44t.messenger;

import android.text.TextUtils;

public class MrChat {

    public final static int      MR_CHAT_UNDEFINED          =   0;
    public final static int      MR_CHAT_NORMAL             = 100;
    public final static int      MR_CHAT_GROUP              = 120;

    public final static int      MR_CHAT_ID_DEADDROP        = 1;
    public final static int      MR_CHAT_ID_LAST_SPECIAL    = 9;

    public MrChat(long hChat) {
        m_hChat = hChat;
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        MrChatUnref(m_hChat);
    }

    public native int    getId();
    public native int    getType();
    public native int    getArchived();
    public native String getName();
    public native String getSubtitle();

    public static int MRP_UNPROMOTED = 'U';
    public static int MRP_PROFILE_IMAGE = 'i';
    public native String getParam(int key, String def);
    public native int    getParamInt(int key, int def);

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
    public native int getFreshMsgCount();

    public native int sendText(String text);

    public native int sendMedia(int type, String file, String mime, int w, int h, int time_ms, String author, String trackname);

    private long                  m_hChat;  // must not be renamed as referenced by JNI under the name "m_hChat"
    private native static void    MrChatUnref                (long hChat);
    private native static String  MrChatGetDraft             (long hChat); // returns null for "no draft"
    private native static long    MrChatGetDraftTimestamp    (long hChat); // returns 0 for "no draft"
    private native static int     MrChatGetDraftReplyToMsgId (long hChat); // returns 0 for "no draft"
    private native static int     MrChatSetDraft             (long hChat, String draft/*NULL=delete*/, long replyToMsgId);


    /* additional functions that are not 1:1 available in the backend
     **********************************************************************************************/

    public static TLRPC.Chat chatId2chat(int id)
    {
        TLRPC.Chat ret = new TLRPC.Chat();
        ret.id = id;
        return ret;
    }

    public TLRPC.TL_dialog get_TLRPC_TL_dialog()
    {
        TLRPC.TL_dialog ret = new TLRPC.TL_dialog();
        ret.id = getId();
        return ret;
    }

    public long getCPtr() {
        return m_hChat;
    }

    public TLRPC.DraftMessage getDraftMessageObj() {
        if( getId() == 0 ) {
            return null;
        }
        TLRPC.DraftMessage ret = new TLRPC.DraftMessage();
        ret.message = getDraft();
        if( ret.message==null || ret.message.isEmpty() ) {
            return null;
        }
        ret.date = (int)getDraftTimestamp();
        ret.reply_to_msg_id = getDraftReplyToMsgId();
        return ret;
    }

    public void saveDraft(CharSequence message, TLRPC.Message replyToMessage) {
        if( message == null || TextUtils.isEmpty(message) ) {
            setDraft(null, 0);
        }
        else {
            setDraft(message.toString(), 0);
        }
    }

    public void cleanDraft()
    {
        setDraft(null, 0);
    }

    public String getNameNAddr()
    {
        // returns name of group chats or name+email-address for normal chats
        String name = "ErrGrpNameNAddr";
        if( getType()==MR_CHAT_GROUP ) {
            name = getName();
        }
        else {
            int contacts[] = MrMailbox.getChatContacts(getId());
            if( contacts.length==1 ) {
                name = MrMailbox.getContact(contacts[0]).getNameNAddr();
            }
        }
        return name;
    }
}
