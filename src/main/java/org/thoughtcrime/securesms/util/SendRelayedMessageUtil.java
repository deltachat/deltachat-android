package org.thoughtcrime.securesms.util;

import static org.thoughtcrime.securesms.util.ShareUtil.getForwardedMessageAccountId;
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
import androidx.annotation.Nullable;
import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.thoughtcrime.securesms.ConversationListRelayingActivity;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.PartAuthority;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.video.VideoRecodeManager;

public class SendRelayedMessageUtil {

  public static boolean immediatelyRelay(Activity activity, int chatId) {
    return immediatelyRelay(activity, new Long[] {(long) chatId});
  }

  public static boolean immediatelyRelay(Activity activity, final Long[] chatIds) {
    if (!isForwarding(activity)
        && isSharing(activity)
        && containsVideo(activity, getSharedUris(activity))
        && VideoRecodeManager.getInstance(activity).isBusy()) {
      VideoRecodeManager.showBusyDialog(activity);
      return false;
    }

    ConversationListRelayingActivity.finishActivity();
    if (isForwarding(activity)) {
      int forwardedMsgAccId = getForwardedMessageAccountId(activity);
      int[] forwardedMessageIDs = getForwardedMessageIDs(activity);
      resetRelayingMessageContent(activity);
      if (forwardedMessageIDs == null || forwardedMsgAccId <= 0) return true;

      Util.runOnAnyBackgroundThread(
          () -> {
            DcContext dcContext = DcHelper.getContext(activity);
            int accId = dcContext.getAccountId();
            if (forwardedMsgAccId != accId) {
              Rpc rpc = DcHelper.getRpc(activity);
              List<Integer> list = Util.toList(forwardedMessageIDs);
              for (long longChatId : chatIds) {
                try {
                  rpc.forwardMessagesToAccount(forwardedMsgAccId, list, accId, (int) longChatId);
                } catch (RpcException e) {
                  e.printStackTrace();
                }
              }
              return;
            }

            for (long longChatId : chatIds) {
              int chatId = (int) longChatId;
              if (dcContext.getChat(chatId).isSelfTalk()) {
                for (int msgId : forwardedMessageIDs) {
                  DcMsg msg = dcContext.getMsg(msgId);
                  if (msg.canSave() && msg.getSavedMsgId() == 0 && msg.getChatId() != chatId) {
                    dcContext.saveMsgs(new int[] {msgId});
                  } else {
                    handleForwarding(activity, chatId, new int[] {msgId});
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
      relayFiles(activity, chatIds, sharedUris, sharedText);
    }
    return true;
  }

  public static boolean relayFiles(
      Activity activity, Long[] chatIds, ArrayList<Uri> uris, @Nullable String text) {
    if (containsVideo(activity, uris)) {
      DcContext dcContext = DcHelper.getContext(activity);
      if (!VideoRecodeManager.getInstance(activity)
          .startOperation(dcContext, chatIds, uris, text)) {
        VideoRecodeManager.showBusyDialog(activity);
        return false;
      }
      return true;
    }
    Util.runOnAnyBackgroundThread(
        () -> {
          for (long chatId : chatIds) {
            sendMultipleMsgs(activity, (int) chatId, uris, text);
          }
        });
    return true;
  }

  public static boolean containsVideo(Context context, List<Uri> uris) {
    for (Uri uri : uris) {
      if (MediaUtil.isVideoType(MediaUtil.getMimeType(context, uri))) {
        return true;
      }
    }
    return false;
  }

  private static void handleForwarding(Context context, int chatId, int[] forwardedMessageIDs) {
    DcContext dcContext = DcHelper.getContext(context);
    dcContext.forwardMsgs(forwardedMessageIDs, chatId);
  }

  public static void sendMultipleMsgs(
      Context context, int chatId, ArrayList<Uri> uris, String text) {
    DcContext dcContext = DcHelper.getContext(context);

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

  public static DcMsg createMessage(Context context, Uri uri, String text)
      throws NullPointerException {
    return createMessage(context, DcHelper.getContext(context), uri, text);
  }

  public static DcMsg createMessage(Context context, DcContext dcContext, Uri uri, String text)
      throws NullPointerException {
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
      setFileFromUri(context, dcContext, uri, message, mimeType);
    }
    if (text != null) {
      message.setText(text);
    }
    return message;
  }

  private static void setFileFromUri(
      Context context, DcContext dcContext, Uri uri, DcMsg message, String mimeType) {
    String path;
    String filename =
        "cannot-resolve.jpg"; // best guess, this still leads to most images being workable if OS
    // does weird things
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
        Util.copyCancellable(inputStream, outputStream, path);
      }
    } catch (Exception e) {
      e.printStackTrace();
      path = null;
    }
    message.setFileAndDeduplicate(path, filename, mimeType);
  }
}
