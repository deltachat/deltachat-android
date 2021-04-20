package org.thoughtcrime.securesms.search;

import androidx.annotation.NonNull;

import org.thoughtcrime.securesms.search.model.SearchResult;

class SearchRepository {
  public interface Callback {
    void onResult(@NonNull SearchResult result);
  }
}
