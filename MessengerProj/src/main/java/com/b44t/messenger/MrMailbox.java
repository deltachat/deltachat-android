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
 * File:    MrMailbox.java
 * Purpose: Wrap around mrmailbox_t
 *
 ******************************************************************************/


package com.b44t.messenger;


import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;

import com.b44t.messenger.Components.ForegroundDetector;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MrMailbox {

    public static void init () {
        m_hMailbox = MrMailboxNew();
    }

    public native static int    open(String dbfile);
    public native static void   close();
    public native static String getBlobdir();

    public native static int configure();
    public native static void stopOngoingProcess();

    public native static int isConfigured();

    public native static int idle();
    public native static int interruptIdle();
    public native static int poll();

    public native static void setConfig(String key, String value);
    public native static void setConfigInt(String key, int value);
    public native static String getConfig(String key, String def);
    public native static int getConfigInt(String key, int def);

    public native static String getInfo();
    public native static String cmdline(String cmd);

    public native static String initiateKeyTransfer();
    public native static boolean continueKeyTransfer(int msg_id, String setup_code);
    public final static int MR_IMEX_EXPORT_SELF_KEYS = 1;
    public final static int MR_IMEX_IMPORT_SELF_KEYS = 2;
    public final static int MR_IMEX_EXPORT_BACKUP = 11;
    public final static int MR_IMEX_IMPORT_BACKUP = 12;
    public native static int imex(int what, String dir);
    public native static String imexHasBackup(String dir);
    public native static int  checkPassword(String pw);

    public native static void heartbeat();

    private static long           m_hMailbox = 0; // do not rename this, is used in C-part
    private native static long    MrMailboxNew(); // returns hMailbox which must be unref'd after usage (Names as mrmailbox_new don't work due to the additional underscore)

    // contacts
    public final static int MR_GCL_VERIFIED_ONLY = 1;
    public final static int MR_GCL_ADD_SELF = 2;
    public native static int[] getContacts(int flags, String query);
    public native static int   getBlockedCount();
    public native static int[] getBlockedContacts();

    public static MrContact getContact(int contact_id) {
        return new MrContact(MrMailboxGetContact(m_hMailbox, contact_id));
    }

    public static int createContact(String name, String addr) {
        return MrMailboxCreateContact(m_hMailbox, name, addr);
    }

    public static void blockContact(int id, int block) {
        MrMailboxBlockContact(m_hMailbox, id, block);
    }

    public native static String getContactEncrInfo(int contact_id);

    public static int deleteContact(int id) {
        return MrMailboxDeleteContact(m_hMailbox, id);
    }

    public static int addAddressBook(String adrbook) {
        return MrMailboxAddAddressBook(m_hMailbox, adrbook);
    }

    private native static long    MrMailboxGetContact        (long hMailbox, int id);// returns hContact which must be unref'd after usage
    private native static int     MrMailboxCreateContact     (long hMailbox, String name, String addr);
    private native static void    MrMailboxBlockContact      (long hMailbox, int id, int block);
    private native static int     MrMailboxDeleteContact     (long hMailbox, int id); // returns 0 if the contact could not be deleted (eg. it is in use, maybe by deaddrop)
    private native static int     MrMailboxAddAddressBook    (long hMailbox, String adrbook);


    // chats
    public final static int MR_GCL_ARCHIVED_ONLY = 0x01;
    public final static int MR_GCL_NO_SPECIALS = 0x02;
    public static MrChatlist getChatlist(int listflags, String query, int queryId) {
        return new MrChatlist(MrMailboxGetChatlist(m_hMailbox, listflags, query, queryId));
    }

    public static MrChat getChat(int chat_id) {
        return new MrChat(MrMailboxGetChat(m_hMailbox, chat_id));
    }

    public native static void   markseenMsgs       (int msg_ids[]);
    public native static void   marknoticedChat    (int chat_id);
    public native static void   marknoticedContact (int contact_id);
    public native static void   archiveChat        (int chat_id, int archive);

    public native static int getChatIdByContactId (int contact_id);
    public native static int createChatByContactId(int contact_id);
    public native static int createChatByMsgId    (int msg_id);

    public native static int createGroupChat       (boolean verified, String name);
    public native static int isContactInChat       (int chat_id, int contact_id);
    public native static int addContactToChat      (int chat_id, int contact_id);
    public native static int removeContactFromChat (int chat_id, int contact_id);
    public native static int setChatName           (int chat_id, String name);
    public native static int setChatProfileImage   (int chat_id, String name);

    public final static int MR_GCM_ADDDAYMARKER = 0x01;
    public native static int[] getChatMsgs(int chat_id, int flags, int marker1before);

    public native static int[] searchMsgs(int chat_id, String query);

    public native static int[] getFreshMsgs();

    public native static int[] getChatMedia(int chat_id, int msg_type, int or_msg_type);
    public native static int getNextMedia(int msg_id, int dir);
    public native static int[] getChatContacts(int chat_id);
    public native static void deleteChat(int chat_id);

    private native static long    MrMailboxGetChatlist       (long hMailbox, int listflags, String query, int queryId); // returns hChatlist which must be unref'd after usage
    private native static long    MrMailboxGetChat           (long hMailbox, int chat_id); // return hChat which must be unref'd after usage


    // msgs
    public static MrMsg getMsg(int msg_id) {
        return new MrMsg(MrMailboxGetMsg(m_hMailbox, msg_id));
    }

    public static String getMsgInfo(int id) {
        return MrMailboxGetMsgInfo(m_hMailbox, id);
    }

    public static native int getFreshMsgCount(int chat_id);

    public native static void deleteMsgs(int msg_ids[]);
    public native static void forwardMsgs(int msg_ids[], int chat_ids);

    public native static int sendTextMsg(int chat_id, String text);
    public native static int sendVcardMsg(int chat_id, int contact_id);
    public native static int sendMediaMsg(int chat_id, int type, String file, String mime, int w, int h, int time_ms, String author, String trackname);

    private native static long    MrMailboxGetMsg            (long hMailbox, int id); // return hMsg which must be unref'd after usage
    private native static String  MrMailboxGetMsgInfo        (long hMailbox, int id);

    // out-of-band verification
    public final static int MR_QR_ASK_VERIFYCONTACT        = 200;
    public final static int MR_QR_ASK_VERIFYGROUP          = 202;
    public final static int MR_QR_FPR_OK                   = 210;
    public final static int MR_QR_FPR_MISMATCH             = 220;
    public final static int MR_QR_FPR_WITHOUT_ADDR         = 230;
    public final static int MR_QR_ADDR                     = 320;
    public final static int MR_QR_TEXT                     = 330;
    public final static int MR_QR_URL                      = 332;
    public final static int MR_QR_ERROR                    = 400;
    public native static int checkQrCPtr(String qr);
    public static MrLot checkQr(String qr) { return new MrLot(checkQrCPtr(qr)); }

    public native static String getSecurejoinQr(int chat_id);
    public native static int joinSecurejoin(String qr);

    // static
    public native static String   MrGetVersionStr            ();
    public native static String   CPtr2String                (long hString); // get strings eg. from data1 from the callback
    public native static long     String2CPtr                (String str);

    /* receive events
     **********************************************************************************************/

    public final static int MR_EVENT_INFO                     =  100;
    public final static int MR_EVENT_WARNING                  =  300;
    public final static int MR_EVENT_ERROR                    =  400; // INFO and WARNING are blocked in the mrwrapper.c

    public final static int MR_EVENT_MSGS_CHANGED             = 2000;
    public final static int MR_EVENT_INCOMING_MSG             = 2005;
    public final static int MR_EVENT_MSG_DELIVERED            = 2010;
    public final static int MR_EVENT_MSG_READ                 = 2015;

    public final static int MR_EVENT_CHAT_MODIFIED            = 2020;

    public final static int MR_EVENT_CONTACTS_CHANGED         = 2030;

    public final static int MR_EVENT_CONFIGURE_PROGRESS       = 2041;

    public final static int MR_EVENT_IMEX_PROGRESS            = 2051;
    public final static int MR_EVENT_IMEX_FILE_WRITTEN        = 2052;

    public final static int MR_EVENT_SECUREJOIN_INVITER_PROGRESS = 2060;
    public final static int MR_EVENT_SECUREJOIN_JOINER_PROGRESS  = 2061;

    public final static int MR_EVENT_IS_OFFLINE               = 2081;
    public final static int MR_EVENT_GET_STRING               = 2091;
    public final static int MR_EVENT_GET_QUANTITIY_STRING     = 2092;
    public final static int MR_EVENT_HTTP_GET                 = 2100;

    public static final Object m_lastErrorLock = new Object();
    public static int          m_lastErrorCode = 0;
    public static String       m_lastErrorString = "";
    public static boolean      m_showNextErrorAsToast = true;

    public static long MrCallback(final int event, final long data1, final long data2) // this function is called from within the C-wrapper
    {
        switch(event) {
            case MR_EVENT_CONFIGURE_PROGRESS:
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.configureProgress, (int)data1);
                    }
                });
                return 0;

            case MR_EVENT_IMEX_PROGRESS:
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.imexProgress, (int)data1);
                    }
                });
                return 0;

            case MR_EVENT_IMEX_FILE_WRITTEN: {
                final String fileName = CPtr2String(data1);
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.imexFileWritten, fileName);
                    }
                });
                }
                return 0;

            case MR_EVENT_SECUREJOIN_INVITER_PROGRESS:
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.secureJoinInviterProgress, (int)data1, (int)data2);
                    }
                });
                return 0;

            case MR_EVENT_SECUREJOIN_JOINER_PROGRESS:
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.secureJoinJoinerProgress, (int)data1, (int)data2);
                    }
                });
                return 0;

            case MR_EVENT_MSGS_CHANGED:
            case MR_EVENT_INCOMING_MSG:
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
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
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.messagesSentOrRead, event, (int)data1, (int)data2);
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
                    }
                });
                return 0;

            case MR_EVENT_CONTACTS_CHANGED:
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.contactsDidLoaded, (int)data1);
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.blockedUsersDidLoaded);
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
                    }
                });
                return 0;

            case MR_EVENT_CHAT_MODIFIED:
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces,
                                UPDATE_MASK_NAME|UPDATE_MASK_CHAT_NAME|
                                UPDATE_MASK_CHAT_MEMBERS|UPDATE_MASK_AVATAR);
                    }
                });
                return 0;

            case MR_EVENT_INFO:
                Log.i("DeltaChat", CPtr2String(data2));
                break;

            case MR_EVENT_WARNING:
                Log.w("DeltaChat", CPtr2String(data2));
                break;

            case MR_EVENT_ERROR:
                Log.e("DeltaChat", CPtr2String(data2));
                synchronized (m_lastErrorLock) {
                    m_lastErrorCode   = (int)data1;
                    m_lastErrorString = CPtr2String(data2);
                }
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (m_lastErrorLock) {
                            if( m_showNextErrorAsToast ) {
                                if(ForegroundDetector.getInstance().isForeground()) {
                                    AndroidUtilities.showHint(ApplicationLoader.applicationContext, m_lastErrorString);
                                }
                            }
                            m_showNextErrorAsToast = true;
                        }
                    }
                });
                return 0;

            case MR_EVENT_GET_STRING:
                String s;
                switch( (int)data1 ) {
                    // the string-IDs are defined in the backend; as this is the only place where they're used, there is no benefit in creating an enum or sth. like that.
                    case  1: s = ApplicationLoader.applicationContext.getString(R.string.NoMessages); break;
                    case  2: s = ApplicationLoader.applicationContext.getString(R.string.FromSelf); break;
                    case  3: s = ApplicationLoader.applicationContext.getString(R.string.Draft); break;
                    case  7: s = ApplicationLoader.applicationContext.getString(R.string.AttachVoiceMessage); break;
                    case  8: s = ApplicationLoader.applicationContext.getString(R.string.Deaddrop); break;
                    case  9: s = ApplicationLoader.applicationContext.getString(R.string.AttachPhoto); break;
                    case 10: s = ApplicationLoader.applicationContext.getString(R.string.AttachVideo); break;
                    case 11: s = ApplicationLoader.applicationContext.getString(R.string.Audio); break;
                    case 12: s = ApplicationLoader.applicationContext.getString(R.string.AttachDocument); break;
                    case 13: s = ApplicationLoader.applicationContext.getString(R.string.DefaultStatusText); break;
                    case 14: s = ApplicationLoader.applicationContext.getString(R.string.MsgNewGroupDraft); break;
                    case 15: s = ApplicationLoader.applicationContext.getString(R.string.MsgGroupNameChanged); break;
                    case 16: s = ApplicationLoader.applicationContext.getString(R.string.MsgGroupImageChanged); break;
                    case 17: s = ApplicationLoader.applicationContext.getString(R.string.MsgMemberAddedToGroup); break;
                    case 18: s = ApplicationLoader.applicationContext.getString(R.string.MsgMemberRemovedFromToGroup); break;
                    case 19: s = ApplicationLoader.applicationContext.getString(R.string.MsgGroupLeft); break;
                    case 20: s = ApplicationLoader.applicationContext.getString(R.string.Error); break;
                    case 21: s = ApplicationLoader.applicationContext.getString(R.string.ErrSelfNotInGroup); break;
                    case 22: s = ApplicationLoader.applicationContext.getString(R.string.NoNetwork); break;
                    case 23: s = ApplicationLoader.applicationContext.getString(R.string.AttachGif); break;
                    case 24: s = ApplicationLoader.applicationContext.getString(R.string.EncryptedMessage); break;
                    case 25: s = ApplicationLoader.applicationContext.getString(R.string.EncrinfoE2EAvailable); break;
                    case 27: s = ApplicationLoader.applicationContext.getString(R.string.EncrinfoTransport); break;
                    case 28: s = ApplicationLoader.applicationContext.getString(R.string.EncrinfoNone); break;
                    case 29: s = ApplicationLoader.applicationContext.getString(R.string.CannotDecryptBody); break;
                    case 30: s = ApplicationLoader.applicationContext.getString(R.string.EncrinfoFingerprints); break;
                    case 31: s = ApplicationLoader.applicationContext.getString(R.string.ReadReceipt); break;
                    case 32: s = ApplicationLoader.applicationContext.getString(R.string.ReadReceiptMailBody); break;
                    case 33: s = ApplicationLoader.applicationContext.getString(R.string.MsgGroupImageDeleted); break;
                    case 34: s = ApplicationLoader.applicationContext.getString(R.string.E2EEncryptionPreferred); break;
                    case 40: s = ApplicationLoader.applicationContext.getString(R.string.ArchivedChats); break;
                    case 42: s = ApplicationLoader.applicationContext.getString(R.string.AutocryptSetupMessageSubject); break;
                    case 43: s = ApplicationLoader.applicationContext.getString(R.string.AutocryptSetupMessageGeneralBody); break;
                    case 50: s = ApplicationLoader.applicationContext.getString(R.string.SelfTalkSubtitle); break;
                    default: s = null; break;
                }
                return String2CPtr(s);

            case MR_EVENT_GET_QUANTITIY_STRING:
                String sp = "ErrQtyStrBadId";
                switch( (int)data1 ) {
                    // the string-IDs are defined in the backend; as this is the only place where they're used, there is no benefit in creating an enum or sth. like that.
                    case 4: sp = ApplicationLoader.applicationContext.getResources().getQuantityString(R.plurals.Members, (int)data2, (int)data2); break;
                    case 6: sp = ApplicationLoader.applicationContext.getResources().getQuantityString(R.plurals.Contacts, (int)data2, (int)data2); break;
                }
                return String2CPtr(sp);

            case MR_EVENT_IS_OFFLINE:
                return ApplicationLoader.isNetworkOnline()? 0 : 1;

            case MR_EVENT_HTTP_GET:
                String httpContent = null;
                try {
                    URL url = new URL(CPtr2String(data1));
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    try {
                        InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());

                        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
                        StringBuilder total = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) {
                            total.append(line).append('\n');
                        }
                        httpContent = total.toString();
                    } finally {
                        urlConnection.disconnect();
                    }
                }
                catch(Exception e) {}
                return String2CPtr(httpContent);
        }
        return 0;
    }


    /* additional functions that are not 1:1 available in the backend
     **********************************************************************************************/

    public static void log_i(String tag, String msg)
    {
        Log.i(tag, msg);
    }

    public native static int     getCurrentTime             ();

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

    public static String getInviteText() {
        String url = "https://delta.chat";
        String email = getConfig("addr", "");
        String text = String.format(ApplicationLoader.applicationContext.getString(R.string.InviteText), url, email);
        return text;
    }

    public static TLRPC.User getUser(Integer id) {
        TLRPC.User u = new TLRPC.User(); // legacy function, information should be loaded as needed by the caller
        u.id = id;
        return u;
    }

    public static void cancelTyping(int action, long dialog_id) {
    }

    public static void sendTyping(final long dialog_id, final int action, int classGuid) {
    }

    public static void markMessageContentAsRead(final MessageObject messageObject) {
    }

    public static boolean isDialogMuted(long dialog_id) {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
        int mute_type = preferences.getInt("notify2_" + dialog_id, 0);
        if (mute_type == 2) {
            return true;
        } else if (mute_type == 3) {
            int mute_until = preferences.getInt("notifyuntil_" + dialog_id, 0);
            if (mute_until >= MrMailbox.getCurrentTime()) {
                return true;
            }
        }
        return false;
    }

    // legacy update masks
    public static final int UPDATE_MASK_NAME = 1;
    public static final int UPDATE_MASK_AVATAR = 2;
    public static final int UPDATE_MASK_STATUS = 4;
    public static final int UPDATE_MASK_CHAT_AVATAR = 8;
    public static final int UPDATE_MASK_CHAT_NAME = 16;
    public static final int UPDATE_MASK_CHAT_MEMBERS = 32;
    public static final int UPDATE_MASK_SELECT_DIALOG = 512;
    public static final int UPDATE_MASK_NEW_MESSAGE = 2048;
    public static final int UPDATE_MASK_SEND_STATE = 4096;
}
