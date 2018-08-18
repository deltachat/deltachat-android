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
 ******************************************************************************/


package com.b44t.messenger;


import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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

    public static void init () {
        m_hContext = DcContextNew();
    }

    public native static int open(String dbfile);
    public native static void close();
    public native static String getBlobdir();
    public native static void configure();
    public native static void stopOngoingProcess();
    public native static int isConfigured();
    public native static void performJobs();
    public native static void fetch();
    public native static void idle();
    public native static void interruptIdle();
    public native static void performSmtpJobs();
    public native static void performSmtpIdle();
    public native static void interruptSmtpIdle();
    public native static void setConfig(String key, String value);
    public native static void setConfigInt(String key, int value);
    public native static String getConfig(String key, String def);
    public native static int getConfigInt(String key, int def);
    public native static String getInfo();
    public native static String cmdline(String cmd);
    public native static String initiateKeyTransfer();
    public native static boolean continueKeyTransfer(int msg_id, String setup_code);
    public native static void imex(int what, String dir);
    public native static String imexHasBackup(String dir);
    public native static int  checkPassword(String pw);
    public native static int[] getContacts(int flags, String query);
    public native static int   getBlockedCount();
    public native static int[] getBlockedContacts();
    public static DcContact getContact(int contact_id) { return new DcContact(DcContextGetContact(m_hContext, contact_id)); }
    public native static int createContact(String name, String addr);
    public native static void blockContact(int id, int block);
    public native static String getContactEncrInfo(int contact_id);
    public native static int deleteContact(int id);
    public native static int addAddressBook(String adrbook);
    public static DcChatlist getChatlist(int listflags, String query, int queryId) { return new DcChatlist(DcContextGetChatlist(m_hContext, listflags, query, queryId)); }
    public static DcChat getChat(int chat_id) { return new DcChat(DcContextGetChat(m_hContext, chat_id)); }
    public native static void markseenMsgs(int msg_ids[]);
    public native static void marknoticedChat(int chat_id);
    public native static void marknoticedContact(int contact_id);
    public native static void archiveChat(int chat_id, int archive);
    public native static int getChatIdByContactId(int contact_id);
    public native static int createChatByContactId(int contact_id);
    public native static int createChatByMsgId(int msg_id);
    public native static int createGroupChat(boolean verified, String name);
    public native static int isContactInChat(int chat_id, int contact_id);
    public native static int addContactToChat(int chat_id, int contact_id);
    public native static int removeContactFromChat(int chat_id, int contact_id);
    public native static void setDraft(int chat_id, String draft/*NULL=delete*/);
    public native static int setChatName(int chat_id, String name);
    public native static int setChatProfileImage(int chat_id, String name);
    public native static int[] getChatMsgs(int chat_id, int flags, int marker1before);
    public native static int[] searchMsgs(int chat_id, String query);
    public native static int[] getFreshMsgs();
    public native static int[] getChatMedia(int chat_id, int msg_type, int or_msg_type);
    public native static int getNextMedia(int msg_id, int dir);
    public native static int[] getChatContacts(int chat_id);
    public native static void deleteChat(int chat_id);
    public static DcMsg getMsg(int msg_id) { return new DcMsg(DcContextGetMsg(m_hContext, msg_id)); }
    public native static String getMsgInfo(int id);
    public native static int getFreshMsgCount(int chat_id);
    public native static void deleteMsgs(int msg_ids[]);
    public native static void forwardMsgs(int msg_ids[], int chat_ids);
    public native static int sendTextMsg(int chat_id, String text);
    public native static int sendVcardMsg(int chat_id, int contact_id);
    public native static int sendMediaMsg(int chat_id, int type, String file, String mime, int w, int h, int time_ms, String author, String trackname);
    public native static int checkQrCPtr(String qr);
    public static DcLot checkQr(String qr) { return new DcLot(checkQrCPtr(qr)); }
    public native static String getSecurejoinQr(int chat_id);
    public native static int joinSecurejoin(String qr);

    // working with raw c-data
    private static long m_hContext = 0; // must not be renamed as referenced by JNI
    private native static long DcContextNew();
    private native static long DcContextGetChatlist(long hContext, int listflags, String query, int queryId);
    private native static long DcContextGetChat(long hContext, int chat_id);
    private native static long DcContextGetMsg(long hMailbox, int id);
    private native static long DcContextGetContact(long hContext, int id);
    public native static String CPtr2String(long hString); // get strings eg. from data1 from the callback
    public native static long String2CPtr(String str);

    // event handling
    public static final Object m_lastErrorLock = new Object();
    public static int m_lastErrorCode = 0;
    public static String m_lastErrorString = "";
    public static boolean m_showNextErrorAsToast = true;

    public static long DcCallback(final int event, final long data1, final long data2) // this function is called from within the C-wrapper
    {
        switch(event) {
            case DC_EVENT_INFO:
                Log.i("DeltaChat", CPtr2String(data2));
                break;

            case DC_EVENT_WARNING:
                Log.w("DeltaChat", CPtr2String(data2));
                break;

            case DC_EVENT_ERROR:
                Log.e("DeltaChat", CPtr2String(data2));
                synchronized (m_lastErrorLock) {
                    m_lastErrorCode   = (int)data1;
                    m_lastErrorString = CPtr2String(data2);
                }
                /*AndroidUtilities.runOnUIThread(new Runnable() {
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
                });*/
                return 0;

            case DC_EVENT_HTTP_GET:
                String httpContent = null;
                try {
                    URL url = new URL(CPtr2String(data1));
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    try {
                        urlConnection.setConnectTimeout(10*1000);
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
                catch(Exception e) {
                    e.printStackTrace();
                }
                return String2CPtr(httpContent);
        }
        return 0;
    }
}
