/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *     Copyright (C) 2016 Björn Petersen Software Design and Development
 *                   Contact: r10s@b44t.com, http://b44t.com
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
 * Authors: Björn Petersen
 * Purpose: Wrap around mrmsg_t
 *
 ******************************************************************************/


package com.b44t.messenger;

public class MrMsg {

    public final static int      MR_MSG_UNDEFINED           =  0;
    public final static int      MR_MSG_TEXT                = 10;
    public final static int      MR_MSG_IMAGE               = 20;
    public final static int      MR_MSG_AUDIO               = 40;
    public final static int      MR_MSG_VIDEO               = 50;
    public final static int      MR_MSG_FILE                = 60;

    public final static int      MR_STATE_UNDEFINED         =  0;
    public final static int      MR_IN_UNREAD               = 10;
    public final static int      MR_IN_READ                 = 16;
    public final static int      MR_OUT_PENDING             = 20;
    public final static int      MR_OUT_SENDING             = 22;
    public final static int      MR_OUT_ERROR               = 24;
    public final static int      MR_OUT_DELIVERED           = 26;
    public final static int      MR_OUT_READ                = 28;

    public MrMsg(long hMsg) {
        m_hMsg = hMsg;
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        MrMsgUnref(m_hMsg);
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

    public String getParam (int key, String def) {
        return MrMsgGetParam(m_hMsg, key, def);
    }
    public int getParamInt(int key, int def) {
        return MrMsgGetParamInt(m_hMsg, key, def);
    }

    private long                  m_hMsg;
    private native static void    MrMsgUnref                 (long hMsg);
    private native static int     MrMsgGetId                 (long hMsg);
    private native static String  MrMsgGetText               (long hMsg);
    private native static long    MrMsgGetTimestamp          (long hMsg);
    private native static int     MrMsgGetType               (long hMsg);
    private native static int     MrMsgGetState              (long hMsg);
    private native static int     MrMsgGetChatId             (long hMsg);
    private native static int     MrMsgGetFromId             (long hMsg);
    private native static int     MrMsgGetToId               (long hMsg);
    private native static String  MrMsgGetParam              (long hMsg, int key, String def);
    private native static int     MrMsgGetParamInt           (long hMsg, int key, int def);


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
            case MR_OUT_READ:      ret.send_state = MessageObject.MESSAGE_SEND_STATE_SENT; break;
        }

        ret.id            = getId();
        ret.from_id       = getFromId();
        ret.to_id         = new TLRPC.TL_peerUser();
        ret.to_id.user_id = getToId();
        ret.date          = (int)getTimestamp();
        ret.dialog_id     = getChatId();
        ret.unread        = state!=MR_OUT_READ; // the state of outgoing messages
        ret.media_unread  = ret.unread;
        ret.flags         = 0; // posible flags: MESSAGE_FLAG_HAS_FROM_ID, however, this seems to be read only
        ret.post          = false; // ? true=avatar wird in gruppen nicht angezeigt, wird aber in isFromUser() auch überprüft...
        ret.out           = ret.from_id==MrContact.MR_CONTACT_ID_SELF; // true=outgoing message, read eg. in MessageObject.isOutOwner()
        ret.created_by_mr = true;

        if( type == MR_MSG_TEXT ) {
            ret.message       = getText();
        }
        else if( type == MR_MSG_IMAGE ) {
            String path = getParam('f', "");
            TLRPC.TL_photo photo = null;
            if( !path.isEmpty() ) {
                try {
                    TLRPC.TL_photoSize photoSize = new TLRPC.TL_photoSize();
                    photoSize.w = getParamInt('w', 800);
                    photoSize.h = getParamInt('h', 800);
                    photoSize.size = 0;
                    photoSize.location = new TLRPC.TL_fileLocation();
                    photoSize.location.mr_path = path;
                    photoSize.location.local_id = -ret.id; // this forces the document to be searched in the cache dir
                    photoSize.type = "x";
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
        else if( type == MR_MSG_AUDIO || type == MR_MSG_VIDEO ) {
            String path = getParam('f', "");
            if( !path.isEmpty()) {
                ret.message = "-1"; // may be misused for video editing information
                ret.media = new TLRPC.TL_messageMediaDocument();
                ret.media.caption = "";
                ret.media.document = new TLRPC.TL_document();
                ret.media.document.mr_path = path;
                if( type == MR_MSG_AUDIO ) {
                    TLRPC.TL_documentAttributeAudio attr = new TLRPC.TL_documentAttributeAudio();
                    attr.voice = true; // !voice = music
                    attr.duration = getParamInt('d', 0) / 1000;
                    ret.media.document.attributes.add(attr);
                }
                else if( type == MR_MSG_VIDEO) {
                    TLRPC.TL_documentAttributeVideo attr = new TLRPC.TL_documentAttributeVideo();
                    attr.duration = getParamInt('d', 0) / 1000;
                    attr.w = getParamInt('w', 0);
                    attr.h = getParamInt('h', 0);
                    ret.media.document.attributes.add(attr);
                }

            }
            else {
                ret.message = "<path missing>";
            }
        }
        else {
            ret.message = String.format("<unsupported message type #%d for id #%d>", type, ret.id);
        }

        return ret;
    }
};
