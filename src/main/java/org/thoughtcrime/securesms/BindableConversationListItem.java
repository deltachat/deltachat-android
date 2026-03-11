package org.thoughtcrime.securesms;

import androidx.annotation.NonNull;
import com.b44t.messenger.DcLot;
import java.util.Set;
import org.thoughtcrime.securesms.database.model.ThreadRecord;
import org.thoughtcrime.securesms.mms.GlideRequests;

public interface BindableConversationListItem extends Unbindable {

  public void bind(
      @NonNull ThreadRecord thread,
      int msgId,
      @NonNull DcLot dcSummary,
      @NonNull GlideRequests glideRequests,
      @NonNull Set<Long> selectedThreads,
      boolean batchMode);
}
