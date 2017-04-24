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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import com.b44t.messenger.audioinfo.AudioInfo;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;

public class SendMessagesHelper implements NotificationCenter.NotificationCenterDelegate {

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
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FilePreparingStarted);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileNewChunkAvailable);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FilePreparingFailed);
    }

    @Override
    public void didReceivedNotification(int id, final Object... args) {
        if (id == NotificationCenter.FilePreparingStarted)
        {
            // encoding started
        }
        else if (id == NotificationCenter.FileNewChunkAvailable)
        {
            // encoding progress
            MessageObject messageObject = (MessageObject) args[0];
            long finalSize = (Long) args[2];
            if( finalSize != 0 && messageObject.isVideo() )
            {
                // encoding done
                new File(messageObject.messageOwner.attachPath+".increation").delete();
                NotificationCenter.getInstance().postNotificationName(NotificationCenter.messagesSentOrRead);
            }
        }
        else if (id == NotificationCenter.FilePreparingFailed)
        {
            // encoding error
            MessageObject messageObject = (MessageObject) args[0];
            new File(messageObject.messageOwner.attachPath+".increation").delete();
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.messagesSentOrRead);
        }
    }

    public void sendSticker(TLRPC.Document document, long peer) {
        if (document == null) {
            return;
        }
        SendMessagesHelper.getInstance().sendMessageDocument((TLRPC.TL_document) document, null, null, peer, null);
    }

    public void sendMessageContact(int contact_id, int dialog_id) {
        MrContact contact = MrMailbox.getContact(contact_id);
        String msg = contact.getAuthName();
        if(msg.isEmpty()) {
            msg = contact.getAddr();
        }
        else {
            msg += ": " + contact.getAddr();
        }
        SendMessagesHelper.getInstance().sendMessageText(msg, dialog_id, null);
    }

    public void sendMessageDocument(TLRPC.TL_document document, VideoEditedInfo videoEditedInfo, String path, long peer, HashMap<String, String> params) {
        sendMessage__(null, null, videoEditedInfo, document, peer, path, params);
    }

    public void sendMessageText(String message, long peer, HashMap<String, String> params) {
        sendMessage__(message, null, null, null, peer, null, params);
    }

    private void sendMessagePhoto(TLRPC.TL_photo photo, String path, long peer, HashMap<String, String> params) {
        sendMessage__(null, photo, null, null, peer, path, params);
    }

    private void updateInterfaceForNewMessage(int chat_id, boolean success, int msg_id)
    {
        if( msg_id <= 0 ) {success = false;}
        MrMailbox.reloadMainChatlist();
        if( success ) {
            NotificationsController.getInstance().playOutChatSound();
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.didReceivedNewMessages, chat_id, msg_id);
        }
        else {
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.messageSendError, msg_id);
        }
        NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
    }

    private void sendMessage__(String message,
                             TLRPC.TL_photo photo,
                             VideoEditedInfo videoEditedInfo,
                             TLRPC.TL_document document,
                             long dialog_id,
                             String path,
                             HashMap<String, String> params)
    {
        int newMsg_id = 0;

        try {
            MrChat mrChat = MrMailbox.getChat((int)dialog_id);
            if (message != null)
            {
                // SEND TEXT
                newMsg_id = mrChat.sendText(message);
                mrChat.cleanDraft();
            }
            else if (photo != null)
            {
                // SEND IMAGE
                TLRPC.FileLocation location1 = photo.sizes.get(photo.sizes.size() - 1).location;
                String newMsg_attachPath = FileLoader.getPathToAttach(location1, true).toString();

                TLRPC.PhotoSize size1 = photo.sizes.get(photo.sizes.size() - 1);
                newMsg_id = mrChat.sendMedia(MrMsg.MR_MSG_IMAGE,
                        newMsg_attachPath, null, size1.w, size1.h, 0, null, null);
            }
            else if (document != null && MessageObject.isVideoDocument(document))
            {
                // SEND VIDEO, encoding is done in a working thread, the backend waits automatically until the `.increation`-file is deleted
                new File(path+".increation").createNewFile();

                int time_ms = 0;
                for (int i = 0; i < document.attributes.size(); i++) {
                    TLRPC.DocumentAttribute a = document.attributes.get(i);
                    if (a instanceof TLRPC.TL_documentAttributeVideo) {
                        time_ms = a.duration * 1000;
                        break;
                    }
                }

                newMsg_id = mrChat.sendMedia(MrMsg.MR_MSG_VIDEO,
                        path, document.mime_type, videoEditedInfo.resultWidth, videoEditedInfo.resultHeight, time_ms, null, null);

                TLRPC.TL_message mown = new TLRPC.TL_message();
                mown.dialog_id = dialog_id;
                mown.media = new TLRPC.TL_messageMediaDocument();
                mown.media.document = document;
                mown.attachPath = path;
                MessageObject mobj = new MessageObject(mown, false);
                mobj.videoEditedInfo = videoEditedInfo;
                MediaController.getInstance().scheduleVideoConvert(mobj);
            }
            else if ( MessageObject.isVoiceDocument(document) || MessageObject.isMusicDocument(document) )
            {
                // SEND AUDIO
                int time_ms = 0;
                String author = null, trackname = null;
                for (int i = 0; i < document.attributes.size(); i++) {
                    TLRPC.DocumentAttribute a = document.attributes.get(i);
                    if (a instanceof TLRPC.TL_documentAttributeAudio) {
                        time_ms = a.duration * 1000;
                        author = a.performer;
                        trackname = a.title;
                        break;
                    }
                }

                if( params!=null && params.get("mr_time_ms") != null ) {
                    time_ms = Integer.parseInt(params.get("mr_time_ms")); // if possible, use a higher resolution
                }

                newMsg_id = mrChat.sendMedia(
                        MessageObject.isVoiceDocument(document)? MrMsg.MR_MSG_VOICE : MrMsg.MR_MSG_AUDIO,
                        path, document.mime_type, 0, 0, time_ms, author, trackname);

            }
            else if (document != null)
            {
                // SEND FILE
                newMsg_id = mrChat.sendMedia(MrMsg.MR_MSG_FILE,
                        path, document.mime_type, 0, 0, 0, null, null);
            }
            else
            {
                return; // should not happen
            }

            updateInterfaceForNewMessage((int)dialog_id, true, newMsg_id);

        } catch (Exception e) {
            updateInterfaceForNewMessage((int)dialog_id, false, newMsg_id);
        }
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
            UserConfig.saveConfig();
            TLRPC.TL_photo photo = new TLRPC.TL_photo();
            photo.date = MrMailbox.getCurrentTime();
            photo.sizes = sizes;
            return photo;
        }
    }

    private static boolean prepareSendingDocumentInternal(String path, String originalPath, Uri uri, String mime, final long dialog_id, String caption) {
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
        //final boolean allowSticker = !isEncrypted;

        String name = f.getName();
        String ext = "";
        int idx = path.lastIndexOf('.');
        if (idx != -1) {
            ext = path.substring(idx + 1);
        }
        if (ext.toLowerCase().equals("mp3") || ext.toLowerCase().equals("m4a")) {
            AudioInfo audioInfo = AudioInfo.getAudioInfo(f);
            if (audioInfo != null && audioInfo.getDuration() != 0) {
                attributeAudio = new TLRPC.TL_documentAttributeAudio();
                attributeAudio.duration = (int) (audioInfo.getDuration() / 1000);
                attributeAudio.title = audioInfo.getTitle();
                attributeAudio.performer = audioInfo.getArtist();
                if (attributeAudio.title == null) {
                    attributeAudio.title = "";
                }
                if (attributeAudio.performer == null) {
                    attributeAudio.performer = "";
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
        {
            document = new TLRPC.TL_document();
            document.id = 0;
            document.date = MrMailbox.getCurrentTime();
            TLRPC.TL_documentAttributeFilename fileName = new TLRPC.TL_documentAttributeFilename();
            fileName.file_name = name;
            document.attributes.add(fileName);
            document.size = (int) f.length();
            document.dc_id = 0;
            if (attributeAudio != null) {
                document.attributes.add(attributeAudio);
            }
            if (ext.length() != 0) {
                /*if (ext.toLowerCase().equals("webp")) {
                    document.mime_type = "image/webp";
                } else*/ {
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

                }
            }
            /*if (document.mime_type.equals("image/webp") && allowSticker) {
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                try {
                    bmOptions.inJustDecodeBounds = true;
                    RandomAccessFile file = new RandomAccessFile(path, "r");
                    ByteBuffer buffer = file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, path.length());
                    Utilities.loadWebpImage(null, buffer, buffer.limit(), bmOptions, true);
                    file.close();
                } catch (Exception e) {

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
            }*/
            if (document.thumb == null) {
                document.thumb = new TLRPC.TL_photoSizeEmpty();
                document.thumb.type = "s";
            }
        }
        document.caption = caption;

        /*final HashMap<String, String> params = new HashMap<>();
        if (originalPath != null) {
            params.put("originalPath", originalPath);
        }*/
        final TLRPC.TL_document documentFinal = document;
        final String pathFinal = path;
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                SendMessagesHelper.getInstance().sendMessageDocument(documentFinal, null, pathFinal, dialog_id, null);
            }
        });
        return true;
    }

    public static void prepareSendingDocument(String path, String originalPath, Uri uri, String mine, long dialog_id) {
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
        prepareSendingDocuments(paths, originalPaths, uris, mine, dialog_id);
    }

    public static void prepareSendingDocuments(final ArrayList<String> paths, final ArrayList<String> originalPaths, final ArrayList<Uri> uris, final String mime, final long dialog_id) {
        if (paths == null && originalPaths == null && uris == null || paths != null && originalPaths != null && paths.size() != originalPaths.size()) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean error = false;
                if (paths != null) {
                    for (int a = 0; a < paths.size(); a++) {
                        if (!prepareSendingDocumentInternal(paths.get(a), originalPaths.get(a), null, mime, dialog_id, null)) {
                            error = true;
                        }
                    }
                }
                if (uris != null) {
                    for (int a = 0; a < uris.size(); a++) {
                        if (!prepareSendingDocumentInternal(null, null, uris.get(a), mime, dialog_id, null)) {
                            error = true;
                        }
                    }
                }
                if (error) {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                AndroidUtilities.showErrorHint(ApplicationLoader.applicationContext); // should not happen
                            } catch (Exception e) {
                            }
                        }
                    });
                }
            }
        }).start();
    }

    public static void prepareSendingPhoto(String imageFilePath, Uri imageUri, long dialog_id, CharSequence caption) {
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
        prepareSendingPhotos(paths, uris, dialog_id, captions);
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
                Utilities.stageQueue.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                String textFinal = getTrimmedString(text);
                                if (textFinal.length() != 0) {
                                        SendMessagesHelper.getInstance().sendMessageText(textFinal, dialog_id, null);
                                }
                            }
                        });
                    }
                });
    }

    public static void prepareSendingPhotos(ArrayList<String> paths, ArrayList<Uri> uris, final long dialog_id, final ArrayList<String> captions) {
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
                        tempPath = AndroidUtilities.getPath(uri); // convert uri to path, this may fail
                        originalPath = uri.toString();
                    }

                    boolean isDocument = false;
                    if (tempPath != null && tempPath.toLowerCase().endsWith(".gif")) {
                        // Do not compress GIF files to JPG.
                        extension = "gif";
                        isDocument = true;
                    }
                    else if( tempPath!=null && tempPath.toLowerCase().endsWith(".png")) {
                        // Do not compress PNG files to JPG. If still some PNG are compressed, we should look for the file content using mr_get_filemeta() (as done for GIF below)
                        extension = "png";
                        isDocument = true;
                    } else if (tempPath == null && uri != null) {
                        // if conversion from uri to path fails above, we have a look at the content of the file
                        if (MediaController.isGif(uri)) {
                            isDocument = true;
                            originalPath = uri.toString();
                            tempPath = MediaController.copyFileToCache(uri, "gif");
                            extension = "gif";
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
                            /*final HashMap<String, String> params = new HashMap<>();
                            if (originalPath != null) {
                                params.put("originalPath", originalPath);
                            }*/
                            AndroidUtilities.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    SendMessagesHelper.getInstance().sendMessagePhoto(photoFinal, null, dialog_id, null);
                                }
                            });
                        }
                    }
                }
                if (sendAsDocuments != null && !sendAsDocuments.isEmpty()) {
                    for (int a = 0; a < sendAsDocuments.size(); a++) {
                        prepareSendingDocumentInternal(sendAsDocuments.get(a), sendAsDocumentsOriginal.get(a), null, extension, dialog_id, sendAsDocumentsCaptions.get(a));
                    }
                }
            }
        }).start();
    }

    public static void prepareSendingVideo(final String videoPath, final long estimatedSize, final long duration, final int width, final int height, final VideoEditedInfo videoEditedInfo, final long dialog_id) {
        if (videoPath == null || videoPath.length() == 0) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {

                if (videoEditedInfo != null || videoPath.endsWith("mp4")) {
                    String path = videoPath;
                    //String originalPath = videoPath;
                    File temp = new File(videoPath);
                    /*originalPath += temp.length() + "_" + temp.lastModified();
                    if (videoEditedInfo != null) {
                        originalPath += duration + "_" + videoEditedInfo.startTime + "_" + videoEditedInfo.endTime;
                        if (videoEditedInfo.resultWidth == videoEditedInfo.originalWidth) {
                            originalPath += "_" + videoEditedInfo.resultWidth;
                        }
                    }*/
                    TLRPC.TL_document document = null;
                    {
                        document = new TLRPC.TL_document();
                        document.mime_type = "video/mp4";
                        UserConfig.saveConfig();
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
                            String fileName = temp.getName(); // we could also all videoEditInformation to the filename and re-use already encoded videos this way. however, for the moment, I have no time to check this out (bp)
                            File cacheFile = AndroidUtilities.getFineFilename(FileLoader.getInstance().getDirectory(FileLoader.MEDIA_DIR_CACHE), fileName);
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

                            } finally {
                                try {
                                    if (mediaMetadataRetriever != null) {
                                        mediaMetadataRetriever.release();
                                    }
                                } catch (Exception e) {

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

                                }
                            }
                        }
                    }
                    final TLRPC.TL_document videoFinal = document;
                    final String finalPath = path;

                    // create thumbnail from original video (the recoded one is not yet preset ...)
                    Bitmap thumb = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.MINI_KIND);
                    File vfile = new File(finalPath);
                    File tfile = new File(MrMailbox.getBlobdir(), vfile.getName()+"-preview.jpg");
                    ImageLoader.scaleAndSaveImage(tfile, thumb, 90, 90, 55, false);

                    /*final HashMap<String, String> params = new HashMap<>();
                    if (originalPath != null) {
                        params.put("originalPath", originalPath);
                    }*/
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            SendMessagesHelper.getInstance().sendMessageDocument(videoFinal, videoEditedInfo, finalPath, dialog_id, null);
                        }
                    });
                } else {
                    prepareSendingDocumentInternal(videoPath, videoPath, null, null, dialog_id, null);
                }
            }
        }).start();
    }
}
