/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.b44t.messenger.query;

import android.text.TextUtils;

import com.b44t.messenger.MrChat;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.TLRPC;
import java.util.ArrayList;


public class DraftQuery {

    public static TLRPC.DraftMessage getDraft(long did) { // returns null for "no draft"
        MrChat mrChat = MrMailbox.getChat((int)did);
        if( mrChat.getId() == 0 ) {
            return null;
        }
        TLRPC.DraftMessage ret = new TLRPC.DraftMessage();
        ret.message = mrChat.getDraft();
        ret.date = (int)mrChat.getDraftTimestamp();
        ret.reply_to_msg_id = mrChat.getDraftReplyToMsgId();
        return ret;
    }

    private static void saveDraft__(long did, String message, long replyToMessageId) // message may be null
    {
        MrChat mrChat = MrMailbox.getChat((int)did);
        mrChat.setDraft(message, replyToMessageId);
    }

    public static void saveDraft(long did, CharSequence message, TLRPC.Message replyToMessage) {
        if( message == null || TextUtils.isEmpty(message) ) {
            saveDraft__(did, null, 0);
        }
        else {
            saveDraft__(did, message.toString(), 0);
        }
    }

    public static void cleanDraft(long did) {
        saveDraft__(did, null, 0);
    }
}
