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


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.database.model.Quote;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientProvider;

import java.util.Collections;
import java.util.List;

public class DcMsg {

    public final static int DC_MSG_UNDEFINED = 0;
    public final static int DC_MSG_TEXT = 10;
    public final static int DC_MSG_IMAGE = 20;
    public final static int DC_MSG_GIF = 21;
    public final static int DC_MSG_AUDIO = 40;
    public final static int DC_MSG_VOICE = 41;
    public final static int DC_MSG_VIDEO = 50;
    public final static int DC_MSG_FILE = 60;

    public final static int DC_STATE_UNDEFINED =  0;
    public final static int DC_STATE_IN_FRESH = 10;
    public final static int DC_STATE_IN_NOTICED = 13;
    public final static int DC_STATE_OUT_PENDING = 20;
    public final static int DC_STATE_OUT_ERROR = 24;
    public final static int DC_STATE_OUT_DELIVERED = 26;
    public final static int DC_STATE_OUT_MDN_RCVD = 28;

    public final static int DC_MSG_ID_MARKER1 = 1;
    public final static int DC_MSG_ID_DAYMARKER = 9;

    public DcMsg(DcContext context) {
        msgCPtr = context.createMsgCPtr();
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
    public boolean equals(Object other) {
        if (other == null || !(other instanceof DcMsg)) {
            return false;
        }

        DcMsg that = (DcMsg) other;
        return this.getId()==that.getId() && this.getId()!=0;
    }

    public native int     getId              ();
    public native String  getText            ();
    public native long    getTimestamp       ();
    public native int     getType            ();
    public native int     getState           ();
    public native int     getChatId          ();
    public native int     getFromId          ();
    public native int     getWidth           (int def);
    public native int     getHeight          (int def);
    public native int     getDuration        ();
    public native void    lateFilingMediaSize(int width, int height, int duration);
    public native int     getBytes           ();
    public @NonNull DcLot getSummary         (DcChat chat) { return new DcLot(getSummaryCPtr(chat.getChatCPtr())); }
    public native String  getSummarytext     (int approx_characters);
    public native int     showPadlock        ();
    public @NonNull DcLot getMediainfo       () { return new DcLot(getMediainfoCPtr()); }
    public boolean        hasFile            () { String file = getFile(); return file!=null && !file.isEmpty(); }
    public native String  getFile            ();
    public native String  getFilemime        ();
    public native String  getFilename        ();
    public native boolean isForwarded        ();
    public native boolean isInfo             ();
    public native boolean isSetupMessage     ();
    public native String  getSetupCodeBegin  ();
    public native boolean isIncreation       ();
    public native void    setType            (int type);
    public native void    setText            (String text);
    public native void    setFile            (String file, String filemime);
    public native void    setDimension       (int width, int height);
    public native void    setDuration        (int duration);
    public native void    setMediainfo       (String author, String trackname);

    // aliases and higher-level tools

    public boolean isOutgoing() {
        return getFromId() == DcContact.DC_CONTACT_ID_SELF;
    }

    public boolean isGroupAction() {
        return false;
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
    public long getExpiresIn() {
        return -1; // never.
    }
    public long getExpireStarted() {
        return 0;
    }
    public boolean isSecure() {
        return showPadlock()!=0;
    }
    public boolean isPending() {
        return getState() == DC_STATE_OUT_PENDING;
    }
    public boolean isMediaPending() {
        return isPending();
    }
    public boolean isDelivered() {
        return getState() == DC_STATE_OUT_DELIVERED;
    }
    public boolean isRemoteRead() {
        return getState() == DC_STATE_OUT_MDN_RCVD;
    }
    public int getSubscriptionId() {
        return -1;
    }
    public boolean isMms() {
        return false;
    }

    // the following are probably system messages in delta-land
    public boolean isJoined() { return false; }
    public boolean isExpirationTimerUpdate() { return false; }
    public boolean isEndSession() { return false; }
    public boolean isIdentityUpdate() { return false; }
    public boolean isIdentityVerified() { return false; }
    public boolean isIdentityDefault() { return false; }

    @Nullable
    public Quote getQuote() {
        // TODO: remove the dependency to org.thoughtcrime.securesms.database.model.Quote
        return null;
    }

    public Recipient getIndividualRecipient() {
        // TODO: remove the dependency to org.thoughtcrime.securesms.recipients.Recipient
        RecipientProvider.RecipientDetails recipientDetails = new RecipientProvider.RecipientDetails("individualRecipient", null, false, null, null);
        return new Recipient(Address.fromContact(getFromId()), recipientDetails);
    }

    // working with raw c-data
    private long        msgCPtr;        // CAVE: the name is referenced in the JNI
    private native void unrefMsgCPtr    ();
    private native long getSummaryCPtr  (long chatCPtr);
    private native long getMediainfoCPtr();
};
