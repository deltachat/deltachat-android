/*
 * This is the source code of Telegram for Android v. 2.0.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.b44t.messenger.query;

import com.b44t.messenger.ChatObject;
import com.b44t.messenger.MessagesController;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.NotificationCenter;
import com.b44t.messenger.Utilities;
import com.b44t.messenger.TLRPC;

public class SharedMediaQuery {

    public final static int MEDIA_PHOTOVIDEO = 0;
    public final static int MEDIA_FILE = 1;
    public final static int MEDIA_AUDIO = 2;
    public final static int MEDIA_URL = 3;
    public final static int MEDIA_MUSIC = 4;
    public final static int MEDIA_TYPES_COUNT = 5;

    public static void loadMedia(final long uid, final int offset, final int count, final int max_id, final int type, final boolean fromCache, final int classGuid) {
        final boolean isChannel = (int) uid < 0 && ChatObject.isChannel(-(int) uid);

        int lower_part = (int)uid;
        if (fromCache || lower_part == 0) {
            loadMediaDatabase(uid, offset, count, max_id, type, classGuid, isChannel);
        } else {
            TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
            req.offset = offset;
            req.limit = count + 1;
            req.max_id = max_id;
            if (type == MEDIA_PHOTOVIDEO) {
                req.filter = new TLRPC.TL_inputMessagesFilterPhotoVideo();
            } else if (type == MEDIA_FILE) {
                req.filter = new TLRPC.TL_inputMessagesFilterDocument();
            } else if (type == MEDIA_AUDIO) {
                req.filter = new TLRPC.TL_inputMessagesFilterVoice();
            } else if (type == MEDIA_URL) {
                req.filter = new TLRPC.TL_inputMessagesFilterUrl();
            } else if (type == MEDIA_MUSIC) {
                req.filter = new TLRPC.TL_inputMessagesFilterMusic();
            }
            req.q = "";
            req.peer = MessagesController.getInputPeer(lower_part);
            if (req.peer == null) {
                return;
            }
            /*int reqId = ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                @Override
                public void run(TLObject response, TLRPC.TL_error error) {
                    if (error == null) {
                        final TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                        boolean topReached;
                        if (res.messages.size() > count) {
                            topReached = false;
                            res.messages.remove(res.messages.size() - 1);
                        } else {
                            topReached = true;
                        }
                        processLoadedMedia(res, uid, offset, count, max_id, type, false, classGuid, isChannel, topReached);
                    }
                }
            });
            ConnectionsManager.getInstance().bindRequestToGuid(reqId, classGuid);*/
        }
    }

    public static void getMediaCount(final long uid, final int type, final int classGuid, boolean fromCache) {

        Utilities.globalQueue.postRunnable(new Runnable() {
            @Override
            public void run() {

                int[] media = new int[0];
                if( type == SharedMediaQuery.MEDIA_PHOTOVIDEO ) {
                    media = MrMailbox.MrMailboxGetChatMedia(MrMailbox.hMailbox, (int)uid, MrMailbox.MR_MSG_IMAGE, MrMailbox.MR_MSG_VIDEO);
                }

                NotificationCenter.getInstance().postNotificationName(NotificationCenter.mediaCountDidLoaded,
                            uid, media.length, false /*not from cache*/, type, media);


            }
        });


        /*
        int lower_part = (int)uid;
        if (fromCache || lower_part == 0) {
            getMediaCountDatabase(uid, type, classGuid);
        } else {
            TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
            req.offset = 0;
            req.limit = 1;
            req.max_id = 0;
            if (type == MEDIA_PHOTOVIDEO) {
                req.filter = new TLRPC.TL_inputMessagesFilterPhotoVideo();
            } else if (type == MEDIA_FILE) {
                req.filter = new TLRPC.TL_inputMessagesFilterDocument();
            } else if (type == MEDIA_AUDIO) {
                req.filter = new TLRPC.TL_inputMessagesFilterVoice();
            } else if (type == MEDIA_URL) {
                req.filter = new TLRPC.TL_inputMessagesFilterUrl();
            } else if (type == MEDIA_MUSIC) {
                req.filter = new TLRPC.TL_inputMessagesFilterMusic();
            }
            req.q = "";
            req.peer = MessagesController.getInputPeer(lower_part);
            if (req.peer == null) {
                return;
            }
            int reqId = ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                @Override
                public void run(TLObject response, TLRPC.TL_error error) {
                    if (error == null) {
                        final TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                        //MessagesStorage.getInstance().putUsersAndChats(res.users, res.chats, true, true);
                        int count;
                        if (res instanceof TLRPC.TL_messages_messages) {
                            count = res.messages.size();
                        } else {
                            count = res.count;
                        }
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                MessagesController.getInstance().putUsers(res.users, false);
                                MessagesController.getInstance().putChats(res.chats, false);
                            }
                        });

                        processLoadedMediaCount(count, uid, type, classGuid, false);
                    }
                }
            });
            ConnectionsManager.getInstance().bindRequestToGuid(reqId, classGuid);
        }
        */
    }

    /*
    public static int getMediaType(TLRPC.Message message) {
        if (message == null) {
            return -1;
        }
        if (message.media instanceof TLRPC.TL_messageMediaPhoto) {
            return MEDIA_PHOTOVIDEO;
        } else if (message.media instanceof TLRPC.TL_messageMediaDocument) {
            if (MessageObject.isVoiceMessage(message)) {
                return MEDIA_AUDIO;
            } else if (MessageObject.isVideoMessage(message)) {
                return MEDIA_PHOTOVIDEO;
            } else if (MessageObject.isStickerMessage(message)) {
                return -1;
            } else if (MessageObject.isMusicMessage(message)) {
                return MEDIA_MUSIC;
            } else {
                return MEDIA_FILE;
            }
        } else if (!message.entities.isEmpty()) {
            for (int a = 0; a < message.entities.size(); a++) {
                TLRPC.MessageEntity entity = message.entities.get(a);
                if (entity instanceof TLRPC.TL_messageEntityUrl || entity instanceof TLRPC.TL_messageEntityTextUrl || entity instanceof TLRPC.TL_messageEntityEmail) {
                    return MEDIA_URL;
                }
            }
        }
        return -1;
    }
    */

    /*
    private static void processLoadedMedia(final TLRPC.messages_Messages res, final long uid, int offset, int count, int max_id, final int type, final boolean fromCache, final int classGuid, final boolean isChannel, final boolean topReached) {
        int lower_part = (int)uid;
        if (fromCache && res.messages.isEmpty() && lower_part != 0) {
            loadMedia(uid, offset, count, max_id, type, false, classGuid);
        } else {
            if (!fromCache) {
                ImageLoader.saveMessagesThumbs(res.messages);
                //MessagesStorage.getInstance().putUsersAndChats(res.users, res.chats, true, true);
                putMediaDatabase(uid, type, res.messages, max_id, topReached);
            }

            final HashMap<Integer, TLRPC.User> usersDict = new HashMap<>();
            for (int a = 0; a < res.users.size(); a++) {
                TLRPC.User u = res.users.get(a);
                usersDict.put(u.id, u);
            }
            final ArrayList<MessageObject> objects = new ArrayList<>();
            for (int a = 0; a < res.messages.size(); a++) {
                TLRPC.Message message = res.messages.get(a);
                objects.add(new MessageObject(message, usersDict, true));
            }

            AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    int totalCount = res.count;
                    MessagesController.getInstance().putUsers(res.users, fromCache);
                    MessagesController.getInstance().putChats(res.chats, fromCache);
                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.mediaDidLoaded, uid, totalCount, objects, classGuid, type, topReached);
                }
            });
        }
    }
    */

    private static void loadMediaDatabase(final long uid, final int offset, final int count, final int max_id, final int type, final int classGuid, final boolean isChannel) {
        /*
        MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
            @Override
            public void run() {
                boolean topReached = false;
                TLRPC.TL_messages_messages res = new TLRPC.TL_messages_messages();
                try {
                    ArrayList<Integer> usersToLoad = new ArrayList<>();
                    ArrayList<Integer> chatsToLoad = new ArrayList<>();
                    int countToLoad = count + 1;

                    SQLiteCursor cursor;
                    SQLiteDatabase database = MessagesStorage.getInstance().getDatabase();
                    boolean isEnd = false;
                    if ((int) uid != 0) {
                        int channelId = 0;
                        long messageMaxId = max_id;
                        if (isChannel) {
                            channelId = -(int) uid;
                        }
                        if (messageMaxId != 0 && channelId != 0) {
                            messageMaxId |= ((long) channelId) << 32;
                        }

                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT start FROM media_holes_v2 WHERE uid = %d AND type = %d AND start IN (0, 1)", uid, type));
                        if (cursor.next()) {
                            isEnd = cursor.intValue(0) == 1;
                            cursor.dispose();
                        } else {
                            cursor.dispose();
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT min(mid) FROM media_v2 WHERE uid = %d AND type = %d AND mid > 0", uid, type));
                            if (cursor.next()) {
                                int mid = cursor.intValue(0);
                                if (mid != 0) {
                                    SQLitePreparedStatement state = database.executeFast("REPLACE INTO media_holes_v2 VALUES(?, ?, ?, ?)");
                                    state.requery();
                                    state.bindLong(1, uid);
                                    state.bindInteger(2, type);
                                    state.bindInteger(3, 0);
                                    state.bindInteger(4, mid);
                                    state.step();
                                    state.dispose();
                                }
                            }
                            cursor.dispose();
                        }

                        if (messageMaxId != 0) {
                            long holeMessageId = 0;
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT end FROM media_holes_v2 WHERE uid = %d AND type = %d AND end <= %d ORDER BY end DESC LIMIT 1", uid, type, max_id));
                            if (cursor.next()) {
                                holeMessageId = cursor.intValue(0);
                                if (channelId != 0) {
                                    holeMessageId |= ((long) channelId) << 32;
                                }
                            }
                            cursor.dispose();
                            if (holeMessageId > 1) {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT data, mid FROM media_v2 WHERE uid = %d AND mid > 0 AND mid < %d AND mid >= %d AND type = %d ORDER BY date DESC, mid DESC LIMIT %d", uid, messageMaxId, holeMessageId, type, countToLoad));
                            } else {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT data, mid FROM media_v2 WHERE uid = %d AND mid > 0 AND mid < %d AND type = %d ORDER BY date DESC, mid DESC LIMIT %d", uid, messageMaxId, type, countToLoad));
                            }
                        } else {
                            long holeMessageId = 0;
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT max(end) FROM media_holes_v2 WHERE uid = %d AND type = %d", uid, type));
                            if (cursor.next()) {
                                holeMessageId = cursor.intValue(0);
                                if (channelId != 0) {
                                    holeMessageId |= ((long) channelId) << 32;
                                }
                            }
                            cursor.dispose();
                            if (holeMessageId > 1) {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT data, mid FROM media_v2 WHERE uid = %d AND mid >= %d AND type = %d ORDER BY date DESC, mid DESC LIMIT %d,%d", uid, holeMessageId, type, offset, countToLoad));
                            } else {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT data, mid FROM media_v2 WHERE uid = %d AND mid > 0 AND type = %d ORDER BY date DESC, mid DESC LIMIT %d,%d", uid, type, offset, countToLoad));
                            }
                        }
                    } else {
                        isEnd = true;
                        if (max_id != 0) {
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT m.data, m.mid, r.random_id FROM media_v2 as m LEFT JOIN randoms as r ON r.mid = m.mid WHERE m.uid = %d AND m.mid > %d AND type = %d ORDER BY m.mid ASC LIMIT %d", uid, max_id, type, countToLoad));
                        } else {
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT m.data, m.mid, r.random_id FROM media_v2 as m LEFT JOIN randoms as r ON r.mid = m.mid WHERE m.uid = %d AND type = %d ORDER BY m.mid ASC LIMIT %d,%d", uid, type, offset, countToLoad));
                        }
                    }

                    while (cursor.next()) {
                        NativeByteBuffer data = cursor.byteBufferValue(0);
                        if (data != null) {
                            TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                            data.reuse();
                            message.id = cursor.intValue(1);
                            message.dialog_id = uid;
                            if ((int) uid == 0) {
                                message.random_id = cursor.longValue(2);
                            }
                            res.messages.add(message);
                            if (message.from_id > 0) {
                                if (!usersToLoad.contains(message.from_id)) {
                                    usersToLoad.add(message.from_id);
                                }
                            } else {
                                if (!chatsToLoad.contains(-message.from_id)) {
                                    chatsToLoad.add(-message.from_id);
                                }
                            }
                        }
                    }
                    cursor.dispose();

                    if (!usersToLoad.isEmpty()) {
                        //MessagesStorage.getInstance().getUsersInternal(TextUtils.join(",", usersToLoad), res.users);
                    }
                    if (!chatsToLoad.isEmpty()) {
                        //MessagesStorage.getInstance().getChatsInternal(TextUtils.join(",", chatsToLoad), res.chats);
                    }
                    if (res.messages.size() > count) {
                        topReached = false;
                        res.messages.remove(res.messages.size() - 1);
                    } else {
                        topReached = isEnd;
                    }
                } catch (Exception e) {
                    res.messages.clear();
                    res.chats.clear();
                    res.users.clear();
                    FileLog.e("messenger", e);
                } finally {
                    processLoadedMedia(res, uid, offset, count, max_id, type, true, classGuid, isChannel, topReached);
                }
            }
        });
        */
    }

    /*
    private static void putMediaDatabase(final long uid, final int type, final ArrayList<TLRPC.Message> messages, final int max_id, final boolean topReached) {
        MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    if (messages.isEmpty() || topReached) {
                        MessagesStorage.getInstance().doneHolesInMedia(uid, max_id, type);
                        if (messages.isEmpty()) {
                            return;
                        }
                    }
                    MessagesStorage.getInstance().getDatabase().beginTransaction();
                    SQLitePreparedStatement state2 = MessagesStorage.getInstance().getDatabase().executeFast("REPLACE INTO media_v2 VALUES(?, ?, ?, ?, ?)");
                    for (TLRPC.Message message : messages) {
                        if (canAddMessageToMedia(message)) {

                            long messageId = message.id;
                            if (message.to_id.channel_id != 0) {
                                messageId |= ((long) message.to_id.channel_id) << 32;
                            }

                            state2.requery();
                            NativeByteBuffer data = new NativeByteBuffer(message.getObjectSize());
                            message.serializeToStream(data);
                            state2.bindLong(1, messageId);
                            state2.bindLong(2, uid);
                            state2.bindInteger(3, message.date);
                            state2.bindInteger(4, type);
                            state2.bindByteBuffer(5, data);
                            state2.step();
                            data.reuse();
                        }
                    }
                    state2.dispose();
                    if (!topReached || max_id != 0) {
                        int minId = topReached ? 1 : messages.get(messages.size() - 1).id;
                        if (max_id != 0) {
                            MessagesStorage.getInstance().closeHolesInMedia(uid, minId, max_id, type);
                        } else {
                            MessagesStorage.getInstance().closeHolesInMedia(uid, minId, Integer.MAX_VALUE, type);
                        }
                    }
                    MessagesStorage.getInstance().getDatabase().commitTransaction();
                } catch (Exception e) {
                    FileLog.e("messenger", e);
                }
            }
        });
    }
    */

    public static void loadMusic(final long uid, final int max_id) {
        /*
        MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
            @Override
            public void run() {
                final ArrayList<MessageObject> arrayList = new ArrayList<>();
                try {
                    SQLiteCursor cursor = MessagesStorage.getInstance().getDatabase().queryFinalized(String.format(Locale.US, "SELECT data, mid FROM media_v2 WHERE uid = %d AND mid < %d AND type = %d ORDER BY date DESC, mid DESC LIMIT 1000", uid, max_id, MEDIA_MUSIC));

                    while (cursor.next()) {
                        NativeByteBuffer data = cursor.byteBufferValue(0);
                        if (data != null) {
                            TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                            data.reuse();
                            if (MessageObject.isMusicMessage(message)) {
                                message.id = cursor.intValue(1);
                                message.dialog_id = uid;
                                arrayList.add(0, new MessageObject(mess age, null, false));
                            }
                        }
                    }
                    cursor.dispose();
                } catch (Exception e) {
                    FileLog.e("messenger", e);
                }
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.musicDidLoaded, uid, arrayList);
                    }
                });
            }
        });
        */
    }
}
