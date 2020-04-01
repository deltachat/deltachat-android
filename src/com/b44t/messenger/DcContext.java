package com.b44t.messenger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DcContext {

    public final static int DC_PREF_DEFAULT_MDNS_ENABLED = 1;
    public final static int DC_PREF_DEFAULT_TRIM_ENABLED = 0;
    public final static int DC_PREF_DEFAULT_TRIM_LENGTH  = 500;

    public final static int DC_EVENT_INFO                        = 100;
    public final static int DC_EVENT_WARNING                     = 300;
    public final static int DC_EVENT_ERROR                       = 400;
    public final static int DC_EVENT_ERROR_NETWORK               = 401;
    public final static int DC_EVENT_ERROR_SELF_NOT_IN_GROUP     = 410;
    public final static int DC_EVENT_MSGS_CHANGED                = 2000;
    public final static int DC_EVENT_INCOMING_MSG                = 2005;
    public final static int DC_EVENT_MSG_DELIVERED               = 2010;
    public final static int DC_EVENT_MSG_FAILED                  = 2012;
    public final static int DC_EVENT_MSG_READ                    = 2015;
    public final static int DC_EVENT_CHAT_MODIFIED               = 2020;
    public final static int DC_EVENT_CONTACTS_CHANGED            = 2030;
    public final static int DC_EVENT_LOCATION_CHANGED            = 2035;
    public final static int DC_EVENT_CONFIGURE_PROGRESS          = 2041;
    public final static int DC_EVENT_IMEX_PROGRESS               = 2051;
    public final static int DC_EVENT_IMEX_FILE_WRITTEN           = 2052;
    public final static int DC_EVENT_SECUREJOIN_INVITER_PROGRESS = 2060;
    public final static int DC_EVENT_SECUREJOIN_JOINER_PROGRESS  = 2061;

    public final static int DC_IMEX_EXPORT_SELF_KEYS = 1;
    public final static int DC_IMEX_IMPORT_SELF_KEYS = 2;
    public final static int DC_IMEX_EXPORT_BACKUP    = 11;
    public final static int DC_IMEX_IMPORT_BACKUP    = 12;

    public final static int DC_GCL_VERIFIED_ONLY    = 1;
    public final static int DC_GCL_ADD_SELF         = 2;
    public final static int DC_GCL_ARCHIVED_ONLY    = 0x01;
    public final static int DC_GCL_NO_SPECIALS      = 0x02;
    public final static int DC_GCL_ADD_ALLDONE_HINT = 0x04;
    public final static int DC_GCL_FOR_FORWARDING   = 0x08;

    public final static int DC_GCM_ADDDAYMARKER = 0x01;

    public final static int DC_QR_ASK_VERIFYCONTACT = 200;
    public final static int DC_QR_ASK_VERIFYGROUP   = 202;
    public final static int DC_QR_FPR_OK            = 210;
    public final static int DC_QR_FPR_MISMATCH      = 220;
    public final static int DC_QR_FPR_WITHOUT_ADDR  = 230;
    public final static int DC_QR_ACCOUNT           = 250;
    public final static int DC_QR_ADDR              = 320;
    public final static int DC_QR_TEXT              = 330;
    public final static int DC_QR_URL               = 332;
    public final static int DC_QR_ERROR             = 400;

    public final static int DC_LP_AUTH_OAUTH2          =     0x2;
    public final static int DC_LP_AUTH_NORMAL          =     0x4;
    public final static int DC_LP_IMAP_SOCKET_STARTTLS =   0x100;
    public final static int DC_LP_IMAP_SOCKET_SSL      =   0x200;
    public final static int DC_LP_IMAP_SOCKET_PLAIN    =   0x400;
    public final static int DC_LP_SMTP_SOCKET_STARTTLS = 0x10000;
    public final static int DC_LP_SMTP_SOCKET_SSL      = 0x20000;
    public final static int DC_LP_SMTP_SOCKET_PLAIN    = 0x40000;

    public final static int DC_SHOW_EMAILS_OFF               = 0;
    public final static int DC_SHOW_EMAILS_ACCEPTED_CONTACTS = 1;
    public final static int DC_SHOW_EMAILS_ALL               = 2;

    public final static int DC_EMPTY_MVBOX           = 0x01;
    public final static int DC_EMPTY_INBOX           = 0x02;

    public DcContext(String osName) {
        handleEvent(0,0,0); // call handleEvent() to make sure it is not optimized away and JNI won't find it
        contextCPtr = createContextCPtr(osName);
    }

    public native int          open                 (String dbfile);
    public native void         close                ();
    public native void         setStockTranslation  (int stockId, String translation);
    public native String       getBlobdir           ();
    public native void         configure            ();
    public native void         stopOngoingProcess   ();
    public native int          isConfigured         ();

    public native void         performImapJobs      ();
    public native void         performImapFetch     ();
    public native void         performImapIdle      ();
    public native void         interruptImapIdle    ();

    public native void         performMvboxJobs     ();
    public native void         performMvboxFetch    ();
    public native void         performMvboxIdle     ();
    public native void         interruptMvboxIdle   ();

    public native void         performSentboxJobs   ();
    public native void         performSentboxFetch  ();
    public native void         performSentboxIdle   ();
    public native void         interruptSentboxIdle ();

    public native void         performSmtpJobs      ();
    public native void         performSmtpIdle      ();

    public native void         maybeNetwork         ();
    public native void         setConfig            (String key, String value);
    public void                setConfigInt         (String key, int value) { setConfig(key, Integer.toString(value)); }
    public native boolean      setConfigFromQr      (String qr);
    public native String       getConfig            (String key);
    public int                 getConfigInt         (String key) { try{return Integer.parseInt(getConfig(key));} catch(Exception e) {} return 0; }
    @Deprecated public String  getConfig            (String key, String def) { return getConfig(key); }
    @Deprecated public int     getConfigInt         (String key, int def) { return getConfigInt(key); }
    public native String       getInfo              ();
    public native String       getOauth2Url         (String addr, String redirectUrl);
    public native String       initiateKeyTransfer  ();
    public native boolean      continueKeyTransfer  (int msg_id, String setup_code);
    public native void         imex                 (int what, String dir);
    public native String       imexHasBackup        (String dir);
    public native void         emptyServer          (int flags);
    public native boolean      mayBeValidAddr       (String addr);
    public native int          lookupContactIdByAddr(String addr);
    public native int[]        getContacts          (int flags, String query);
    public native int          getBlockedCount      ();
    public native int[]        getBlockedContacts   ();
    public @NonNull DcContact  getContact           (int contact_id) { return new DcContact(getContactCPtr(contact_id)); }
    public native int          createContact        (String name, String addr);
    public native void         blockContact         (int id, int block);
    public native String       getContactEncrInfo   (int contact_id);
    public native boolean      deleteContact        (int id);
    public native int          addAddressBook       (String adrbook);
    public @NonNull DcChatlist getChatlist          (int listflags, String query, int queryId) { return new DcChatlist(getChatlistCPtr(listflags, query, queryId)); }
    public @NonNull DcChat     getChat              (int chat_id) { return new DcChat(getChatCPtr(chat_id)); }
    public native void         markseenMsgs         (int msg_ids[]);
    public native void         marknoticedChat      (int chat_id);
    public native void         marknoticedAllChats  ();
    public native void         marknoticedContact   (int contact_id);
    public native void         setChatVisibility    (int chat_id, int visibility);
    public native int          getChatIdByContactId (int contact_id);
    public native int          createChatByContactId(int contact_id);
    public native int          createChatByMsgId    (int msg_id);
    public native int          createGroupChat      (boolean verified, String name);
    public native boolean      isContactInChat      (int chat_id, int contact_id);
    public native int          addContactToChat     (int chat_id, int contact_id);
    public native int          removeContactFromChat(int chat_id, int contact_id);
    public native void         setDraft             (int chat_id, DcMsg msg/*null=delete*/);
    public @Nullable DcMsg     getDraft             (int chat_id) { return new DcMsg(getDraftCPtr(chat_id)); }
    public native int          setChatName          (int chat_id, String name);
    public native int          setChatProfileImage  (int chat_id, String name);
    public native int[]        getChatMsgs          (int chat_id, int flags, int marker1before);
    public native int[]        searchMsgs           (int chat_id, String query);
    public native int[]        getFreshMsgs         ();
    public native int[]        getChatMedia         (int chat_id, int type1, int type2, int type3);
    public native int          getNextMedia         (int msg_id, int dir, int type1, int type2, int type3);
    public native int[]        getChatContacts      (int chat_id);
    public native void         deleteChat           (int chat_id);
    public @NonNull DcMsg      getMsg               (int msg_id) { return new DcMsg(getMsgCPtr(msg_id)); }
    public native String       getMsgInfo           (int id);
    public native int          getFreshMsgCount     (int chat_id);
    public native int          estimateDeletionCount(boolean from_server, long seconds);
    public native void         deleteMsgs           (int msg_ids[]);
    public native void         forwardMsgs          (int msg_ids[], int chat_id);
    public native int          prepareMsg           (int chat_id, DcMsg msg);
    public native int          sendMsg              (int chat_id, DcMsg msg);
    public native int          sendTextMsg          (int chat_id, String text);
    public native int          addDeviceMsg         (String label, DcMsg msg);
    public native boolean      wasDeviceMsgEverAdded(String label);
    public native void         updateDeviceChats    ();
    public @NonNull DcLot      checkQr              (String qr) { return new DcLot(checkQrCPtr(qr)); }
    public native String       getSecurejoinQr      (int chat_id);
    public native int          joinSecurejoin       (String qr);
    public native void         sendLocationsToChat  (int chat_id, int seconds);
    public native boolean      isSendingLocationsToChat(int chat_id);
    public @NonNull DcArray    getLocations         (int chat_id, int contact_id, long timestamp_start, long timestamp_end) { return new DcArray(getLocationsCPtr(chat_id, contact_id, timestamp_start, timestamp_end)); }
    public native void         deleteAllLocations   ();
    public @Nullable DcProvider getProviderFromEmail (String email) { long cptr = getProviderFromEmailCPtr(email); return cptr!=0 ? new DcProvider(cptr) : null; }

    /**
     * @return true if at least one chat has location streaming enabled
     */
    public native boolean      setLocation          (float latitude, float longitude, float accuracy);

    // event handling - you should @Override this function in derived classes
    public long handleEvent(int event, long data1, long data2) {
        return 0;
    }

    // helper to get/return strings from/to handleEvent()
    public native static boolean data1IsString(int event);
    public native static boolean data2IsString(int event);
    public native static String  dataToString (long data);

    // working with raw c-data
    private long        contextCPtr;     // CAVE: the name is referenced in the JNI
    private native long createContextCPtr(String osName);
    public  native long createMsgCPtr    (int viewtype);
    private native long getChatlistCPtr  (int listflags, String query, int queryId);
    private native long getChatCPtr      (int chat_id);
    private native long getMsgCPtr       (int id);
    private native long getDraftCPtr    (int id);
    private native long getContactCPtr   (int id);
    private native long getLocationsCPtr (int chat_id, int contact_id, long timestamp_start, long timestamp_end);
    private native long checkQrCPtr      (String qr);
    private native long getProviderFromEmailCPtr  (String addr);
}
