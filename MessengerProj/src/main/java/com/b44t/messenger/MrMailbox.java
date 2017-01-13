/*******************************************************************************
 *
 *                          Messenger Android Frontend
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
 * File:    MrMailbox.java
 * Purpose: Wrap around mrmailbox_t
 *
 ******************************************************************************/


package com.b44t.messenger;


public class MrMailbox {

    public static void init () {
        m_hMailbox = MrMailboxNew();
    }

    public native static int    open(String dbfile);
    public native static void   close();
    public native static String getBlobdir();

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

    public native static String getErrorDescr();

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

    private static long           m_hMailbox = 0; // do not rename this, is used in C-part
    private native static long    MrMailboxNew               (); // returns hMailbox which must be unref'd after usage (Names as mrmailbox_new don't work due to the additional underscore)
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
    public native static int[] getKnownContacts(String query);
    public native static int   getBlockedCount();
    public native static int[] getBlockedContacts();

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

    private native static long    MrMailboxGetContact        (long hMailbox, int id);// returns hContact which must be unref'd after usage
    private native static int     MrMailboxCreateContact     (long hMailbox, String name, String addr);
    private native static int     MrMailboxBlockContact      (long hMailbox, int id, int block);
    private native static int     MrMailboxDeleteContact     (long hMailbox, int id); // returns 0 if the contact could not be deleted (eg. it is in use, maybe by deaddrop)
    private native static int     MrMailboxAddAddressBook    (long hMailbox, String adrbook);


    // chats
    public static MrChatlist getChatlist(String query) {
        return new MrChatlist(MrMailboxGetChatlist(m_hMailbox, query));
    }

    public static MrChat getChat(int chat_id) {
        return new MrChat(MrMailboxGetChat(m_hMailbox, chat_id));
    }

    public native static int    markseenMsg        (int msg_id);
    public native static int    markseenChat       (int chat_id);

    public static int getChatIdByContactId (int contact_id) {
        return MrMailboxGetChatIdByContactId(m_hMailbox, contact_id);
    }

    public static int createChatByContactId(int contact_id) {
        return MrMailboxCreateChatByContactId(m_hMailbox, contact_id);
    }

    public native static int createGroupChat       (String name);
    public native static int addContactToChat      (int chat_id, int contact_id);
    public native static int removeContactFromChat (int chat_id, int contact_id);
    public native static int setChatName           (int chat_id, String name);

    public final static int MR_GCM_ADDDAYMARKER = 0x01;
    public native static int[] getChatMsgs(int chat_id, int flags, int marker1before);

    public native static int[] searchMsgs(int chat_id, String query);

    public native static int[] getUnseenMsgs();

    public static int[] getChatMedia(int chat_id, int msg_type, int or_msg_type) {
        return MrMailboxGetChatMedia(m_hMailbox, chat_id, msg_type, or_msg_type);
    }

    public native static int[] getChatContacts(int chat_id);
    public native static int deleteChat(int chat_id);

    private native static long    MrMailboxGetChatlist       (long hMailbox, String query); // returns hChatlist which must be unref'd after usage
    private native static long    MrMailboxGetChat           (long hMailbox, int chat_id); // return hChat which must be unref'd after usage
    private native static int     MrMailboxGetChatIdByContactId (long hMailbox, int contact_id);
    private native static int     MrMailboxCreateChatByContactId(long hMailbox, int contact_id); // returns chat_id
    private native static int[]   MrMailboxGetChatMedia      (long hMailbox, int chat_id, int msg_type, int or_msg_type);


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
    private native static void     MrStockAddStr              (int id, String str);
    public native static String   MrGetVersionStr            ();
    public native static String   CPtr2String                (long hString); // get strings eg. from data1 from the callback


    /* receive events
     **********************************************************************************************/

    public final static int MR_EVENT_MSGS_CHANGED             = 2000;
    public final static int MR_EVENT_INCOMING_MSG             = 2005;
    public final static int MR_EVENT_MSG_DELIVERED            = 2010;
    public final static int MR_EVENT_MSG_READ                 = 2015;
    public final static int MR_EVENT_CHAT_MODIFIED            = 2020;
    public final static int MR_EVENT_CONTACTS_CHANGED         = 2030;
    public final static int MR_EVENT_CONNECTION_STATE_CHANGED = 2040;
    public final static int MR_EVENT_REPORT                   = 2050;

    public final static int MR_REPORT_ERR_SELF_NOT_IN_GROUP   = 1;

    public static long MrCallback(final int event, final long data1, final long data2) // this function is called from within the C-wrapper
    {
        switch(event) {
            case MR_EVENT_CONNECTION_STATE_CHANGED:
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.connectionStateChanged, (int)data1);
                    }
                });
                return 0;

            case MR_EVENT_MSGS_CHANGED:
            case MR_EVENT_INCOMING_MSG:
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        reloadMainChatlist();
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload, event, (int)data1, (int)data2);
                        if( event == MR_EVENT_INCOMING_MSG ) {
                            NotificationsController.getInstance().processNewMessages((int)data1, (int)data2);
                        }
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

            case MR_EVENT_CHAT_MODIFIED:
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        reloadMainChatlist();
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces,
                                MessagesController.UPDATE_MASK_NAME|MessagesController.UPDATE_MASK_CHAT_NAME|
                                MessagesController.UPDATE_MASK_CHAT_MEMBERS|MessagesController.UPDATE_MASK_AVATAR);
                    }
                });
                return 0;

            case MR_EVENT_REPORT:
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if(data1==MR_REPORT_ERR_SELF_NOT_IN_GROUP) {
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.errSelfNotInGroup);
                        }
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
        m_currChatlist = getChatlist(null);
    }

    public final static int MEDIA_PHOTOVIDEO = 0;
    public static void getMediaCount(final long uid, final int type, final int classGuid, boolean fromCache) {
        Utilities.globalQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                int[] media = new int[0];
                if( type == MEDIA_PHOTOVIDEO ) {
                    media = MrMailbox.getChatMedia((int)uid, MrMsg.MR_MSG_IMAGE, MrMsg.MR_MSG_VIDEO);
                }

                NotificationCenter.getInstance().postNotificationName(NotificationCenter.mediaCountDidLoaded,
                        uid, media.length, false /*not from cache*/, type, media);


            }
        });
    }

    public static void initStockStrings()
    {
        MrStockAddStr(1, LocaleController.getString("NoMessages", R.string.NoMessages));
        MrStockAddStr(2, LocaleController.getString("FromSelf", R.string.FromSelf));
        MrStockAddStr(3, LocaleController.getString("Draft", R.string.Draft));
        MrStockAddStr(4, LocaleController.getString("MemberSg", R.string.MemberSg));
        MrStockAddStr(5, LocaleController.getString("MemberPl", R.string.MemberPl));
        MrStockAddStr(6, LocaleController.getString("ContactSg", R.string.ContactSg));
        MrStockAddStr(7, LocaleController.getString("ContactPl", R.string.ContactPl));
        MrStockAddStr(8, LocaleController.getString("Deaddrop", R.string.Deaddrop));
        MrStockAddStr(9, LocaleController.getString("AttachPhoto", R.string.AttachPhoto));
        MrStockAddStr(10, LocaleController.getString("AttachVideo", R.string.AttachVideo));
        MrStockAddStr(11, LocaleController.getString("AttachAudio", R.string.AttachAudio));
        MrStockAddStr(12, LocaleController.getString("AttachDocument", R.string.AttachDocument));
        MrStockAddStr(13, LocaleController.getString("DefaultStatusText", R.string.DefaultStatusText));
        MrStockAddStr(14, LocaleController.getString("MsgNewGroupDraft", R.string.MsgNewGroupDraft));
        MrStockAddStr(15, LocaleController.getString("MsgGroupNameChanged", R.string.MsgGroupNameChanged));
        MrStockAddStr(16, LocaleController.getString("MsgGroupImageChanged", R.string.MsgGroupImageChanged));
        MrStockAddStr(17, LocaleController.getString("MsgMemberAddedToGroup", R.string.MsgMemberAddedToGroup));
        MrStockAddStr(18, LocaleController.getString("MsgMemberRemovedFromToGroup", R.string.MsgMemberRemovedFromToGroup));
    }

    public static String getInviteText() {
        String url = "https://getdelta.org";
        String email = getConfig("addr", "");
        String text = LocaleController.formatString("InviteText", R.string.InviteText, url, email);
        return text;
    }
}
