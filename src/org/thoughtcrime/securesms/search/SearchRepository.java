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

  private static final Set<Character> BANNED_CHARACTERS = new HashSet<>();
  static {
    // Several ranges of invalid ASCII characters
    for (int i = 33; i <= 47; i++) {
      BANNED_CHARACTERS.add((char) i);
    }
    for (int i = 58; i <= 64; i++) {
      BANNED_CHARACTERS.add((char) i);
    }
    for (int i = 91; i <= 96; i++) {
      BANNED_CHARACTERS.add((char) i);
    }
    for (int i = 123; i <= 126; i++) {
      BANNED_CHARACTERS.add((char) i);
    }
  }

  private final Context              context;
  private final ApplicationDcContext dcContext;
  private final Executor             executor;
  private boolean                    queryMessages = true;

  SearchRepository(@NonNull Context          context,
                   @NonNull Executor         executor)
  {
    this.context          = context.getApplicationContext();
    this.dcContext        = DcHelper.getContext(context);
    this.executor         = executor;
  }

  void query(@NonNull String query, @NonNull Callback callback) {
    if (TextUtils.isEmpty(query)) {
      callback.onResult(SearchResult.EMPTY);
      return;
    }

    executor.execute(() -> {
      String cleanQuery = sanitizeQuery(query);

      int[]      contacts      = dcContext.getContacts(DcContext.DC_GCL_ADD_SELF, cleanQuery);
      DcChatlist conversations = dcContext.getChatlist(0, cleanQuery, 0);
      int[]      messages      = new int[0];
      if (queryMessages) {
        messages = dcContext.searchMsgs(0, cleanQuery);
      }

      callback.onResult(new SearchResult(cleanQuery, contacts, conversations, messages));
    });
  }

  public void setQueryMessages(boolean includeMessageQueries) {
    this.queryMessages = includeMessageQueries;
  }

  /**
   * Unfortunately {@link DatabaseUtils#sqlEscapeString(String)} is not sufficient for our purposes.
   * MATCH queries have a separate format of their own that disallow most "special" characters.
   *
   * Also, SQLite can't search for apostrophes, meaning we can't normally find words like "I'm".
   * However, if we replace the apostrophe with a space, then the query will find the match.
   */
  private String sanitizeQuery(@NonNull String query) {
    StringBuilder out = new StringBuilder();

    for (int i = 0; i < query.length(); i++) {
      char c = query.charAt(i);
      if (!BANNED_CHARACTERS.contains(c)) {
        out.append(c);
      } else if (c == '\'') {
        out.append(' ');
      }
    }

    return out.toString();
  }

  public interface Callback {
    void onResult(@NonNull SearchResult result);
  }
}
