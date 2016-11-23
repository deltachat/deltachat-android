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

    public static void init () {
        m_hMailbox = MrMailboxNew();
    }

    public static int open(String dbfile, String blobdir) {
        return MrMailboxOpen(m_hMailbox, dbfile, blobdir);
    }

    public static void close() {
        MrMailboxClose(m_hMailbox);
    }

    public static int configure() {
        return MrMailboxConfigure(m_hMailbox);
    }

    public static int isConfigured() {
        return MrMailboxIsConfigured(m_hMailbox);
    }

    public static int connect() {
        return MrMailboxConnect(m_hMailbox);
    }

    public static void disconnect() {
        MrMailboxDisconnect(m_hMailbox);
    }

    public static int fetch() {
        return MrMailboxFetch(m_hMailbox);
    }

    public static int setConfig(String key, String value) {
        return MrMailboxSetConfig(m_hMailbox, key, value);
    }

    public static String getConfig(String key, String def) {
        return MrMailboxGetConfig(m_hMailbox, key, def);
    }

    public static int getConfigInt(String key, int def) {
        return MrMailboxGetConfigInt(m_hMailbox, key, def);
    }

    public static String getInfo() {
        return MrMailboxGetInfo(m_hMailbox);
    }

    public static String execute(String cmd) {
        return MrMailboxExecute(m_hMailbox, cmd);
    }

    private static long           m_hMailbox = 0;
    private native static long    MrMailboxNew               (); // returns hMailbox which must be unref'd after usage (Names as mrmailbox_new don't work due to the additional underscore)
    private native static int     MrMailboxOpen              (long hMailbox, String dbfile, String blobdir);
    private native static void    MrMailboxClose             (long hMailbox);
    private native static int     MrMailboxConfigure         (long hMailbox);
    private native static int     MrMailboxIsConfigured      (long hMailbox);
    private native static int     MrMailboxConnect           (long hMailbox);
    private native static void    MrMailboxDisconnect        (long hMailbox);
    private native static int     MrMailboxFetch             (long hMailbox);
    private native static int     MrMailboxSetConfig         (long hMailbox, String key, String value); // value may be NULL
    private native static String  MrMailboxGetConfig         (long hMailbox, String key, String def); // def may be NULL, returns empty string as NULL
    private native static int     MrMailboxGetConfigInt      (long hMailbox, String key, int def); // def may be NULL, returns empty string as NULL
    private native static String  MrMailboxGetInfo           (long hMailbox);
    private native static String  MrMailboxExecute           (long hMailbox, String cmd);


    // contacts
    public static int[] getKnownContacts(String query) {
        return MrMailboxGetKnownContacts(m_hMailbox, query);
    }

    public static int[] getBlockedContacts() {
        return MrMailboxGetBlockedContacts(m_hMailbox);
    }

    public static MrContact getContact(int contact_id) {
        return new MrContact(MrMailboxGetContact(m_hMailbox, contact_id));
    }

    public static int createContact(String name, String addr) {
        return MrMailboxCreateContact(m_hMailbox, name, addr);
    }

    public static int blockContact(int id, int block) {
        return MrMailboxBlockContact(m_hMailbox, id, block);
    }

    public static int deleteContact(int id) {
        return MrMailboxDeleteContact(m_hMailbox, id);
    }

    public static int addAddressBook(String adrbook) {
        return MrMailboxAddAddressBook(m_hMailbox, adrbook);
    }

    private native static int[]   MrMailboxGetKnownContacts  (long hMailbox, String query);
    private native static int[]   MrMailboxGetBlockedContacts(long hMailbox);
    private native static long    MrMailboxGetContact        (long hMailbox, int id);// returns hContact which must be unref'd after usage
    private native static int     MrMailboxCreateContact     (long hMailbox, String name, String addr);
    private native static int     MrMailboxBlockContact      (long hMailbox, int id, int block);
    private native static int     MrMailboxDeleteContact     (long hMailbox, int id); // returns 0 if the contact could not be deleted (eg. it is in use, maybe by deaddrop)
    private native static int     MrMailboxAddAddressBook    (long hMailbox, String adrbook);


    // chats
    public static MrChatlist getChatlist() {
        return new MrChatlist(MrMailboxGetChatlist(m_hMailbox));
    }

    public static MrChat getChat(int contact_id) {
        return new MrChat(MrMailboxGetChat(m_hMailbox, contact_id));
    }

    public static int markseenChat(int id) {
        return MrMailboxMarkseenChat(m_hMailbox, id);
    }

    public static int getChatIdByContactId (int contact_id) {
        return MrMailboxGetChatIdByContactId(m_hMailbox, contact_id);
    }

    public static int createChatByContactId(int contact_id) {
        return MrMailboxCreateChatByContactId(m_hMailbox, contact_id);
    }

    public static int[] getChatMsgs(int chat_id) {
        return MrMailboxGetChatMsgs(m_hMailbox, chat_id);
    }

    public static int[] getChatMedia(int chat_id, int msg_type, int or_msg_type) {
        return MrMailboxGetChatMedia(m_hMailbox, chat_id, msg_type, or_msg_type);
    }

    public static int[] getChatContacts(int chat_id) {
        return MrMailboxGetChatContacts(m_hMailbox, chat_id);
    }

    public static int deleteChat(int chat_id) {
        return MrMailboxDeleteChat(m_hMailbox, chat_id);
    }

    private native static long    MrMailboxGetChatlist       (long hMailbox); // returns hChatlist which must be unref'd after usage
    private native static long    MrMailboxGetChat           (long hMailbox, int chat_id); // return hChat which must be unref'd after usage
    private native static int     MrMailboxMarkseenChat      (long hMailbox, int id);
    private native static int     MrMailboxGetChatIdByContactId (long hMailbox, int contact_id);
    private native static int     MrMailboxCreateChatByContactId(long hMailbox, int contact_id); // returns chat_id
    private native static int[]   MrMailboxGetChatMsgs       (long hMailbox, int chat_id);
    private native static int[]   MrMailboxGetChatMedia      (long hMailbox, int chat_id, int msg_type, int or_msg_type);
    private native static int[]   MrMailboxGetChatContacts   (long hMailbox, int chat_id);
    private native static int     MrMailboxDeleteChat        (long hMailbox, int chat_id);


    // msgs
    public static MrMsg getMsg(int msg_id) {
        return new MrMsg(MrMailboxGetMsg(m_hMailbox, msg_id));
    }

    public static String getMsgInfo(int id) {
        return MrMailboxGetMsgInfo(m_hMailbox, id);
    }

    public static void deleteMsg(int id) {
        MrMailboxDeleteMsg(m_hMailbox, id);
    }

    private native static long    MrMailboxGetMsg            (long hMailbox, int id); // return hMsg which must be unref'd after usage
    private native static String  MrMailboxGetMsgInfo        (long hMailbox, int id);
    private native static void    MrMailboxDeleteMsg         (long hMailbox, int id);

    // static
    public native static void     MrStockAddStr              (int id, String str);
    public native static String   MrGetVersionStr            ();
    public native static String   CPtr2String                (long hString); // get strings eg. from data1 from the callback


    /* receive events
     **********************************************************************************************/

    public final static int MR_EVENT_MSGS_UPDATED     = 2000;
    public final static int MR_EVENT_CONTACTS_CHANGED = 2030;
    public final static int MR_EVENT_MSG_DELIVERED    = 3000;
    public final static int MR_EVENT_MSG_READ         = 3010;
    public static long MrCallback(final int event, final long data1, final long data2) // this function is called from within the C-wrapper
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


    /* additional functions that are not 1:1 available in the backend
     **********************************************************************************************/

    public static MrChatlist     m_currChatlist = new MrChatlist(0);
    public native static int     getCurrentTime             ();
    public static void reloadMainChatlist()
    {
        m_currChatlist = getChatlist();
    }
}
