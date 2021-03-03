package com.b44t.messenger;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;

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

    public final static int DC_STATE_UNDEFINED =  0;
    public final static int DC_STATE_IN_FRESH = 10;
    public final static int DC_STATE_IN_NOTICED = 13;
    public final static int DC_STATE_IN_SEEN = 16;
    public final static int DC_STATE_OUT_PREPARING = 18;
    public final static int DC_STATE_OUT_PENDING = 20;
    public final static int DC_STATE_OUT_ERROR = 24;
    public final static int DC_STATE_OUT_DELIVERED = 26;
    public final static int DC_STATE_OUT_MDN_RCVD = 28;

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

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        unrefMsgCPtr();
        msgCPtr = 0;
    }

    @Override
    public int hashCode() {
        if (this.getId() == 0) {
            Log.e(TAG, "encountered a DcMsg with id 0.");
        }
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
    public static int getMessagePosition(DcMsg msg, ApplicationDcContext dcContext) {
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
    public native long    getTimestamp       ();
    public native long    getSortTimestamp   ();
    public native boolean hasDeviatingTimestamp();
    public native boolean hasLocation        ();
    public native int     getType            ();
    public native int     getState           ();
    public native int     getChatId          ();
    public native int     getRealChatId      ();
    public native int     getFromId          ();
    public native int     getWidth           (int def);
    public native int     getHeight          (int def);
    public native int     getDuration        ();
    public native void    lateFilingMediaSize(int width, int height, int duration);
    public @NonNull DcLot getSummary         (DcChat chat) { return new DcLot(getSummaryCPtr(chat.getChatCPtr())); }
    public native String  getSummarytext     (int approx_characters);
    public native int     showPadlock        ();
    public boolean        hasFile            () { String file = getFile(); return file!=null && !file.isEmpty(); }
    public native String  getFile            ();
    public native String  getFilemime        ();
    public native String  getFilename        ();
    public native long    getFilebytes       ();
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
    private native @Nullable String getOverrideSenderName();

    public @NonNull String getSenderName(@NonNull DcContact dcContact, boolean markOverride) {
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

    public File getFileAsFile() {
        if(getFile()==null)
            throw new AssertionError("expected a file to be present.");
        return new File(getFile());
    }

    // aliases and higher-level tools
    public static int[] msgSetToIds(final Set<DcMsg> dcMsgs) {
        int   cnt = dcMsgs==null? 0 : dcMsgs.size();
        int[] ids = new int[cnt];
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
        return getState() == DC_STATE_OUT_ERROR;
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
};
