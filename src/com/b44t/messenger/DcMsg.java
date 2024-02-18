package com.b44t.messenger;

import android.text.TextUtils;

import org.json.JSONObject;

import java.io.File;
import java.util.Set;

public class DcMsg {

    public final static int DC_MSG_UNDEFINED = 0;
    public final static int DC_MSG_TEXT = 10;
    public final static int DC_MSG_IMAGE = 20;
    public final static int DC_MSG_GIF = 21;
    public final static int DC_MSG_STICKER = 23;
    public final static int DC_MSG_AUDIO = 40;
    public final static int DC_MSG_VOICE = 41;
    public final static int DC_MSG_VIDEO = 50;
    public final static int DC_MSG_FILE = 60;
    public final static int DC_MSG_VIDEOCHAT_INVITATION = 70;
    public final static int DC_MSG_WEBXDC = 80;

    public final static int DC_INFO_UNKNOWN                   = 0;
    public final static int DC_INFO_GROUP_NAME_CHANGED        = 2;
    public final static int DC_INFO_GROUP_IMAGE_CHANGED       = 3;
    public final static int DC_INFO_MEMBER_ADDED_TO_GROUP     = 4;
    public final static int DC_INFO_MEMBER_REMOVED_FROM_GROUP = 5;
    public final static int DC_INFO_AUTOCRYPT_SETUP_MESSAGE   = 6;
    public final static int DC_INFO_SECURE_JOIN_MESSAGE       = 7;
    public final static int DC_INFO_LOCATIONSTREAMING_ENABLED = 8;
    public final static int DC_INFO_LOCATION_ONLY             = 9;
    public final static int DC_INFO_EPHEMERAL_TIMER_CHANGED   = 10;
    public final static int DC_INFO_PROTECTION_ENABLED        = 11;
    public final static int DC_INFO_PROTECTION_DISABLED       = 12;
    public final static int DC_INFO_INVALID_UNENCRYPTED_MAIL  = 13;
    public final static int DC_INFO_WEBXDC_INFO_MESSAGE       = 32;

    public final static int DC_STATE_UNDEFINED =  0;
    public final static int DC_STATE_IN_FRESH = 10;
    public final static int DC_STATE_IN_NOTICED = 13;
    public final static int DC_STATE_IN_SEEN = 16;
    public final static int DC_STATE_OUT_PREPARING = 18;
    public final static int DC_STATE_OUT_DRAFT = 19;
    public final static int DC_STATE_OUT_PENDING = 20;
    public final static int DC_STATE_OUT_FAILED = 24;
    public final static int DC_STATE_OUT_DELIVERED = 26;
    public final static int DC_STATE_OUT_MDN_RCVD = 28;

    public final static int DC_DOWNLOAD_DONE = 0;
    public final static int DC_DOWNLOAD_AVAILABLE = 10;
    public final static int DC_DOWNLOAD_FAILURE = 20;
    public final static int DC_DOWNLOAD_UNDECIPHERABLE = 30;
    public final static int DC_DOWNLOAD_IN_PROGRESS = 1000;

    public static final int DC_MSG_NO_ID = 0;
    public final static int DC_MSG_ID_MARKER1 = 1;
    public final static int DC_MSG_ID_DAYMARKER = 9;

    public final static int DC_VIDEOCHATTYPE_UNKNOWN = 0;
    public final static int DC_VIDEOCHATTYPE_BASICWEBRTC = 1;

    private static final String TAG = DcMsg.class.getSimpleName();

    public DcMsg(DcContext context, int viewtype) {
        msgCPtr = context.createMsgCPtr(viewtype);
    }

    public DcMsg(long msgCPtr) {
        this.msgCPtr = msgCPtr;
    }

    public boolean isOk() {
      return msgCPtr != 0;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        unrefMsgCPtr();
        msgCPtr = 0;
    }

    @Override
    public int hashCode() {
        return this.getId();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof DcMsg)) {
            return false;
        }

        DcMsg that = (DcMsg) other;
        return this.getId()==that.getId() && this.getId()!=0;
    }

    /**
     * If given a message, calculates the position of the message in the chat
     */
    public static int getMessagePosition(DcMsg msg, DcContext dcContext) {
        int msgs[] = dcContext.getChatMsgs(msg.getChatId(), 0, 0);
        int startingPosition = -1;
        int msgId = msg.getId();
        for (int i = 0; i < msgs.length; i++) {
            if (msgs[i] == msgId) {
                startingPosition = msgs.length - 1 - i;
                break;
            }
        }
        return startingPosition;
    }

    public native int     getId              ();
    public native String  getText            ();
    public native String  getSubject         ();
    public native long    getTimestamp       ();
    public native long    getSortTimestamp   ();
    public native boolean hasDeviatingTimestamp();
    public native boolean hasLocation        ();
    public native int     getType            ();
    public native int     getInfoType        ();
    public native int     getState           ();
    public native int     getDownloadState   ();
    public native int     getChatId          ();
    public native int     getFromId          ();
    public native int     getWidth           (int def);
    public native int     getHeight          (int def);
    public native int     getDuration        ();
    public native void    lateFilingMediaSize(int width, int height, int duration);
    public DcLot          getSummary         (DcChat chat) { return new DcLot(getSummaryCPtr(chat.getChatCPtr())); }
    public native String  getSummarytext     (int approx_characters);
    public native int     showPadlock        ();
    public boolean        hasFile            () { String file = getFile(); return file!=null && !file.isEmpty(); }
    public native String  getFile            ();
    public native String  getFilemime        ();
    public native String  getFilename        ();
    public native long    getFilebytes       ();
    public native byte[]  getWebxdcBlob      (String filename);
    public JSONObject     getWebxdcInfo      () {
      try {
        return new JSONObject(getWebxdcInfoJson());
      } catch(Exception e) {
        e.printStackTrace();
        return new JSONObject();
      }
    }
    public native boolean isForwarded        ();
    public native boolean isInfo             ();
    public native boolean isSetupMessage     ();
    public native boolean hasHtml            ();
    public native String  getSetupCodeBegin  ();
    public native String  getVideochatUrl    ();
    public native int     getVideochatType   ();
    public native boolean isIncreation       ();
    public native void    setText            (String text);
    public native void    setFile            (String file, String filemime);
    public native void    setDimension       (int width, int height);
    public native void    setDuration        (int duration);
    public native void    setLocation        (float latitude, float longitude);
    public void           setQuote           (DcMsg quote) { setQuoteCPtr(quote.msgCPtr); }
    public native String  getQuotedText      ();
    public native String  getError           ();
    public native String  getOverrideSenderName();

    public String getSenderName(DcContact dcContact, boolean markOverride) {
        String overrideName = getOverrideSenderName();
        if (overrideName != null) {
            return (markOverride ? "~" : "") + overrideName;
        } else {
            return dcContact.getDisplayName();
        }
    }

    public DcMsg          getQuotedMsg       () {
        long cPtr = getQuotedMsgCPtr();
        return cPtr != 0 ? new DcMsg(cPtr) : null;
    }

    public DcMsg getParent() {
      long cPtr = getParentCPtr();
      return cPtr != 0 ? new DcMsg(cPtr) : null;
    }

    public DcMsg getOriginalMsg() {
      long cPtr = getOriginalMsgCPtr();
      return cPtr != 0 ? new DcMsg(cPtr) : null;
    }

    public File getFileAsFile() {
        if(getFile()==null)
            throw new AssertionError("expected a file to be present.");
        return new File(getFile());
    }

    // aliases and higher-level tools
    public static int[] msgSetToIds(final Set<DcMsg> dcMsgs) {
        if (dcMsgs == null) {
            return new int[0];
        }
        int[] ids = new int[dcMsgs.size()];
        int   i = 0;
        for (DcMsg dcMsg : dcMsgs) {
            ids[i++] = dcMsg.getId();
        }
        return ids;
    }

    public boolean isOutgoing() {
        return getFromId() == DcContact.DC_CONTACT_ID_SELF;
    }

    public String getDisplayBody()  {
        return getText();
    }

    public String getBody()  {
        return getText();
    }

    public long getDateReceived() {
        return getTimestamp();
    }

    public boolean isFailed() {
        return (getState() == DC_STATE_OUT_FAILED) || (!TextUtils.isEmpty(getError()));
    }
    public boolean isPreparing() {
        return getState() == DC_STATE_OUT_PREPARING;
    }
    public boolean isSecure() {
        return showPadlock()!=0;
    }
    public boolean isPending() {
        return getState() == DC_STATE_OUT_PENDING;
    }
    public boolean isDelivered() {
        return getState() == DC_STATE_OUT_DELIVERED;
    }
    public boolean isRemoteRead() {
        return getState() == DC_STATE_OUT_MDN_RCVD;
    }
    public boolean isSeen() {
        return getState() == DC_STATE_IN_SEEN;
    }


    // working with raw c-data
    private long        msgCPtr;        // CAVE: the name is referenced in the JNI
    private native void unrefMsgCPtr    ();
    private native long getSummaryCPtr  (long chatCPtr);
    private native void setQuoteCPtr    (long quoteCPtr);
    private native long getQuotedMsgCPtr ();
    private native long getParentCPtr   ();
    private native long getOriginalMsgCPtr();
    private native String getWebxdcInfoJson ();
};
