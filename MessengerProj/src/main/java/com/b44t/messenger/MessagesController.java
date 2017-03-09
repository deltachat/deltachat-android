/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *                        (C) 2013-2016 Nikolai Kudashov
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

import android.app.Activity;
import android.content.SharedPreferences;

import com.b44t.ui.SettingsAdvActivity;

import java.util.ArrayList;

public class MessagesController {

    public int fontSize;

    public static final int UPDATE_MASK_NAME = 1;
    public static final int UPDATE_MASK_AVATAR = 2;
    public static final int UPDATE_MASK_STATUS = 4;
    public static final int UPDATE_MASK_CHAT_AVATAR = 8;
    public static final int UPDATE_MASK_CHAT_NAME = 16;
    public static final int UPDATE_MASK_CHAT_MEMBERS = 32;
    public static final int UPDATE_MASK_SELECT_DIALOG = 512;
    public static final int UPDATE_MASK_NEW_MESSAGE = 2048;
    public static final int UPDATE_MASK_SEND_STATE = 4096;

    private static volatile MessagesController Instance = null;

    public static MessagesController getInstance() {
        MessagesController localInstance = Instance;
        if (localInstance == null) {
            synchronized (MessagesController.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new MessagesController();
                }
            }
        }
        return localInstance;
    }

    public MessagesController() {
        ImageLoader.getInstance();

        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        fontSize = preferences.getInt("msg_font_size", SettingsAdvActivity.defMsgFontSize());
    }

    public TLRPC.User getUser(Integer id) {
        // EDIT BY MR - additional information should be loaded as needed by the caller
        TLRPC.User u = new TLRPC.User();
        u.id = id;
        return u;
    }

    public MediaController.SearchImage saveGif(TLRPC.Document document) {
        MediaController.SearchImage searchImage = new MediaController.SearchImage();
        searchImage.type = 2;
        searchImage.document = document;
        searchImage.date = (int) (System.currentTimeMillis() / 1000);
        searchImage.id = "" + searchImage.document.id;

        ArrayList<MediaController.SearchImage> arrayList = new ArrayList<>();
        arrayList.add(searchImage);
        //MessagesStorage.getInstance().putWebRecent(arrayList);
        /*TLRPC.TL_messages_saveGif req = new TLRPC.TL_messages_saveGif();
        req.id = new TLRPC.TL_inputDocument();
        req.id.id = searchImage.document.id;
        req.id.access_hash = searchImage.document.access_hash;
        req.unsave = false;
        ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {

            }
        });*/
        return searchImage;
    }

    public void cancelTyping(int action, long dialog_id) {
    }

    public void sendTyping(final long dialog_id, final int action, int classGuid) {
    }

    public void markMessageContentAsRead(final MessageObject messageObject) {
    }

    public void changeChatAvatar(int chat_id, TLRPC.InputFile uploadedAvatar) {
        /*TLObject request;
        {
            TLRPC.TL_messages_editChatPhoto req = new TLRPC.TL_messages_editChatPhoto();
            req.chat_id = chat_id;
            if (uploadedAvatar != null) {
                req.photo = new TLRPC.TL_inputChatUploadedPhoto();
                req.photo.file = uploadedAvatar;
                req.photo.crop = new TLRPC.TL_inputPhotoCropAuto();
            } else {
                req.photo = new TLRPC.TL_inputChatPhotoEmpty();
            }
            request = req;
        }*/
        /*ConnectionsManager.getInstance().sendRequest(request, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {
                if (error != null) {
                    return;
                }
                processUpdates((TLRPC.Updates) response, false);
            }
        }, ConnectionsManager.RequestFlagInvokeAfter);*/
    }

    public boolean isDialogMuted(long dialog_id) {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
        int mute_type = preferences.getInt("notify2_" + dialog_id, 0);
        if (mute_type == 2) {
            return true;
        } else if (mute_type == 3) {
            int mute_until = preferences.getInt("notifyuntil_" + dialog_id, 0);
            if (mute_until >= ConnectionsManager.getInstance().getCurrentTime()) {
                return true;
            }
        }
        return false;
    }
}
