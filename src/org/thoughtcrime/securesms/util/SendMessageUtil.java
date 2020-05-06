package org.thoughtcrime.securesms.util;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.ConversationActivity;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
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

public class SendMessageUtil {
  private static final String TAG = SendMessageUtil.class.getSimpleName();

  public static void immediatelyRelay(Activity activity, int chatId) {
    activity.setResult(RESULT_OK);
    if (isForwarding(activity)) {
      handleForwarding(activity, chatId);
    } else if (isSharing(activity)) {
      handleSharing(activity, chatId);
    }
  }

  private static void handleForwarding(Activity activity, int chatId) {
    DcContext dcContext = DcHelper.getContext(activity);
    dcContext.forwardMsgs(getForwardedMessageIDs(activity), chatId);
  }

  private static void handleSharing(Activity activity, int chatId) {
    DcContext dcContext = DcHelper.getContext(activity);
    ArrayList<Uri> uris = getSharedUris(activity);

    Log.e(TAG, "HandleSharing, size: " + uris.size()+" text: "+ getSharedText(activity));
    DcMsg textMessage = createMessage(activity, null, getSharedText(activity));
    dcContext.sendMsg(chatId, textMessage);
    for(Uri uri : uris) {
      Log.e(TAG, "HandleSharing "+uri + " text: "+ getSharedText(activity));
      DcMsg message = createMessage(activity, uri, null);
      dcContext.sendMsg(chatId, message);
      cleanup(activity, uri);
    }
  }

  private static void cleanup(Context context, final @Nullable Uri uri) {
    if (uri != null && PersistentBlobProvider.isAuthority(context, uri)) {
      Log.w(TAG, "cleaning up " + uri);
      PersistentBlobProvider.getInstance(context).delete(context, uri);
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
    }
    else if (MediaUtil.isAudioType(mimeType)) {
      message = new DcMsg(dcContext,DcMsg.DC_MSG_AUDIO);
    }
    else if (MediaUtil.isVideoType(mimeType)) {
      message = new DcMsg(dcContext, DcMsg.DC_MSG_VIDEO);
    }
    else {
      message = new DcMsg(dcContext, DcMsg.DC_MSG_FILE);
    }

    if (uri != null) {
      message.setFile(getRealPathFromUri(context, uri), mimeType);
    }
    message.setText(text);
    return message;
  }

  private static String getRealPathFromUri(Context context, Uri uri) throws NullPointerException {
    ApplicationDcContext dcContext = DcHelper.getContext(context);
    try {
      String filename = uri.getPathSegments().get(2); // Get real file name from Uri
      String ext = "";
      int i = filename.lastIndexOf(".");
      if(i>=0) {
        ext = filename.substring(i);
        filename = filename.substring(0, i);
      }
      String path = dcContext.getBlobdirFile(filename, ext);

      // copy content to this file
      if(path != null) {
        InputStream inputStream = PartAuthority.getAttachmentStream(context, uri);
        OutputStream outputStream = new FileOutputStream(path);
        Util.copy(inputStream, outputStream);
      }

      return path;
    }
    catch(Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
