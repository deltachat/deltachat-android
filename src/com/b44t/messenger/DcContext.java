package com.b44t.messenger;

public class DcContext {

    public final static int DC_PREF_DEFAULT_MDNS_ENABLED = 1;
    public final static int DC_PREF_DEFAULT_TRIM_ENABLED = 0;
    public final static int DC_PREF_DEFAULT_TRIM_LENGTH  = 500;

    public final static int DC_EVENT_INFO                        = 100;
    public final static int DC_EVENT_WARNING                     = 300;
    public final static int DC_EVENT_ERROR                       = 400;
    public final static int DC_EVENT_ERROR_SELF_NOT_IN_GROUP     = 410;
    public final static int DC_EVENT_MSGS_CHANGED                = 2000;
    public final static int DC_EVENT_REACTIONS_CHANGED           = 2001;
    public final static int DC_EVENT_INCOMING_MSG                = 2005;
    public final static int DC_EVENT_MSGS_NOTICED                = 2008;
    public final static int DC_EVENT_MSG_DELIVERED               = 2010;
    public final static int DC_EVENT_MSG_FAILED                  = 2012;
    public final static int DC_EVENT_MSG_READ                    = 2015;
    public final static int DC_EVENT_CHAT_MODIFIED               = 2020;
    public final static int DC_EVENT_CHAT_EPHEMERAL_TIMER_MODIFIED = 2021;
    public final static int DC_EVENT_CONTACTS_CHANGED            = 2030;
    public final static int DC_EVENT_LOCATION_CHANGED            = 2035;
    public final static int DC_EVENT_CONFIGURE_PROGRESS          = 2041;
    public final static int DC_EVENT_IMEX_PROGRESS               = 2051;
    public final static int DC_EVENT_IMEX_FILE_WRITTEN           = 2052;
    public final static int DC_EVENT_SECUREJOIN_INVITER_PROGRESS = 2060;
    public final static int DC_EVENT_SECUREJOIN_JOINER_PROGRESS  = 2061;
    public final static int DC_EVENT_CONNECTIVITY_CHANGED        = 2100;
    public final static int DC_EVENT_SELFAVATAR_CHANGED          = 2110;
    public final static int DC_EVENT_WEBXDC_STATUS_UPDATE        = 2120;

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
    public final static int DC_QR_BACKUP            = 251;
    public final static int DC_QR_WEBRTC            = 260;
    public final static int DC_QR_ADDR              = 320;
    public final static int DC_QR_TEXT              = 330;
    public final static int DC_QR_URL               = 332;
    public final static int DC_QR_ERROR             = 400;
    public final static int DC_QR_WITHDRAW_VERIFYCONTACT = 500;
    public final static int DC_QR_WITHDRAW_VERIFYGROUP   = 502;
    public final static int DC_QR_REVIVE_VERIFYCONTACT   = 510;
    public final static int DC_QR_REVIVE_VERIFYGROUP     = 512;
    public final static int DC_QR_LOGIN             = 520;

    public final static int DC_LP_AUTH_OAUTH2          =     0x2;
    public final static int DC_LP_AUTH_NORMAL          =     0x4;

    public final static int DC_SOCKET_AUTO     = 0;
    public final static int DC_SOCKET_SSL      = 1;
    public final static int DC_SOCKET_STARTTLS = 2;
    public final static int DC_SOCKET_PLAIN    = 3;

    public final static int DC_SHOW_EMAILS_OFF               = 0;
    public final static int DC_SHOW_EMAILS_ACCEPTED_CONTACTS = 1;
    public final static int DC_SHOW_EMAILS_ALL               = 2;

    public final static int DC_MEDIA_QUALITY_BALANCED = 0;
    public final static int DC_MEDIA_QUALITY_WORSE    = 1;

    public final static int DC_DECISION_START_CHAT = 0;
    public final static int DC_DECISION_BLOCK      = 1;
    public final static int DC_DECISION_NOT_NOW    = 2;

    public final static int DC_CONNECTIVITY_NOT_CONNECTED = 1000;
    public final static int DC_CONNECTIVITY_CONNECTING = 2000;
    public final static int DC_CONNECTIVITY_WORKING = 3000;
    public final static int DC_CONNECTIVITY_CONNECTED = 4000;

    // when using DcAccounts, use DcAccounts.addAccount() instead
    public DcContext(String osName, String dbfile) {
        contextCPtr = createContextCPtr(osName, dbfile);
    }

    public DcContext(long contextCPtr) {
        this.contextCPtr = contextCPtr;
    }

    public boolean isOk() {
        return contextCPtr != 0;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (contextCPtr != 0) {
            unrefContextCPtr();
            contextCPtr = 0;
        }
    }

    public native int          getAccountId         ();

    // when using DcAccounts, use DcAccounts.getEventEmitter() instead
    public DcEventEmitter      getEventEmitter      () { return new DcEventEmitter(getEventEmitterCPtr()); }

    public native void         setStockTranslation  (int stockId, String translation);
    public native String       getBlobdir           ();
    public native String       getLastError         ();
    public native void         configure            ();
    public native void         stopOngoingProcess   ();
    public native int          isConfigured         ();
    public native boolean      open                 (String passphrase);
    public native boolean      isOpen               ();

    // when using DcAccounts, use DcAccounts.startIo() instead
    public native void         startIo              ();

    // when using DcAccounts, use DcAccounts.stopIo() instead
    public native void         stopIo               ();

    // when using DcAccounts, use DcAccounts.maybeNetwork() instead
    public native void         maybeNetwork         ();

    public native void         setConfig            (String key, String value);
    public void                setConfigInt         (String key, int value) { setConfig(key, Integer.toString(value)); }
    public native boolean      setConfigFromQr      (String qr);
    public native String       getConfig            (String key);
    public int                 getConfigInt         (String key) { try{return Integer.parseInt(getConfig(key));} catch(Exception e) {} return 0; }
    @Deprecated public String  getConfig            (String key, String def) { return getConfig(key); }
    @Deprecated public int     getConfigInt         (String key, int def) { return getConfigInt(key); }
    public native String       getInfo              ();
    public native int          getConnectivity      ();
    public native String       getConnectivityHtml  ();
    public native String       getOauth2Url         (String addr, String redirectUrl);
    public native String       initiateKeyTransfer  ();
    public native boolean      continueKeyTransfer  (int msg_id, String setup_code);
    public native void         imex                 (int what, String dir);
    public native String       imexHasBackup        (String dir);
    public DcBackupProvider    newBackupProvider    () { return new DcBackupProvider(newBackupProviderCPtr()); }
    public native boolean      receiveBackup        (String qr);
    public native boolean      mayBeValidAddr       (String addr);
    public native int          lookupContactIdByAddr(String addr);
    public native int[]        getContacts          (int flags, String query);
    public native int[]        getBlockedContacts   ();
    public DcContact           getContact           (int contact_id) { return new DcContact(getContactCPtr(contact_id)); }
    public native int          createContact        (String name, String addr);
    public native void         blockContact         (int id, int block);
    public native String       getContactEncrInfo   (int contact_id);
    public native boolean      deleteContact        (int id);
    public native int          addAddressBook       (String adrbook);
    public DcChatlist          getChatlist          (int listflags, String query, int queryId) { return new DcChatlist(getAccountId(), getChatlistCPtr(listflags, query, queryId)); }
    public DcChat              getChat              (int chat_id) { return new DcChat(getAccountId(), getChatCPtr(chat_id)); }
    public native String       getChatEncrInfo      (int chat_id);
    public native void         markseenMsgs         (int msg_ids[]);
    public native void         marknoticedChat      (int chat_id);
    public native void         setChatVisibility    (int chat_id, int visibility);
    public native int          getChatIdByContactId (int contact_id);
    public native int          createChatByContactId(int contact_id);
    public native int          createGroupChat      (boolean verified, String name);
    public native int          createBroadcastList  ();
    public native boolean      isContactInChat      (int chat_id, int contact_id);
    public native int          addContactToChat     (int chat_id, int contact_id);
    public native int          removeContactFromChat(int chat_id, int contact_id);
    public native void         setDraft             (int chat_id, DcMsg msg/*null=delete*/);
    public DcMsg               getDraft             (int chat_id) { return new DcMsg(getDraftCPtr(chat_id)); }
    public native int          setChatName          (int chat_id, String name);
    public native int          setChatProfileImage  (int chat_id, String name);
    public native int[]        getChatMsgs          (int chat_id, int flags, int marker1before);
    public native int[]        searchMsgs           (int chat_id, String query);
    public native int[]        getFreshMsgs         ();
    public native int[]        getChatMedia         (int chat_id, int type1, int type2, int type3);
    public native int          getNextMedia         (int msg_id, int dir, int type1, int type2, int type3);
    public native int[]        getChatContacts      (int chat_id);
    public native int          getChatEphemeralTimer (int chat_id);
    public native boolean      setChatEphemeralTimer (int chat_id, int timer);
    public native boolean      setChatMuteDuration  (int chat_id, long duration);
    public native void         deleteChat           (int chat_id);
    public native void         blockChat            (int chat_id);
    public native void         acceptChat           (int chat_id);
    public DcMsg               getMsg               (int msg_id) { return new DcMsg(getMsgCPtr(msg_id)); }
    public native String       getMsgInfo           (int id);
    public native String       getMsgHtml           (int msg_id);
    public native void         downloadFullMsg      (int msg_id);
    public native int          getFreshMsgCount     (int chat_id);
    public native int          estimateDeletionCount(boolean from_server, long seconds);
    public native void         deleteMsgs           (int msg_ids[]);
    public native void         forwardMsgs          (int msg_ids[], int chat_id);
    public native boolean      resendMsgs           (int msg_ids[]);
    public native int          prepareMsg           (int chat_id, DcMsg msg);
    public native int          sendMsg              (int chat_id, DcMsg msg);
    public native int          sendTextMsg          (int chat_id, String text);
    public native int          sendVideochatInvitation(int chat_id);
    public native boolean      sendWebxdcStatusUpdate(int msg_id, String payload, String descr);
    public native String       getWebxdcStatusUpdates(int msg_id, int last_known_serial);
    public native int          addDeviceMsg         (String label, DcMsg msg);
    public native boolean      wasDeviceMsgEverAdded(String label);
    public DcLot               checkQr              (String qr) { return new DcLot(checkQrCPtr(qr)); }
    public native String       getSecurejoinQr      (int chat_id);
    public native String       getSecurejoinQrSvg   (int chat_id);
    public native int          joinSecurejoin       (String qr);
    public native void         sendLocationsToChat  (int chat_id, int seconds);
    public native boolean      isSendingLocationsToChat(int chat_id);
    public DcArray             getLocations         (int chat_id, int contact_id, long timestamp_start, long timestamp_end) { return new DcArray(getLocationsCPtr(chat_id, contact_id, timestamp_start, timestamp_end)); }
    public native void         deleteAllLocations   ();
    public DcProvider          getProviderFromEmailWithDns (String email) { long cptr = getProviderFromEmailWithDnsCPtr(email); return cptr!=0 ? new DcProvider(cptr) : null; }

    public String getNameNAddr() {
      String displayname = getConfig("displayname");
      String addr = getConfig("addr");
      String ret = "";

      if (!displayname.isEmpty() && !addr.isEmpty()) {
        ret = String.format("%s (%s)", displayname, addr);
      } else if (!addr.isEmpty()) {
        ret = addr;
      }

      if (ret.isEmpty() || isConfigured() == 0) {
        ret += " (not configured)";
      }

      return ret.trim();
    }

    /**
     * @return true if at least one chat has location streaming enabled
     */
    public native boolean      setLocation          (float latitude, float longitude, float accuracy);

    // working with raw c-data
    private long        contextCPtr;     // CAVE: the name is referenced in the JNI
    private native long createContextCPtr(String osName, String dbfile);
    private native void unrefContextCPtr ();
    private native long getEventEmitterCPtr();
    public  native long createMsgCPtr    (int viewtype);
    private native long getChatlistCPtr  (int listflags, String query, int queryId);
    private native long getChatCPtr      (int chat_id);
    private native long getMsgCPtr       (int id);
    private native long getDraftCPtr    (int id);
    private native long getContactCPtr   (int id);
    private native long getLocationsCPtr (int chat_id, int contact_id, long timestamp_start, long timestamp_end);
    private native long checkQrCPtr      (String qr);
    private native long getProviderFromEmailWithDnsCPtr  (String addr);
    private native long newBackupProviderCPtr();
}
