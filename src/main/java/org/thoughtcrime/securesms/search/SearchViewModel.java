package org.thoughtcrime.securesms.search;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.b44t.messenger.DcChatlist;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.search.model.SearchResult;
import org.thoughtcrime.securesms.util.Util;

class SearchViewModel extends ViewModel {
  private static final String        TAG = SearchViewModel.class.getSimpleName();
  private final ObservingLiveData    searchResult;
  private String                     lastQuery;
  private final DcContext            dcContext;
  private boolean                    forwarding = false;
  private boolean                    inBgSearch;
  private boolean                    needsAnotherBgSearch;

  SearchViewModel(@NonNull Context context) {
    this.dcContext        = DcHelper.getContext(context.getApplicationContext());
    this.searchResult     = new ObservingLiveData();
  }

  LiveData<SearchResult> getSearchResult() {
    return searchResult;
  }

  public void setForwardingMode(boolean forwarding) {
    this.forwarding = forwarding;
  }


  void updateQuery(String query) {
    lastQuery = query;
    updateQuery();
  }

  public void updateQuery() {
    if (inBgSearch) {
      needsAnotherBgSearch = true;
      Log.i(TAG, "... search call debounced");
    } else {
      inBgSearch = true;
      Util.runOnBackground(() -> {

        Util.sleep(100);
        needsAnotherBgSearch = false;
        queryAndCallback(lastQuery, searchResult::postValue);

        while (needsAnotherBgSearch) {
          Util.sleep(100);
          needsAnotherBgSearch = false;
          Log.i(TAG, "... executing debounced search call");
          queryAndCallback(lastQuery, searchResult::postValue);
        }

        inBgSearch = false;
      });
    }
  }

  private void queryAndCallback(@NonNull String query, @NonNull SearchViewModel.Callback callback) {
    int overallCnt = 0;

    if (TextUtils.isEmpty(query)) {
      callback.onResult(SearchResult.EMPTY);
      return;
    }

    // #1 search for chats
    long startMs = System.currentTimeMillis();
    DcChatlist conversations = dcContext.getChatlist(forwarding? DcContext.DC_GCL_FOR_FORWARDING : 0, query, 0);
    overallCnt += conversations.getCnt();
    Log.i(TAG, "⏰ getChatlist(" + query + "): " + (System.currentTimeMillis() - startMs) + "ms");

    // #2 search for contacts
    if (!query.equals(lastQuery) && overallCnt > 0) {
      Log.i(TAG, "... skipping getContacts() and searchMsgs(), more recent search pending");
      callback.onResult(new SearchResult(query, new int[0], conversations, new int[0]));
      return;
    }

    startMs = System.currentTimeMillis();
    int[] contacts = dcContext.getContacts(DcContext.DC_GCL_ADD_SELF, query);
    overallCnt += contacts.length;
    Log.i(TAG, "⏰ getContacts(" + query + "): " + (System.currentTimeMillis() - startMs) + "ms");

    // #3 search for messages
    if (forwarding) {
      Log.i(TAG, "... searchMsgs() disabled by caller");
      callback.onResult(new SearchResult(query, contacts, conversations, new int[0]));
      return;
    }

    if (query.length() <= 1) {
      Log.i(TAG, "... skipping searchMsgs(), string too short");
      callback.onResult(new SearchResult(query, contacts, conversations, new int[0]));
      return;
    }

    if (!query.equals(lastQuery) && overallCnt > 0) {
      Log.i(TAG, "... skipping searchMsgs(), more recent search pending");
      callback.onResult(new SearchResult(query, contacts, conversations, new int[0]));
      return;
    }

    startMs = System.currentTimeMillis();
    int[] messages = dcContext.searchMsgs(0, query);
    Log.i(TAG, "⏰ searchMsgs(" + query + "): " + (System.currentTimeMillis() - startMs) + "ms");

    callback.onResult(new SearchResult(query, contacts, conversations, messages));
  }

  @NonNull
  String getLastQuery() {
    return lastQuery == null ? "" : lastQuery;
  }

  @Override
  protected void onCleared() {
  }

  private static class ObservingLiveData extends MutableLiveData<SearchResult> {
  }

  public static class Factory extends ViewModelProvider.NewInstanceFactory {

    private final Context context;

    public Factory(@NonNull Context context) {
      this.context = context;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return modelClass.cast(new SearchViewModel(context));
    }
  }

  public interface Callback {
    void onResult(@NonNull SearchResult result);
  }
}
