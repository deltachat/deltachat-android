/*******************************************************************************
 *
 *                              Delta Chat Android
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

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.Spannable;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.util.Linkify;

import com.b44t.messenger.ActionBar.Theme;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class MessageObject {

    public static final int MESSAGE_SEND_STATE_SENDING = 1;
    public static final int MESSAGE_SEND_STATE_SENT = 0;
    public static final int MESSAGE_SEND_STATE_SEND_ERROR = 2;

    public TLRPC.Message messageOwner;
    public CharSequence messageText;
    public CharSequence caption;
    public final MessageObject replyMessageObject = null;

    public final static int MO_TYPE0_TEXT           = 0;
    public final static int MO_TYPE1_PHOTO          = 1;
    public final static int MO_TYPE2_VOICE          = 2;
    public final static int MO_TYPE3_VIDEO          = 3;
    public final static int MO_TYPE8_GIF            = 8;
    public final static int MO_TYPE9_FILE           = 9;
    public final static int MO_TYPE10_DATE_HEADLINE = 10;
    public final static int MO_TYPE13_STICKER       = 13;
    public final static int MO_TYPE14_MUSIC         = 14;
    public final static int MO_TYPE100_SYSTEM_MSG   = 100;
    public final static int MO_TYPE1000_INIT_VAL    = 1000; // unsused types: 4=LOCATION, 12=CONTACT
    public int type = MO_TYPE1000_INIT_VAL;

    public int contentType; // one of ChatActivity.ROWTYPE_MESSAGE_CELL, .ROWTYPE_ACTION_CELL or .ROWTYPE_UNREAD_CELL
    public float audioProgress;
    public int audioProgressSec;
    public ArrayList<TLRPC.PhotoSize> photoThumbs;
    public VideoEditedInfo videoEditedInfo;
    public boolean attachPathExists;
    public boolean mediaExists;

    public boolean forceUpdate;

    private static TextPaint textPaint;
    private static TextPaint systemCmdPaint;
    public int lastLineWidth;
    public int textWidth;
    public int textHeight;

    public static class TextLayoutBlock {
        public StaticLayout textLayout;
        public float textXOffset;
        public float textYOffset;
        public int charactersOffset;
        public int height;
    }

    private static final int LINES_PER_BLOCK = 10;

    public ArrayList<TextLayoutBlock> textLayoutBlocks;

    public MessageObject(TLRPC.Message message, boolean generateLayout) {
        if (textPaint == null) {
            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Theme.MSG_TEXT_COLOR);
            textPaint.linkColor = Theme.MSG_LINK_TEXT_COLOR;
        }

        if( systemCmdPaint == null ) {
            systemCmdPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            systemCmdPaint.setColor(Theme.MSG_SYSTEM_CMD_COLOR);
            systemCmdPaint.linkColor = Theme.MSG_SYSTEM_CMD_COLOR;
        }

        messageOwner = message;

        if( !messageOwner.is_system_cmd ) {
            textPaint.setTextSize(AndroidUtilities.dp(ApplicationLoader.fontSize));
        }
        else {
            systemCmdPaint.setTextSize(AndroidUtilities.dp(ApplicationLoader.fontSize));
        }

        messageText = message.message;
        if (messageText == null) {
            messageText = "";
        }

        setType();

        generateCaption();
        if (generateLayout) {
            messageText = EmojiInputView.replaceEmoji(messageText, false);
            generateLayout();
        }
        generateThumbs(false);
        checkMediaExistance();
    }

    public static TextPaint getTextPaint() {
        if (textPaint == null) {
            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Theme.MSG_TEXT_COLOR);
            textPaint.linkColor = Theme.MSG_LINK_TEXT_COLOR;
            textPaint.setTextSize(AndroidUtilities.dp(ApplicationLoader.fontSize));
        }
        return textPaint;
    }

    private void setType() {
        int oldType = type;
        if (messageOwner instanceof TLRPC.TL_message /*|| messageOwner instanceof TLRPC.TL_messageForwarded_old2*/) {
            if (isMediaEmpty()) {
                type = MO_TYPE0_TEXT;
                if (messageText == null || messageText.length() == 0) {
                    messageText = "Empty message";
                }
            } else if (messageOwner.media instanceof TLRPC.TL_messageMediaPhoto) {
                type = MO_TYPE1_PHOTO;
            } else if (isVideo()) {
                type = MO_TYPE3_VIDEO;
            } else if (isVoice()) {
                type = MO_TYPE2_VOICE;
            } else if (isMusic()) {
                type = MO_TYPE14_MUSIC;
            } else if (messageOwner.media instanceof TLRPC.TL_messageMediaDocument) {
                if (messageOwner.media.document.mime_type != null) {
                    if (isGifDocument(messageOwner.media.document)) {
                        type = MO_TYPE8_GIF;
                    } else if (messageOwner.media.document.mime_type.equals("image/webp") && isSticker()) {
                        type = MO_TYPE13_STICKER;
                    } else {
                        type = MO_TYPE9_FILE;
                    }
                } else {
                    type = MO_TYPE9_FILE;
                }
            }
        }
        if (oldType != 1000 && oldType != type) {
            generateThumbs(false);
        }
    }

    public static boolean isGifDocument(TLRPC.Document document) {
        return document != null && document.thumb != null && document.mime_type != null && document.mime_type.equals("image/gif");
    }

    private void generateThumbs(boolean update) {
        if (messageOwner.media != null && !(messageOwner.media instanceof TLRPC.TL_messageMediaEmpty)) {
            if (messageOwner.media instanceof TLRPC.TL_messageMediaPhoto) {
                if (!update || photoThumbs != null && photoThumbs.size() != messageOwner.media.photo.sizes.size()) {
                    photoThumbs = new ArrayList<>(messageOwner.media.photo.sizes);
                } else if (photoThumbs != null && !photoThumbs.isEmpty()) {
                    for (int a = 0; a < photoThumbs.size(); a++) {
                        TLRPC.PhotoSize photoObject = photoThumbs.get(a);
                        for (int b = 0; b < messageOwner.media.photo.sizes.size(); b++) {
                            TLRPC.PhotoSize size = messageOwner.media.photo.sizes.get(b);
                            if (size instanceof TLRPC.TL_photoSizeEmpty) {
                                continue;
                            }
                            if (size.type.equals(photoObject.type)) {
                                photoObject.location = size.location;
                                break;
                            }
                        }
                    }
                }
            } else if (messageOwner.media instanceof TLRPC.TL_messageMediaDocument) {
                if (!(messageOwner.media.document.thumb instanceof TLRPC.TL_photoSizeEmpty)) {
                    if (!update) {
                        photoThumbs = new ArrayList<>();
                        photoThumbs.add(messageOwner.media.document.thumb);
                    } else if (photoThumbs != null && !photoThumbs.isEmpty() && messageOwner.media.document.thumb != null) {
                        TLRPC.PhotoSize photoObject = photoThumbs.get(0);
                        photoObject.location = messageOwner.media.document.thumb.location;
                        photoObject.w = messageOwner.media.document.thumb.w;
                        photoObject.h = messageOwner.media.document.thumb.h;
                    }
                }
            }
        }
    }

    public String getFileName() {
        if (messageOwner.media instanceof TLRPC.TL_messageMediaDocument) {
            return FileLoader.getAttachFileName(messageOwner.media.document);
        } else if (messageOwner.media instanceof TLRPC.TL_messageMediaPhoto) {
            ArrayList<TLRPC.PhotoSize> sizes = messageOwner.media.photo.sizes;
            if (sizes.size() > 0) {
                TLRPC.PhotoSize sizeFull = FileLoader.getClosestPhotoSizeWithSize(sizes, AndroidUtilities.getPhotoSize());
                if (sizeFull != null) {
                    return FileLoader.getAttachFileName(sizeFull);
                }
            }
        }
        return "";
    }

    public int getFileType() {
        if (isVideo()) {
            return FileLoader.MEDIA_DIR_VIDEO;
        } else if (isVoice()) {
            return FileLoader.MEDIA_DIR_AUDIO;
        } else if (messageOwner.media instanceof TLRPC.TL_messageMediaDocument) {
            return FileLoader.MEDIA_DIR_DOCUMENT;
        } else if (messageOwner.media instanceof TLRPC.TL_messageMediaPhoto) {
            return FileLoader.MEDIA_DIR_IMAGE;
        }
        return FileLoader.MEDIA_DIR_CACHE;
    }

    private static boolean containsUrls(CharSequence message) {
        if (message == null || message.length() < 2 || message.length() > 1024 * 20) {
            return false;
        }

        int length = message.length();

        int digitsInRow = 0;
        int schemeSequence = 0;
        int dotSequence = 0;

        char lastChar = 0;

        for (int i = 0; i < length; i++) {
            char c = message.charAt(i);

            if (c >= '0' && c <= '9') {
                digitsInRow++;
                if (digitsInRow >= 6) {
                    return true;
                }
                schemeSequence = 0;
                dotSequence = 0;
            } else if (!(c != ' ' && digitsInRow > 0)) {
                digitsInRow = 0;
            }
            if ((c == '@' || c == '#' || c == '/') && i == 0 || i != 0 && (message.charAt(i - 1) == ' ' || message.charAt(i - 1) == '\n')) {
                return true;
            }
            if (c == ':') {
                if (schemeSequence == 0) {
                    schemeSequence = 1;
                } else {
                    schemeSequence = 0;
                }
            } else if (c == '/') {
                if (schemeSequence == 2) {
                    return true;
                }
                if (schemeSequence == 1) {
                    schemeSequence++;
                } else {
                    schemeSequence = 0;
                }
            } else if (c == '.') {
                if (dotSequence == 0 && lastChar != ' ') {
                    dotSequence++;
                } else {
                    dotSequence = 0;
                }
            } else if (c != ' ' && lastChar == '.' && dotSequence == 1) {
                return true;
            } else {
                dotSequence = 0;
            }
            lastChar = c;
        }
        return false;
    }

    private void generateCaption() {
        if (caption != null) {
            return;
        }
        if (messageOwner.media != null && messageOwner.media.caption != null && messageOwner.media.caption.length() > 0) {
            caption = EmojiInputView.replaceEmoji(messageOwner.media.caption, false);
            if (containsUrls(caption)) {
                try {
                    Linkify.addLinks((Spannable) caption, Linkify.WEB_URLS | Linkify.PHONE_NUMBERS | Linkify.EMAIL_ADDRESSES);
                } catch (Exception e) {

                }
            }
        }
    }

    private static void addLinks(CharSequence messageText) {
        if (messageText instanceof Spannable && containsUrls(messageText)) {
            try {
                Linkify.addLinks((Spannable) messageText, Linkify.WEB_URLS | Linkify.PHONE_NUMBERS | Linkify.EMAIL_ADDRESSES);
            } catch (Exception e) {

            }
        }
    }

    private void generateLayout() {
        if (type != MO_TYPE0_TEXT || messageText == null || messageText.length() == 0) {
            return;
        }

        textLayoutBlocks = new ArrayList<>();

        addLinks(messageText);

        int maxWidth;
        boolean substractAvatar = !isOut() && MrMailbox.getChat((int)messageOwner.dialog_id).getType()==MrChat.MR_CHAT_GROUP;
        if (substractAvatar) {
            maxWidth = Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) - AndroidUtilities.dp(122);
        } else {
            maxWidth = Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) - AndroidUtilities.dp(80);
        }

        StaticLayout textLayout;

        try {
            textLayout = new StaticLayout(messageText, messageOwner.is_system_cmd? systemCmdPaint : textPaint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        } catch (Exception e) {

            return;
        }

        textHeight = textLayout.getHeight();
        int linesCount = textLayout.getLineCount();

        int blocksCount = (int) Math.ceil((float) linesCount / LINES_PER_BLOCK);
        int linesOffset = 0;
        float prevOffset = 0;

        for (int a = 0; a < blocksCount; a++) {
            int currentBlockLinesCount = Math.min(LINES_PER_BLOCK, linesCount - linesOffset);
            TextLayoutBlock block = new TextLayoutBlock();

            if (blocksCount == 1) {
                block.textLayout = textLayout;
                block.textYOffset = 0;
                block.charactersOffset = 0;
                block.height = textHeight;
            } else {
                int startCharacter = textLayout.getLineStart(linesOffset);
                int endCharacter = textLayout.getLineEnd(linesOffset + currentBlockLinesCount - 1);
                if (endCharacter < startCharacter) {
                    continue;
                }
                block.charactersOffset = startCharacter;
                try {
                    CharSequence str = messageText.subSequence(startCharacter, endCharacter);
                    block.textLayout = new StaticLayout(str, messageOwner.is_system_cmd? systemCmdPaint : textPaint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                    block.textYOffset = textLayout.getLineTop(linesOffset);
                    if (a != 0) {
                        block.height = (int) (block.textYOffset - prevOffset);
                    }
                    block.height = Math.max(block.height, block.textLayout.getLineBottom(block.textLayout.getLineCount() - 1));
                    prevOffset = block.textYOffset;
                } catch (Exception e) {

                    continue;
                }
                if (a == blocksCount - 1) {
                    currentBlockLinesCount = Math.max(currentBlockLinesCount, block.textLayout.getLineCount());
                    try {
                        textHeight = Math.max(textHeight, (int) (block.textYOffset + block.textLayout.getHeight()));
                    } catch (Exception e) {

                    }
                }
            }

            textLayoutBlocks.add(block);

            float lastLeft = block.textXOffset = 0;
            try {
                lastLeft = block.textXOffset = block.textLayout.getLineLeft(currentBlockLinesCount - 1);
            } catch (Exception e) {

            }

            float lastLine = 0;
            try {
                lastLine = block.textLayout.getLineWidth(currentBlockLinesCount - 1);
            } catch (Exception e) {

            }

            int linesMaxWidth = (int) Math.ceil(lastLine);
            int lastLineWidthWithLeft;
            int linesMaxWidthWithLeft;
            boolean hasNonRTL = false;

            if (a == blocksCount - 1) {
                lastLineWidth = linesMaxWidth;
            }

            linesMaxWidthWithLeft = lastLineWidthWithLeft = (int) Math.ceil(lastLine + lastLeft);
            if (lastLeft == 0) {
                hasNonRTL = true;
            }

            if (currentBlockLinesCount > 1) {
                float textRealMaxWidth = 0, textRealMaxWidthWithLeft = 0, lineWidth, lineLeft;
                for (int n = 0; n < currentBlockLinesCount; n++) {
                    try {
                        lineWidth = block.textLayout.getLineWidth(n);
                    } catch (Exception e) {

                        lineWidth = 0;
                    }

                    if (lineWidth > maxWidth + 100) {
                        lineWidth = maxWidth;
                    }

                    try {
                        lineLeft = block.textLayout.getLineLeft(n);
                    } catch (Exception e) {

                        lineLeft = 0;
                    }

                    block.textXOffset = Math.min(block.textXOffset, lineLeft);

                    if (lineLeft == 0) {
                        hasNonRTL = true;
                    }
                    textRealMaxWidth = Math.max(textRealMaxWidth, lineWidth);
                    textRealMaxWidthWithLeft = Math.max(textRealMaxWidthWithLeft, lineWidth + lineLeft);
                    linesMaxWidth = Math.max(linesMaxWidth, (int) Math.ceil(lineWidth));
                    linesMaxWidthWithLeft = Math.max(linesMaxWidthWithLeft, (int) Math.ceil(lineWidth + lineLeft));
                }
                if (hasNonRTL) {
                    textRealMaxWidth = textRealMaxWidthWithLeft;
                    if (a == blocksCount - 1) {
                        lastLineWidth = lastLineWidthWithLeft;
                    }
                } else if (a == blocksCount - 1) {
                    lastLineWidth = linesMaxWidth;
                }
                textWidth = Math.max(textWidth, (int) Math.ceil(textRealMaxWidth));
            } else {
                textWidth = Math.max(textWidth, Math.min(maxWidth, linesMaxWidth));
            }

            if (hasNonRTL) {
                block.textXOffset = 0;
            }

            linesOffset += currentBlockLinesCount;
        }
    }

    public boolean isOut() {
        return messageOwner.out;
    }

    public boolean isOutOwner() { // is the message an outgoing message?
        return messageOwner.out && messageOwner.from_id > 0 && !messageOwner.post;
    }

    public boolean isFromUser() { // is the message from any user? send or received?
        return messageOwner.from_id > 0 && !messageOwner.post;
    }

    public boolean isUnread() {
        return messageOwner.unread;
    }

    public boolean isContentUnread() {
        return messageOwner.media_unread;
    }

    public int getId() {
        return messageOwner.id;
    }

    public long getDialogId() {
        return messageOwner.dialog_id;
    }

    public boolean isSending() {
        return messageOwner.send_state == MESSAGE_SEND_STATE_SENDING;
    }

    public boolean isSendError() {
        return messageOwner.send_state == MESSAGE_SEND_STATE_SEND_ERROR;
    }

    public boolean isSent() {
        return messageOwner.send_state == MESSAGE_SEND_STATE_SENT;
    }

    public static boolean isVoiceDocument(TLRPC.Document document) {
        if (document != null) {
            for (int a = 0; a < document.attributes.size(); a++) {
                TLRPC.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                    return attribute.voice;
                }
            }
        }
        return false;
    }

    public static boolean isMusicDocument(TLRPC.Document document) {
        if (document != null) {
            for (int a = 0; a < document.attributes.size(); a++) {
                TLRPC.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                    return !attribute.voice;
                }
            }
        }
        return false;
    }

    public static boolean isVideoDocument(TLRPC.Document document) {
        if (document != null) {
            boolean isAnimated = false;
            boolean isVideo = false;
            for (int a = 0; a < document.attributes.size(); a++) {
                TLRPC.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute instanceof TLRPC.TL_documentAttributeVideo) {
                    isVideo = true;
                } else if (attribute instanceof TLRPC.TL_documentAttributeAnimated) {
                    isAnimated = true;
                }
            }
            return isVideo && !isAnimated;
        }
        return false;
    }

    public TLRPC.Document getDocument() {
        return messageOwner.media != null ? messageOwner.media.document : null;
    }

    private static boolean isMusicMessage(TLRPC.Message message) {
        return message.media != null && message.media.document != null && isMusicDocument(message.media.document);
    }

    private static boolean isVoiceMessage(TLRPC.Message message) {
        return message.media != null && message.media.document != null && isVoiceDocument(message.media.document);
    }

    private static boolean isVideoMessage(TLRPC.Message message) {
        return message.media != null && message.media.document != null && isVideoDocument(message.media.document);
    }

    public boolean isSelectable() {
        if( type == MO_TYPE10_DATE_HEADLINE || type == MO_TYPE100_SYSTEM_MSG ) {
            return false;
        }
        return true;
    }

    public int getApproximateHeight() {
        if (type == MO_TYPE0_TEXT) {
            int height = textHeight;
            return height;
        } else if (type == MO_TYPE2_VOICE ) {
            return AndroidUtilities.dp(72);
        } else if (type == MO_TYPE9_FILE) {
            return AndroidUtilities.dp(100);
        } else if (type == MO_TYPE14_MUSIC) {
            return AndroidUtilities.dp(82);
        } else if (type == MO_TYPE10_DATE_HEADLINE||type == MO_TYPE100_SYSTEM_MSG) {
            return AndroidUtilities.dp(30);
        } else if (type == MO_TYPE13_STICKER) {
            float maxHeight = AndroidUtilities.displaySize.y * 0.4f;
            float maxWidth;
            maxWidth = AndroidUtilities.displaySize.x * 0.5f;
            int photoHeight = 0;
            int photoWidth = 0;
            for (TLRPC.DocumentAttribute attribute : messageOwner.media.document.attributes) {
                if (attribute instanceof TLRPC.TL_documentAttributeImageSize) {
                    photoWidth = attribute.w;
                    photoHeight = attribute.h;
                    break;
                }
            }
            if (photoWidth == 0) {
                photoHeight = (int) maxHeight;
                photoWidth = photoHeight + AndroidUtilities.dp(100);
            }
            if (photoHeight > maxHeight) {
                photoWidth *= maxHeight / photoHeight;
                photoHeight = (int)maxHeight;
            }
            if (photoWidth > maxWidth) {
                photoHeight *= maxWidth / photoWidth;
            }
            return photoHeight + AndroidUtilities.dp(14);
        } else {
            int photoHeight;
            int photoWidth;

            photoWidth = (int) (Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) * 0.7f);
            photoHeight = photoWidth + AndroidUtilities.dp(100);
            if (photoWidth > AndroidUtilities.getPhotoSize()) {
                photoWidth = AndroidUtilities.getPhotoSize();
            }
            if (photoHeight > AndroidUtilities.getPhotoSize()) {
                photoHeight = AndroidUtilities.getPhotoSize();
            }
            TLRPC.PhotoSize currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(photoThumbs, AndroidUtilities.getPhotoSize());

            if (currentPhotoObject != null) {
                float scale = (float) currentPhotoObject.w / (float) photoWidth;
                int h = (int) (currentPhotoObject.h / scale);
                if (h == 0) {
                    h = AndroidUtilities.dp(100);
                }
                if (h > photoHeight) {
                    h = photoHeight;
                } else if (h < AndroidUtilities.dp(120)) {
                    h = AndroidUtilities.dp(120);
                }
                photoHeight = h;
            }
            return photoHeight + AndroidUtilities.dp(14);
        }
    }

    public boolean isSticker() {
        return type == MO_TYPE13_STICKER;
    }

    public boolean isMusic() {
        return isMusicMessage(messageOwner);
    }

    public boolean isVoice() {
        return isVoiceMessage(messageOwner);
    }

    public boolean isVideo() {
        return isVideoMessage(messageOwner);
    }

    public boolean isGif() {
        return messageOwner.media instanceof TLRPC.TL_messageMediaDocument && isGifDocument(messageOwner.media.document);
    }

    public int getDuration() {
        TLRPC.Document document;
        document = messageOwner.media.document;
        for (int a = 0; a < document.attributes.size(); a++) {
            TLRPC.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                return attribute.duration;
            }
        }
        return 0;
    }

    public boolean isForwarded() {
        return (messageOwner.flags & TLRPC.MESSAGE_FLAG_FWD) != 0;
    }

    private boolean isMediaEmpty() {
        return isMediaEmpty(messageOwner);
    }

    private static boolean isMediaEmpty(TLRPC.Message message) {
        return message == null || message.media == null || message.media instanceof TLRPC.TL_messageMediaEmpty;
    }

    private void checkMediaExistance() {
        attachPathExists = false;
        mediaExists = false;
        if (type == MO_TYPE1_PHOTO) {
            TLRPC.PhotoSize currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(photoThumbs, AndroidUtilities.getPhotoSize());
            if (currentPhotoObject != null) {
                File f = FileLoader.getPathToMessage(messageOwner);
                if( f != null ) {
                    mediaExists = f.exists();
                }
            }
        } else if (type == MO_TYPE8_GIF || type == MO_TYPE3_VIDEO || type == MO_TYPE9_FILE || type == MO_TYPE2_VOICE || type == MO_TYPE14_MUSIC) {
            if (messageOwner.attachPath != null && messageOwner.attachPath.length() > 0) {
                File f = new File(messageOwner.attachPath);
                attachPathExists = f.exists();
            }
            if (!attachPathExists) {
                mediaExists = FileLoader.getPathToMessage(messageOwner).exists();
            }
        } else {
            TLRPC.Document document = getDocument();
            if (document != null) {
                mediaExists = FileLoader.getPathToAttach(document).exists();
            } else if (type == MO_TYPE0_TEXT) {
                TLRPC.PhotoSize currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(photoThumbs, AndroidUtilities.getPhotoSize());
                if (currentPhotoObject == null) {
                    return;
                }
                mediaExists = FileLoader.getPathToAttach(currentPhotoObject, true).exists();
            }
        }

        // we use the attachPath for the normal images (bp)
        if( (mediaExists || attachPathExists) && messageOwner.created_by_mr ) {
            attachPathExists = true;
            mediaExists = true;
        }
    }
}
