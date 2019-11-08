package org.thoughtcrime.securesms.search;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import androidx.fragment.app.Fragment;

import org.thoughtcrime.securesms.search.model.SearchResult;
import org.thoughtcrime.securesms.util.Debouncer;

/**
 * A {@link ViewModel} for handling all the business logic and interactions that take place inside
 * of the {@link SearchFragment}.
 *
 * This class should be view- and Android-agnostic, and therefore should contain no references to
 * things like {@link android.content.Context}, {@link android.view.View},
 * {@link Fragment}, etc.
 */
class SearchViewModel extends ViewModel {

  private final ObservingLiveData searchResult;
  private final SearchRepository  searchRepository;
  private final Debouncer         debouncer;

  private String lastQuery;

  SearchViewModel(@NonNull SearchRepository searchRepository) {
    this.searchResult     = new ObservingLiveData();
    this.searchRepository = searchRepository;
    this.debouncer        = new Debouncer(150);

//    searchResult.registerContentObserver(new ContentObserver(new Handler()) {
//      @Override
//      public void onChange(boolean selfChange) {
//        if (!TextUtils.isEmpty(getLastQuery())) {
//          searchRepository.query(getLastQuery(), searchResult::postValue);
//        }
//      }
//    });
  }

  LiveData<SearchResult> getSearchResult() {
    return searchResult;
  }

  public void includeMessageQueries(boolean include) {
    this.searchRepository.setQueryMessages(include);
  }

  void updateQuery(String query) {
    lastQuery = query;
    debouncer.publish(() -> searchRepository.query(query, searchResult::postValue));
  }

  @NonNull
  String getLastQuery() {
    return lastQuery == null ? "" : lastQuery;
  }

  @Override
  protected void onCleared() {
    debouncer.clear();
  }

  private static class ObservingLiveData extends MutableLiveData<SearchResult> {
  }

  public static class Factory extends ViewModelProvider.NewInstanceFactory {

    private final SearchRepository searchRepository;

    public Factory(@NonNull SearchRepository searchRepository) {
      this.searchRepository = searchRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return modelClass.cast(new SearchViewModel(searchRepository));
    }
  }
}
