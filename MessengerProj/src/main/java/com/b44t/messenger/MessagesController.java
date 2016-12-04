/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.b44t.messenger;

import android.app.Activity;
import android.content.SharedPreferences;

import com.b44t.messenger.query.DraftQuery;
import com.b44t.ui.ActionBar.BaseFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class MessagesController implements NotificationCenter.NotificationCenterDelegate {

    public ArrayList<TLRPC.TL_dialog> dialogs = new ArrayList<>();
    public int nextDialogsCacheOffset;
    public ConcurrentHashMap<Long, TLRPC.TL_dialog> dialogs_dict = new ConcurrentHashMap<>(100, 1.0f, 2);
    public HashMap<Long, MessageObject> dialogMessage = new HashMap<>();
    public HashMap<Long, MessageObject> dialogMessagesByRandomIds = new HashMap<>();
    public HashMap<Integer, MessageObject> dialogMessagesByIds = new HashMap<>();
    public HashMap<Long, CharSequence> printingStrings = new HashMap<>();
    public ConcurrentHashMap<Integer, Integer> onlinePrivacy = new ConcurrentHashMap<>(20, 1.0f, 2);

    private HashMap<String, ArrayList<MessageObject>> reloadingWebpages = new HashMap<>();

    public int secretWebpagePreview = 2;

    private String uploadingAvatar = null;

    public boolean enableJoined = true;
    public int fontSize = AndroidUtilities.dp(16);
    public int ratingDecay;

    public static final int UPDATE_MASK_NAME = 1;
    public static final int UPDATE_MASK_AVATAR = 2;
    public static final int UPDATE_MASK_STATUS = 4;
    public static final int UPDATE_MASK_CHAT_AVATAR = 8;
    public static final int UPDATE_MASK_CHAT_NAME = 16;
    public static final int UPDATE_MASK_CHAT_MEMBERS = 32;
    public static final int UPDATE_MASK_USER_PRINT = 64;
    public static final int UPDATE_MASK_READ_DIALOG_MESSAGE = 256;
    public static final int UPDATE_MASK_SELECT_DIALOG = 512;
    public static final int UPDATE_MASK_NEW_MESSAGE = 2048;
    public static final int UPDATE_MASK_SEND_STATE = 4096;
    public static final int UPDATE_MASK_CHANNEL = 8192;
    public static final int UPDATE_MASK_CHAT_ADMINS = 16384;

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
        //MessagesStorage.getInstance();
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileDidUpload);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileDidFailUpload);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileDidFailedLoad);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.messageReceivedByServer);
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
        enableJoined = preferences.getBoolean("EnableContactJoined", true);

        preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        secretWebpagePreview = preferences.getInt("secretWebpage2", 2);
        ratingDecay = preferences.getInt("ratingDecay", 2419200);
        fontSize = preferences.getInt("fons_size", AndroidUtilities.isTablet() ? 18 : 16);
    }

    public static TLRPC.InputUser getInputUser(TLRPC.User user) {
        if (user == null) {
            return new TLRPC.TL_inputUserEmpty();
        }
        TLRPC.InputUser inputUser;
        if (user.id == UserConfig.getClientUserId()) {
            inputUser = new TLRPC.TL_inputUserSelf();
        } else {
            inputUser = new TLRPC.TL_inputUser();
            inputUser.user_id = user.id;
            inputUser.access_hash = user.access_hash;
        }
        return inputUser;
    }

    public static TLRPC.InputUser getInputUser(int user_id) {
        TLRPC.User user = getInstance().getUser(user_id);
        return getInputUser(user);
    }

    public static TLRPC.InputPeer getInputPeer(int id) {
        TLRPC.InputPeer inputPeer;
        {
            TLRPC.User user = getInstance().getUser(id);
            inputPeer = new TLRPC.TL_inputPeerUser();
            inputPeer.user_id = id;
            if (user != null) {
                inputPeer.access_hash = user.access_hash;
            }
        }
        return inputPeer;
    }

    public static TLRPC.Peer getPeer(int id) {
        TLRPC.Peer inputPeer;
        if (id < 0) {
            inputPeer = new TLRPC.TL_peerChat();
            inputPeer.chat_id = -id;
        } else {
            inputPeer = new TLRPC.TL_peerUser();
            inputPeer.user_id = id;
        }
        return inputPeer;
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.FileDidUpload) {
            final String location = (String) args[0];
            final TLRPC.InputFile file = (TLRPC.InputFile) args[1];

            if (uploadingAvatar != null && uploadingAvatar.equals(location)) {
                /*TLRPC.TL_photos_uploadProfilePhoto req = new TLRPC.TL_photos_uploadProfilePhoto();
                req.caption = "";
                req.crop = new TLRPC.TL_inputPhotoCropAuto();
                req.file = file;
                req.geo_point = new TLRPC.TL_inputGeoPointEmpty();*/
                /*ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                    @Override
                    public void run(TLObject response, TLRPC.TL_error error) {
                        if (error == null) {
                            TLRPC.User user = getUser(UserConfig.getClientUserId());
                            if (user == null) {
                                user = UserConfig.getCurrentUser();
                                putUser(user, true);
                            } else {
                                UserConfig.setCurrentUser(user);
                            }
                            if (user == null) {
                                return;
                            }
                            TLRPC.TL_photos_photo photo = (TLRPC.TL_photos_photo) response;
                            ArrayList<TLRPC.PhotoSize> sizes = photo.photo.sizes;
                            TLRPC.PhotoSize smallSize = FileLoader.getClosestPhotoSizeWithSize(sizes, 100);
                            TLRPC.PhotoSize bigSize = FileLoader.getClosestPhotoSizeWithSize(sizes, 1000);
                            user.photo = new TLRPC.TL_userProfilePhoto();
                            user.photo.photo_id = photo.photo.id;
                            if (smallSize != null) {
                                user.photo.photo_small = smallSize.location;
                            }
                            if (bigSize != null) {
                                user.photo.photo_big = bigSize.location;
                            } else if (smallSize != null) {
                                user.photo.photo_small = smallSize.location;
                            }
                            //MessagesStorage.getInstance().clearUserPhotos(user.id);
                            ArrayList<TLRPC.User> users = new ArrayList<>();
                            users.add(user);
                            //MessagesStorage.getInstance().putUsersAndChats(users, null, false, true);
                            AndroidUtilities.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces, UPDATE_MASK_AVATAR);
                                    UserConfig.saveConfig(true);
                                }
                            });
                        }
                    }
                });*/
            }
        } else if (id == NotificationCenter.FileDidFailUpload) {
            final String location = (String) args[0];
            if (uploadingAvatar != null && uploadingAvatar.equals(location)) {
                uploadingAvatar = null;
            }
        } else if (id == NotificationCenter.messageReceivedByServer) {
            Integer msgId = (Integer) args[0];
            Integer newMsgId = (Integer) args[1];
            Long did = (Long) args[3];
            MessageObject obj = dialogMessage.get(did);
            if (obj != null && obj.getId() == msgId) {
                obj.messageOwner.id = newMsgId;
                obj.messageOwner.send_state = MessageObject.MESSAGE_SEND_STATE_SENT;
                TLRPC.TL_dialog dialog = dialogs_dict.get(did);
                if (dialog != null) {
                    if (dialog.top_message == msgId) {
                        dialog.top_message = newMsgId;
                    }
                }
                NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
            }
            obj = dialogMessagesByIds.remove(msgId);
            if (obj != null) {
                dialogMessagesByIds.put(newMsgId, obj);
            }
        }
    }

    public TLRPC.User getUser(Integer id) {
        // EDIT BY MR - additional information should be loaded as needed by the caller
        TLRPC.User u = new TLRPC.User();
        u.id = id;
        return u;
    }

    public TLRPC.Chat getChat(Integer id) {
        return null;
    }

    public void deleteMessages(ArrayList<Integer> messages, ArrayList<Long> randoms, Object encryptedChat, final int channelId) {

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

    public void reloadWebPages(final long dialog_id, HashMap<String, ArrayList<MessageObject>> webpagesToReload) {
        for (HashMap.Entry<String, ArrayList<MessageObject>> entry : webpagesToReload.entrySet()) {
            final String url = entry.getKey();
            final ArrayList<MessageObject> messages = entry.getValue();
            ArrayList<MessageObject> arrayList = reloadingWebpages.get(url);
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                reloadingWebpages.put(url, arrayList);
            }
            arrayList.addAll(messages);
            /*TLRPC.TL_messages_getWebPagePreview req = new TLRPC.TL_messages_getWebPagePreview();
            req.message = url;
            ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                @Override
                public void run(final TLObject response, final TLRPC.TL_error error) {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            ArrayList<MessageObject> arrayList = reloadingWebpages.remove(url);
                            if (arrayList == null) {
                                return;
                            }
                            TLRPC.TL_messages_messages messagesRes = new TLRPC.TL_messages_messages();
                            if (!(response instanceof TLRPC.TL_messageMediaWebPage)) {
                                for (int a = 0; a < arrayList.size(); a++) {
                                    arrayList.get(a).messageOwner.media.webpage = new TLRPC.TL_webPageEmpty();
                                    messagesRes.messages.add(arrayList.get(a).messageOwner);
                                }
                            } else {
                                TLRPC.TL_messageMediaWebPage media = (TLRPC.TL_messageMediaWebPage) response;
                                if (media.webpage instanceof TLRPC.TL_webPage || media.webpage instanceof TLRPC.TL_webPageEmpty) {
                                    for (int a = 0; a < arrayList.size(); a++) {
                                        arrayList.get(a).messageOwner.media.webpage = media.webpage;
                                        if (a == 0) {
                                            ImageLoader.saveMessageThumbs(arrayList.get(a).messageOwner);
                                        }
                                        messagesRes.messages.add(arrayList.get(a).messageOwner);
                                    }
                                } else {
                                    reloadingWebpagesPending.put(media.webpage.id, arrayList);
                                }
                            }
                            if (!messagesRes.messages.isEmpty()) {
                                //MessagesStorage.getInstance().putMessages(messagesRes, dialog_id, -2, 0, false);
                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.replaceMessagesObjects, dialog_id, arrayList);
                            }
                        }
                    });
                }
            });*/
        }
    }

    public void markMessageContentAsRead(final MessageObject messageObject) {
    }

    public void markMessageAsRead(final long dialog_id, final long random_id, int ttl) {
    }

    public void markDialogAsRead(final long dialog_id, final int max_id, final int max_positive_id, final int max_date, final boolean was, final boolean popup) {
    }

    public void addUserToChat(final int chat_id, final TLRPC.User user, final TLRPC.ChatFull info, int count_fwd, String botHash, final BaseFragment fragment) {
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

    protected void updateInterfaceWithMessages(long uid, ArrayList<MessageObject> messages) {
        updateInterfaceWithMessages(uid, messages, false);
    }

    protected static void addNewGifToRecent(TLRPC.Document document, int date) {
        ArrayList<MediaController.SearchImage> arrayList = new ArrayList<>();
        MediaController.SearchImage searchImage = new MediaController.SearchImage();
        searchImage.type = 2;
        searchImage.document = document;
        searchImage.date = date;
        searchImage.id = "" + searchImage.document.id;
        arrayList.add(searchImage);
    }

    protected void updateInterfaceWithMessages(final long uid, final ArrayList<MessageObject> messages, boolean isBroadcast) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        final boolean isEncryptedChat = false;
        MessageObject lastMessage = null;
        int channelId = 0;
        for (int a = 0; a < messages.size(); a++) {
            MessageObject message = messages.get(a);
            if (lastMessage == null || (!isEncryptedChat && message.getId() > lastMessage.getId() || (isEncryptedChat || message.getId() < 0 && lastMessage.getId() < 0) && message.getId() < lastMessage.getId()) || message.messageOwner.date > lastMessage.messageOwner.date) {
                lastMessage = message;
            }
            if (message.isOut() && message.isNewGif() && !message.isSending() && !message.isForwarded()) {
                addNewGifToRecent(message.messageOwner.media.document, message.messageOwner.date);
            }
        }
        //MessagesQuery.loadReplyMessagesForMessages(messages, uid);
        NotificationCenter.getInstance().postNotificationName(NotificationCenter.didReceivedNewMessages, uid, messages);

        if (lastMessage == null) {
            return;
        }
        TLRPC.TL_dialog dialog = dialogs_dict.get(uid);

        if (dialog == null) {
            if (!isBroadcast) {
                TLRPC.Chat chat = getChat(channelId);
                if (channelId != 0 && chat == null || chat != null && false/*chat.left*/) {
                    return;
                }
                dialog = new TLRPC.TL_dialog();
                dialog.id = uid;
                dialog.unread_count = 0;
                dialog.top_message = lastMessage.getId();
                dialog.last_message_date = lastMessage.messageOwner.date;
                //dialog.flags = ChatObject.isChannel(chat) ? 1 : 0;
                dialogs_dict.put(uid, dialog);
                dialogs.add(dialog);
                dialogMessage.put(uid, lastMessage);
                if (lastMessage.messageOwner.to_id.channel_id == 0) {
                    dialogMessagesByIds.put(lastMessage.getId(), lastMessage);
                    if (lastMessage.messageOwner.random_id != 0) {
                        dialogMessagesByRandomIds.put(lastMessage.messageOwner.random_id, lastMessage);
                    }
                }
                nextDialogsCacheOffset++;
            }
        } else {
            if ((dialog.top_message > 0 && lastMessage.getId() > 0 && lastMessage.getId() > dialog.top_message) ||
                    (dialog.top_message < 0 && lastMessage.getId() < 0 && lastMessage.getId() < dialog.top_message) ||
                    !dialogMessage.containsKey(uid) || dialog.top_message < 0 || dialog.last_message_date <= lastMessage.messageOwner.date) {
                MessageObject object = dialogMessagesByIds.remove(dialog.top_message);
                if (object != null && object.messageOwner.random_id != 0) {
                    dialogMessagesByRandomIds.remove(object.messageOwner.random_id);
                }
                dialog.top_message = lastMessage.getId();
                if (!isBroadcast) {
                    dialog.last_message_date = lastMessage.messageOwner.date;
                }
                dialogMessage.put(uid, lastMessage);
                if (lastMessage.messageOwner.to_id.channel_id == 0) {
                    dialogMessagesByIds.put(lastMessage.getId(), lastMessage);
                    if (lastMessage.messageOwner.random_id != 0) {
                        dialogMessagesByRandomIds.put(lastMessage.messageOwner.random_id, lastMessage);
                    }
                }
            }
        }
    }
}
