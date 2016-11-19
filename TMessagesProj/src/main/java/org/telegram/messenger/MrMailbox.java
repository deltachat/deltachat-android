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
 * Purpose: The Java part of the Java<->C Wrapper, see also mr_wrapper.c
 *
 ******************************************************************************/


package org.telegram.messenger;


import android.util.Log;
import org.telegram.tgnet.TLRPC;

public class MrMailbox {

    public static long           hMailbox = 0;
    public static long           hCurrChatlist = 0;
    private static final String  TAG = "LibreChat"; // TAG is an Android convention


    // tools
    public static TLRPC.TL_dialog hChat2dialog(long hChat)
    {
        TLRPC.TL_dialog ret = new TLRPC.TL_dialog();
        ret.id = MrMailbox.MrChatGetId(hChat);
        return ret;
    }

    public static TLRPC.TL_dialog hChatlist2dialog(long hChatlist, int index)
    {
        long hChat = MrMailbox.MrChatlistGetChatByIndex(hChatlist, index);
        TLRPC.TL_dialog dlg = hChat2dialog(hChat);
        MrMailbox.MrChatUnref(hChat);
        return dlg;
    }

    public static TLRPC.Message hMsg2Message(long hMsg)
    {
        TLRPC.Message ret = new TLRPC.TL_message(); // the class derived from TLRPC.Message defines the basic type:
                                                    //  TLRPC.TL_messageService is used to display messages as "You joined the group"
                                                    //  TLRPC.TL_message is a normal message (also photos?)

        int state = MrMsgGetState(hMsg);
        int type  = MrMsgGetType(hMsg);
        switch( state ) {
            case MR_OUT_DELIVERED: ret.send_state = MessageObject.MESSAGE_SEND_STATE_SENT; break;
            case MR_OUT_ERROR:     ret.send_state = MessageObject.MESSAGE_SEND_STATE_SEND_ERROR; break;
            case MR_OUT_PENDING:   ret.send_state = MessageObject.MESSAGE_SEND_STATE_SENDING; break;
            case MR_OUT_READ:      ret.send_state = MessageObject.MESSAGE_SEND_STATE_SENT; break;
        }

        ret.id            = MrMailbox.MrMsgGetId(hMsg);
        ret.from_id       = MrMailbox.MrMsgGetFromId(hMsg);
        ret.to_id         = new TLRPC.TL_peerUser();
        ret.to_id.user_id = MrMailbox.MrMsgGetToId(hMsg);
        ret.date          = (int)MrMsgGetTimestamp(hMsg);
        ret.dialog_id     = MrMsgGetChatId(hMsg);
        ret.unread        = state!=MR_OUT_READ; // the state of outgoing messages
        ret.media_unread  = ret.unread;
        ret.flags         = 0; // posible flags: MESSAGE_FLAG_HAS_FROM_ID, however, this seems to be read only
        ret.post          = false; // ? true=avatar wird in gruppen nicht angezeigt, wird aber in isFromUser() auch überprüft...
        ret.out           = ret.from_id==MR_CONTACT_ID_SELF; // true=outgoing message, read eg. in MessageObject.isOutOwner()
        ret.created_by_mr = true;

        if( type == MrMailbox.MR_MSG_TEXT ) {
            ret.message       = MrMailbox.MrMsgGetText(hMsg);
        }
        else if( type == MrMailbox.MR_MSG_IMAGE ) {
            String path = MrMailbox.MrMsgGetParam(hMsg, 'f', "");
            TLRPC.TL_photo photo = null;
            if( !path.isEmpty() ) {
                try {
                    TLRPC.TL_photoSize photoSize = new TLRPC.TL_photoSize();
                    photoSize.w = MrMailbox.MrMsgGetParamInt(hMsg, 'w', 800);
                    photoSize.h = MrMailbox.MrMsgGetParamInt(hMsg, 'h', 800);
                    photoSize.size = 0;
                    photoSize.location = new TLRPC.TL_fileLocation();
                    photoSize.location.mr_path = path;
                    photoSize.location.local_id = -ret.id; // this forces the document to be searched in the cache dir
                    photoSize.type = "x";
                    photo = new TLRPC.TL_photo();
                    photo.sizes.add(photoSize);
                } catch (Exception e) {
                    // the most common reason is a simple "file not found error"
                }
            }

            if(photo!=null) {
                ret.message = "-1";
                ret.media = new TLRPC.TL_messageMediaPhoto();
                ret.media.photo = photo;
                ret.attachPath = path; // ret.attachPathExists set later in MessageObject.checkMediaExistance()
            }
            else {
                ret.message = "<cannot load image>";
            }
        }
        else if( type == MrMailbox.MR_MSG_AUDIO || type == MrMailbox.MR_MSG_VIDEO ) {
            String path = MrMailbox.MrMsgGetParam(hMsg, 'f', "");
            if( !path.isEmpty()) {
                ret.message = "-1"; // may be misused for video editing information
                ret.media = new TLRPC.TL_messageMediaDocument();
                ret.media.caption = "";
                ret.media.document = new TLRPC.TL_document();
                ret.media.document.mr_path = path;
                if( type == MrMailbox.MR_MSG_AUDIO ) {
                    TLRPC.TL_documentAttributeAudio attr = new TLRPC.TL_documentAttributeAudio();
                    attr.voice = true; // !voice = music
                    attr.duration = MrMailbox.MrMsgGetParamInt(hMsg, 'd', 0) / 1000;
                    ret.media.document.attributes.add(attr);
                }
                else if( type == MrMailbox.MR_MSG_VIDEO) {
                    TLRPC.TL_documentAttributeVideo attr = new TLRPC.TL_documentAttributeVideo();
                    attr.duration = MrMailbox.MrMsgGetParamInt(hMsg, 'd', 0) / 1000;
                    attr.w = MrMailbox.MrMsgGetParamInt(hMsg, 'w', 0);
                    attr.h = MrMailbox.MrMsgGetParamInt(hMsg, 'h', 0);
                    ret.media.document.attributes.add(attr);
                }

            }
            else {
                ret.message = "<path missing>";
            }
        }
        else {
            ret.message = String.format("<unsupported message type #%d for id #%d>", type, ret.id);
        }

        return ret;
    }

    public static TLRPC.User contactId2user(int id)
    {
        TLRPC.User ret = new TLRPC.User();
        ret.id = id;
        return ret;
    }

    public static TLRPC.Chat chatId2chat(int id)
    {
        TLRPC.Chat ret = new TLRPC.Chat();
        ret.id = id;
        return ret;
    }

    public static void reloadMainChatlist()
    {
        MrChatlistUnref(hCurrChatlist); // it's not optimal running this on the UI thread, maybe we should lock it and run it in a separate thread
        hCurrChatlist = MrMailboxGetChatlist(hMailbox);
    }

    // this function is called from within the C-wrapper
    public final static int MR_EVENT_MSGS_UPDATED     = 2000;
    public final static int MR_EVENT_CONTACTS_CHANGED = 2030;
    public final static int MR_EVENT_IS_EMAIL_KNOWN   = 2010;
    public final static int MR_EVENT_MSG_DELIVERED    = 3000;
    public final static int MR_EVENT_MSG_READ         = 3010;
    public static long MrCallback(final int event, final long data1, final long data2)
    {
        switch(event) {
            case MR_EVENT_MSGS_UPDATED:
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        reloadMainChatlist();
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
                    }
                });
                return 0;

            case MR_EVENT_MSG_DELIVERED:
            case MR_EVENT_MSG_READ:
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        reloadMainChatlist();
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.messagesSentOrRead, event, (int)data1, (int)data2);
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
                    }
                });
                return 0;

            case MR_EVENT_CONTACTS_CHANGED:
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        reloadMainChatlist();
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.contactsDidLoaded);
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.blockedUsersDidLoaded);
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
                    }
                });
                return 0;

            case MR_EVENT_IS_EMAIL_KNOWN:
                String emailAdr = CPtr2String(data1);
                return 0;
        }
        return 0;
    }

    // MrMailbox objects
    public native static long    MrMailboxNew               (); // returns hMailbox which must be unref'd after usage (Names as mrmailbox_new don't work due to the additional underscore)
    public native static int     MrMailboxOpen              (long hMailbox, String dbfile, String blobdir);
    public native static void    MrMailboxClose             (long hMailbox);
    public native static int     MrMailboxConfigure         (long hMailbox);
    public native static int     MrMailboxIsConfigured      (long hMailbox);
    public native static int     MrMailboxConnect           (long hMailbox);
    public native static void    MrMailboxDisconnect        (long hMailbox);
    public native static int     MrMailboxFetch             (long hMailbox);

    public native static int[]   MrMailboxGetKnownContacts  (long hMailbox, String query);
    public native static int[]   MrMailboxGetBlockedContacts(long hMailbox);
    public native static long    MrMailboxGetContact        (long hMailbox, int id);// returns hContact which must be unref'd after usage
    public native static int     MrMailboxCreateContact     (long hMailbox, String name, String addr);
    public native static int     MrMailboxBlockContact      (long hMailbox, int id, int block);
    public native static int     MrMailboxDeleteContact     (long hMailbox, int id); // returns 0 if the contact could not be deleted (eg. it is in use, maybe by strangers)

    public native static long    MrMailboxGetChatlist       (long hMailbox); // returns hChatlist which must be unref'd after usage
    public native static long    MrMailboxGetChat           (long hMailbox, int chat_id); // return hChat which must be unref'd after usage
    public native static int     MrMailboxMarkseenChat      (long hMailbox, int id);
    public native static int     MrMailboxGetChatIdByContactId (long hMailbox, int chat_id);
    public native static int     MrMailboxCreateChatByContactId(long hMailbox, int contact_id); // returns chat_id
    public native static int[]   MrMailboxGetChatMedia      (long hMailbox, int chat_id, int msg_type, int or_msg_type);
    public native static int[]   MrMailboxGetChatContacts   (long hMailbox, int chat_id);
    public native static long    MrMailboxGetMsg            (long hMailbox, int id); // return hMsg which must be unref'd after usage
    public native static String  MrMailboxGetMsgInfo        (long hMailbox, int id);
    public native static void    MrMailboxDeleteMsg         (long hMailbox, int id);
    public native static int     MrMailboxSetConfig         (long hMailbox, String key, String value); // value may be NULL
    public native static String  MrMailboxGetConfig         (long hMailbox, String key, String def); // def may be NULL, returns empty string as NULL
    public native static int     MrMailboxGetConfigInt      (long hMailbox, String key, int def); // def may be NULL, returns empty string as NULL

    public native static String  MrMailboxGetInfo           (long hMailbox);
    public native static String  MrMailboxExecute           (long hMailbox, String cmd);

    // MrChatlist objects
    public native static void    MrChatlistUnref            (long hChatlist);
    public native static int     MrChatlistGetCnt           (long hChatlist);
    public native static int     MrChatlistGetChatByIndex   (long hChatlist, int index); // returns hChat which must be unref'd after usage

    // MrChat objects
    public native static void    MrChatUnref                (long hChat);
    public native static int     MrChatGetId                (long hChat);
    public native static int     MrChatGetType              (long hChat);
    public native static String  MrChatGetName              (long hChat);
    public static int            MrChatIsEncrypted          (long hChat) { return 0; }
    public native static String  MrChatGetSubtitle          (long hChat);
    public native static String  MrChatGetDraft             (long hChat); // returns null for "no draft"
    public native static long    MrChatGetDraftTimestamp    (long hChat); // returns 0 for "no draft"
    public native static int     MrChatGetDraftReplyToMsgId (long hChat); // returns 0 for "no draft"
    public native static int     MrChatSetDraft             (long hChat, String draft/*NULL=delete*/, long replyToMsgId);
    public native static int     MrChatGetUnseenCount       (long hChat);
    public native static int     MrChatGetTotalMsgCount     (long hChat);
    public native static long    MrChatGetSummary           (long hChat); // returns hPoortext
    public native static long    MrChatGetMsglist           (long hChat, int offset, int amount); // returns hMsglist
    public native static int     MrChatSendText             (long hChat, String text); // returns message id
    public native static int     MrChatSendMedia            (long hChat, int type, String file, String mime, int w, int h, int time_ms);

    // MrMsglist objects
    public native static void    MrMsglistUnref             (long hMsglist);
    public native static int     MrMsglistGetCnt            (long hMsglist);
    public native static int     MrMsglistGetMsgByIndex     (long hMsglist, int index); // returns hMsg which must be unref'd after usage

    // MrMsg objects
    public native static void    MrMsgUnref                 (long hMsg);
    public native static int     MrMsgGetId                 (long hMsg);
    public native static String  MrMsgGetText               (long hMsg);
    public native static long    MrMsgGetTimestamp          (long hMsg);
    public native static int     MrMsgGetType               (long hMsg);
    public native static int     MrMsgGetState              (long hMsg);
    public native static int     MrMsgGetChatId             (long hMsg);
    public native static int     MrMsgGetFromId             (long hMsg);
    public native static int     MrMsgGetToId               (long hMsg);
    public native static String  MrMsgGetParam              (long hMsg, int key, String def);
    public native static int     MrMsgGetParamInt           (long hMsg, int key, int def);

    // MrContact objects
    public native static void    MrContactUnref             (long hContact);
    public native static String  MrContactGetName           (long hContact);
    public native static String  MrContactGetAddr           (long hContact);
    public static String         MrContactGetDisplayName    (long hContact) { String s=MrContactGetName(hContact); if(s.isEmpty()) {s=MrContactGetAddr(hContact);} return s; }
    public static String         MrContactGetNameNAddr      (long hContact) { String s=MrContactGetName(hContact); if(s.isEmpty()) {s=MrContactGetAddr(hContact);} else { s+=" ("+MrContactGetAddr(hContact)+")"; } return s; }
    public native static int     MrContactIsBlocked         (long hContact);

    // MrPoortext objects
    public native static void    MrPoortextUnref            (long hPoortext);
    public native static String  MrPoortextGetTitle         (long hPoortext);
    public native static int     MrPoortextGetTitleMeaning  (long hPoortext);
    public native static String  MrPoortextGetText          (long hPoortext);
    public native static long    MrPoortextGetTimestamp     (long hPoortext);
    public native static int     MrPoortextGetState         (long hPoortext);

    // Tools
    public native static void    MrStockAddStr              (int id, String str);
    public native static String  MrGetVersionStr            ();
    public native static String  CPtr2String                (long hString); // get strings eg. from data1 from the callback

    public final static int      MR_CHAT_UNDEFINED          =   0;
    public final static int      MR_CHAT_NORMAL             = 100;
    public final static int      MR_CHAT_GROUP              = 120;

    public final static int      MR_CONTACT_ID_SELF         = 1;
    public final static int      MR_CHAT_ID_STRANGERS       = 1;

    public final static int      MR_MSG_UNDEFINED           =  0;
    public final static int      MR_MSG_TEXT                = 10;
    public final static int      MR_MSG_IMAGE               = 20;
    public final static int      MR_MSG_AUDIO               = 40;
    public final static int      MR_MSG_VIDEO               = 50;
    public final static int      MR_MSG_FILE                = 60;

    public final static int      MR_STATE_UNDEFINED         =  0;
    public final static int      MR_IN_UNREAD               = 10;
    public final static int      MR_IN_READ                 = 16;
    public final static int      MR_OUT_PENDING             = 20;
    public final static int      MR_OUT_SENDING             = 22;
    public final static int      MR_OUT_ERROR               = 24;
    public final static int      MR_OUT_DELIVERED           = 26;
    public final static int      MR_OUT_READ                = 28;

    public final static int      MR_TITLE_NORMAL            = 0;
    public final static int      MR_TITLE_DRAFT             = 1;
    public final static int      MR_TITLE_USERNAME          = 2;
    public final static int      MR_TITLE_SELF              = 3;

    // some rest of Telegram ...
    public native static long    getCurrentTimeMillis       ();
    public native static int     getCurrentTime             ();
}
