/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package org.telegram.messenger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.SparseArray;
import android.util.SparseIntArray;

import org.telegram.messenger.query.DraftQuery;
import org.telegram.messenger.query.MessagesQuery;
import org.telegram.messenger.query.SearchQuery;
import org.telegram.messenger.query.StickersQuery;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.ProfileActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class MessagesController implements NotificationCenter.NotificationCenterDelegate {

    private ConcurrentHashMap<Integer, TLRPC.Chat> chats = new ConcurrentHashMap<>(100, 1.0f, 2);
    private ConcurrentHashMap<Integer, TLRPC.EncryptedChat> encryptedChats = new ConcurrentHashMap<>(10, 1.0f, 2);
    private ConcurrentHashMap<Integer, TLRPC.User> users = new ConcurrentHashMap<>(100, 1.0f, 2);
    private ConcurrentHashMap<String, TLRPC.User> usersByUsernames = new ConcurrentHashMap<>(100, 1.0f, 2);

    public ArrayList<TLRPC.TL_dialog> dialogs = new ArrayList<>();
    public ArrayList<TLRPC.TL_dialog> dialogsServerOnly = new ArrayList<>();
    public ArrayList<TLRPC.TL_dialog> dialogsGroupsOnly = new ArrayList<>();
    public int nextDialogsCacheOffset;
    public ConcurrentHashMap<Long, Integer> dialogs_read_inbox_max = new ConcurrentHashMap<>(100, 1.0f, 2);
    public ConcurrentHashMap<Long, Integer> dialogs_read_outbox_max = new ConcurrentHashMap<>(100, 1.0f, 2);
    public ConcurrentHashMap<Long, TLRPC.TL_dialog> dialogs_dict = new ConcurrentHashMap<>(100, 1.0f, 2);
    public HashMap<Long, MessageObject> dialogMessage = new HashMap<>();
    public HashMap<Long, MessageObject> dialogMessagesByRandomIds = new HashMap<>();
    public HashMap<Integer, MessageObject> dialogMessagesByIds = new HashMap<>();
    public ConcurrentHashMap<Long, ArrayList<PrintingUser>> printingUsers = new ConcurrentHashMap<>(20, 1.0f, 2);
    public HashMap<Long, CharSequence> printingStrings = new HashMap<>();
    public HashMap<Long, Integer> printingStringsTypes = new HashMap<>();
    public HashMap<Integer, HashMap<Long, Boolean>> sendingTypings = new HashMap<>();
    public ConcurrentHashMap<Integer, Integer> onlinePrivacy = new ConcurrentHashMap<>(20, 1.0f, 2);
    private int lastPrintingStringCount = 0;

    private HashMap<Long, Boolean> loadingPeerSettings = new HashMap<>();

    private ArrayList<Long> createdDialogIds = new ArrayList<>();

    private SparseIntArray shortPollChannels = new SparseIntArray();
    private SparseIntArray needShortPollChannels = new SparseIntArray();

    public boolean loadingBlockedUsers = false;
    public ArrayList<Integer> blockedUsers = new ArrayList<>();

    private SparseArray<ArrayList<Integer>> channelViewsToSend = new SparseArray<>();
    private SparseArray<ArrayList<Integer>> channelViewsToReload = new SparseArray<>();

    private HashMap<Integer, ArrayList<TLRPC.Updates>> updatesQueueChannels = new HashMap<>();
    private HashMap<Integer, Long> updatesStartWaitTimeChannels = new HashMap<>();
    private HashMap<Integer, Integer> channelsPts = new HashMap<>();
    private HashMap<Integer, Boolean> gettingDifferenceChannels = new HashMap<>();

    private HashMap<Integer, Boolean> gettingUnknownChannels = new HashMap<>();
    private HashMap<Integer, Boolean> checkingLastMessagesDialogs = new HashMap<>();

    private ArrayList<Integer> loadingFullUsers = new ArrayList<>();
    private ArrayList<Integer> loadedFullUsers = new ArrayList<>();
    private ArrayList<Integer> loadingFullChats = new ArrayList<>();
    private ArrayList<Integer> loadingFullParticipants = new ArrayList<>();
    private ArrayList<Integer> loadedFullParticipants = new ArrayList<>();
    private ArrayList<Integer> loadedFullChats = new ArrayList<>();

    private HashMap<String, ArrayList<MessageObject>> reloadingWebpages = new HashMap<>();
    private HashMap<Long, ArrayList<MessageObject>> reloadingWebpagesPending = new HashMap<>();

    private HashMap<Long, ArrayList<Integer>> reloadingMessages = new HashMap<>();

    private boolean gettingNewDeleteTask = false;
    private int currentDeletingTaskTime = 0;
    private ArrayList<Integer> currentDeletingTaskMids = null;
    private Runnable currentDeleteTaskRunnable = null;

    public boolean loadingDialogs = false;
    public boolean dialogsEndReached = false;
    public boolean gettingDifference = false;
    public boolean updatingState = false;
    public boolean firstGettingTask = false;
    public boolean registeringForPush = false;

    public int secretWebpagePreview = 2;

    private int statusRequest = 0;
    private int statusSettingState = 0;
    private boolean offlineSent = false;
    private String uploadingAvatar = null;

    public boolean enableJoined = true;
    public int fontSize = AndroidUtilities.dp(16);
    public int maxGroupCount = 200;
    public int maxBroadcastCount = 100;
    public int maxMegagroupCount = 5000;
    public int minGroupConvertSize = 200;
    public int maxEditTime = 172800;
    public int groupBigSize;
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

    public static class PrintingUser {
        public int userId;
        public TLRPC.SendMessageAction action;
    }

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
        maxGroupCount = preferences.getInt("maxGroupCount", 200);
        maxMegagroupCount = preferences.getInt("maxMegagroupCount", 1000);
        maxEditTime = preferences.getInt("maxEditTime", 3600);
        groupBigSize = preferences.getInt("groupBigSize", 10);
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
            TLRPC.Chat chat = getInstance().getChat(-id);
            /*if (chat instanceof TLRPC.TL_channel || chat instanceof TLRPC.TL_channelForbidden) {
                inputPeer = new TLRPC.TL_peerChannel();
                inputPeer.channel_id = -id;
            } else*/ {
                inputPeer = new TLRPC.TL_peerChat();
                inputPeer.chat_id = -id;
            }
        } else {
            TLRPC.User user = getInstance().getUser(id);
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
                TLRPC.TL_photos_uploadProfilePhoto req = new TLRPC.TL_photos_uploadProfilePhoto();
                req.caption = "";
                req.crop = new TLRPC.TL_inputPhotoCropAuto();
                req.file = file;
                req.geo_point = new TLRPC.TL_inputGeoPointEmpty();
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

    public TLRPC.EncryptedChat getEncryptedChat(Integer id) {
        return encryptedChats.get(id);
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
                    oldChat.broadcast = chat.broadcast;
                    oldChat.verified = chat.verified;
                    oldChat.megagroup = chat.megagroup;
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
                chat.broadcast = oldChat.broadcast;
                chat.verified = oldChat.verified;
                chat.megagroup = oldChat.megagroup;
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

    protected void clearFullUsers() {
        loadedFullUsers.clear();
        loadedFullChats.clear();
    }

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

    public void blockUser(int user_id) {
        final TLRPC.User user = getUser(user_id);
        if (user == null || blockedUsers.contains(user_id)) {
            return;
        }
        blockedUsers.add(user_id);
        if (user.bot) {
            SearchQuery.removeInline(user_id);
        } else {
            SearchQuery.removePeer(user_id);
        }
        NotificationCenter.getInstance().postNotificationName(NotificationCenter.blockedUsersDidLoaded);
        TLRPC.TL_contacts_block req = new TLRPC.TL_contacts_block();
        req.id = getInputUser(user);
        /*ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {
                if (error == null) {
                    ArrayList<Integer> ids = new ArrayList<>();
                    ids.add(user.id);
                    //MessagesStorage.getInstance().putBlockedUsers(ids, false);
                }
            }
        });*/
    }

    public void unblockUser(int user_id) {
        TLRPC.TL_contacts_unblock req = new TLRPC.TL_contacts_unblock();
        final TLRPC.User user = getUser(user_id);
        if (user == null) {
            return;
        }
        blockedUsers.remove((Integer) user.id);
        req.id = getInputUser(user);
        NotificationCenter.getInstance().postNotificationName(NotificationCenter.blockedUsersDidLoaded);
        /*ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {
                //MessagesStorage.getInstance().deleteBlockedUser(user.id);
            }
        });*/
    }

    public void getBlockedUsers(boolean cache) {
        if (!UserConfig.isClientActivated() || loadingBlockedUsers) {
            return;
        }
        loadingBlockedUsers = true;
        if (cache) {
            //MessagesStorage.getInstance().getBlockedUsers();
        } else {
            TLRPC.TL_contacts_getBlocked req = new TLRPC.TL_contacts_getBlocked();
            req.offset = 0;
            req.limit = 200;
            /*ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                @Override
                public void run(TLObject response, TLRPC.TL_error error) {
                    ArrayList<Integer> blocked = new ArrayList<>();
                    ArrayList<TLRPC.User> users = null;
                    if (error == null) {
                        final TLRPC.contacts_Blocked res = (TLRPC.contacts_Blocked) response;
                        for (TLRPC.TL_contactBlocked contactBlocked : res.blocked) {
                            blocked.add(contactBlocked.user_id);
                        }
                        users = res.users;
                        //MessagesStorage.getInstance().putUsersAndChats(res.users, null, true, true);
                        //MessagesStorage.getInstance().putBlockedUsers(blocked, true);
                    }
                    processLoadedBlockedUsers(blocked, users, false);
                }
            });*/
        }
    }

    public void deleteMessages(ArrayList<Integer> messages, ArrayList<Long> randoms, TLRPC.EncryptedChat encryptedChat, final int channelId) {

    }

    public void deleteDialog(final long did, final int onlyHistory) {
        deleteDialog(did, true, onlyHistory, 0);
    }

    private void deleteDialog(final long did, final boolean first, final int onlyHistory, final int max_id) {

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
        FileLog.e("tmessages", "load cacheOffset = " + offset + " count = " + count + " cache = " + fromCache);
        if (fromCache) {
            //MessagesStorage.getInstance().getDialogs(offset == 0 ? 0 : nextDialogsCacheOffset, count);
        } else {
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

    public int createChat(String title, ArrayList<Integer> selectedContacts, final String about, int type, final BaseFragment fragment) {
        /*
        if (type == ChatObject.CHAT_TYPE_BROADCAST) {
            TLRPC.TL_chat chat = new TLRPC.TL_chat();
            chat.id = UserConfig.lastBroadcastId;
            chat.title = title;
            chat.photo = new TLRPC.TL_chatPhotoEmpty();
            chat.participants_count = selectedContacts.size();
            chat.date = (int) (System.currentTimeMillis() / 1000);
            chat.version = 1;
            UserConfig.lastBroadcastId--;
            putChat(chat, false);
            ArrayList<TLRPC.Chat> chatsArrays = new ArrayList<>();
            chatsArrays.add(chat);
            MessagesStorage.getInstance().putUsersAndChats(null, chatsArrays, true, true);

            TLRPC.TL_chatFull chatFull = new TLRPC.TL_chatFull();
            chatFull.id = chat.id;
            chatFull.chat_photo = new TLRPC.TL_photoEmpty();
            chatFull.notify_settings = new TLRPC.TL_peerNotifySettingsEmpty();
            chatFull.exported_invite = new TLRPC.TL_chatInviteEmpty();
            chatFull.participants = new TLRPC.TL_chatParticipants();
            chatFull.participants.chat_id = chat.id;
            chatFull.participants.admin_id = UserConfig.getClientUserId();
            chatFull.participants.version = 1;
            for (int a = 0; a < selectedContacts.size(); a++) {
                TLRPC.TL_chatParticipant participant = new TLRPC.TL_chatParticipant();
                participant.user_id = selectedContacts.get(a);
                participant.inviter_id = UserConfig.getClientUserId();
                participant.date = (int) (System.currentTimeMillis() / 1000);
                chatFull.participants.participants.add(participant);
            }
            //MessagesStorage.getInstance().updateChatInfo(chatFull, false);

            TLRPC.TL_messageService newMsg = new TLRPC.TL_messageService();
            newMsg.action = new TLRPC.TL_messageActionCreatedBroadcastList();
            newMsg.local_id = newMsg.id = UserConfig.getNewMessageId();
            newMsg.from_id = UserConfig.getClientUserId();
            newMsg.dialog_id = AndroidUtilities.makeBroadcastId(chat.id);
            newMsg.to_id = new TLRPC.TL_peerChat();
            newMsg.to_id.chat_id = chat.id;
            newMsg.date = ConnectionsManager.getInstance().getCurrentTime();
            newMsg.random_id = 0;
            newMsg.flags |= TLRPC.MESSAGE_FLAG_HAS_FROM_ID;
            UserConfig.saveConfig(false);
            MessageObject newMsgObj = new MessageObject(newMsg, users, true);
            newMsgObj.messageOwner.send_state = MessageObject.MESSAGE_SEND_STATE_SENT;

            ArrayList<MessageObject> objArr = new ArrayList<>();
            objArr.add(newMsgObj);
            ArrayList<TLRPC.Message> arr = new ArrayList<>();
            arr.add(newMsg);
            //MessagesStorage.getInstance().putMessages(arr, false, true, false, 0);
            updateInterfaceWithMessages(newMsg.dialog_id, objArr);
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.chatDidCreated, chat.id);

            return 0;
        } else if (type == ChatObject.CHAT_TYPE_CHAT) {
            TLRPC.TL_messages_createChat req = new TLRPC.TL_messages_createChat();
            req.title = title;
            for (int a = 0; a < selectedContacts.size(); a++) {
                TLRPC.User user = getUser(selectedContacts.get(a));
                if (user == null) {
                    continue;
                }
                req.users.add(getInputUser(user));
            }
            return ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                @Override
                public void run(TLObject response, final TLRPC.TL_error error) {
                    if (error != null) {
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                if (error.text.startsWith("FLOOD_WAIT")) {
                                    AlertsCreator.showFloodWaitAlert(error.text, fragment);
                                } else {
                                    AlertsCreator.showAddUserAlert(error.text, fragment, false);
                                }
                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.chatDidFailCreate);
                            }
                        });
                        return;
                    }
                    final TLRPC.Updates updates = (TLRPC.Updates) response;
                    processUpdates(updates, false);
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            putUsers(updates.users, false);
                            putChats(updates.chats, false);
                            if (updates.chats != null && !updates.chats.isEmpty()) {
                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.chatDidCreated, updates.chats.get(0).id);
                            } else {
                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.chatDidFailCreate);
                            }
                        }
                    });
                }
            }, ConnectionsManager.RequestFlagFailOnServerErrors);
        }
        */
        return 0;
    }

    public void addUserToChat(final int chat_id, final TLRPC.User user, final TLRPC.ChatFull info, int count_fwd, String botHash, final BaseFragment fragment) {
    }

    public void deleteUserFromChat(final int chat_id, final TLRPC.User user, final TLRPC.ChatFull info) {
    }

    public void changeChatTitle(int chat_id, String title) {
    }

    public void changeChatAvatar(int chat_id, TLRPC.InputFile uploadedAvatar) {
        TLObject request;
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
        }
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
                dialog.flags = ChatObject.isChannel(chat) ? 1 : 0;
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
                if (DialogObject.isChannel(d)) {
                    TLRPC.Chat chat = getChat(-lower_id);
                    if (chat != null && (chat.megagroup && chat.editor || chat.creator)) {
                        dialogsGroupsOnly.add(d);
                    }
                } else if (lower_id < 0) {
                    if (chatsDict != null) {
                        TLRPC.Chat chat = chatsDict.get(-lower_id);
                        if (chat != null && chat.migrated_to != null) {
                            dialogs.remove(a);
                            a--;
                            continue;
                        }
                    }
                    dialogsGroupsOnly.add(d);
                }
            }
        }
    }

    private static String getRestrictionReason(String reason) {
        if (reason == null || reason.length() == 0) {
            return null;
        }
        int index = reason.indexOf(": ");
        if (index > 0) {
            String type = reason.substring(0, index);
            if (type.contains("-all") || type.contains("-android")) {
                return reason.substring(index + 2);
            }
        }
        return null;
    }

    private static void showCantOpenAlert(BaseFragment fragment, String reason) {
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
        builder.setMessage(reason);
        fragment.showDialog(builder.create());
    }

    public static boolean checkCanOpenChat(Bundle bundle, BaseFragment fragment) {
        return true;
    }

    public static void openChatOrProfileWith(TLRPC.User user, TLRPC.Chat chat, BaseFragment fragment, int type) {
        if (user == null && chat == null || fragment == null) {
            return;
        }
        String reason = null;
        boolean closeLast = false;
        if (chat != null) {
            reason = getRestrictionReason(chat.restriction_reason);
        } else if (user != null) {
            reason = getRestrictionReason(user.restriction_reason);
            if (user.bot) {
                type = 1;
                closeLast = true;
            }
        }
        if (reason != null) {
            showCantOpenAlert(fragment, reason);
        } else {
            Bundle args = new Bundle();
            if (chat != null) {
                args.putInt("chat_id", chat.id);
            } else {
                args.putInt("user_id", user.id);
            }
            if (type == 0) {
                fragment.presentFragment(new ProfileActivity(args));
            } else {
                fragment.presentFragment(new ChatActivity(args), closeLast);
            }
        }
    }

    public static void openByUserName(String username, final BaseFragment fragment, final int type) {
        if (username == null || fragment == null) {
            return;
        }
        TLRPC.User user = getInstance().getUser(username);
        if (user != null) {
            openChatOrProfileWith(user, null, fragment, type);
        } else {
            if (fragment.getParentActivity() == null) {
                return;
            }
            final ProgressDialog progressDialog = new ProgressDialog(fragment.getParentActivity());
            progressDialog.setMessage(LocaleController.getString("Loading", R.string.Loading));
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(false);

            TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
            req.username = username;
            final int reqId = 0; /*ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                @Override
                public void run(final TLObject response, final TLRPC.TL_error error) {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                progressDialog.dismiss();
                            } catch (Exception e) {
                                FileLog.e("tmessages", e);
                            }
                            fragment.setVisibleDialog(null);
                            if (error == null) {
                                TLRPC.TL_contacts_resolvedPeer res = (TLRPC.TL_contacts_resolvedPeer) response;
                                getInstance().putUsers(res.users, false);
                                getInstance().putChats(res.chats, false);
                                //MessagesStorage.getInstance().putUsersAndChats(res.users, res.chats, false, true);
                                if (!res.chats.isEmpty()) {
                                    openChatOrProfileWith(null, res.chats.get(0), fragment, 1);
                                } else if (!res.users.isEmpty()) {
                                    openChatOrProfileWith(res.users.get(0), null, fragment, type);
                                }
                            } else {
                                if (fragment != null && fragment.getParentActivity() != null) {
                                    try {
                                        Toast.makeText(fragment.getParentActivity(), LocaleController.getString("NoUsernameFound", R.string.NoUsernameFound), Toast.LENGTH_SHORT).show();
                                    } catch (Exception e) {
                                        FileLog.e("tmessages", e);
                                    }
                                }
                            }
                        }
                    });
                }
            });
            */
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, LocaleController.getString("Cancel", R.string.Cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ConnectionsManager.getInstance().cancelRequest(reqId, true);
                    try {
                        dialog.dismiss();
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                    if (fragment != null) {
                        fragment.setVisibleDialog(null);
                    }
                }
            });
            fragment.setVisibleDialog(progressDialog);
            progressDialog.show();
        }
    }
}
