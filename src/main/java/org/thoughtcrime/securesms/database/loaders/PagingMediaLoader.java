package org.thoughtcrime.securesms.database.loaders;


import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMediaGalleryElement;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.AsyncLoader;

public class PagingMediaLoader extends AsyncLoader<DcMediaGalleryElement> {

  private static final String TAG = PagingMediaLoader.class.getSimpleName();

  private final DcMsg     msg;
  private final boolean   leftIsRecent;

  public PagingMediaLoader(@NonNull Context context, @NonNull DcMsg msg, boolean leftIsRecent) {
    super(context);
    this.msg          = msg;
    this.leftIsRecent = leftIsRecent;
  }

  @Nullable
  @Override
  public DcMediaGalleryElement loadInBackground() {
    DcContext context = DcHelper.getContext(getContext());
    int[] mediaMessages = context.getChatMedia(msg.getChatId(), DcMsg.DC_MSG_IMAGE, DcMsg.DC_MSG_GIF, DcMsg.DC_MSG_VIDEO);
    // first id is the oldest message.
    int currentIndex = -1;
    for(int ii = 0; ii < mediaMessages.length; ii++) {
      if(mediaMessages[ii] == msg.getId()) {
        currentIndex = ii;
        break;
      }
    }
    if(currentIndex == -1) {
      currentIndex = 0;
      DcMsg unfound = context.getMsg(msg.getId());
      Log.e(TAG, "did not find message in list: " + unfound.getId() + " / " + unfound.getFile() + " / " + unfound.getText());
    }
    return new DcMediaGalleryElement(mediaMessages, currentIndex, context, leftIsRecent);
  }
}
