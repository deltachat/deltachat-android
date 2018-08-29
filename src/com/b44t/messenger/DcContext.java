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


public class DcContext {

    public final static int DC_EVENT_INFO = 100;
    public final static int DC_EVENT_WARNING = 300;
    public final static int DC_EVENT_ERROR = 400;
    public final static int DC_EVENT_MSGS_CHANGED = 2000;
    public final static int DC_EVENT_INCOMING_MSG = 2005;
    public final static int DC_EVENT_MSG_DELIVERED = 2010;
    public final static int DC_EVENT_MSG_FAILED = 2012;
    public final static int DC_EVENT_MSG_READ = 2015;
    public final static int DC_EVENT_CHAT_MODIFIED = 2020;
    public final static int DC_EVENT_CONTACTS_CHANGED = 2030;
    public final static int DC_EVENT_CONFIGURE_PROGRESS = 2041;
    public final static int DC_EVENT_IMEX_PROGRESS = 2051;
    public final static int DC_EVENT_IMEX_FILE_WRITTEN = 2052;
    public final static int DC_EVENT_SECUREJOIN_INVITER_PROGRESS = 2060;
    public final static int DC_EVENT_SECUREJOIN_JOINER_PROGRESS = 2061;
    public final static int DC_EVENT_IS_OFFLINE = 2081;
    public final static int DC_EVENT_GET_STRING = 2091;
    public final static int DC_EVENT_GET_QUANTITIY_STRING = 2092;
    public final static int DC_EVENT_HTTP_GET = 2100;

    public final static int DC_IMEX_EXPORT_SELF_KEYS = 1;
    public final static int DC_IMEX_IMPORT_SELF_KEYS = 2;
    public final static int DC_IMEX_EXPORT_BACKUP = 11;
    public final static int DC_IMEX_IMPORT_BACKUP = 12;

    public final static int DC_GCL_VERIFIED_ONLY = 1;
    public final static int DC_GCL_ADD_SELF = 2;
    public final static int DC_GCL_ARCHIVED_ONLY = 0x01;
    public final static int DC_GCL_NO_SPECIALS = 0x02;

    public final static int DC_GCM_ADDDAYMARKER = 0x01;

    public final static int DC_QR_ASK_VERIFYCONTACT = 200;
    public final static int DC_QR_ASK_VERIFYGROUP = 202;
    public final static int DC_QR_FPR_OK = 210;
    public final static int DC_QR_FPR_MISMATCH = 220;
    public final static int DC_QR_FPR_WITHOUT_ADDR = 230;
    public final static int DC_QR_ADDR = 320;
    public final static int DC_QR_TEXT = 330;
    public final static int DC_QR_URL = 332;
    public final static int DC_QR_ERROR = 400;

    public DcContext(String osName) {
        handleEvent(0,0,0); // call handleEvent() to make sure it is not optimized away and JNI won't find it
        m_hContext = DcContextNew(osName);
    }

    public native int open(String dbfile);
    public native void close();
    public native String getBlobdir();
    public native void configure();
    public native void stopOngoingProcess();
    public native int isConfigured();
    public native void performJobs();
    public native void fetch();
    public native void idle();
    public native void interruptIdle();
    public native void performSmtpJobs();
    public native void performSmtpIdle();
    public native void interruptSmtpIdle();
    public native void setConfig(String key, String value);
    public native void setConfigInt(String key, int value);
    public native String getConfig(String key, String def);
    public native int getConfigInt(String key, int def);
    public native String getInfo();
    public native String cmdline(String cmd);
    public native String initiateKeyTransfer();
    public native boolean continueKeyTransfer(int msg_id, String setup_code);
    public native void imex(int what, String dir);
    public native String imexHasBackup(String dir);
    public native int checkPassword(String pw);
    public native int[] getContacts(int flags, String query);
    public native int   getBlockedCount();
    public native int[] getBlockedContacts();
    public DcContact getContact(int contact_id) { return new DcContact(DcContextGetContact(m_hContext, contact_id)); }
    public native int createContact(String name, String addr);
    public native void blockContact(int id, int block);
    public native String getContactEncrInfo(int contact_id);
    public native int deleteContact(int id);
    public native int addAddressBook(String adrbook);
    public DcChatlist getChatlist(int listflags, String query, int queryId) { return new DcChatlist(DcContextGetChatlist(m_hContext, listflags, query, queryId)); }
    public DcChat getChat(int chat_id) { return new DcChat(DcContextGetChat(m_hContext, chat_id)); }
    public native void markseenMsgs(int msg_ids[]);
    public native void marknoticedChat(int chat_id);
    public native void marknoticedContact(int contact_id);
    public native void archiveChat(int chat_id, int archive);
    public native int getChatIdByContactId(int contact_id);
    public native int createChatByContactId(int contact_id);
    public native int createChatByMsgId(int msg_id);
    public native int createGroupChat(boolean verified, String name);
    public native int isContactInChat(int chat_id, int contact_id);
    public native int addContactToChat(int chat_id, int contact_id);
    public native int removeContactFromChat(int chat_id, int contact_id);
    public native void setDraft(int chat_id, String draft/*NULL=delete*/);
    public native int setChatName(int chat_id, String name);
    public native int setChatProfileImage(int chat_id, String name);
    public native int[] getChatMsgs(int chat_id, int flags, int marker1before);
    public native int[] searchMsgs(int chat_id, String query);
    public native int[] getFreshMsgs();
    public native int[] getChatMedia(int chat_id, int msg_type, int or_msg_type);
    public native int getNextMedia(int msg_id, int dir);
    public native int[] getChatContacts(int chat_id);
    public native void deleteChat(int chat_id);
    public DcMsg getMsg(int msg_id) { return new DcMsg(DcContextGetMsg(m_hContext, msg_id)); }
    public native String getMsgInfo(int id);
    public native int getFreshMsgCount(int chat_id);
    public native void deleteMsgs(int msg_ids[]);
    public native void forwardMsgs(int msg_ids[], int chat_ids);
    public native int sendTextMsg(int chat_id, String text);
    public native int sendVcardMsg(int chat_id, int contact_id);
    public native int sendMediaMsg(int chat_id, int type, String file, String mime, int w, int h, int time_ms, String author, String trackname);
    public native int checkQrCPtr(String qr);
    public DcLot checkQr(String qr) { return new DcLot(checkQrCPtr(qr)); }
    public native String getSecurejoinQr(int chat_id);
    public native int joinSecurejoin(String qr);

    // event handling - you should @Override this function in derived classes
    public long handleEvent(int event, long data1, long data2) {
        return 0;
    }

    // helper to get/return strings from/to handleEvent()
    public native static boolean data1IsString(int event);
    public native static boolean data2IsString(int event);
    public native static String dataToString(long hString);
    public native static long stringToData(String str);

    // working with raw c-data
    private long m_hContext = 0; // must not be renamed as referenced by JNI
    private native long DcContextNew(String osName);
    private native static long DcContextGetChatlist(long hContext, int listflags, String query, int queryId);
    private native static long DcContextGetChat(long hContext, int chat_id);
    private native static long DcContextGetMsg(long hMailbox, int id);
    private native static long DcContextGetContact(long hContext, int id);
}
