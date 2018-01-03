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
 * File:    MrMsg.java
 * Purpose: Wrap around mrmsg_t
 *
 ******************************************************************************/


package com.b44t.messenger;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;

import java.io.File;

public class MrMsg {

    private static final String TAG = "MrMsg";

    public final static int      MR_MSG_UNDEFINED           =  0;
    public final static int      MR_MSG_TEXT                = 10;
    public final static int      MR_MSG_IMAGE               = 20;
    public final static int      MR_MSG_GIF                 = 21;
    public final static int      MR_MSG_AUDIO               = 40;
    public final static int      MR_MSG_VOICE               = 41;
    public final static int      MR_MSG_VIDEO               = 50;
    public final static int      MR_MSG_FILE                = 60;

    public final static int      MR_STATE_UNDEFINED         =  0;
    public final static int      MR_IN_FRESH                = 10;
    public final static int      MR_IN_NOTICED              = 13;
    public final static int      MR_OUT_PENDING             = 20;
    public final static int      MR_OUT_ERROR               = 24;
    public final static int      MR_OUT_DELIVERED           = 26;
    public final static int      MR_OUT_MDN_RCVD            = 28;

    public final static int      MR_MSG_ID_MARKER1    = 1;
    public final static int      MR_MSG_ID_DAYMARKER  = 9;

    public MrMsg(long hMsg) {
        m_hMsg = hMsg;
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        MrMsgUnref(m_hMsg);
        m_hMsg = 0;
    }

    public int getId() {
        return MrMsgGetId(m_hMsg);
    }

    public String getText() {
        return MrMsgGetText(m_hMsg);
    }

    public long getTimestamp() {
        return MrMsgGetTimestamp(m_hMsg);
    }

    public int getType() {
        return MrMsgGetType(m_hMsg);
    }

    public int getState() {
        return MrMsgGetState(m_hMsg);
    }
    public int getChatId() {
        return MrMsgGetChatId(m_hMsg);
    }
    public int getFromId() {
        return MrMsgGetFromId(m_hMsg);
    }
    public int getToId() {
        return MrMsgGetToId(m_hMsg);
    }

    public native int    getWidth(int def);
    public native int    getHeight(int def);
    public native int    getDuration();
    public native void   lateFilingMediaSize(int width, int height, int duration);

    public native int    getBytes();
    public MrLot getSummary(MrChat chat) { return new MrLot(getSummaryCPtr(chat.getCPtr())); }
    private native long  getSummaryCPtr(long hChat);
    public native String getSummarytext(int approx_characters);
    public native int    showPadlock();
    public MrLot getMediainfo() { return new MrLot(getMediainfoCPtr()); }
    private native long  getMediainfoCPtr();
    public native String getFile();
    public native String getFilemime();
    public native String getFilename();
    public native boolean isForwarded();
    public native boolean isSystemcmd();
    public native boolean isSetupMessage();
    public native String getSetupCodeBegin();
    public native boolean isIncreation();

    private long                  m_hMsg; // must not be renamed as referenced by JNI under the name "m_hMsg"
    private native static void    MrMsgUnref                 (long hMsg);
    private native static int     MrMsgGetId                 (long hMsg);
    private native static String  MrMsgGetText               (long hMsg);
    private native static long    MrMsgGetTimestamp          (long hMsg);
    private native static int     MrMsgGetType               (long hMsg);
    private native static int     MrMsgGetState              (long hMsg);
    private native static int     MrMsgGetChatId             (long hMsg);
    private native static int     MrMsgGetFromId             (long hMsg);
    private native static int     MrMsgGetToId               (long hMsg);


    /* additional functions that are not 1:1 available in the backend
     **********************************************************************************************/

    public TLRPC.Message get_TLRPC_Message()
    {
        TLRPC.Message ret = new TLRPC.TL_message(); // the class derived from TLRPC.Message defines the basic type:
        //  TLRPC.TL_messageService is used to display messages as "You joined the group"
        //  TLRPC.TL_message is a normal message (also photos?)

        int state = getState();
        int type  = getType();
        switch( state ) {
            case MR_OUT_DELIVERED: ret.send_state = MessageObject.MESSAGE_SEND_STATE_SENT; break;
            case MR_OUT_ERROR:     ret.send_state = MessageObject.MESSAGE_SEND_STATE_SEND_ERROR; break;
            case MR_OUT_PENDING:   ret.send_state = MessageObject.MESSAGE_SEND_STATE_SENDING; break;
            case MR_OUT_MDN_RCVD:  ret.send_state = MessageObject.MESSAGE_SEND_STATE_SENT; break;
        }

        ret.id            = getId();
        ret.from_id       = getFromId();
        ret.date          = (int)getTimestamp();
        ret.dialog_id     = getChatId();
        ret.unread        = state!=MR_OUT_MDN_RCVD; // the state of outgoing messages
        ret.media_unread  = ret.unread;
        ret.flags         = 0; // posible flags: MESSAGE_FLAG_HAS_FROM_ID, however, this seems to be read only
        ret.out           = ret.from_id==MrContact.MR_CONTACT_ID_SELF; // true=outgoing message, read eg. in MessageObject.isOutOwner()
        ret.created_by_mr = true;
        ret.show_padlock  = showPadlock()!=0;
        ret.is_system_cmd = isSystemcmd();
        ret.is_setup_message = isSetupMessage();

        if( type == MR_MSG_TEXT ) {
            ret.message       = getText();
        }
        else if( type == MR_MSG_FILE && isSetupMessage() )
        {
            ret.message = ApplicationLoader.applicationContext.getString(R.string.AutocryptSetupMessageTapBody);
        }
        else if( type == MR_MSG_IMAGE ) {
            String path = getFile();
            TLRPC.TL_photo photo = null;
            if( !path.isEmpty() ) {
                try {
                    TLRPC.TL_photoSize photoSize = new TLRPC.TL_photoSize();
                    photoSize.w = getWidth(800);
                    photoSize.h = getHeight(800);
                    photoSize.size = 0; // not sure what to use here, maybe `getBytes();`?
                    photoSize.location = new TLRPC.TL_fileLocation();
                    photoSize.location.mr_path = path;
                    photoSize.location.local_id = -ret.id; // this forces the document to be searched in the cache dir
                    if (photoSize.w <= 100 && photoSize.h <= 100) {
                        photoSize.type = "s";
                    } else if (photoSize.w <= 320 && photoSize.h <= 320) {
                        photoSize.type = "m";
                    } else if (photoSize.w <= 800 && photoSize.h <= 800) {
                        photoSize.type = "x";
                    } else if (photoSize.w <= 1280 && photoSize.h <= 1280) {
                        photoSize.type = "y";
                    } else {
                        photoSize.type = "w";
                    }
                    photo = new TLRPC.TL_photo();
                    photo.sizes.add(photoSize);
                } catch (Exception e) {
                    // the most common reason is a simple "file not found error"
                }
            }

            if(photo!=null) {
                ret.message = "-1";
                ret.media = new TLRPC.TL_messageMediaPhoto();
                ret.media.photo = photo;
                ret.attachPath = path; // ret.attachPathExists set later in MessageObject.checkMediaExistance()
            }
            else {
                ret.message = "<cannot load image>";
            }
        }
        else if( type == MR_MSG_GIF || type == MR_MSG_AUDIO || type == MR_MSG_VOICE || type == MR_MSG_VIDEO || type == MR_MSG_FILE ) {
            String path = getFile();
            if( !path.isEmpty()) {
                ret.message = "-1"; // may be misused for video editing information
                ret.media = new TLRPC.TL_messageMediaDocument();
                ret.media.caption = "";
                ret.media.document = new TLRPC.TL_document();
                ret.media.document.file_name = getFilename();
                ret.media.document.mr_path = path;
                ret.media.document.size = getBytes();
                if( type == MR_MSG_GIF ) {
                    ret.media.document.mime_type = getFilemime();
                    TLRPC.PhotoSize size = new TLRPC.PhotoSize();
                    size.location = new TLRPC.TL_fileLocation();
                    size.location.mr_path = path;
                    size.location.local_id = -ret.id;
                    size.w = getWidth(320);
                    size.h = getHeight(240);
                    size.type = "s";
                    ret.media.document.thumb = size;
                }
                else if( type == MR_MSG_AUDIO || type == MR_MSG_VOICE ) {
                    TLRPC.TL_documentAttributeAudio attr = new TLRPC.TL_documentAttributeAudio();
                    attr.voice = type == MR_MSG_VOICE;
                    attr.duration = getDuration() / 1000;
                    ret.media.document.attributes.add(attr);
                }
                else if( type == MR_MSG_VIDEO ) {
                    File vfile = new File(path);
                    File tfile = new File(MrMailbox.getBlobdir(), vfile.getName()+"-preview.jpg");
                    if( !tfile.exists() ) {
                        try {
                            Bitmap thumb = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MINI_KIND);
                            TLRPC.PhotoSize size = ImageLoader.scaleAndSaveImage(tfile, thumb, 90, 90, 55, false);
                            size.location.mr_path = tfile.getAbsolutePath();
                            size.type = "s";
                            ret.media.document.thumb = size;

                            lateFilingMediaSize(size.w, size.h, 0);
                        }
                        catch (Exception e) {}
                    }
                    else {
                        TLRPC.PhotoSize size = new TLRPC.PhotoSize();
                        size.location = new TLRPC.TL_fileLocation();
                        size.location.mr_path = tfile.getAbsolutePath();
                        size.location.local_id = -ret.id;
                        size.w = getWidth(320);
                        size.h = getHeight(240);
                        size.type = "s";
                        ret.media.document.thumb = size;
                    }

                    TLRPC.TL_documentAttributeVideo attr = new TLRPC.TL_documentAttributeVideo();
                    attr.duration = getDuration() / 1000;
                    attr.w = getWidth(320);
                    attr.h = getHeight(240);
                    ret.media.document.attributes.add(attr);
                }
                else {
                    ret.media.document.mime_type = getFilemime();
                }

            }
            else {
                ret.message = "<file path missing>";
            }
        }
        else {
            ret.message = String.format("<unsupported message type #%d for id #%d>", type, ret.id);
        }

        if( isForwarded() ) {
            ret.flags |= TLRPC.MESSAGE_FLAG_FWD;
        }

        return ret;
    }
};
