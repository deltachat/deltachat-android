package org.thoughtcrime.securesms.search;

import android.content.Context;
import android.database.DatabaseUtils;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.b44t.messenger.DcChatlist;
import com.b44t.messenger.DcContext;


import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.search.model.SearchResult;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Manages data retrieval for search.
 */
class SearchRepository {

  private final ApplicationDcContext dcContext;
  private final Executor             executor;
  private boolean                    queryMessages = true;

  SearchRepository(@NonNull Context          context,
                   @NonNull Executor         executor)
  {
    this.dcContext        = DcHelper.getContext(context.getApplicationContext());
    this.executor         = executor;
  }

  void query(@NonNull String query, @NonNull Callback callback) {
    if (TextUtils.isEmpty(query)) {
      callback.onResult(SearchResult.EMPTY);
      return;
    }

    executor.execute(() -> {
      int[]      contacts      = dcContext.getContacts(DcContext.DC_GCL_ADD_SELF, query);
      DcChatlist conversations = dcContext.getChatlist(0, query, 0);
      int[]      messages      = new int[0];
      if (queryMessages) {
        messages = dcContext.searchMsgs(0, query);
      }

      callback.onResult(new SearchResult(query, contacts, conversations, messages));
    });
  }

  public void setQueryMessages(boolean includeMessageQueries) {
    this.queryMessages = includeMessageQueries;
  }

  public interface Callback {
    void onResult(@NonNull SearchResult result);
  }
}
