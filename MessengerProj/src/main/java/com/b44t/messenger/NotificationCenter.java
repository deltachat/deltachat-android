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

import android.util.SparseArray;

import java.util.ArrayList;

public class NotificationCenter {

    private static int totalEvents = 1;

    public static final int connectionStateChanged = totalEvents++;
    public static final int didReceivedNewMessages = totalEvents++;
    public static final int updateInterfaces = totalEvents++;
    public static final int dialogsNeedReload = totalEvents++;
    public static final int closeChats = totalEvents++;
    public static final int messagesDeleted = totalEvents++;
    public static final int messagesSentOrRead = totalEvents++;
    public static final int messageSendError = totalEvents++;
    public static final int contactsDidLoaded = totalEvents++;
    public static final int chatDidCreated = totalEvents++;
    public static final int mediaDidLoaded = totalEvents++;
    public static final int mediaCountDidLoaded = totalEvents++;
    public static final int dialogPhotosLoaded = totalEvents++;
    public static final int notificationsSettingsUpdated = totalEvents++;
    //public static final int pushMessagesUpdated = totalEvents++;
    public static final int blockedUsersDidLoaded = totalEvents++;
    public static final int openedChatChanged = totalEvents++;
    public static final int mainUserInfoChanged = totalEvents++;
    public static final int recentImagesDidLoaded = totalEvents++;
    public static final int waveformCalculated = totalEvents++;
    public static final int didSetPasscode = totalEvents++;
    //public static final int screenStateChanged = totalEvents++; -- currently not used, but this may get handy
    public static final int stickersDidLoaded = totalEvents++;
    public static final int didReplacedPhotoInMemCache = totalEvents++;
    //public static final int musicDidLoaded = totalEvents++;
    public static final int reloadHints = totalEvents++;

    public static final int httpFileDidLoaded = totalEvents++;
    public static final int httpFileDidFailedLoad = totalEvents++;

    public static final int messageThumbGenerated = totalEvents++;

    public static final int wallpapersDidLoaded = totalEvents++;
    public static final int closeOtherAppActivities = totalEvents++;
    public static final int didUpdatedConnectionState = totalEvents++;
    public static final int emojiDidLoaded = totalEvents++;

    public static final int FileLoadProgressChanged = totalEvents++;
    public static final int FileDidLoaded = totalEvents++;
    public static final int FileDidFailedLoad = totalEvents++;
    public static final int FilePreparingStarted = totalEvents++;
    public static final int FileNewChunkAvailable = totalEvents++;
    public static final int FilePreparingFailed = totalEvents++;

    public static final int audioProgressDidChanged = totalEvents++;
    public static final int audioDidReset = totalEvents++;
    public static final int audioPlayStateChanged = totalEvents++;
    public static final int recordProgressChanged = totalEvents++;
    public static final int recordStarted = totalEvents++;
    public static final int recordStartError = totalEvents++;
    public static final int recordStopped = totalEvents++;
    public static final int albumsDidLoaded = totalEvents++;
    public static final int audioDidSent = totalEvents++;
    public static final int audioDidStarted = totalEvents++;
    public static final int audioRouteChanged = totalEvents++;

    private SparseArray<ArrayList<Object>> observers = new SparseArray<>();
    private SparseArray<ArrayList<Object>> removeAfterBroadcast = new SparseArray<>();
    private SparseArray<ArrayList<Object>> addAfterBroadcast = new SparseArray<>();
    private ArrayList<DelayedPost> delayedPosts = new ArrayList<>(10);

    private int broadcasting = 0;
    private boolean animationInProgress;

    private int[] allowedNotifications;

    public interface NotificationCenterDelegate {
        void didReceivedNotification(int id, Object... args);
    }

    private class DelayedPost {

        private DelayedPost(int id, Object[] args) {
            this.id = id;
            this.args = args;
        }

        private int id;
        private Object[] args;
    }

    private static volatile NotificationCenter Instance = null;

    public static NotificationCenter getInstance() {
        NotificationCenter localInstance = Instance;
        if (localInstance == null) {
            synchronized (NotificationCenter.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new NotificationCenter();
                }
            }
        }
        return localInstance;
    }

    public void setAllowedNotificationsDutingAnimation(int notifications[]) {
        allowedNotifications = notifications;
    }

    public void setAnimationInProgress(boolean value) {
        animationInProgress = value;
        if (!animationInProgress && !delayedPosts.isEmpty()) {
            for (DelayedPost delayedPost : delayedPosts) {
                postNotificationNameInternal(delayedPost.id, true, delayedPost.args);
            }
            delayedPosts.clear();
        }
    }


    public boolean isAnimationInProgress() {
        return animationInProgress;
    }


    public void postNotificationName(int id, Object... args) {
        boolean allowDuringAnimation = false;
        if (allowedNotifications != null) {
            for (int a = 0; a < allowedNotifications.length; a++) {
                if (allowedNotifications[a] == id) {
                    allowDuringAnimation = true;
                    break;
                }
            }
        }
        postNotificationNameInternal(id, allowDuringAnimation, args);
    }

    public void postNotificationNameInternal(int id, boolean allowDuringAnimation, Object... args) {
        if (BuildVars.DEBUG_VERSION) {
            if (Thread.currentThread() != ApplicationLoader.applicationHandler.getLooper().getThread()) {
                throw new RuntimeException("postNotificationName allowed only from MAIN thread");
            }
        }
        if (!allowDuringAnimation && animationInProgress) {
            DelayedPost delayedPost = new DelayedPost(id, args);
            delayedPosts.add(delayedPost);
            if (BuildVars.DEBUG_VERSION) {
                FileLog.e("messenger", "delay post notification " + id + " with args count = " + args.length);
            }
            return;
        }
        broadcasting++;
        ArrayList<Object> objects = observers.get(id);
        if (objects != null && !objects.isEmpty()) {
            for (int a = 0; a < objects.size(); a++) {
                Object obj = objects.get(a);
                ((NotificationCenterDelegate) obj).didReceivedNotification(id, args);
            }
        }
        broadcasting--;
        if (broadcasting == 0) {
            if (removeAfterBroadcast.size() != 0) {
                for (int a = 0; a < removeAfterBroadcast.size(); a++) {
                    int key = removeAfterBroadcast.keyAt(a);
                    ArrayList<Object> arrayList = removeAfterBroadcast.get(key);
                    for (int b = 0; b < arrayList.size(); b++) {
                        removeObserver(arrayList.get(b), key);
                    }
                }
                removeAfterBroadcast.clear();
            }
            if (addAfterBroadcast.size() != 0) {
                for (int a = 0; a < addAfterBroadcast.size(); a++) {
                    int key = addAfterBroadcast.keyAt(a);
                    ArrayList<Object> arrayList = addAfterBroadcast.get(key);
                    for (int b = 0; b < arrayList.size(); b++) {
                        addObserver(arrayList.get(b), key);
                    }
                }
                addAfterBroadcast.clear();
            }
        }
    }

    public void addObserver(Object observer, int id) {
        if (BuildVars.DEBUG_VERSION) {
            if (Thread.currentThread() != ApplicationLoader.applicationHandler.getLooper().getThread()) {
                throw new RuntimeException("addObserver allowed only from MAIN thread");
            }
        }
        if (broadcasting != 0) {
            ArrayList<Object> arrayList = addAfterBroadcast.get(id);
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                addAfterBroadcast.put(id, arrayList);
            }
            arrayList.add(observer);
            return;
        }
        ArrayList<Object> objects = observers.get(id);
        if (objects == null) {
            observers.put(id, (objects = new ArrayList<>()));
        }
        if (objects.contains(observer)) {
            return;
        }
        objects.add(observer);
    }

    public void removeObserver(Object observer, int id) {
        if (BuildVars.DEBUG_VERSION) {
            if (Thread.currentThread() != ApplicationLoader.applicationHandler.getLooper().getThread()) {
                throw new RuntimeException("removeObserver allowed only from MAIN thread");
            }
        }
        if (broadcasting != 0) {
            ArrayList<Object> arrayList = removeAfterBroadcast.get(id);
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                removeAfterBroadcast.put(id, arrayList);
            }
            arrayList.add(observer);
            return;
        }
        ArrayList<Object> objects = observers.get(id);
        if (objects != null) {
            objects.remove(observer);
        }
    }
}
