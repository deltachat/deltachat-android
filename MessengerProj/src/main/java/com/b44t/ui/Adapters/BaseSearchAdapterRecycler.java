/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.b44t.ui.Adapters;

import com.b44t.messenger.support.widget.RecyclerView;
import com.b44t.messenger.ConnectionsManager;
import com.b44t.messenger.TLObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseSearchAdapterRecycler extends RecyclerView.Adapter {

    protected static class HashtagObject {
        String hashtag;
        int date;
    }

    protected ArrayList<TLObject> globalSearch = new ArrayList<>();
    private int reqId = 0;
    protected String lastFoundUsername = null;

    protected ArrayList<HashtagObject> hashtags;
    protected HashMap<String, HashtagObject> hashtagsByText;
    protected boolean hashtagsLoadedFromDb = false;

    public void queryServerSearch(final String query, final boolean allowChats) {
        if (reqId != 0) {
            ConnectionsManager.getInstance().cancelRequest(reqId, true);
            reqId = 0;
        }
        if (query == null || query.length() < 5) {
            globalSearch.clear();
            notifyDataSetChanged();
            return;
        }
        /*TLRPC.TL_contacts_search req = new TLRPC.TL_contacts_search();
        req.q = query;
        req.limit = 50;*/
        /*final int currentReqId = ++lastReqId; =*/
        reqId = 0; /*ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
            @Override
            public void run(final TLObject response, final TLRPC.TL_error error) {
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if (currentReqId == lastReqId) {
                            if (error == null) {
                                TLRPC.TL_contacts_found res = (TLRPC.TL_contacts_found) response;
                                globalSearch.clear();
                                if (allowChats) {
                                    for (int a = 0; a < res.chats.size(); a++) {
                                        globalSearch.add(res.chats.get(a));
                                    }
                                }
                                for (int a = 0; a < res.users.size(); a++) {
                                    globalSearch.add(res.users.get(a));
                                }
                                lastFoundUsername = query;
                                notifyDataSetChanged();
                            }
                        }
                        reqId = 0;
                    }
                });
            }
        }, ConnectionsManager.RequestFlagFailOnServerErrors);*/
    }

        public void addHashtagsFromMessage(CharSequence message) {
        if (message == null) {
            return;
        }
        boolean changed = false;
        Pattern pattern = Pattern.compile("(^|\\s)#[\\w@\\.]+");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            if (message.charAt(start) != '@' && message.charAt(start) != '#') {
                start++;
            }
            String hashtag = message.subSequence(start, end).toString();
            if (hashtagsByText == null) {
                hashtagsByText = new HashMap<>();
                hashtags = new ArrayList<>();
            }
            HashtagObject hashtagObject = hashtagsByText.get(hashtag);
            if (hashtagObject == null) {
                hashtagObject = new HashtagObject();
                hashtagObject.hashtag = hashtag;
                hashtagsByText.put(hashtagObject.hashtag, hashtagObject);
            } else {
                hashtags.remove(hashtagObject);
            }
            hashtagObject.date = (int) (System.currentTimeMillis() / 1000);
            hashtags.add(0, hashtagObject);
            changed = true;
        }
        if (changed) {
            putRecentHashtags(hashtags);
        }
    }

    private void putRecentHashtags(final ArrayList<HashtagObject> arrayList) {
        /*
        MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    MessagesStorage.getInstance().getDatabase().beginTransaction();
                    SQLitePreparedStatement state = MessagesStorage.getInstance().getDatabase().executeFast("REPLACE INTO hashtag_recent_v2 VALUES(?, ?)");
                    for (int a = 0; a < arrayList.size(); a++) {
                        if (a == 100) {
                            break;
                        }
                        HashtagObject hashtagObject = arrayList.get(a);
                        state.requery();
                        state.bindString(1, hashtagObject.hashtag);
                        state.bindInteger(2, hashtagObject.date);
                        state.step();
                    }
                    state.dispose();
                    MessagesStorage.getInstance().getDatabase().commitTransaction();
                    if (arrayList.size() >= 100) {
                        MessagesStorage.getInstance().getDatabase().beginTransaction();
                        for (int a = 100; a < arrayList.size(); a++) {
                            MessagesStorage.getInstance().getDatabase().executeFast("DELETE FROM hashtag_recent_v2 WHERE id = '" + arrayList.get(a).hashtag + "'").stepThis().dispose();
                        }
                        MessagesStorage.getInstance().getDatabase().commitTransaction();
                    }
                } catch (Exception e) {
                    FileLog.e("messenger", e);
                }
            }
        });
        */
    }

    public void clearRecentHashtags() {
        hashtags = new ArrayList<>();
        hashtagsByText = new HashMap<>();
        /*
        MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    MessagesStorage.getInstance().getDatabase().executeFast("DELETE FROM hashtag_recent_v2 WHERE 1").stepThis().dispose();
                } catch (Exception e) {
                    FileLog.e("messenger", e);
                }
            }
        });
        */
    }

    protected void setHashtags(ArrayList<HashtagObject> arrayList, HashMap<String, HashtagObject> hashMap) {
        hashtags = arrayList;
        hashtagsByText = hashMap;
        hashtagsLoadedFromDb = true;
    }
}
