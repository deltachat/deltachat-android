package org.thoughtcrime.securesms.util;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

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

import static android.app.Activity.RESULT_OK;
import static org.thoughtcrime.securesms.util.RelayUtil.getForwardedMessageIDs;
import static org.thoughtcrime.securesms.util.RelayUtil.getSharedText;
import static org.thoughtcrime.securesms.util.RelayUtil.getSharedUris;
import static org.thoughtcrime.securesms.util.RelayUtil.isForwarding;
import static org.thoughtcrime.securesms.util.RelayUtil.isSharing;
import static org.thoughtcrime.securesms.util.RelayUtil.resetRelayingMessageContent;

public class SendRelayedMessageUtil {
  private static final String TAG = SendRelayedMessageUtil.class.getSimpleName();

  public static void immediatelyRelay(Activity activity, int chatId) {
    immediatelyRelay(activity, new Long[]{(long) chatId});
  }

  public static void immediatelyRelay(Activity activity, final Long[] chatIds) {
    ConversationListRelayingActivity.finishActivity();
    if (isForwarding(activity)) {
      int[] forwardedMessageIDs = getForwardedMessageIDs(activity);
      resetRelayingMessageContent(activity);
      Util.runOnAnyBackgroundThread(() -> {
        for (long chatId : chatIds) {
          handleForwarding(activity, (int) chatId, forwardedMessageIDs);
        }

      });
    } else if (isSharing(activity)) {
      ArrayList<Uri> sharedUris = getSharedUris(activity);
      String sharedText = getSharedText(activity);
      resetRelayingMessageContent(activity);
      Util.runOnAnyBackgroundThread(() -> {
        for (long chatId : chatIds) {
          handleSharing(activity, (int) chatId, sharedUris, sharedText);
        }
      });
    }
  }

  private static void handleForwarding(Context context, int chatId, int[] forwardedMessageIDs) {
    DcContext dcContext = DcHelper.getContext(context);
    dcContext.forwardMsgs(forwardedMessageIDs, chatId);
  }

  private static void handleSharing(Context context, int chatId, ArrayList<Uri> sharedUris, String sharedText) {
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

  private static void cleanup(Activity activity) {
    for (Uri uri : getSharedUris(activity)) {
      if (uri != null && PersistentBlobProvider.isAuthority(activity, uri)) {
        Log.i(TAG, "cleaning up " + uri);
        PersistentBlobProvider.getInstance().delete(activity, uri);
      }
    }
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
      message.setFile(getRealPathFromUri(context, uri), mimeType);
    }
    if (text != null) {
      message.setText(text);
    }
    return message;
  }

  private static String getRealPathFromUri(Context context, Uri uri) throws NullPointerException {
    DcContext dcContext = DcHelper.getContext(context);
    try {
      String filename = uri.getPathSegments().get(2); // Get real file name from Uri
      String ext = "";
      int i = filename.lastIndexOf(".");
      if (i >= 0) {
        ext = filename.substring(i);
        filename = filename.substring(0, i);
      }
      String path = DcHelper.getBlobdirFile(dcContext, filename, ext);

      // copy content to this file
      if (path != null) {
        InputStream inputStream = PartAuthority.getAttachmentStream(context, uri);
        OutputStream outputStream = new FileOutputStream(path);
        Util.copy(inputStream, outputStream);
      }

      return path;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
