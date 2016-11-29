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
import com.b44t.messenger.query.MessagesQuery;
import com.b44t.messenger.query.SearchQuery;
import com.b44t.ui.ActionBar.BaseFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class MessagesController implements NotificationCenter.NotificationCenterDelegate {

    private ConcurrentHashMap<Integer, TLRPC.Chat> chats = new ConcurrentHashMap<>(100, 1.0f, 2);
    private ConcurrentHashMap<Integer, TLRPC.User> users = new ConcurrentHashMap<>(100, 1.0f, 2);
    private ConcurrentHashMap<String, TLRPC.User> usersByUsernames = new ConcurrentHashMap<>(100, 1.0f, 2);

    public ArrayList<TLRPC.TL_dialog> dialogs = new ArrayList<>();
    public ArrayList<TLRPC.TL_dialog> dialogsServerOnly = new ArrayList<>();
    public ArrayList<TLRPC.TL_dialog> dialogsGroupsOnly = new ArrayList<>();
    public int nextDialogsCacheOffset;
    public ConcurrentHashMap<Long, TLRPC.TL_dialog> dialogs_dict = new ConcurrentHashMap<>(100, 1.0f, 2);
    public HashMap<Long, MessageObject> dialogMessage = new HashMap<>();
    public HashMap<Long, MessageObject> dialogMessagesByRandomIds = new HashMap<>();
    public HashMap<Integer, MessageObject> dialogMessagesByIds = new HashMap<>();
    public HashMap<Long, CharSequence> printingStrings = new HashMap<>();
    public ConcurrentHashMap<Integer, Integer> onlinePrivacy = new ConcurrentHashMap<>(20, 1.0f, 2);

    private ArrayList<Long> createdDialogIds = new ArrayList<>();

    private ArrayList<Integer> loadedFullUsers = new ArrayList<>();
    private ArrayList<Integer> loadedFullChats = new ArrayList<>();

    private HashMap<String, ArrayList<MessageObject>> reloadingWebpages = new HashMap<>();

    public boolean loadingDialogs = false;
    public boolean dialogsEndReached = false;

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
    public static final int UPDATE_MASK_USER_PHONE = 128;
    public static final int UPDATE_MASK_READ_DIALOG_MESSAGE = 256;
    public static final int UPDATE_MASK_SELECT_DIALOG = 512;
    public static final int UPDATE_MASK_NEW_MESSAGE = 2048;
    public static final int UPDATE_MASK_SEND_STATE = 4096;
    public static final int UPDATE_MASK_CHANNEL = 8192;
    public static final int UPDATE_MASK_CHAT_ADMINS = 16384;

    private static volatile MessagesController Instance = null;

    private final Comparator<TLRPC.TL_dialog> dialogComparator = new Comparator<TLRPC.TL_dialog>() {
        @Override
        public int compare(TLRPC.TL_dialog dialog1, TLRPC.TL_dialog dialog2) {
            TLRPC.DraftMessage draftMessage = DraftQuery.getDraft(dialog1.id);
            int date1 = draftMessage != null && draftMessage.date >= dialog1.last_message_date ? draftMessage.date : dialog1.last_message_date;
            draftMessage = DraftQuery.getDraft(dialog2.id);
            int date2 = draftMessage != null && draftMessage.date >= dialog2.last_message_date ? draftMessage.date : dialog2.last_message_date;
            if (date1 < date2) {
                return 1;
            } else if (date1 > date2) {
                return -1;
            }
            return 0;
        }
    };

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
        /*if (id < 0) {
            TLRPC.Chat chat = getInstance().getChat(-id);
            if (ChatObject.isChannel(chat)) {
                inputPeer = new TLRPC.TL_inputPeerChannel();
                inputPeer.channel_id = -id;
                inputPeer.access_hash = chat.access_hash;
            } else {
                inputPeer = new TLRPC.TL_inputPeerChat();
                inputPeer.chat_id = -id;
            }
        } else */ {
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

    /*
    public void cleanup() {
        ContactsController.getInstance().cleanup();
        MediaController.getInstance().cleanup();
        NotificationsController.getInstance().cleanup();
        SendMessagesHelper.getInstance().cleanup();
        //SecretChatHelper.getInstance().cleanup();
        StickersQuery.cleanup();
        SearchQuery.cleanup();
        DraftQuery.cleanup();

        reloadingWebpages.clear();
        reloadingWebpagesPending.clear();
        dialogs_dict.clear();
        dialogs_read_inbox_max.clear();
        dialogs_read_outbox_max.clear();
        dialogs.clear();
        channelViewsToSend.clear();
        channelViewsToReload.clear();
        dialogsServerOnly.clear();
        dialogsGroupsOnly.clear();
        dialogMessagesByIds.clear();
        dialogMessagesByRandomIds.clear();
        users.clear();
        usersByUsernames.clear();
        chats.clear();
        dialogMessage.clear();
        printingUsers.clear();
        printingStrings.clear();
        printingStringsTypes.clear();
        onlinePrivacy.clear();
        loadingPeerSettings.clear();
        lastPrintingStringCount = 0;
        nextDialogsCacheOffset = 0;
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                gettingUnknownChannels.clear();
                createdDialogIds.clear();
                gettingDifference = false;
            }
        });
        blockedUsers.clear();
        sendingTypings.clear();
        loadingFullUsers.clear();
        loadedFullUsers.clear();
        reloadingMessages.clear();
        loadingFullChats.clear();
        loadingFullParticipants.clear();
        loadedFullParticipants.clear();
        loadedFullChats.clear();

        currentDeletingTaskTime = 0;
        currentDeletingTaskMids = null;
        gettingNewDeleteTask = false;
        loadingDialogs = false;
        dialogsEndReached = false;
        loadingBlockedUsers = false;
        firstGettingTask = false;
        updatingState = false;
        offlineSent = false;
        registeringForPush = false;
        uploadingAvatar = null;
        statusRequest = 0;
        statusSettingState = 0;

        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                //ConnectionsManager.getInstance().setIsUpdating(false);
                updatesQueueChannels.clear();
                updatesStartWaitTimeChannels.clear();
                gettingDifferenceChannels.clear();
                channelsPts.clear();
                shortPollChannels.clear();
                needShortPollChannels.clear();
            }
        });

        if (currentDeleteTaskRunnable != null) {
            Utilities.stageQueue.cancelRunnable(currentDeleteTaskRunnable);
            currentDeleteTaskRunnable = null;
        }

        NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
    }
    */

    public TLRPC.User getUser(Integer id) {
        // EDIT BY MR - additional information should be loaded as needed by the caller
        TLRPC.User u = new TLRPC.User();
        u.id = id;
        return u;
    }

    public TLRPC.User getUser(String username) {
        if (username == null || username.length() == 0) {
            return null;
        }
        return usersByUsernames.get(username.toLowerCase());
    }

    public ConcurrentHashMap<Integer, TLRPC.User> getUsers() {
        return users;
    }

    public TLRPC.Chat getChat(Integer id) {
        return chats.get(id);
    }

    public void setLastCreatedDialogId(final long dialog_id, final boolean set) {
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (set) {
                    createdDialogIds.add(dialog_id);
                } else {
                    createdDialogIds.remove(dialog_id);
                }
            }
        });
    }

    public boolean putUser(TLRPC.User user, boolean fromCache) {
        if (user == null) {
            return false;
        }
        fromCache = fromCache && user.id / 1000 != 333 && user.id != 777000;
        TLRPC.User oldUser = users.get(user.id);
        if (oldUser != null && oldUser.username != null && oldUser.username.length() > 0) {
            usersByUsernames.remove(oldUser.username);
        }
        if (user.username != null && user.username.length() > 0) {
            usersByUsernames.put(user.username.toLowerCase(), user);
        }
        if (user.min) {
            if (oldUser != null) {
                if (!fromCache) {
                    if (user.username != null) {
                        oldUser.username = user.username;
                        oldUser.flags |= 8;
                    } else {
                        oldUser.username = null;
                        oldUser.flags = oldUser.flags &~ 8;
                    }
                    if (user.photo != null) {
                        oldUser.photo = user.photo;
                        oldUser.flags |= 32;
                    } else {
                        oldUser.photo = null;
                        oldUser.flags = oldUser.flags &~ 32;
                    }
                }
            } else {
                users.put(user.id, user);
            }
        } else {
            if (!fromCache) {
                users.put(user.id, user);
                if (user.id == UserConfig.getClientUserId()) {
                    UserConfig.setCurrentUser(user);
                    UserConfig.saveConfig(true);
                }
                if (oldUser != null && user.status != null && oldUser.status != null && user.status.expires != oldUser.status.expires) {
                    return true;
                }
            } else if (oldUser == null) {
                users.put(user.id, user);
            } else if (oldUser.min) {
                user.min = false;
                if (oldUser.username != null) {
                    user.username = oldUser.username;
                    user.flags |= 8;
                } else {
                    user.username = null;
                    user.flags = user.flags &~ 8;
                }
                if (oldUser.photo != null) {
                    user.photo = oldUser.photo;
                    user.flags |= 32;
                } else {
                    user.photo = null;
                    user.flags = user.flags &~ 32;
                }
                users.put(user.id, user);
            }
        }
        return false;
    }

    public void putUsers(ArrayList<TLRPC.User> users, boolean fromCache) {
        if (users == null || users.isEmpty()) {
            return;
        }
        boolean updateStatus = false;
        int count = users.size();
        for (int a = 0; a < count; a++) {
            TLRPC.User user = users.get(a);
            if (putUser(user, fromCache)) {
                updateStatus = true;
            }
        }
        if (updateStatus) {
            AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces, UPDATE_MASK_STATUS);
                }
            });
        }
    }

    /*
    public void putChat(TLRPC.Chat chat, boolean fromCache) {
        if (chat == null) {
            return;
        }
        TLRPC.Chat oldChat = chats.get(chat.id);

        if (chat.min) {
            if (oldChat != null) {
                if (!fromCache) {
                    oldChat.title = chat.title;
                    oldChat.photo = chat.photo;
                    //oldChat.broadcast = chat.broadcast;
                    oldChat.verified = chat.verified;
                    //oldChat.megagroup = chat.megagroup;
                    oldChat.democracy = chat.democracy;
                    if (chat.username != null) {
                        oldChat.username = chat.username;
                        oldChat.flags |= 64;
                    } else {
                        oldChat.username = null;
                        oldChat.flags = oldChat.flags &~ 64;
                    }
                }
            } else {
                chats.put(chat.id, chat);
            }
        } else {
            if (!fromCache) {
                if (oldChat != null && chat.version != oldChat.version) {
                    loadedFullChats.remove((Integer) chat.id);
                }
                chats.put(chat.id, chat);
            } else if (oldChat == null) {
                chats.put(chat.id, chat);
            } else if (oldChat.min) {
                chat.min = false;
                chat.title = oldChat.title;
                chat.photo = oldChat.photo;
                //chat.broadcast = oldChat.broadcast;
                chat.verified = oldChat.verified;
                //chat.megagroup = oldChat.megagroup;
                chat.democracy = oldChat.democracy;
                if (oldChat.username != null) {
                    chat.username = oldChat.username;
                    chat.flags |= 64;
                } else {
                    chat.username = null;
                    chat.flags = chat.flags &~ 64;
                }
                chats.put(chat.id, chat);
            }
        }
    }
    */

    /*
    public void putChats(ArrayList<TLRPC.Chat> chats, boolean fromCache) {
        if (chats == null || chats.isEmpty()) {
            return;
        }
        int count = chats.size();
        for (int a = 0; a < count; a++) {
            TLRPC.Chat chat = chats.get(a);
            putChat(chat, fromCache);
        }
    }
    */

    /*
    protected void clearFullUsers() {
        loadedFullUsers.clear();
        loadedFullChats.clear();
    }
    */

    /*
    public void loadDialogPhotos(final int did, final int offset, final int count, final long max_id, final boolean fromCache, final int classGuid) {
        if (fromCache) {
            MessagesStorage.getInstance().getDialogPhotos(did, offset, count, max_id, classGuid);
        } else {
            if (did > 0) {
                TLRPC.User user = getUser(did);
                if (user == null) {
                    return;
                }
                TLRPC.TL_photos_getUserPhotos req = new TLRPC.TL_photos_getUserPhotos();
                req.limit = count;
                req.offset = offset;
                req.max_id = (int) max_id;
                req.user_id = getInputUser(user);
                int reqId = ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                    @Override
                    public void run(TLObject response, TLRPC.TL_error error) {
                        if (error == null) {
                            TLRPC.photos_Photos res = (TLRPC.photos_Photos) response;
                            processLoadedUserPhotos(res, did, offset, count, max_id, false, classGuid);
                        }
                    }
                });
                ConnectionsManager.getInstance().bindRequestToGuid(reqId, classGuid);
            } else if (did < 0) {
                TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
                req.filter = new TLRPC.TL_inputMessagesFilterChatPhotos();
                req.limit = count;
                req.offset = offset;
                req.max_id = (int) max_id;
                req.q = "";
                req.peer = MessagesController.getInputPeer(did);
                int reqId = ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                    @Override
                    public void run(TLObject response, TLRPC.TL_error error) {
                        if (error == null) {
                            TLRPC.messages_Messages messages = (TLRPC.messages_Messages) response;
                            TLRPC.TL_photos_photos res = new TLRPC.TL_photos_photos();
                            res.count = messages.count;
                            res.users.addAll(messages.users);
                            for (int a = 0; a < messages.messages.size(); a++) {
                                TLRPC.Message message = messages.messages.get(a);
                                if (message.action == null || message.action.photo == null) {
                                    continue;
                                }
                                res.photos.add(message.action.photo);
                            }
                            processLoadedUserPhotos(res, did, offset, count, max_id, false, classGuid);
                        }
                    }
                });
                ConnectionsManager.getInstance().bindRequestToGuid(reqId, classGuid);
            }
        }
    }
    */

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

    public void loadChatInfo(final int chat_id, Semaphore semaphore, boolean force) {
        //MessagesStorage.getInstance().loadChatInfo(chat_id, semaphore, force, false);
    }

    public void cancelTyping(int action, long dialog_id) {
    }

    public void sendTyping(final long dialog_id, final int action, int classGuid) {
    }

    public void loadMessages(final long dialog_id, final int count, final int max_id, boolean fromCache, int midDate, final int classGuid, final int load_type, final int last_message_id, final boolean isChannel, final int loadIndex) {
        loadMessages(dialog_id, count, max_id, fromCache, midDate, classGuid, load_type, last_message_id, isChannel, loadIndex, 0, 0, 0, false);
    }

    public void loadMessages(final long dialog_id, final int count, final int max_id, boolean fromCache, int midDate, final int classGuid, final int load_type, final int last_message_id, final boolean isChannel, final int loadIndex, final int first_unread, final int unread_count, final int last_date, final boolean queryFromServer) {

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

    public void loadDialogs(final int offset, final int count, boolean fromCache) {
        if (loadingDialogs) {
            return;
        }
        loadingDialogs = true;
        NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
        FileLog.e("messenger", "load cacheOffset = " + offset + " count = " + count + " cache = " + fromCache);
        if (fromCache) {
            //MessagesStorage.getInstance().getDialogs(offset == 0 ? 0 : nextDialogsCacheOffset, count);
        } else {
            /*
            TLRPC.TL_messages_getDialogs req = new TLRPC.TL_messages_getDialogs();
            req.limit = count;
            boolean found = false;
            for (int a = dialogs.size() - 1; a >= 0; a--) {
                TLRPC.TL_dialog dialog = dialogs.get(a);
                int lower_id = (int) dialog.id;
                int high_id = (int) (dialog.id >> 32);
                if (lower_id != 0 && high_id != 1 && dialog.top_message > 0) {
                    MessageObject message = dialogMessage.get(dialog.id);
                    if (message != null && message.getId() > 0) {
                        req.offset_date = message.messageOwner.date;
                        req.offset_id = message.messageOwner.id;
                        int id;
                        if (message.messageOwner.to_id.channel_id != 0) {
                            id = -message.messageOwner.to_id.channel_id;
                        } else if (message.messageOwner.to_id.chat_id != 0) {
                            id = -message.messageOwner.to_id.chat_id;
                        } else {
                            id = message.messageOwner.to_id.user_id;
                        }
                        req.offset_peer = getInputPeer(id);
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                req.offset_peer = new TLRPC.TL_inputPeerEmpty();
            }
            */
            /*ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                @Override
                public void run(TLObject response, TLRPC.TL_error error) {
                    if (error == null) {
                        //final TLRPC.messages_Dialogs dialogsRes = (TLRPC.messages_Dialogs) response;
                        //processLoadedDialogs(dialogsRes, null, 0, count, 0, false, false);
                    }
                }
            });*/
        }
    }

    public void markMessageContentAsRead(final MessageObject messageObject) {
    }

    public void markMessageAsRead(final long dialog_id, final long random_id, int ttl) {
        if (random_id == 0 || dialog_id == 0 || ttl <= 0 && ttl != Integer.MIN_VALUE) {
            return;
        }
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
        //MessagesStorage.getInstance().putWebRecent(arrayList);
    }

    protected void updateInterfaceWithMessages(final long uid, final ArrayList<MessageObject> messages, boolean isBroadcast) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        boolean isEncryptedChat = ((int) uid) == 0;
        MessageObject lastMessage = null;
        int channelId = 0;
        boolean updateRating = false;
        for (int a = 0; a < messages.size(); a++) {
            MessageObject message = messages.get(a);
            if (lastMessage == null || (!isEncryptedChat && message.getId() > lastMessage.getId() || (isEncryptedChat || message.getId() < 0 && lastMessage.getId() < 0) && message.getId() < lastMessage.getId()) || message.messageOwner.date > lastMessage.messageOwner.date) {
                lastMessage = message;
                if (message.messageOwner.to_id.channel_id != 0) {
                    channelId = message.messageOwner.to_id.channel_id;
                }
            }
            if (message.isOut() && message.isNewGif() && !message.isSending() && !message.isForwarded()) {
                addNewGifToRecent(message.messageOwner.media.document, message.messageOwner.date);
            }
            if (message.isOut() && message.isSent()) {
                updateRating = true;
            }
        }
        MessagesQuery.loadReplyMessagesForMessages(messages, uid);
        NotificationCenter.getInstance().postNotificationName(NotificationCenter.didReceivedNewMessages, uid, messages);

        if (lastMessage == null) {
            return;
        }
        TLRPC.TL_dialog dialog = dialogs_dict.get(uid);

        boolean changed = false;

        if (dialog == null) {
            if (!isBroadcast) {
                TLRPC.Chat chat = getChat(channelId);
                if (channelId != 0 && chat == null || chat != null && chat.left) {
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
                changed = true;
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
                    changed = true;
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

        if (changed) {
            sortDialogs(null);
        }

        if (updateRating) {
            SearchQuery.increasePeerRaiting(uid);
        }
    }

    public void sortDialogs(HashMap<Integer, TLRPC.Chat> chatsDict) {
        dialogsServerOnly.clear();
        dialogsGroupsOnly.clear();
        Collections.sort(dialogs, dialogComparator);
        for (int a = 0; a < dialogs.size(); a++) {
            TLRPC.TL_dialog d = dialogs.get(a);
            int high_id = (int) (d.id >> 32);
            int lower_id = (int) d.id;
            if (lower_id != 0 && high_id != 1) {
                dialogsServerOnly.add(d);
                /*if (DialogObject.isChannel(d)) {
                    TLRPC.Chat chat = getChat(-lower_id);
                    if (chat != null && (chat.megagroup && chat.editor || chat.creator)) {
                        dialogsGroupsOnly.add(d);
                    }
                } else*/ if (lower_id < 0) {
                    /*if (chatsDict != null) {
                        TLRPC.Chat chat = chatsDict.get(-lower_id);
                        if (chat != null && chat.migrated_to != null) {
                            dialogs.remove(a);
                            a--;
                            continue;
                        }
                    }*/
                    dialogsGroupsOnly.add(d);
                }
            }
        }
    }
}
