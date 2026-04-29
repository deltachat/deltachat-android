package org.thoughtcrime.securesms.components.audioplay;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;
import java.util.ArrayList;
import java.util.List;
import org.thoughtcrime.securesms.connect.DcHelper;

public class ChatAudioQueueProvider {

  private final Context context;
  private final int chatId;

  public ChatAudioQueueProvider(@NonNull Context context, int chatId) {
    this.context = context.getApplicationContext();
    this.chatId = chatId;
  }

  @NonNull
  public List<MediaItem> buildAudioQueue() {
    DcContext dcContext = DcHelper.getContext(context);
    int[] msgIds = dcContext.getChatMedia(chatId, DcMsg.DC_MSG_AUDIO, DcMsg.DC_MSG_VOICE, 0);

    List<MediaItem> items = new ArrayList<>(msgIds.length);
    for (int msgId : msgIds) {
      String id = String.valueOf(msgId);
      items.add(new MediaItem.Builder().setMediaId(id).setUri(Uri.parse("dcmsg://" + id)).build());
    }
    return items;
  }
}
