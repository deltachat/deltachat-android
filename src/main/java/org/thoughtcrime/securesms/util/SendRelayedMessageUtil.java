package org.thoughtcrime.securesms.util;

import static org.thoughtcrime.securesms.util.ShareUtil.getForwardedMessageIDs;
import static org.thoughtcrime.securesms.util.ShareUtil.getSharedText;
import static org.thoughtcrime.securesms.util.ShareUtil.getSharedUris;
import static org.thoughtcrime.securesms.util.ShareUtil.isForwarding;
import static org.thoughtcrime.securesms.util.ShareUtil.isSharing;
import static org.thoughtcrime.securesms.util.ShareUtil.resetRelayingMessageContent;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.ConversationListRelayingActivity;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.PartAuthority;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class SendRelayedMessageUtil {

  public static void immediatelyRelay(Activity activity, int chatId) {
    immediatelyRelay(activity, new Long[]{(long) chatId});
  }

  public static void immediatelyRelay(Activity activity, final Long[] chatIds) {
    ConversationListRelayingActivity.finishActivity();
    if (isForwarding(activity)) {
      int[] forwardedMessageIDs = getForwardedMessageIDs(activity);
      resetRelayingMessageContent(activity);
      if (forwardedMessageIDs == null) return;

      Util.runOnAnyBackgroundThread(() -> {
        DcContext dcContext = DcHelper.getContext(activity);
        for (long longChatId : chatIds) {
          int chatId = (int) longChatId;
          if (dcContext.getChat(chatId).isSelfTalk()) {
            for (int msgId : forwardedMessageIDs) {
              DcMsg msg = dcContext.getMsg(msgId);
              if (msg.canSave() && msg.getSavedMsgId() == 0 && msg.getChatId() != chatId) {
                dcContext.saveMsgs(new int[]{msgId});
              } else {
                handleForwarding(activity, chatId, new int[]{msgId});
              }
            }
          } else {
            handleForwarding(activity, chatId, forwardedMessageIDs);
          }
        }

      });
    } else if (isSharing(activity)) {
      ArrayList<Uri> sharedUris = getSharedUris(activity);
      String sharedText = getSharedText(activity);
      resetRelayingMessageContent(activity);
      Util.runOnAnyBackgroundThread(() -> {
        for (long chatId : chatIds) {
          sendMultipleMsgs(activity, (int) chatId, sharedUris, sharedText);
        }
      });
    }
  }

  private static void handleForwarding(Context context, int chatId, int[] forwardedMessageIDs) {
    DcContext dcContext = DcHelper.getContext(context);
    dcContext.forwardMsgs(forwardedMessageIDs, chatId);
  }

  public static void sendMultipleMsgs(Context context, int chatId, ArrayList<Uri> sharedUris, String sharedText) {
    DcContext dcContext = DcHelper.getContext(context);
    ArrayList<Uri> uris = sharedUris;
    String text = sharedText;

    if (uris.size() == 1) {
      dcContext.sendMsg(chatId, createMessage(context, uris.get(0), text));
    } else {
      if (text != null) {
        dcContext.sendMsg(chatId, createMessage(context, null, text));
      }
      for (Uri uri : uris) {
        dcContext.sendMsg(chatId, createMessage(context, uri, null));
      }
    }
  }

  public static boolean containsVideoType(Context context, ArrayList<Uri> uris) {
    for (final Uri uri : uris) {
      final String mimeType = MediaUtil.getMimeType(context, uri);
      if (MediaUtil.isVideoType(mimeType)) {
        return true;
      }
    }
    return false;
  }

  public static DcMsg createMessage(Context context, Uri uri, String text) throws NullPointerException {
    DcContext dcContext = DcHelper.getContext(context);
    DcMsg message;
    String mimeType = MediaUtil.getMimeType(context, uri);
    if (uri == null) {
      message = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
    } else if (MediaUtil.isImageType(mimeType)) {
      message = new DcMsg(dcContext, DcMsg.DC_MSG_IMAGE);
    } else if (MediaUtil.isAudioType(mimeType)) {
      message = new DcMsg(dcContext, DcMsg.DC_MSG_AUDIO);
    } else if (MediaUtil.isVideoType(mimeType)) {
      message = new DcMsg(dcContext, DcMsg.DC_MSG_VIDEO);
    } else {
      message = new DcMsg(dcContext, DcMsg.DC_MSG_FILE);
    }

    if (uri != null) {
      setFileFromUri(context, uri, message, mimeType);
    }
    if (text != null) {
      message.setText(text);
    }
    return message;
  }

  private static void setFileFromUri(Context context, Uri uri, DcMsg message, String mimeType) {
    String path;
    DcContext dcContext = DcHelper.getContext(context);
    String filename = "cannot-resolve.jpg"; // best guess, this still leads to most images being workable if OS does weird things
    try {

      if (PartAuthority.isLocalUri(uri)) {
        filename = uri.getPathSegments().get(PersistentBlobProvider.FILENAME_PATH_SEGMENT);
      } else if (uri.getScheme().equals("content")) {
        final ContentResolver contentResolver = context.getContentResolver();
        final Cursor cursor = contentResolver.query(uri, null, null, null, null);
        try {
          if (cursor != null && cursor.moveToFirst()) {
            final int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex >= 0) {
              filename = cursor.getString(nameIndex);
            }
          }
        } finally {
          cursor.close();
        }
      }

      path = DcHelper.getBlobdirFile(dcContext, filename, "temp");

      // copy content to this file
      if (path != null) {
        InputStream inputStream = PartAuthority.getAttachmentStream(context, uri);
        OutputStream outputStream = new FileOutputStream(path);
        Util.copy(inputStream, outputStream);
      }
    } catch (Exception e) {
      e.printStackTrace();
      path = null;
    }
    message.setFileAndDeduplicate(path, filename, mimeType);
  }

}
