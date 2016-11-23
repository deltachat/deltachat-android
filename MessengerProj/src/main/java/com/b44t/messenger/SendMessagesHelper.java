/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.b44t.messenger;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.b44t.messenger.audioinfo.AudioInfo;
import com.b44t.messenger.query.DraftQuery;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;

public class SendMessagesHelper implements NotificationCenter.NotificationCenterDelegate {

    //private TLRPC.ChatFull currentChatInfo = null;
    //private HashMap<String, ArrayList<DelayedMessage>> delayedMessages = new HashMap<>();
    //private HashMap<Integer, MessageObject> unsentMessages = new HashMap<>();
    //private HashMap<Integer, TLRPC.Message> sendingMessages = new HashMap<>();
    //private HashMap<String, MessageObject> waitingForLocation = new HashMap<>();
    //private HashMap<String, MessageObject> waitingForCallback = new HashMap<>();

    /*
    private LocationProvider locationProvider = new LocationProvider(new LocationProvider.LocationProviderDelegate() {
        @Override
        public void onLocationAcquired(Location location) {
            sendLocation(location);
            waitingForLocation.clear();
        }

        @Override
        public void onUnableLocationAcquire() {
            HashMap<String, MessageObject> waitingForLocationCopy = new HashMap<>(waitingForLocation);
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.wasUnableToFindCurrentLocation, waitingForLocationCopy);
            waitingForLocation.clear();
        }
    });

    public static class LocationProvider {

        public interface LocationProviderDelegate {
            void onLocationAcquired(Location location);
            void onUnableLocationAcquire();
        }

        private LocationProviderDelegate delegate;
        private LocationManager locationManager;
        private GpsLocationListener gpsLocationListener = new GpsLocationListener();
        private GpsLocationListener networkLocationListener = new GpsLocationListener();
        private Runnable locationQueryCancelRunnable;
        private Location lastKnownLocation;

        private class GpsLocationListener implements LocationListener {

            @Override
            public void onLocationChanged(Location location) {
                if (location == null || locationQueryCancelRunnable == null) {
                    return;
                }
                FileLog.e("messenger", "found location " + location);
                lastKnownLocation = location;
                if (location.getAccuracy() < 100) {
                    if (delegate != null) {
                        delegate.onLocationAcquired(location);
                    }
                    if (locationQueryCancelRunnable != null) {
                        AndroidUtilities.cancelRunOnUIThread(locationQueryCancelRunnable);
                    }
                    cleanup();
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        }

        public LocationProvider() {

        }

        public LocationProvider(LocationProviderDelegate locationProviderDelegate) {
            delegate = locationProviderDelegate;
        }

        public void setDelegate(LocationProviderDelegate locationProviderDelegate) {
            delegate = locationProviderDelegate;
        }

        private void cleanup() {
            locationManager.removeUpdates(gpsLocationListener);
            locationManager.removeUpdates(networkLocationListener);
            lastKnownLocation = null;
            locationQueryCancelRunnable = null;
        }

        public void start() {
            if (locationManager == null) {
                locationManager = (LocationManager) ApplicationLoader.applicationContext.getSystemService(Context.LOCATION_SERVICE);
            }
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 0, gpsLocationListener);
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
            try {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 0, networkLocationListener);
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
            try {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation == null) {
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
            if (locationQueryCancelRunnable != null) {
                AndroidUtilities.cancelRunOnUIThread(locationQueryCancelRunnable);
            }
            locationQueryCancelRunnable = new Runnable() {
                @Override
                public void run() {
                    if (locationQueryCancelRunnable != this) {
                        return;
                    }
                    if (delegate != null) {
                        if (lastKnownLocation != null) {
                            delegate.onLocationAcquired(lastKnownLocation);
                        } else {
                            delegate.onUnableLocationAcquire();
                        }
                    }
                    cleanup();
                }
            };
            AndroidUtilities.runOnUIThread(locationQueryCancelRunnable, 5000);
        }

        public void stop() {
            if (locationManager == null) {
                return;
            }
            if (locationQueryCancelRunnable != null) {
                AndroidUtilities.cancelRunOnUIThread(locationQueryCancelRunnable);

            }
            cleanup();
        }
    }
    */


    protected class DelayedMessage {
        public TLObject sendRequest;
        public int type;
        public String originalPath;
        public TLRPC.FileLocation location;
        public TLRPC.TL_document documentLocation;
        public String httpLocation;
        public MessageObject obj;
        public VideoEditedInfo videoEditedInfo;
    }

    private static volatile SendMessagesHelper Instance = null;

    public static SendMessagesHelper getInstance() {
        SendMessagesHelper localInstance = Instance;
        if (localInstance == null) {
            synchronized (SendMessagesHelper.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new SendMessagesHelper();
                }
            }
        }
        return localInstance;
    }

    public SendMessagesHelper() {
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileDidUpload);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileDidFailUpload);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FilePreparingStarted);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileNewChunkAvailable);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FilePreparingFailed);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.httpFileDidFailedLoad);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.httpFileDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileDidFailedLoad);
    }

    public void cleanup() {
        //delayedMessages.clear();
        //unsentMessages.clear();
        //sendingMessages.clear();
        //waitingForLocation.clear();
        //waitingForCallback.clear();
        //currentChatInfo = null;
        //locationProvider.stop();
    }

    /*
    public void setCurrentChatInfo(TLRPC.ChatFull info) {
        currentChatInfo = info;
    }
    */

    @Override
    public void didReceivedNotification(int id, final Object... args) {
        if (id == NotificationCenter.FileDidUpload) {
            /*
            final String location = (String) args[0];
            final TLRPC.InputFile file = (TLRPC.InputFile) args[1];
            final TLRPC.InputEncryptedFile encryptedFile = (TLRPC.InputEncryptedFile) args[2];
            ArrayList<DelayedMessage> arr = delayedMessages.get(location);
            if (arr != null) {
                for (int a = 0; a < arr.size(); a++) {
                    DelayedMessage message = arr.get(a);
                    TLRPC.InputMedia media = null;
                    if (message.sendRequest instanceof TLRPC.TL_messages_sendMedia) {
                        media = ((TLRPC.TL_messages_sendMedia) message.sendRequest).media;
                    } else if (message.sendRequest instanceof TLRPC.TL_messages_sendBroadcast) {
                        media = ((TLRPC.TL_messages_sendBroadcast) message.sendRequest).media;
                    }

                    if (file != null && media != null) {
                        if (message.type == 0) {
                            media.file = file;
                            performSendMessageRequest(message.sendRequest, message.obj, message.originalPath);
                        } else if (message.type == 1) {
                            if (media.file == null) {
                                media.file = file;
                                if (media.thumb == null && message.location != null) {
                                    performSendDelayedMessage(message);
                                } else {
                                    performSendMessageRequest(message.sendRequest, message.obj, message.originalPath);
                                }
                            } else {
                                media.thumb = file;
                                performSendMessageRequest(message.sendRequest, message.obj, message.originalPath);
                            }
                        } else if (message.type == 2) {
                            if (media.file == null) {
                                media.file = file;
                                if (media.thumb == null && message.location != null) {
                                    performSendDelayedMessage(message);
                                } else {
                                    performSendMessageRequest(message.sendRequest, message.obj, message.originalPath);
                                }
                            } else {
                                media.thumb = file;
                                performSendMessageRequest(message.sendRequest, message.obj, message.originalPath);
                            }
                        } else if (message.type == 3) {
                            media.file = file;
                            performSendMessageRequest(message.sendRequest, message.obj, message.originalPath);
                        }
                        arr.remove(a);
                        a--;
                    } else if (encryptedFile != null && message.sendEncryptedRequest != null) {
                        if (message.sendEncryptedRequest.media instanceof TLRPC.TL_decryptedMessageMediaVideo || message.sendEncryptedRequest.media instanceof TLRPC.TL_decryptedMessageMediaPhoto) {
                            long size = (Long) args[5];
                            message.sendEncryptedRequest.media.size = (int) size;
                        }
                        message.sendEncryptedRequest.media.key = (byte[]) args[3];
                        message.sendEncryptedRequest.media.iv = (byte[]) args[4];
                        //SecretChatHelper.getInstance().performSendEncryptedRequest(message.sendEncryptedRequest, message.obj.messageOwner, message.encryptedChat, encryptedFile, message.originalPath, message.obj);
                        arr.remove(a);
                        a--;
                    }
                }
                if (arr.isEmpty()) {
                    delayedMessages.remove(location);
                }
            }
            */
        } else if (id == NotificationCenter.FileDidFailUpload) {
            /*
            final String location = (String) args[0];
            final boolean enc = (Boolean) args[1];
            ArrayList<DelayedMessage> arr = delayedMessages.get(location);
            if (arr != null) {
                for (int a = 0; a < arr.size(); a++) {
                    DelayedMessage obj = arr.get(a);
                    if (enc && obj.sendEncryptedRequest != null || !enc && obj.sendRequest != null) {
                        //MessagesStorage.getInstance().markMessageAsSendError(obj.obj.messageOwner);
                        obj.obj.messageOwner.send_state = MessageObject.MESSAGE_SEND_STATE_SEND_ERROR;
                        arr.remove(a);
                        a--;
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.messageSendError, obj.obj.getId());
                        processSentMessage(obj.obj.getId());
                    }
                }
                if (arr.isEmpty()) {
                    delayedMessages.remove(location);
                }
            }
            */
        } else if (id == NotificationCenter.FilePreparingStarted) {
            /*
            MessageObject messageObject = (MessageObject) args[0];
            String finalPath = (String) args[1];
            ArrayList<DelayedMessage> arr = delayedMessages.get(messageObject.messageOwner.attachPath);
            if (arr != null) {
                for (int a = 0; a < arr.size(); a++) {
                    DelayedMessage message = arr.get(a);
                    if (message.obj == messageObject) {
                        message.videoEditedInfo = null;
                        performSendDelayedMessage(message);
                        arr.remove(a);
                        break;
                    }
                }
                if (arr.isEmpty()) {
                    delayedMessages.remove(messageObject.messageOwner.attachPath);
                }
            }
            */
        } else if (id == NotificationCenter.FileNewChunkAvailable) {
            //MessageObject messageObject = (MessageObject) args[0];
            //String finalPath = (String) args[1];
            //long finalSize = (Long) args[2];
            //boolean isEncrypted = false;//((int) messageObject.getDialogId()) == 0;
            //FileLoader.getInstance().checkUploadNewDataAvailable(finalPath, isEncrypted, finalSize);
            //if (finalSize != 0) {
                /*
                ArrayList<DelayedMessage> arr = delayedMessages.get(messageObject.messageOwner.attachPath);
                if (arr != null) {
                    for (int a = 0; a < arr.size(); a++) {
                        DelayedMessage message = arr.get(a);
                        if (message.obj == messageObject) {
                            message.obj.videoEditedInfo = null;
                            message.obj.messageOwner.message = "-1";
                            message.obj.messageOwner.media.document.size = (int) finalSize;

                            ArrayList<TLRPC.Message> messages = new ArrayList<>();
                            messages.add(message.obj.messageOwner);
                            //MessagesStorage.getInstance().putMessages(messages, false, true, false, 0);
                            break;
                        }
                    }
                    if (arr.isEmpty()) {
                        delayedMessages.remove(messageObject.messageOwner.attachPath);
                    }
                }
                */
            //}
        } else if (id == NotificationCenter.FilePreparingFailed) {
            MessageObject messageObject = (MessageObject) args[0];
            //String finalPath = (String) args[1];
            stopVideoService(messageObject.messageOwner.attachPath);
            /*
            ArrayList<DelayedMessage> arr = delayedMessages.get(finalPath);
            if (arr != null) {
                for (int a = 0; a < arr.size(); a++) {
                    DelayedMessage message = arr.get(a);
                    if (message.obj == messageObject) {
                        //MessagesStorage.getInstance().markMessageAsSendError(message.obj.messageOwner);
                        message.obj.messageOwner.send_state = MessageObject.MESSAGE_SEND_STATE_SEND_ERROR;
                        arr.remove(a);
                        a--;
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.messageSendError, message.obj.getId());
                        processSentMessage(message.obj.getId());
                    }
                }
                if (arr.isEmpty()) {
                    delayedMessages.remove(finalPath);
                }
            }
            */
        } else if (id == NotificationCenter.httpFileDidLoaded) {
            /*
            String path = (String) args[0];
            ArrayList<DelayedMessage> arr = delayedMessages.get(path);
            if (arr != null) {
                for (int a = 0; a < arr.size(); a++) {
                    final DelayedMessage message = arr.get(a);
                    if (message.type == 0) {
                        String md5 = Utilities.MD5(message.httpLocation) + "." + ImageLoader.getHttpUrlExtension(message.httpLocation, "file");
                        final File cacheFile = new File(FileLoader.getInstance().getDirectory(FileLoader.MEDIA_DIR_CACHE), md5);
                        Utilities.globalQueue.postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                final TLRPC.TL_photo photo = SendMessagesHelper.getInstance().generatePhotoSizes(cacheFile.toString(), null);
                                AndroidUtilities.runOnUIThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (photo != null) {
                                            message.httpLocation = null;
                                            message.obj.messageOwner.media.photo = photo;
                                            message.obj.messageOwner.attachPath = cacheFile.toString();
                                            message.location = photo.sizes.get(photo.sizes.size() - 1).location;
                                            ArrayList<TLRPC.Message> messages = new ArrayList<>();
                                            messages.add(message.obj.messageOwner);
                                            //MessagesStorage.getInstance().putMessages(messages, false, true, false, 0);
                                            performSendDelayedMessage(message);
                                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateMessageMedia, message.obj);
                                        } else {
                                            FileLog.e("messenger", "can't load image " + message.httpLocation + " to file " + cacheFile.toString());
                                            //MessagesStorage.getInstance().markMessageAsSendError(message.obj.messageOwner);
                                            message.obj.messageOwner.send_state = MessageObject.MESSAGE_SEND_STATE_SEND_ERROR;
                                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.messageSendError, message.obj.getId());
                                            processSentMessage(message.obj.getId());
                                        }
                                    }
                                });
                            }
                        });
                    } else if (message.type == 2) {
                        String md5 = Utilities.MD5(message.httpLocation) + ".gif";
                        final File cacheFile = new File(FileLoader.getInstance().getDirectory(FileLoader.MEDIA_DIR_CACHE), md5);
                        Utilities.globalQueue.postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                if (message.documentLocation.thumb.location instanceof TLRPC.TL_fileLocationUnavailable) {
                                    try {
                                        Bitmap bitmap = ImageLoader.loadBitmap(cacheFile.getAbsolutePath(), null, 90, 90, true);
                                        if (bitmap != null) {
                                            message.documentLocation.thumb = ImageLoader.scaleAndSaveImage(bitmap, 90, 90, 55, message.sendEncryptedRequest != null);
                                            bitmap.recycle();
                                        }
                                    } catch (Exception e) {
                                        message.documentLocation.thumb = null;
                                        FileLog.e("messenger", e);
                                    }
                                    if (message.documentLocation.thumb == null) {
                                        message.documentLocation.thumb = new TLRPC.TL_photoSizeEmpty();
                                        message.documentLocation.thumb.type = "s";
                                    }
                                }
                                AndroidUtilities.runOnUIThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        message.httpLocation = null;
                                        message.obj.messageOwner.attachPath = cacheFile.toString();
                                        message.location = message.documentLocation.thumb.location;
                                        ArrayList<TLRPC.Message> messages = new ArrayList<>();
                                        messages.add(message.obj.messageOwner);
                                        //MessagesStorage.getInstance().putMessages(messages, false, true, false, 0);
                                        performSendDelayedMessage(message);
                                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateMessageMedia, message.obj);
                                    }
                                });
                            }
                        });
                    }
                }
                delayedMessages.remove(path);
            }
            */
        } else if (id == NotificationCenter.FileDidLoaded) {
            /*
            String path = (String) args[0];
            ArrayList<DelayedMessage> arr = delayedMessages.get(path);
            if (arr != null) {
                for (int a = 0; a < arr.size(); a++) {
                    performSendDelayedMessage(arr.get(a));
                }
                delayedMessages.remove(path);
            }
            */
        } else if (id == NotificationCenter.httpFileDidFailedLoad || id == NotificationCenter.FileDidFailedLoad) {
            /*
            String path = (String) args[0];
            ArrayList<DelayedMessage> arr = delayedMessages.get(path);
            if (arr != null) {
                for (DelayedMessage message : arr) {
                    //MessagesStorage.getInstance().markMessageAsSendError(message.obj.messageOwner);
                    message.obj.messageOwner.send_state = MessageObject.MESSAGE_SEND_STATE_SEND_ERROR;
                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.messageSendError, message.obj.getId());
                    processSentMessage(message.obj.getId());
                }
                delayedMessages.remove(path);
            }
            */
        }
    }

    public void cancelSendingMessage(MessageObject object) {
        /*
        String keyToRemvoe = null;
        boolean enc = false;
        for (HashMap.Entry<String, ArrayList<DelayedMessage>> entry : delayedMessages.entrySet()) {
            ArrayList<DelayedMessage> messages = entry.getValue();
            for (int a = 0; a < messages.size(); a++) {
                DelayedMessage message = messages.get(a);
                if (message.obj.getId() == object.getId()) {
                    messages.remove(a);
                    MediaController.getInstance().cancelVideoConvert(message.obj);
                    if (messages.size() == 0) {
                        keyToRemvoe = entry.getKey();
                        if (message.sendEncryptedRequest != null) {
                            enc = true;
                        }
                    }
                    break;
                }
            }
        }
        if (keyToRemvoe != null) {
            if (keyToRemvoe.startsWith("http")) {
                ImageLoader.getInstance().cancelLoadHttpFile(keyToRemvoe);
            } else {
                FileLoader.getInstance().cancelUploadFile(keyToRemvoe, enc);
            }
            stopVideoService(keyToRemvoe);
        }
        */
        ArrayList<Integer> messages = new ArrayList<>();
        messages.add(object.getId());
        MessagesController.getInstance().deleteMessages(messages, null, null, object.messageOwner.to_id.channel_id);
    }

    public void sendSticker(TLRPC.Document document, long peer, MessageObject replyingMessageObject) {
        if (document == null) {
            return;
        }
        if ((int) peer == 0) {
            int high_id = (int) (peer >> 32);
            TLRPC.EncryptedChat encryptedChat = MessagesController.getInstance().getEncryptedChat(high_id);
            if (encryptedChat == null) {
                return;
            }
            if (document.thumb instanceof TLRPC.TL_photoSize) {
                File file = FileLoader.getPathToAttach(document.thumb, true);
                if (file.exists()) {
                    try {
                        int len = (int) file.length();
                        byte[] arr = new byte[(int) file.length()];
                        RandomAccessFile reader = new RandomAccessFile(file, "r");
                        reader.readFully(arr);
                        TLRPC.TL_document newDocument = new TLRPC.TL_document();
                        newDocument.thumb = new TLRPC.TL_photoCachedSize();
                        newDocument.thumb.location = document.thumb.location;
                        newDocument.thumb.size = document.thumb.size;
                        newDocument.thumb.w = document.thumb.w;
                        newDocument.thumb.h = document.thumb.h;
                        newDocument.thumb.type = document.thumb.type;
                        newDocument.thumb.bytes = arr;

                        newDocument.id = document.id;
                        newDocument.access_hash = document.access_hash;
                        newDocument.date = document.date;
                        newDocument.mime_type = document.mime_type;
                        newDocument.size = document.size;
                        newDocument.dc_id = document.dc_id;
                        newDocument.attributes = document.attributes;
                        if (newDocument.mime_type == null) {
                            newDocument.mime_type = "";
                        }
                        document = newDocument;
                    } catch (Exception e) {
                        FileLog.e("messenger", e);
                    }
                }
            }
        }
        SendMessagesHelper.getInstance().sendMessageDocument((TLRPC.TL_document) document, null, null, peer, replyingMessageObject, null);
    }

    public void sendMessageContact(TLRPC.User user, long peer, MessageObject reply_to_msg, HashMap<String, String> params) {
        sendMessage__(null, null, null, null, user, null, peer, null, reply_to_msg, null, null, params);
    }

    public void sendMessageDocument(TLRPC.TL_document document, VideoEditedInfo videoEditedInfo, String path, long peer, MessageObject reply_to_msg, HashMap<String, String> params) {
        sendMessage__(null, null, null, videoEditedInfo, null, document, peer, path, reply_to_msg, null, null, params);
    }

    public void sendMessageText(String message, long peer, MessageObject reply_to_msg, TLRPC.WebPage webPage, boolean searchLinks, ArrayList<TLRPC.MessageEntity> entities, HashMap<String, String> params) {
        sendMessage__(message, null, null, null, null, null, peer, null, reply_to_msg, webPage, entities, params);
    }

    public void sendMessagePhoto(TLRPC.TL_photo photo, String path, long peer, MessageObject reply_to_msg, HashMap<String, String> params) {
        sendMessage__(null, null, photo, null, null, null, peer, path, reply_to_msg, null, null, params);
    }

    private void sendMessage__(String message,
                             TLRPC.MessageMedia location,
                             TLRPC.TL_photo photo,
                             VideoEditedInfo videoEditedInfo,
                             TLRPC.User user,
                             TLRPC.TL_document document,
                             long peer,
                             String path,
                             MessageObject reply_to_msg,
                             TLRPC.WebPage webPage,
                             ArrayList<TLRPC.MessageEntity> entities,
                             HashMap<String, String> params) {
        if (peer == 0) { // peer == dialog id
            return;
        }

        String originalPath = null;
        if (params != null && params.containsKey("originalPath")) {
            originalPath = params.get("originalPath");
        }

        TLRPC.Message newMsg = null;    // data
        MessageObject newMsgObj = null; // drawing
        int type = -1;
        int lower_id = (int) peer;
        //int high_id = (int) (peer >> 32);
        //boolean isChannel = false;
        //TLRPC.EncryptedChat encryptedChat = null;
        //TLRPC.InputPeer sendToPeer = lower_id != 0 ? MessagesController.getInputPeer(lower_id) : null;
        //ArrayList<TLRPC.InputUser> sendToPeers = null;
        /*if (lower_id == 0) {
            encryptedChat = MessagesController.getInstance().getEncryptedChat(high_id);
            if (encryptedChat == null) {
                if (retryMessageObject != null) {
                    //MessagesStorage.getInstance().markMessageAsSendError(retryMessageObject.messageOwner);
                    retryMessageObject.messageOwner.send_state = MessageObject.MESSAGE_SEND_STATE_SEND_ERROR;
                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.messageSendError, retryMessageObject.getId());
                    processSentMessage(retryMessageObject.getId());
                }
                return;
            }
        } else  if (sendToPeer instanceof TLRPC.TL_inputPeerChannel) {
            TLRPC.Chat chat = MessagesController.getInstance().getChat(sendToPeer.channel_id);
            isChannel = chat != null && !chat.megagroup;
        }*/

        try {
            {
                if (message != null) {
                    newMsg = new TLRPC.TL_message();
                    if (entities != null && !entities.isEmpty()) {
                        newMsg.entities = entities;
                    }
                    if (webPage == null) {
                        newMsg.media = new TLRPC.TL_messageMediaEmpty();
                    } else {
                        newMsg.media = new TLRPC.TL_messageMediaWebPage();
                        newMsg.media.webpage = webPage;
                    }
                    if (params != null && params.containsKey("query_id")) {
                        type = 9;
                    } else {
                        type = 0;
                    }
                    newMsg.message = message;
                } else if (location != null) {
                    newMsg = new TLRPC.TL_message();
                    newMsg.media = location;
                    newMsg.message = "";
                    if (params != null && params.containsKey("query_id")) {
                        type = 9;
                    } else {
                        type = 1;
                    }
                } else if (photo != null) {
                    newMsg = new TLRPC.TL_message();
                    newMsg.media = new TLRPC.TL_messageMediaPhoto();
                    newMsg.media.caption = photo.caption != null ? photo.caption : "";
                    newMsg.media.photo = photo;
                    if (params != null && params.containsKey("query_id")) {
                        type = 9;
                    } else {
                        type = 2;
                    }
                    newMsg.message = "-1";
                    if (path != null && path.length() > 0 && path.startsWith("http")) {
                        newMsg.attachPath = path;
                    } else {
                        TLRPC.FileLocation location1 = photo.sizes.get(photo.sizes.size() - 1).location;
                        newMsg.attachPath = FileLoader.getPathToAttach(location1, true).toString();
                    }
                } else if (user != null) {
                    newMsg = new TLRPC.TL_message();
                    newMsg.media = new TLRPC.TL_messageMediaContact();
                    newMsg.media.phone_number = user.phone;
                    newMsg.media.first_name = user.first_name;
                    newMsg.media.last_name = user.last_name;
                    newMsg.media.user_id = user.id;
                    if (newMsg.media.first_name == null) {
                        user.first_name = newMsg.media.first_name = "";
                    }
                    if (newMsg.media.last_name == null) {
                        user.last_name = newMsg.media.last_name = "";
                    }
                    newMsg.message = "";
                    if (params != null && params.containsKey("query_id")) {
                        type = 9;
                    } else {
                        type = 6;
                    }
                } else if (document != null) {
                    // AUDIO, VIDEO etc.
                    newMsg = new TLRPC.TL_message();
                    newMsg.media = new TLRPC.TL_messageMediaDocument();
                    newMsg.media.caption = document.caption != null ? document.caption : "";
                    newMsg.media.document = document;
                    if (params != null && params.containsKey("query_id")) {
                        type = 9;
                    } else if (MessageObject.isVideoDocument(document)) {
                        type = 3;
                    } else if (MessageObject.isVoiceDocument(document)) {
                        type = 8;
                    } else {
                        type = 7;
                    }
                    if (videoEditedInfo == null) {
                        newMsg.message = "-1";
                    } else {
                        newMsg.message = videoEditedInfo.getString();
                    }
                    newMsg.attachPath = path;
                }
                if (newMsg.attachPath == null) {
                    newMsg.attachPath = "";
                }
                /*newMsg.local_id =*/ newMsg.id = UserConfig.getNewMessageId();
                newMsg.out = true;
                newMsg.from_id = UserConfig.getClientUserId();
                newMsg.flags |= TLRPC.MESSAGE_FLAG_HAS_FROM_ID;
                UserConfig.saveConfig(false);
            }

            if (newMsg.random_id == 0) {
                newMsg.random_id = getNextRandomId();
            }
            newMsg.params = params;
            newMsg.date = ConnectionsManager.getInstance().getCurrentTime();
            newMsg.flags |= TLRPC.MESSAGE_FLAG_HAS_MEDIA;
            newMsg.unread = true;
            newMsg.dialog_id = peer;
            if (reply_to_msg != null) {
                newMsg.flags |= TLRPC.MESSAGE_FLAG_REPLY;
                newMsg.reply_to_msg_id = reply_to_msg.getId();
            }

            if (lower_id != 0) {
                {
                    newMsg.to_id = MessagesController.getPeer(lower_id);
                    if (lower_id > 0) {
                        TLRPC.User sendToUser = MessagesController.getInstance().getUser(lower_id);
                        if (sendToUser == null) {
                            //processSentMessage(newMsg.id);
                            return;
                        }
                        /*if (sendToUser.bot) {
                            newMsg.unread = false;
                        }*/
                    }
                }
            }

            newMsg.media_unread = true;

            newMsg.send_state = MessageObject.MESSAGE_SEND_STATE_SENDING;
            newMsgObj = new MessageObject(newMsg, null, true);
            newMsgObj.replyMessageObject = reply_to_msg;
            if (!newMsgObj.isForwarded() && newMsgObj.type == 3) {
                newMsgObj.attachPathExists = true;
            }

            ArrayList<MessageObject> objArr = new ArrayList<>();
            objArr.add(newMsgObj);
            ArrayList<TLRPC.Message> arr = new ArrayList<>();
            arr.add(newMsg);
            //MessagesStorage.getInstance().putMessages(arr, false, true, false, 0);



            // SEND MESSAGE
            // -------------------------------------------------------------------------------------

            MrChat mrChat = MrMailbox.getChat((int)peer);

            if (type == 0 /*|| type == 9 && message != null && encryptedChat != null*/ )
            {
                newMsg.id = mrChat.sendText(newMsg.message);
                DraftQuery.cleanDraft(peer, false);

                MrMailbox.reloadMainChatlist();
            }
            else if (type >= 1 && type <= 3 || type >= 5 && type <= 8 /*|| type == 9 && encryptedChat != null*/) {
                TLRPC.InputMedia inputMedia = null;
                DelayedMessage delayedMessage = null;
                if (type == 1) {
                    if (location instanceof TLRPC.TL_messageMediaVenue) {
                        inputMedia = new TLRPC.TL_inputMediaVenue();
                        inputMedia.address = location.address;
                        inputMedia.title = location.title;
                        inputMedia.provider = location.provider;
                        inputMedia.venue_id = location.venue_id;
                    } else {
                        inputMedia = new TLRPC.TL_inputMediaGeoPoint();
                    }
                    inputMedia.geo_point = new TLRPC.TL_inputGeoPoint();
                    inputMedia.geo_point.lat = location.geo.lat;
                    inputMedia.geo_point._long = location.geo._long;
                } else if (type == 2 || type == 9 && photo != null) {
                    if (photo.access_hash == 0) {
                        inputMedia = new TLRPC.TL_inputMediaUploadedPhoto();
                        inputMedia.caption = photo.caption != null ? photo.caption : "";
                        delayedMessage = new DelayedMessage();
                        delayedMessage.originalPath = originalPath;
                        delayedMessage.type = 0;
                        delayedMessage.obj = newMsgObj;
                        if (path != null && path.length() > 0 && path.startsWith("http")) {
                            delayedMessage.httpLocation = path;
                        } else {
                            delayedMessage.location = photo.sizes.get(photo.sizes.size() - 1).location;
                        }
                    } else {
                        TLRPC.TL_inputMediaPhoto media = new TLRPC.TL_inputMediaPhoto();
                        media.id = new TLRPC.TL_inputPhoto();
                        media.caption = photo.caption != null ? photo.caption : "";
                        media.id.id = photo.id;
                        media.id.access_hash = photo.access_hash;
                        inputMedia = media;
                    }
                } else if (type == 3) {
                    if (document.access_hash == 0) {
                        if (document.thumb.location != null) {
                            inputMedia = new TLRPC.TL_inputMediaUploadedThumbDocument();
                        } else {
                            inputMedia = new TLRPC.TL_inputMediaUploadedDocument();
                        }
                        inputMedia.caption = document.caption != null ? document.caption : "";
                        inputMedia.mime_type = document.mime_type;
                        inputMedia.attributes = document.attributes;
                        delayedMessage = new DelayedMessage();
                        delayedMessage.originalPath = originalPath;
                        delayedMessage.type = 1;
                        delayedMessage.obj = newMsgObj;
                        delayedMessage.location = document.thumb.location;
                        delayedMessage.documentLocation = document;
                        delayedMessage.videoEditedInfo = videoEditedInfo;
                    } else {
                        TLRPC.TL_inputMediaDocument media = new TLRPC.TL_inputMediaDocument();
                        media.id = new TLRPC.TL_inputDocument();
                        media.caption = document.caption != null ? document.caption : "";
                        media.id.id = document.id;
                        media.id.access_hash = document.access_hash;
                        inputMedia = media;
                    }
                } else if (type == 6) {
                    inputMedia = new TLRPC.TL_inputMediaContact();
                    inputMedia.phone_number = user.phone;
                    inputMedia.first_name = user.first_name;
                    inputMedia.last_name = user.last_name;
                } else if (type == 7 || type == 9) {
                    if (document.access_hash == 0) {
                        if (/*encryptedChat == null &&*/ originalPath != null && originalPath.length() > 0 && originalPath.startsWith("http") && params != null) {
                            inputMedia = new TLRPC.TL_inputMediaGifExternal();
                            String args[] = params.get("url").split("\\|");
                            if (args.length == 2) {
                                inputMedia.url = args[0];
                                inputMedia.q = args[1];
                            }
                        } else {
                            if (document.thumb.location != null && document.thumb.location instanceof TLRPC.TL_fileLocation) {
                                inputMedia = new TLRPC.TL_inputMediaUploadedThumbDocument();
                            } else {
                                inputMedia = new TLRPC.TL_inputMediaUploadedDocument();
                            }
                            delayedMessage = new DelayedMessage();
                            delayedMessage.originalPath = originalPath;
                            delayedMessage.type = 2;
                            delayedMessage.obj = newMsgObj;
                            delayedMessage.documentLocation = document;
                            delayedMessage.location = document.thumb.location;
                        }
                        inputMedia.mime_type = document.mime_type;
                        inputMedia.attributes = document.attributes;
                        inputMedia.caption = document.caption != null ? document.caption : "";
                    } else {
                        TLRPC.TL_inputMediaDocument media = new TLRPC.TL_inputMediaDocument();
                        media.id = new TLRPC.TL_inputDocument();
                        media.id.id = document.id;
                        media.id.access_hash = document.access_hash;
                        media.caption = document.caption != null ? document.caption : "";
                        inputMedia = media;
                    }
                } else if (type == 8) {
                    if (document.access_hash == 0) {
                        inputMedia = new TLRPC.TL_inputMediaUploadedDocument();
                        inputMedia.mime_type = document.mime_type;
                        inputMedia.attributes = document.attributes;
                        inputMedia.caption = document.caption != null ? document.caption : "";
                        delayedMessage = new DelayedMessage();
                        delayedMessage.type = 3;
                        delayedMessage.obj = newMsgObj;
                        delayedMessage.documentLocation = document;
                    } else {
                        TLRPC.TL_inputMediaDocument media = new TLRPC.TL_inputMediaDocument();
                        media.id = new TLRPC.TL_inputDocument();
                        media.caption = document.caption != null ? document.caption : "";
                        media.id.id = document.id;
                        media.id.access_hash = document.access_hash;
                        inputMedia = media;
                    }
                }


                // perform sending
                if( type == 2 )
                {
                    // SEND IMAGE
                    TLRPC.PhotoSize size1 = photo.sizes.get(photo.sizes.size() - 1);

                    newMsg.id = mrChat.sendMedia(MrMsg.MR_MSG_IMAGE,
                                newMsg.attachPath, null, size1.w, size1.h, 0);
                }
                else if( type == 3 )
                {
                    // SEND VIDEO
                    int width = 0, height = 0, time_ms = 0;
                    for (int i = 0; i < document.attributes.size(); i++) {
                        TLRPC.DocumentAttribute a = document.attributes.get(i);
                        if (a instanceof TLRPC.TL_documentAttributeVideo) {
                            time_ms = a.duration*1000;
                            width   = a.w;
                            height  = a.h;
                            break;
                        }
                    }
                    if( videoEditedInfo!=null ) {
                        width  = videoEditedInfo.resultWidth; // overwrite original attributes with edited size
                        height = videoEditedInfo.resultHeight;
                    }
                    newMsg.id = mrChat.sendMedia(MrMsg.MR_MSG_VIDEO,
                            newMsg.attachPath, document.mime_type, width, height, time_ms);
                }
                else if( type == 7 )
                {
                    // SEND FILE
                    newMsg.id = mrChat.sendMedia(MrMsg.MR_MSG_FILE,
                            newMsg.attachPath, document.mime_type, 0, 0, 0);
                }
                else if( type == 8 )
                {
                    // SEND AUDIO
                    int time_ms = 0;
                    if( params.get("mr_time_ms") != null ) {
                        time_ms = Integer.parseInt(params.get("mr_time_ms"));
                    }

                    newMsg.id = mrChat.sendMedia(MrMsg.MR_MSG_AUDIO,
                                newMsg.attachPath, document.mime_type, 0, 0, time_ms);
                }

                MrMailbox.reloadMainChatlist();



                /*
                TLObject reqSend;

                {
                    TLRPC.TL_messages_sendMedia request = new TLRPC.TL_messages_sendMedia();
                    request.peer = null;//sendToPeer;
                    if (newMsg.to_id instanceof TLRPC.TL_peerChannel) {
                        request.silent = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE).getBoolean("silent_" + peer, false);
                    }
                    request.random_id = newMsg.random_id;
                    request.media = inputMedia;
                    if (reply_to_msg != null) {
                        request.flags |= 1;
                        request.reply_to_msg_id = reply_to_msg.getId();
                    }
                    if (delayedMessage != null) {
                        delayedMessage.sendRequest = request;
                    }
                    reqSend = request;
                }
                if (type == 1) {
                    performSendMessageRequest(reqSend, newMsgObj, null);
                } else if (type == 2) {
                    if (photo.access_hash == 0) {
                        performSendDelayedMessage(delayedMessage);
                    } else {
                        performSendMessageRequest(reqSend, newMsgObj, null);
                    }
                } else if (type == 3) {
                    if (document.access_hash == 0) {
                        performSendDelayedMessage(delayedMessage);
                    } else {
                        performSendMessageRequest(reqSend, newMsgObj, null);
                    }
                } else if (type == 6) {
                    performSendMessageRequest(reqSend, newMsgObj, null);
                } else if (type == 7) {
                    if (document.access_hash == 0 && delayedMessage != null) {
                        performSendDelayedMessage(delayedMessage);
                    } else {
                        performSendMessageRequest(reqSend, newMsgObj, originalPath);
                    }
                } else if (type == 8) {
                    if (document.access_hash == 0) {
                        performSendDelayedMessage(delayedMessage);
                    } else {
                        performSendMessageRequest(reqSend, newMsgObj, null);
                    }
                }
                */
            } else if (type == 4) {
                /*
                TLRPC.TL_messages_forwardMessages reqSend = new TLRPC.TL_messages_forwardMessages();
                reqSend.to_peer = null;//sendToPeer;
                if (retryMessageObject.messageOwner.ttl != 0) {
                    TLRPC.Chat chat = MessagesController.getInstance().getChat(-retryMessageObject.messageOwner.ttl);
                    reqSend.from_peer = new TLRPC.TL_inputPeerChannel();
                    reqSend.from_peer.channel_id = -retryMessageObject.messageOwner.ttl;
                    if (chat != null) {
                        reqSend.from_peer.access_hash = chat.access_hash;
                    }
                } else {
                    reqSend.from_peer = new TLRPC.TL_inputPeerEmpty();
                }
                if (retryMessageObject.messageOwner.to_id instanceof TLRPC.TL_peerChannel) {
                    reqSend.silent = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE).getBoolean("silent_" + peer, false);
                }
                reqSend.random_id.add(newMsg.random_id);
                if (retryMessageObject.getId() >= 0) {
                    reqSend.id.add(retryMessageObject.getId());
                } else {
                    reqSend.id.add(retryMessageObject.messageOwner.fwd_msg_id);
                }
                performSendMessageRequest(reqSend, newMsgObj, null);
                */
            } else if (type == 9) {
                /*
                TLRPC.TL_messages_sendInlineBotResult reqSend = new TLRPC.TL_messages_sendInlineBotResult();
                reqSend.peer = sendToPeer;
                reqSend.random_id = newMsg.random_id;
                if (reply_to_msg != null) {
                    reqSend.flags |= 1;
                    reqSend.reply_to_msg_id = reply_to_msg.getId();
                }
                if (newMsg.to_id instanceof TLRPC.TL_peerChannel) {
                    reqSend.silent = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE).getBoolean("silent_" + peer, false);
                }
                reqSend.query_id = Utilities.parseLong(params.get("query_id"));
                reqSend.id = params.get("id");
                if (retryMessageObject == null) {
                    reqSend.clear_draft = true;
                    DraftQuery.cleanDraft(peer, false);
                }
                performSendMessageRequest(reqSend, newMsgObj, null);
                */
            }


            // finally update the interface, this results u.a. in an didReceivedNewMessages event which requires newMsg.id to be set
            MessagesController.getInstance().updateInterfaceWithMessages(peer, objArr);
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);


         } catch (Exception e) {
            FileLog.e("messenger", e);
            //MessagesStorage.getInstance().markMessageAsSendError(newMsg);
            if (newMsgObj != null) {
                newMsgObj.messageOwner.send_state = MessageObject.MESSAGE_SEND_STATE_SEND_ERROR;
            }
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.messageSendError, newMsg.id);
            //processSentMessage(newMsg.id);
        }
    }

    protected void stopVideoService(final String path) {
        /*
        MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
            @Override
            public void run() {
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.stopEncodingService, path);
                    }
                });
            }
        });
        */
    }


    /*
    public boolean isSendingMessage(int mid) {
        //return sendingMessages.containsKey(mid);
        boolean isSending = false;
        long hMsg = MrMailbox.MrMailboxGetMsg(MrMailbox.hMailbox, mid);
            int state = MrMailbox.MrMsgGetState(hMsg);
            if( state == MrMailbox.MR_OUT_PENDING || state == MrMailbox.MR_OUT_SENDING ) {
                isSending = true;
            }
        MrMailbox.MrMsgUnref(hMsg);
        return isSending;
    }
    */

    protected long getNextRandomId() {
        long val = 0;
        while (val == 0) {
            val = Utilities.random.nextLong();
        }
        return val;
    }

    public void checkUnsentMessages() {
        //MessagesStorage.getInstance().getUnsentMessages(1000);
    }

    public TLRPC.TL_photo generatePhotoSizes(String path, Uri imageUri) {
        Bitmap bitmap = ImageLoader.loadBitmap(path, imageUri, AndroidUtilities.getPhotoSize(), AndroidUtilities.getPhotoSize(), true);
        if (bitmap == null && AndroidUtilities.getPhotoSize() != 800) {
            bitmap = ImageLoader.loadBitmap(path, imageUri, 800, 800, true);
        }
        ArrayList<TLRPC.PhotoSize> sizes = new ArrayList<>();
        TLRPC.PhotoSize size = ImageLoader.scaleAndSaveImage(bitmap, 90, 90, 55, true);
        if (size != null) {
            sizes.add(size);
        }
        size = ImageLoader.scaleAndSaveImage(bitmap, AndroidUtilities.getPhotoSize(), AndroidUtilities.getPhotoSize(), 80, false, 101, 101);
        if (size != null) {
            sizes.add(size);
        }
        if (bitmap != null) {
            bitmap.recycle();
        }
        if (sizes.isEmpty()) {
            return null;
        } else {
            UserConfig.saveConfig(false);
            TLRPC.TL_photo photo = new TLRPC.TL_photo();
            photo.date = ConnectionsManager.getInstance().getCurrentTime();
            photo.sizes = sizes;
            return photo;
        }
    }

    private static boolean prepareSendingDocumentInternal(String path, String originalPath, Uri uri, String mime, final long dialog_id, final MessageObject reply_to_msg, String caption) {
        if ((path == null || path.length() == 0) && uri == null) {
            return false;
        }
        if (uri != null && AndroidUtilities.isInternalUri(uri)) {
            return false;
        }
        if (path != null && AndroidUtilities.isInternalUri(Uri.fromFile(new File(path)))) {
            return false;
        }
        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        TLRPC.TL_documentAttributeAudio attributeAudio = null;
        if (uri != null) {
            String extension = null;
            if (mime != null) {
                extension = myMime.getExtensionFromMimeType(mime);
            }
            if (extension == null) {
                extension = "txt";
            }
            path = MediaController.copyFileToCache(uri, extension);
            if (path == null) {
                return false;
            }
        }
        final File f = new File(path);
        if (!f.exists() || f.length() == 0) {
            return false;
        }

        final boolean isEncrypted = false;//(int) dialog_id == 0;
        final boolean allowSticker = !isEncrypted;

        String name = f.getName();
        String ext = "";
        int idx = path.lastIndexOf('.');
        if (idx != -1) {
            ext = path.substring(idx + 1);
        }
        if (ext.toLowerCase().equals("mp3") || ext.toLowerCase().equals("m4a")) {
            AudioInfo audioInfo = AudioInfo.getAudioInfo(f);
            if (audioInfo != null && audioInfo.getDuration() != 0) {
                /*if (isEncrypted) {
                    int high_id = (int) (dialog_id >> 32);
                    TLRPC.EncryptedChat encryptedChat = MessagesController.getInstance().getEncryptedChat(high_id);
                    if (encryptedChat == null) {
                        return false;
                    }
                    if (AndroidUtilities.getPeerLayerVersion(encryptedChat.layer) >= 46) {
                        attributeAudio = new TLRPC.TL_documentAttributeAudio();
                    } else {
                        attributeAudio = new TLRPC.TL_documentAttributeAudio_old();
                    }
                } else*/ {
                    attributeAudio = new TLRPC.TL_documentAttributeAudio();
                }
                attributeAudio.duration = (int) (audioInfo.getDuration() / 1000);
                attributeAudio.title = audioInfo.getTitle();
                attributeAudio.performer = audioInfo.getArtist();
                if (attributeAudio.title == null) {
                    attributeAudio.title = "";
                    attributeAudio.flags |= 1;
                }
                if (attributeAudio.performer == null) {
                    attributeAudio.performer = "";
                    attributeAudio.flags |= 2;
                }
            }
        }
        if (originalPath != null) {
            if (attributeAudio != null) {
                originalPath += "audio" + f.length();
            } else {
                originalPath += "" + f.length();
            }
        }

        TLRPC.TL_document document = null;
        /*if (!isEncrypted)*/ {
            document = null;//(TLRPC.TL_document) MessagesStorage.getInstance().getSentFile(originalPath, !isEncrypted ? 1 : 4);
            if (document == null && !path.equals(originalPath) /*&& !isEncrypted*/) {
                document = null;//(TLRPC.TL_document) MessagesStorage.getInstance().getSentFile(path + f.length(), !isEncrypted ? 1 : 4);
            }
        }
        if (document == null) {
            document = new TLRPC.TL_document();
            document.id = 0;
            document.date = ConnectionsManager.getInstance().getCurrentTime();
            TLRPC.TL_documentAttributeFilename fileName = new TLRPC.TL_documentAttributeFilename();
            fileName.file_name = name;
            document.attributes.add(fileName);
            document.size = (int) f.length();
            document.dc_id = 0;
            if (attributeAudio != null) {
                document.attributes.add(attributeAudio);
            }
            if (ext.length() != 0) {
                if (ext.toLowerCase().equals("webp")) {
                    document.mime_type = "image/webp";
                } else {
                    String mimeType = myMime.getMimeTypeFromExtension(ext.toLowerCase());
                    if (mimeType != null) {
                        document.mime_type = mimeType;
                    } else {
                        document.mime_type = "application/octet-stream";
                    }
                }
            } else {
                document.mime_type = "application/octet-stream";
            }
            if (document.mime_type.equals("image/gif")) {
                try {
                    Bitmap bitmap = ImageLoader.loadBitmap(f.getAbsolutePath(), null, 90, 90, true);
                    if (bitmap != null) {
                        fileName.file_name = "animation.gif";
                        document.thumb = ImageLoader.scaleAndSaveImage(bitmap, 90, 90, 55, isEncrypted);
                        bitmap.recycle();
                    }
                } catch (Exception e) {
                    FileLog.e("messenger", e);
                }
            }
            if (document.mime_type.equals("image/webp") && allowSticker) {
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                try {
                    bmOptions.inJustDecodeBounds = true;
                    RandomAccessFile file = new RandomAccessFile(path, "r");
                    ByteBuffer buffer = file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, path.length());
                    Utilities.loadWebpImage(null, buffer, buffer.limit(), bmOptions, true);
                    file.close();
                } catch (Exception e) {
                    FileLog.e("messenger", e);
                }
                if (bmOptions.outWidth != 0 && bmOptions.outHeight != 0 && bmOptions.outWidth <= 800 && bmOptions.outHeight <= 800) {
                    TLRPC.TL_documentAttributeSticker attributeSticker = new TLRPC.TL_documentAttributeSticker();
                    attributeSticker.alt = "";
                    attributeSticker.stickerset = new TLRPC.TL_inputStickerSetEmpty();
                    document.attributes.add(attributeSticker);
                    TLRPC.TL_documentAttributeImageSize attributeImageSize = new TLRPC.TL_documentAttributeImageSize();
                    attributeImageSize.w = bmOptions.outWidth;
                    attributeImageSize.h = bmOptions.outHeight;
                    document.attributes.add(attributeImageSize);
                }
            }
            if (document.thumb == null) {
                document.thumb = new TLRPC.TL_photoSizeEmpty();
                document.thumb.type = "s";
            }
        }
        document.caption = caption;

        final HashMap<String, String> params = new HashMap<>();
        if (originalPath != null) {
            params.put("originalPath", originalPath);
        }
        final TLRPC.TL_document documentFinal = document;
        final String pathFinal = path;
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                SendMessagesHelper.getInstance().sendMessageDocument(documentFinal, null, pathFinal, dialog_id, reply_to_msg, params);
            }
        });
        return true;
    }

    public static void prepareSendingDocument(String path, String originalPath, Uri uri, String mine, long dialog_id, MessageObject reply_to_msg) {
        if ((path == null || originalPath == null) && uri == null) {
            return;
        }
        ArrayList<String> paths = new ArrayList<>();
        ArrayList<String> originalPaths = new ArrayList<>();
        ArrayList<Uri> uris = null;
        if (uri != null) {
            uris = new ArrayList<>();
        }
        paths.add(path);
        originalPaths.add(originalPath);
        prepareSendingDocuments(paths, originalPaths, uris, mine, dialog_id, reply_to_msg);
    }

    public static void prepareSendingAudioDocuments(final ArrayList<MessageObject> messageObjects, final long dialog_id, final MessageObject reply_to_msg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int size = messageObjects.size();
                for (int a = 0; a < size; a++) {
                    final MessageObject messageObject = messageObjects.get(a);
                    String originalPath = messageObject.messageOwner.attachPath;
                    final File f = new File(originalPath);

                    final boolean isEncrypted = false;//(int) dialog_id == 0;


                    if (originalPath != null) {
                        originalPath += "audio" + f.length();
                    }

                    TLRPC.TL_document document = null;
                    /*if (!isEncrypted)*/ {
                        document = null;//(TLRPC.TL_document) MessagesStorage.getInstance().getSentFile(originalPath, !isEncrypted ? 1 : 4);
                    }
                    if (document == null) {
                        document = null;//(TLRPC.TL_document) messageObject.messageOwner.media.document;
                    }

                    /*if (isEncrypted) {
                        int high_id = (int) (dialog_id >> 32);
                        TLRPC.EncryptedChat encryptedChat = MessagesController.getInstance().getEncryptedChat(high_id);
                        if (encryptedChat == null) {
                            return;
                        }
                        if (AndroidUtilities.getPeerLayerVersion(encryptedChat.layer) < 46) {
                            for (int b = 0; b < document.attributes.size(); b++) {
                                if (document.attributes.get(b) instanceof TLRPC.TL_documentAttributeAudio) {
                                    TLRPC.TL_documentAttributeAudio_old old = new TLRPC.TL_documentAttributeAudio_old();
                                    old.duration = document.attributes.get(b).duration;
                                    document.attributes.remove(b);
                                    document.attributes.add(old);
                                    break;
                                }
                            }
                        }
                    }*/

                    final HashMap<String, String> params = new HashMap<>();
                    if (originalPath != null) {
                        params.put("originalPath", originalPath);
                    }
                    final TLRPC.TL_document documentFinal = document;
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            SendMessagesHelper.getInstance().sendMessageDocument(documentFinal, null, messageObject.messageOwner.attachPath, dialog_id, reply_to_msg, params);
                        }
                    });
                }
            }
        }).start();
    }

    public static void prepareSendingDocuments(final ArrayList<String> paths, final ArrayList<String> originalPaths, final ArrayList<Uri> uris, final String mime, final long dialog_id, final MessageObject reply_to_msg) {
        if (paths == null && originalPaths == null && uris == null || paths != null && originalPaths != null && paths.size() != originalPaths.size()) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean error = false;
                if (paths != null) {
                    for (int a = 0; a < paths.size(); a++) {
                        if (!prepareSendingDocumentInternal(paths.get(a), originalPaths.get(a), null, mime, dialog_id, reply_to_msg, null)) {
                            error = true;
                        }
                    }
                }
                if (uris != null) {
                    for (int a = 0; a < uris.size(); a++) {
                        if (!prepareSendingDocumentInternal(null, null, uris.get(a), mime, dialog_id, reply_to_msg, null)) {
                            error = true;
                        }
                    }
                }
                if (error) {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Toast toast = Toast.makeText(ApplicationLoader.applicationContext, LocaleController.getString("UnsupportedAttachment", R.string.UnsupportedAttachment), Toast.LENGTH_SHORT);
                                toast.show();
                            } catch (Exception e) {
                                FileLog.e("messenger", e);
                            }
                        }
                    });
                }
            }
        }).start();
    }

    public static void prepareSendingPhoto(String imageFilePath, Uri imageUri, long dialog_id, MessageObject reply_to_msg, CharSequence caption) {
        ArrayList<String> paths = null;
        ArrayList<Uri> uris = null;
        ArrayList<String> captions = null;
        if (imageFilePath != null && imageFilePath.length() != 0) {
            paths = new ArrayList<>();
            paths.add(imageFilePath);
        }
        if (imageUri != null) {
            uris = new ArrayList<>();
            uris.add(imageUri);
        }
        if (caption != null) {
            captions = new ArrayList<>();
            captions.add(caption.toString());
        }
        prepareSendingPhotos(paths, uris, dialog_id, reply_to_msg, captions);
    }

    public static void prepareSendingPhotosSearch(final ArrayList<MediaController.SearchImage> photos, final long dialog_id, final MessageObject reply_to_msg) {
        if (photos == null || photos.isEmpty()) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean isEncrypted = false;//(int) dialog_id == 0;
                for (int a = 0; a < photos.size(); a++) {
                    final MediaController.SearchImage searchImage = photos.get(a);
                    if (searchImage.type == 1) {
                        final HashMap<String, String> params = new HashMap<>();
                        TLRPC.TL_document document = null;
                        File cacheFile;
                        if (searchImage.document instanceof TLRPC.TL_document) {
                            document = (TLRPC.TL_document) searchImage.document;
                            cacheFile = FileLoader.getPathToAttach(document, true);
                        } else {
                            /*if (!isEncrypted)*/ {
                                TLRPC.Document doc = null;//(TLRPC.Document) MessagesStorage.getInstance().getSentFile(searchImage.imageUrl, !isEncrypted ? 1 : 4);
                                if (doc instanceof TLRPC.TL_document) {
                                    document = (TLRPC.TL_document) doc;
                                }
                            }
                            String md5 = Utilities.MD5(searchImage.imageUrl) + "." + ImageLoader.getHttpUrlExtension(searchImage.imageUrl, "jpg");
                            cacheFile = new File(FileLoader.getInstance().getDirectory(FileLoader.MEDIA_DIR_CACHE), md5);
                        }
                        if (document == null) {
                            if (searchImage.localUrl != null) {
                                params.put("url", searchImage.localUrl);
                            }
                            File thumbFile = null;
                            document = new TLRPC.TL_document();
                            document.id = 0;
                            document.date = ConnectionsManager.getInstance().getCurrentTime();
                            TLRPC.TL_documentAttributeFilename fileName = new TLRPC.TL_documentAttributeFilename();
                            fileName.file_name = "animation.gif";
                            document.attributes.add(fileName);
                            document.size = searchImage.size;
                            document.dc_id = 0;
                            if (cacheFile.toString().endsWith("mp4")) {
                                document.mime_type = "video/mp4";
                                document.attributes.add(new TLRPC.TL_documentAttributeAnimated());
                            } else {
                                document.mime_type = "image/gif";
                            }
                            if (cacheFile.exists()) {
                                thumbFile = cacheFile;
                            } else {
                                cacheFile = null;
                            }
                            if (thumbFile == null) {
                                String thumb = Utilities.MD5(searchImage.thumbUrl) + "." + ImageLoader.getHttpUrlExtension(searchImage.thumbUrl, "jpg");
                                thumbFile = new File(FileLoader.getInstance().getDirectory(FileLoader.MEDIA_DIR_CACHE), thumb);
                                if (!thumbFile.exists()) {
                                    thumbFile = null;
                                }
                            }
                            if (thumbFile != null) {
                                try {
                                    Bitmap bitmap;
                                    if (thumbFile.getAbsolutePath().endsWith("mp4")) {
                                        bitmap = ThumbnailUtils.createVideoThumbnail(thumbFile.getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND);
                                    } else {
                                        bitmap = ImageLoader.loadBitmap(thumbFile.getAbsolutePath(), null, 90, 90, true);
                                    }
                                    if (bitmap != null) {
                                        document.thumb = ImageLoader.scaleAndSaveImage(bitmap, 90, 90, 55, isEncrypted);
                                        bitmap.recycle();
                                    }
                                } catch (Exception e) {
                                    FileLog.e("messenger", e);
                                }
                            }
                            if (document.thumb == null) {
                                document.thumb = new TLRPC.TL_photoSize();
                                document.thumb.w = searchImage.width;
                                document.thumb.h = searchImage.height;
                                document.thumb.size = 0;
                                document.thumb.location = new TLRPC.TL_fileLocationUnavailable();
                                document.thumb.type = "x";
                            }
                        }

                        if (searchImage.caption != null) {
                            document.caption = searchImage.caption.toString();
                        }
                        final TLRPC.TL_document documentFinal = document;
                        final String originalPathFinal = searchImage.imageUrl;
                        final String pathFinal = cacheFile == null ? searchImage.imageUrl : cacheFile.toString();
                        if (params != null && searchImage.imageUrl != null) {
                            params.put("originalPath", searchImage.imageUrl);
                        }
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                SendMessagesHelper.getInstance().sendMessageDocument(documentFinal, null, pathFinal, dialog_id, reply_to_msg, params);
                            }
                        });
                    } else {
                        boolean needDownloadHttp = true;
                        TLRPC.TL_photo photo = null;
                        /*if (!isEncrypted)*/ {
                            photo = null;//MessagesStorage.getInstance().getSentFile(searchImage.imageUrl, !isEncrypted ? 0 : 3);
                        }
                        if (photo == null) {
                            String md5 = Utilities.MD5(searchImage.imageUrl) + "." + ImageLoader.getHttpUrlExtension(searchImage.imageUrl, "jpg");
                            File cacheFile = new File(FileLoader.getInstance().getDirectory(FileLoader.MEDIA_DIR_CACHE), md5);
                            if (cacheFile.exists() && cacheFile.length() != 0) {
                                photo = SendMessagesHelper.getInstance().generatePhotoSizes(cacheFile.toString(), null);
                                if (photo != null) {
                                    needDownloadHttp = false;
                                }
                            }
                            if (photo == null) {
                                md5 = Utilities.MD5(searchImage.thumbUrl) + "." + ImageLoader.getHttpUrlExtension(searchImage.thumbUrl, "jpg");
                                cacheFile = new File(FileLoader.getInstance().getDirectory(FileLoader.MEDIA_DIR_CACHE), md5);
                                if (cacheFile.exists()) {
                                    photo = SendMessagesHelper.getInstance().generatePhotoSizes(cacheFile.toString(), null);
                                }
                                if (photo == null) {
                                    photo = new TLRPC.TL_photo();
                                    photo.date = ConnectionsManager.getInstance().getCurrentTime();
                                    TLRPC.TL_photoSize photoSize = new TLRPC.TL_photoSize();
                                    photoSize.w = searchImage.width;
                                    photoSize.h = searchImage.height;
                                    photoSize.size = 0;
                                    photoSize.location = new TLRPC.TL_fileLocationUnavailable();
                                    photoSize.type = "x";
                                    photo.sizes.add(photoSize);
                                }
                            }
                        }
                        if (photo != null) {
                            if (searchImage.caption != null) {
                                photo.caption = searchImage.caption.toString();
                            }
                            final TLRPC.TL_photo photoFinal = photo;
                            final boolean needDownloadHttpFinal = needDownloadHttp;
                            final HashMap<String, String> params = new HashMap<>();
                            if (searchImage.imageUrl != null) {
                                params.put("originalPath", searchImage.imageUrl);
                            }
                            AndroidUtilities.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    SendMessagesHelper.getInstance().sendMessagePhoto(photoFinal, needDownloadHttpFinal ? searchImage.imageUrl : null, dialog_id, reply_to_msg, params);
                                }
                            });
                        }
                    }
                }
            }
        }).start();
    }

    private static String getTrimmedString(String src) {
        String result = src.trim();
        if (result.length() == 0) {
            return result;
        }
        while (src.startsWith("\n")) {
            src = src.substring(1);
        }
        while (src.endsWith("\n")) {
            src = src.substring(0, src.length() - 1);
        }
        return src;
    }

    public static void prepareSendingText(final String text, final long dialog_id) {
        /*
        MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
            @Override
            public void run() {
        */
                Utilities.stageQueue.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                String textFinal = getTrimmedString(text);
                                if (textFinal.length() != 0) {
                                    //int count = (int) Math.ceil(textFinal.length() / 4096.0f);
                                    //for (int a = 0; a < count; a++) {
                                    //    String mess = textFinal.substring(a * 4096, Math.min((a + 1) * 4096, textFinal.length()));
                                        SendMessagesHelper.getInstance().sendMessageText(textFinal, dialog_id, null, null, true, null, null);
                                    //}
                                }
                            }
                        });
                    }
                });
        /*
            }
        });
        */
    }

    public static void prepareSendingPhotos(ArrayList<String> paths, ArrayList<Uri> uris, final long dialog_id, final MessageObject reply_to_msg, final ArrayList<String> captions) {
        if (paths == null && uris == null || paths != null && paths.isEmpty() || uris != null && uris.isEmpty()) {
            return;
        }
        final ArrayList<String> pathsCopy = new ArrayList<>();
        final ArrayList<Uri> urisCopy = new ArrayList<>();
        if (paths != null) {
            pathsCopy.addAll(paths);
        }
        if (uris != null) {
            urisCopy.addAll(uris);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean isEncrypted = false;//(int) dialog_id == 0;

                ArrayList<String> sendAsDocuments = null;
                ArrayList<String> sendAsDocumentsOriginal = null;
                ArrayList<String> sendAsDocumentsCaptions = null;
                int count = !pathsCopy.isEmpty() ? pathsCopy.size() : urisCopy.size();
                String path = null;
                Uri uri = null;
                String extension = null;
                for (int a = 0; a < count; a++) {
                    if (!pathsCopy.isEmpty()) {
                        path = pathsCopy.get(a);
                    } else if (!urisCopy.isEmpty()) {
                        uri = urisCopy.get(a);
                    }

                    String originalPath = path;
                    String tempPath = path;
                    if (tempPath == null && uri != null) {
                        tempPath = AndroidUtilities.getPath(uri);
                        originalPath = uri.toString();
                    }

                    boolean isDocument = false;
                    if (tempPath != null && (tempPath.endsWith(".gif") || tempPath.endsWith(".webp"))) {
                        if (tempPath.endsWith(".gif")) {
                            extension = "gif";
                        } else {
                            extension = "webp";
                        }
                        isDocument = true;
                    } else if (tempPath == null && uri != null) {
                        if (MediaController.isGif(uri)) {
                            isDocument = true;
                            originalPath = uri.toString();
                            tempPath = MediaController.copyFileToCache(uri, "gif");
                            extension = "gif";
                        } else if (MediaController.isWebp(uri)) {
                            isDocument = true;
                            originalPath = uri.toString();
                            tempPath = MediaController.copyFileToCache(uri, "webp");
                            extension = "webp";
                        }
                    }

                    if (isDocument) {
                        if (sendAsDocuments == null) {
                            sendAsDocuments = new ArrayList<>();
                            sendAsDocumentsOriginal = new ArrayList<>();
                            sendAsDocumentsCaptions = new ArrayList<>();
                        }
                        sendAsDocuments.add(tempPath);
                        sendAsDocumentsOriginal.add(originalPath);
                        sendAsDocumentsCaptions.add(captions != null ? captions.get(a) : null);
                    } else {
                        if (tempPath != null) {
                            File temp = new File(tempPath);
                            originalPath += temp.length() + "_" + temp.lastModified();
                        } else {
                            originalPath = null;
                        }
                        TLRPC.TL_photo photo = null;
                        /*if (!isEncrypted)*/ {
                            photo = null;//MessagesStorage.getInstance().getSentFile(originalPath, !isEncrypted ? 0 : 3);
                            if (photo == null && uri != null) {
                                photo = null;//MessagesStorage.getInstance().getSentFile(AndroidUtilities.getPath(uri), !isEncrypted ? 0 : 3);
                            }
                        }
                        if (photo == null) {
                            photo = SendMessagesHelper.getInstance().generatePhotoSizes(path, uri);
                        }
                        if (photo != null) {
                            if (captions != null) {
                                photo.caption = captions.get(a);
                            }
                            final TLRPC.TL_photo photoFinal = photo;
                            final HashMap<String, String> params = new HashMap<>();
                            if (originalPath != null) {
                                params.put("originalPath", originalPath);
                            }
                            AndroidUtilities.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    SendMessagesHelper.getInstance().sendMessagePhoto(photoFinal, null, dialog_id, reply_to_msg, params);
                                }
                            });
                        }
                    }
                }
                if (sendAsDocuments != null && !sendAsDocuments.isEmpty()) {
                    for (int a = 0; a < sendAsDocuments.size(); a++) {
                        prepareSendingDocumentInternal(sendAsDocuments.get(a), sendAsDocumentsOriginal.get(a), null, extension, dialog_id, reply_to_msg, sendAsDocumentsCaptions.get(a));
                    }
                }
            }
        }).start();
    }

    public static void prepareSendingVideo(final String videoPath, final long estimatedSize, final long duration, final int width, final int height, final VideoEditedInfo videoEditedInfo, final long dialog_id, final MessageObject reply_to_msg) {
        if (videoPath == null || videoPath.length() == 0) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {

                final boolean isEncrypted = false;//(int) dialog_id == 0;

                if (videoEditedInfo != null || videoPath.endsWith("mp4")) {
                    String path = videoPath;
                    String originalPath = videoPath;
                    File temp = new File(originalPath);
                    originalPath += temp.length() + "_" + temp.lastModified();
                    if (videoEditedInfo != null) {
                        originalPath += duration + "_" + videoEditedInfo.startTime + "_" + videoEditedInfo.endTime;
                        if (videoEditedInfo.resultWidth == videoEditedInfo.originalWidth) {
                            originalPath += "_" + videoEditedInfo.resultWidth;
                        }
                    }
                    TLRPC.TL_document document = null;
                    /*if (!isEncrypted)*/ {
                        //document = (TLRPC.TL_document) MessagesStorage.getInstance().getSentFile(originalPath, !isEncrypted ? 2 : 5);
                    }
                    if (document == null) {
                        Bitmap thumb = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.MINI_KIND);
                        TLRPC.PhotoSize size = ImageLoader.scaleAndSaveImage(thumb, 90, 90, 55, isEncrypted);
                        document = new TLRPC.TL_document();
                        document.thumb = size;
                        if (document.thumb == null) {
                            document.thumb = new TLRPC.TL_photoSizeEmpty();
                            document.thumb.type = "s";
                        } else {
                            document.thumb.type = "s";
                        }
                        document.mime_type = "video/mp4";
                        UserConfig.saveConfig(false);
                        TLRPC.TL_documentAttributeVideo attributeVideo = new TLRPC.TL_documentAttributeVideo();
                        document.attributes.add(attributeVideo);
                        if (videoEditedInfo != null) {
                            attributeVideo.duration = (int) (duration / 1000);
                            if (videoEditedInfo.rotationValue == 90 || videoEditedInfo.rotationValue == 270) {
                                attributeVideo.w = height;
                                attributeVideo.h = width;
                            } else {
                                attributeVideo.w = width;
                                attributeVideo.h = height;
                            }
                            document.size = (int) estimatedSize;
                            String fileName = Integer.MIN_VALUE + "_" + UserConfig.lastLocalId + ".mp4";
                            UserConfig.lastLocalId--;
                            File cacheFile = new File(FileLoader.getInstance().getDirectory(FileLoader.MEDIA_DIR_CACHE), fileName);
                            UserConfig.saveConfig(false);
                            path = cacheFile.getAbsolutePath();
                        } else {
                            if (temp.exists()) {
                                document.size = (int) temp.length();
                            }
                            boolean infoObtained = false;

                            MediaMetadataRetriever mediaMetadataRetriever = null;
                            try {
                                mediaMetadataRetriever = new MediaMetadataRetriever();
                                mediaMetadataRetriever.setDataSource(videoPath);
                                String width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                                if (width != null) {
                                    attributeVideo.w = Integer.parseInt(width);
                                }
                                String height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                                if (height != null) {
                                    attributeVideo.h = Integer.parseInt(height);
                                }
                                String duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                                if (duration != null) {
                                    attributeVideo.duration = (int) Math.ceil(Long.parseLong(duration) / 1000.0f);
                                }
                                infoObtained = true;
                            } catch (Exception e) {
                                FileLog.e("messenger", e);
                            } finally {
                                try {
                                    if (mediaMetadataRetriever != null) {
                                        mediaMetadataRetriever.release();
                                    }
                                } catch (Exception e) {
                                    FileLog.e("messenger", e);
                                }
                            }
                            if (!infoObtained) {
                                try {
                                    MediaPlayer mp = MediaPlayer.create(ApplicationLoader.applicationContext, Uri.fromFile(new File(videoPath)));
                                    if (mp != null) {
                                        attributeVideo.duration = (int) Math.ceil(mp.getDuration() / 1000.0f);
                                        attributeVideo.w = mp.getVideoWidth();
                                        attributeVideo.h = mp.getVideoHeight();
                                        mp.release();
                                    }
                                } catch (Exception e) {
                                    FileLog.e("messenger", e);
                                }
                            }
                        }
                    }
                    final TLRPC.TL_document videoFinal = document;
                    final String originalPathFinal = originalPath;
                    final String finalPath = path;
                    final HashMap<String, String> params = new HashMap<>();
                    if (originalPath != null) {
                        params.put("originalPath", originalPath);
                    }
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            SendMessagesHelper.getInstance().sendMessageDocument(videoFinal, videoEditedInfo, finalPath, dialog_id, reply_to_msg, params);
                        }
                    });
                } else {
                    prepareSendingDocumentInternal(videoPath, videoPath, null, null, dialog_id, reply_to_msg, null);
                }
            }
        }).start();
    }
}
