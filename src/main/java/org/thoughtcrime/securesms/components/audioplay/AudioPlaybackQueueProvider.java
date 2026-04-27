package org.thoughtcrime.securesms.components.audioplay;

import android.net.Uri;
import androidx.annotation.NonNull;
import java.util.List;

public interface AudioPlaybackQueueProvider {

  @NonNull
  List<QueueItem> buildVoiceQueue(int startMsgId);

  final class QueueItem {
    private final int msgId;
    private final Uri uri;

    public QueueItem(int msgId, @NonNull Uri uri) {
      this.msgId = msgId;
      this.uri = uri;
    }

    public int getMsgId() {
      return msgId;
    }

    @NonNull
    public Uri getUri() {
      return uri;
    }
  }
}
