package org.thoughtcrime.securesms.search;


import static org.thoughtcrime.securesms.util.ShareUtil.isRelayingMessageContent;

import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcChatlist;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.BaseConversationListAdapter;
import org.thoughtcrime.securesms.BaseConversationListFragment;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.search.model.SearchResult;
import org.thoughtcrime.securesms.util.StickyHeaderDecoration;

import java.util.Set;

/**
 * A fragment that is displayed to do full-text search of messages, groups, and contacts.
 */
public class SearchFragment extends BaseConversationListFragment
        implements SearchListAdapter.EventListener, DcEventCenter.DcEventDelegate {

  public static final String TAG          = "SearchFragment";

  private TextView               noResultsView;
  private StickyHeaderDecoration listDecoration;

  private SearchViewModel   viewModel;
  private SearchListAdapter listAdapter;
  private String            pendingQuery;

  public static SearchFragment newInstance() {
    Bundle args = new Bundle();

    SearchFragment fragment = new SearchFragment();
    fragment.setArguments(args);

    return fragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    viewModel = ViewModelProviders.of(this, (ViewModelProvider.Factory) new SearchViewModel.Factory(requireContext())).get(SearchViewModel.class);
    DcEventCenter eventCenter = DcHelper.getEventCenter(requireContext());
    eventCenter.addObserver(DcContext.DC_EVENT_CHAT_MODIFIED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_CONTACTS_CHANGED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_INCOMING_MSG, this);
    eventCenter.addObserver(DcContext.DC_EVENT_MSGS_CHANGED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_MSGS_NOTICED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_MSG_DELIVERED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_MSG_FAILED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_MSG_READ, this);

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
    RecyclerView listView = view.findViewById(R.id.search_list);
    fab           = view.findViewById(R.id.fab);

    listAdapter    = new SearchListAdapter(getContext(), GlideApp.with(this), this);
    listDecoration = new StickyHeaderDecoration(listAdapter, false, true);

    fab.setVisibility(View.GONE);
    listView.setAdapter(listAdapter);
    listView.addItemDecoration(listDecoration);
    listView.setLayoutManager(new LinearLayoutManager(getContext()));
  }

  @Override
  public void onStart() {
    super.onStart();
    viewModel.setForwardingMode(isRelayingMessageContent(getActivity()));
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
  public void onConfigurationChanged(@NonNull Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

    if (listDecoration != null) {
      listDecoration.onConfigurationChanged(newConfig);
    }
  }

  @Override
  public void onDestroy() {
    DcHelper.getEventCenter(requireContext()).removeObservers(this);
    super.onDestroy();
  }

  @Override
  public void onConversationClicked(@NonNull DcChatlist.Item chatlistItem) {
    onItemClick(chatlistItem.chatId);
  }

  @Override
  public void onConversationLongClicked(@NonNull DcChatlist.Item chatlistItem) {
    onItemLongClick(chatlistItem.chatId);
  }

  @Override
  public void onContactClicked(@NonNull DcContact contact) {
    if (actionMode != null) {
      return;
    }

    ConversationListActivity conversationList = (ConversationListActivity) getActivity();
    if (conversationList != null) {
      DcContext dcContext = DcHelper.getContext(requireContext());
      int chatId = dcContext.getChatIdByContactId(contact.getId());
      if(chatId==0) {
        new AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.ask_start_chat_with, contact.getDisplayName()))
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
    if (actionMode != null) {
      return;
    }

    ConversationListActivity conversationList = (ConversationListActivity) getActivity();
    if (conversationList != null) {
      DcContext dcContext = DcHelper.getContext(requireContext());
      int chatId = message.getChatId();
      int startingPosition = DcMsg.getMessagePosition(message, dcContext);
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

  @Override
  public void handleEvent(@NonNull DcEvent event) {
    if (viewModel != null) {
      viewModel.updateQuery();
    }
  }

  @Override
  protected boolean offerToArchive() {
    DcContext dcContext = DcHelper.getContext(requireActivity());
    final Set<Long> selectedChats = listAdapter.getBatchSelections();
    for (long chatId : selectedChats) {
      DcChat dcChat = dcContext.getChat((int)chatId);
      if (dcChat.getVisibility() != DcChat.DC_CHAT_VISIBILITY_ARCHIVED) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected void setFabVisibility(boolean isActionMode) {
    if (isActionMode && isRelayingMessageContent(getActivity())) {
      fab.setVisibility(View.VISIBLE);
    } else {
      fab.setVisibility(View.GONE);
    }
  }

  @Override
  protected BaseConversationListAdapter getListAdapter() {
    return listAdapter;
  }
}
