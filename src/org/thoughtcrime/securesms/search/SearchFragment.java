package org.thoughtcrime.securesms.search;


import androidx.lifecycle.ViewModelProviders;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.b44t.messenger.DcChatlist;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.search.model.SearchResult;
import org.thoughtcrime.securesms.util.StickyHeaderDecoration;

import java.util.Locale;
import java.util.concurrent.Executors;

import static org.thoughtcrime.securesms.util.RelayUtil.isRelayingMessageContent;

/**
 * A fragment that is displayed to do full-text search of messages, groups, and contacts.
 */
public class SearchFragment extends Fragment implements SearchListAdapter.EventListener {

  public static final String TAG          = "SearchFragment";
  public static final String EXTRA_LOCALE = "locale";

  private TextView               noResultsView;
  private RecyclerView           listView;
  private StickyHeaderDecoration listDecoration;

  private SearchViewModel   viewModel;
  private SearchListAdapter listAdapter;
  private String            pendingQuery;
  private Locale            locale;

  public static SearchFragment newInstance(@NonNull Locale locale) {
    Bundle args = new Bundle();
    args.putSerializable(EXTRA_LOCALE, locale);

    SearchFragment fragment = new SearchFragment();
    fragment.setArguments(args);

    return fragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    this.locale = (Locale) getArguments().getSerializable(EXTRA_LOCALE);

    SearchRepository searchRepository = new SearchRepository(getContext(),
                                                             Executors.newSingleThreadExecutor());
    viewModel = ViewModelProviders.of(this, new SearchViewModel.Factory(searchRepository)).get(SearchViewModel.class);

    if (pendingQuery != null) {
      viewModel.updateQuery(pendingQuery);
      pendingQuery = null;
    }
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_search, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    noResultsView = view.findViewById(R.id.search_no_results);
    listView      = view.findViewById(R.id.search_list);

    listAdapter    = new SearchListAdapter(getContext(), GlideApp.with(this), this, locale);
    listDecoration = new StickyHeaderDecoration(listAdapter, false, true);

    listView.setAdapter(listAdapter);
    listView.addItemDecoration(listDecoration);
    listView.setLayoutManager(new LinearLayoutManager(getContext()));
  }

  @Override
  public void onStart() {
    super.onStart();
    viewModel.includeMessageQueries(!isRelayingMessageContent(getActivity()));
    viewModel.getSearchResult().observe(this, result -> {
      result = result != null ? result : SearchResult.EMPTY;

      listAdapter.updateResults(result);
      listDecoration.invalidateLayouts();

      if (result.isEmpty()) {
        if (TextUtils.isEmpty(viewModel.getLastQuery().trim())) {
          noResultsView.setVisibility(View.GONE);
        } else {
          noResultsView.setVisibility(View.VISIBLE);
          noResultsView.setText(getString(R.string.search_no_result_for_x, viewModel.getLastQuery()));
        }
      } else {
        noResultsView.setVisibility(View.VISIBLE);
        noResultsView.setText("");
      }
    });
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

    if (listDecoration != null) {
      listDecoration.onConfigurationChanged(newConfig);
    }
  }


  @Override
  public void onConversationClicked(@NonNull DcChatlist.Item chatlistItem) {
    ConversationListActivity conversationList = (ConversationListActivity) getActivity();
    if (conversationList != null) {
      conversationList.onCreateConversation(chatlistItem.chatId);
    }
  }

  @Override
  public void onContactClicked(@NonNull DcContact contact) {
    ConversationListActivity conversationList = (ConversationListActivity) getActivity();
    if (conversationList != null) {
      ApplicationDcContext dcContext = DcHelper.getContext(getContext());
      int chatId = dcContext.getChatIdByContactId(contact.getId());
      if(chatId==0) {
        new AlertDialog.Builder(getContext())
            .setMessage(getString(R.string.ask_start_chat_with, contact.getNameNAddr()))
            .setCancelable(true)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
              int chatId1 = dcContext.createChatByContactId(contact.getId());
              conversationList.onCreateConversation(chatId1);
            }).show();
      }
      else {
        conversationList.onCreateConversation(chatId);
      }
    }
  }

  @Override
  public void onMessageClicked(@NonNull DcMsg message) {
    ConversationListActivity conversationList = (ConversationListActivity) getActivity();
    if (conversationList != null) {
      ApplicationDcContext dcContext = DcHelper.getContext(getContext());
      int chatId = message.getChatId();
      int msgId = message.getId();
      int startingPosition = DcMsg.getMessagePosition(message, dcContext);
      int msgs[] = dcContext.getChatMsgs(chatId, 0, 0);
      conversationList.openConversation(chatId, startingPosition);
    }
  }

  public void updateSearchQuery(@NonNull String query) {
    if (viewModel != null) {
      viewModel.updateQuery(query);
    } else {
      pendingQuery = query;
    }
  }
}
