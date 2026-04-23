package org.thoughtcrime.securesms.components.audioplay;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.AudioSlide;

public class ChatVoiceQueueProvider implements AudioPlaybackQueueProvider {

  private final Context context;
  private final int chatId;

  public ChatVoiceQueueProvider(@NonNull Context context, int chatId) {
    this.context = context.getApplicationContext();
    this.chatId = chatId;
  }

  @NonNull
  @Override
  public List<QueueItem> buildVoiceQueue(int startMsgId) {
    DcContext dcContext = DcHelper.getContext(context);
    int[] msgIds = dcContext.getChatMsgs(chatId, 0, 0);

    int startIndex = -1;
    for (int i = 0; i < msgIds.length; i++) {
      if (msgIds[i] == startMsgId) {
        startIndex = i;
        break;
      }
    }

    if (startIndex == -1) {
      return Collections.emptyList();
    }

    List<QueueItem> queue = new ArrayList<>();
    for (int i = startIndex; i < msgIds.length; i++) {
      DcMsg msg = dcContext.getMsg(msgIds[i]);
      if (msg.getType() == DcMsg.DC_MSG_VOICE) {
        Uri uri = new AudioSlide(context, msg).getUri();
        if (uri != null) {
          queue.add(new QueueItem(msgIds[i], uri));
        }
      }
    }
    return queue;
  }
}
