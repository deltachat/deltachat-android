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
 * Purpose: Wrap around mrmailbox_t
 *
 ******************************************************************************/


package com.b44t.messenger;


public class MrMailbox {

    public static long           hMailbox = 0;
    public static long           hCurrChatlist = 0;
    private static final String  TAG = "LibreChat"; // TAG is an Android convention


    // tools

    public static TLRPC.TL_dialog hChatlist2dialog(long hChatlist, int index)
    {
        MrChat mrChat = MrMailbox.getChatByIndex(hChatlist, index);
        TLRPC.TL_dialog dlg = MrChat.MrChat2dialog(mrChat);

        return dlg;
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
        }
        return 0;
    }

    // MrMailbox objects
    public static MrMsg getMsg(long hMailbox, int msg_id)
    {
        return new MrMsg(MrMailboxGetMsg(hMailbox, msg_id));
    }

    public static MrContact getContact(long hMailbox, int contact_id)
    {
        return new MrContact(MrMailboxGetContact(hMailbox, contact_id));
    }

    public static MrChat getChat(long hMailbox, int contact_id)
    {
        return new MrChat(MrMailboxGetChat(hMailbox, contact_id));
    }

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
    private native static long   MrMailboxGetContact        (long hMailbox, int id);// returns hContact which must be unref'd after usage
    public native static int     MrMailboxCreateContact     (long hMailbox, String name, String addr);
    public native static int     MrMailboxBlockContact      (long hMailbox, int id, int block);
    public native static int     MrMailboxDeleteContact     (long hMailbox, int id); // returns 0 if the contact could not be deleted (eg. it is in use, maybe by deaddrop)
    public native static int     MrMailboxAddAddressBook    (long hMailbox, String adrbook);

    public native static long    MrMailboxGetChatlist       (long hMailbox); // returns hChatlist which must be unref'd after usage
    private native static long   MrMailboxGetChat           (long hMailbox, int chat_id); // return hChat which must be unref'd after usage
    public native static int     MrMailboxMarkseenChat      (long hMailbox, int id);
    public native static int     MrMailboxGetChatIdByContactId (long hMailbox, int chat_id);
    public native static int     MrMailboxCreateChatByContactId(long hMailbox, int contact_id); // returns chat_id
    public native static int[]   MrMailboxGetChatMsgs       (long hMailbox, int chat_id);
    public native static int[]   MrMailboxGetChatMedia      (long hMailbox, int chat_id, int msg_type, int or_msg_type);
    public native static int[]   MrMailboxGetChatContacts   (long hMailbox, int chat_id);
    public native static int     MrMailboxDeleteChat        (long hMailbox, int chat_id);
    private native static long   MrMailboxGetMsg            (long hMailbox, int id); // return hMsg which must be unref'd after usage
    public native static String  MrMailboxGetMsgInfo        (long hMailbox, int id);
    public native static void    MrMailboxDeleteMsg         (long hMailbox, int id);
    public native static int     MrMailboxSetConfig         (long hMailbox, String key, String value); // value may be NULL
    public native static String  MrMailboxGetConfig         (long hMailbox, String key, String def); // def may be NULL, returns empty string as NULL
    public native static int     MrMailboxGetConfigInt      (long hMailbox, String key, int def); // def may be NULL, returns empty string as NULL

    public native static String  MrMailboxGetInfo           (long hMailbox);
    public native static String  MrMailboxExecute           (long hMailbox, String cmd);

    // MrChatlist objects
    public static MrChat getChatByIndex(long hChatlist, int index)
    {
        return new MrChat(MrChatlistGetChatByIndex(hChatlist, index));
    }
    public native static void    MrChatlistUnref            (long hChatlist);
    public native static int     MrChatlistGetCnt           (long hChatlist);
    private native static long   MrChatlistGetChatByIndex   (long hChatlist, int index); // returns hChat which must be unref'd after usage

    // Tools
    public native static void    MrStockAddStr              (int id, String str);
    public native static String  MrGetVersionStr            ();
    public native static String  CPtr2String                (long hString); // get strings eg. from data1 from the callback

    // some rest of T'gram ...
    public native static long    getCurrentTimeMillis       ();
    public native static int     getCurrentTime             ();
}
