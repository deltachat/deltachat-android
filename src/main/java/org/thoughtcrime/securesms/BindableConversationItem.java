package org.thoughtcrime.securesms;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;

import java.util.Set;

public interface BindableConversationItem extends Unbindable {
  void bind(@NonNull DcMsg                   messageRecord,
            @NonNull DcChat                  dcChat,
            @NonNull GlideRequests           glideRequests,
            @NonNull Set<DcMsg>              batchSelected,
            @NonNull Recipient               recipients,
                     boolean                 pulseHighlight);

  DcMsg getMessageRecord();

  void setEventListener(@Nullable EventListener listener);

  interface EventListener {
    void onQuoteClicked(DcMsg messageRecord);
    void onJumpToOriginalClicked(DcMsg messageRecord);
    void onShowFullClicked(DcMsg messageRecord);
    void onDownloadClicked(DcMsg messageRecord);
    void onReactionClicked(DcMsg messageRecord);
  }
}
