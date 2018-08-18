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

    public DcMsg(long hMsg) {
        m_hMsg = hMsg;
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        DcMsgUnref(m_hMsg);
        m_hMsg = 0;
    }

    public native int getId();
    public native String getText();
    public native long getTimestamp();
    public native int getType();
    public native int getState();
    public native int getChatId();
    public native int getFromId();

    public native int getWidth(int def);
    public native int getHeight(int def);
    public native int getDuration();
    public native void lateFilingMediaSize(int width, int height, int duration);

    public native int getBytes();
    public DcLot getSummary(DcChat chat) { return new DcLot(getSummaryCPtr(chat.getCPtr())); }
    public native String getSummarytext(int approx_characters);
    public native int showPadlock();
    public DcLot getMediainfo() { return new DcLot(getMediainfoCPtr()); }
    public native String getFile();
    public native String getFilemime();
    public native String getFilename();
    public native boolean isForwarded();
    public native boolean isInfo();
    public native boolean isSetupMessage();
    public native String getSetupCodeBegin();
    public native boolean isIncreation();

    // working with raw c-data
    private long m_hMsg; // must not be renamed as referenced by JNI
    private native static void DcMsgUnref(long hMsg);
    private native long getSummaryCPtr(long hChat);
    private native long getMediainfoCPtr();
};
