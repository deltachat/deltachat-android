package org.thoughtcrime.securesms.search;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.b44t.messenger.DcChatlist;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.BaseConversationListAdapter;
import org.thoughtcrime.securesms.ConversationListItem;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.model.ThreadRecord;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.search.model.SearchResult;
import org.thoughtcrime.securesms.util.StickyHeaderDecoration;

import java.util.Set;

class SearchListAdapter extends BaseConversationListAdapter<SearchListAdapter.SearchResultViewHolder>
                        implements StickyHeaderDecoration.StickyHeaderAdapter<SearchListAdapter.HeaderViewHolder>
{
  private static final int TYPE_CHATS         = 1;
  private static final int TYPE_CONTACTS      = 2;
  private static final int TYPE_MESSAGES      = 3;

  private final GlideRequests glideRequests;
  private final EventListener eventListener;

  @NonNull
  private SearchResult searchResult = SearchResult.EMPTY;

  final Context              context;
  final DcContext            dcContext; // reset on account switching is not needed because SearchFragment and SearchListAdapter are recreated in every search start

  SearchListAdapter(Context                context,
                    @NonNull GlideRequests glideRequests,
                    @NonNull EventListener eventListener)
  {
    this.glideRequests = glideRequests;
    this.eventListener = eventListener;
    this.context       = context;
    this.dcContext     = DcHelper.getContext(context);
  }

  @NonNull
  @Override
  public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new SearchResultViewHolder(LayoutInflater.from(parent.getContext())
                                                    .inflate(R.layout.conversation_list_item_view, parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
    DcChatlist.Item conversationResult = getConversationResult(position);

    if (conversationResult != null) {
      holder.bind(context, conversationResult, glideRequests, eventListener, batchSet, batchMode, searchResult.getQuery());
      return;
    }

    DcContact contactResult = getContactResult(position);

    if (contactResult != null) {
      holder.bind(contactResult, glideRequests, eventListener, searchResult.getQuery());
      return;
    }

    DcMsg messageResult = getMessageResult(position);

    if (messageResult != null) {
      holder.bind(messageResult, glideRequests, eventListener, searchResult.getQuery());
    }
  }

  @Override
  public void onViewRecycled(SearchResultViewHolder holder) {
    holder.recycle();
  }

  @Override
  public int getItemCount() {
    return searchResult.size();
  }

  @Override
  public long getHeaderId(int position) {
    if (getConversationResult(position) != null) {
      return TYPE_CHATS;
    } else if (getContactResult(position) != null) {
      return TYPE_CONTACTS;
    } else {
      return TYPE_MESSAGES;
    }
  }

  @Override
  public HeaderViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
    return new HeaderViewHolder(LayoutInflater.from(parent.getContext())
                                              .inflate(R.layout.contact_selection_list_divider, parent, false));
  }

  @Override
  public void onBindHeaderViewHolder(HeaderViewHolder viewHolder, int position) {
    int headerType = (int)getHeaderId(position);
    int textId = R.plurals.n_messages;
    int count = 1;
    boolean maybeLimitedTo1000 = false;

    switch (headerType) {
      case TYPE_CHATS:
        textId = R.plurals.n_chats;
        count = searchResult.getChats().getCnt();
        break;
      case TYPE_CONTACTS:
        textId = R.plurals.n_contacts;
        count = searchResult.getContacts().length;
        break;
      case TYPE_MESSAGES:
        textId = R.plurals.n_messages;
        count = searchResult.getMessages().length;
        maybeLimitedTo1000 = count==1000; // a count of 1000 results may be limited, see documentation of dc_search_msgs()
        break;
    }

    String title = context.getResources().getQuantityString(textId, count, count);
    if (maybeLimitedTo1000) {
      title = title.replace("000", "000+"); // skipping the first digit allows formattings as "1.000" or "1,000"
    }
    viewHolder.bind(title);
  }

  void updateResults(@NonNull SearchResult result) {
    this.searchResult = result;
    notifyDataSetChanged();
  }

  @Override
  public void selectAllThreads() {
    for (int i = 0; i < searchResult.getChats().getCnt(); i++) {
      batchSet.add((long)searchResult.getChats().getItem(i).chatId);
    }
    notifyDataSetChanged();
  }

  @Nullable
  private DcChatlist.Item getConversationResult(int position) {
    if (position < searchResult.getChats().getCnt()) {
      return searchResult.getChats().getItem(position);
    }
    return null;
  }

  @Nullable
  private DcContact getContactResult(int position) {
    if (position >= getFirstContactIndex() && position < getFirstMessageIndex()) {
      return dcContext.getContact(searchResult.getContacts()[position - getFirstContactIndex()]);
    }
    return null;
  }

  @Nullable
  private DcMsg getMessageResult(int position) {
    if (position >= getFirstMessageIndex() && position < searchResult.size()) {
      return dcContext.getMsg(searchResult.getMessages()[position - getFirstMessageIndex()]);
    }
    return null;
  }

  private int getFirstContactIndex() {
    return searchResult.getChats().getCnt();
  }

  private int getFirstMessageIndex() {
    return getFirstContactIndex() + searchResult.getContacts().length;
  }

  public interface EventListener {
    void onConversationClicked(@NonNull DcChatlist.Item chatlistItem);
    void onConversationLongClicked(@NonNull DcChatlist.Item chatlistItem);
    void onContactClicked(@NonNull DcContact contact);
    void onMessageClicked(@NonNull DcMsg message);
  }

  static class SearchResultViewHolder extends RecyclerView.ViewHolder {

    private final ConversationListItem root;

    SearchResultViewHolder(View itemView) {
      super(itemView);
      root = (ConversationListItem) itemView;
    }

    void bind(Context   context,
              @NonNull  DcChatlist.Item chatlistItem,
              @NonNull  GlideRequests glideRequests,
              @NonNull  EventListener eventListener,
              @NonNull Set<Long> selectedThreads,
              boolean   batchMode,
              @Nullable String        query)
    {
      DcContext dcContext = DcHelper.getContext(context);
      ThreadRecord threadRecord = DcHelper.getThreadRecord(context, chatlistItem.summary, dcContext.getChat(chatlistItem.chatId));
      root.bind(threadRecord, chatlistItem.msgId, chatlistItem.summary, glideRequests, selectedThreads, batchMode, query);
      root.setOnClickListener(view -> eventListener.onConversationClicked(chatlistItem));
      root.setOnLongClickListener(view -> {
        eventListener.onConversationLongClicked(chatlistItem);
        return true;
      });
    }

    void bind(@NonNull  DcContact     contactResult,
              @NonNull  GlideRequests glideRequests,
              @NonNull  EventListener eventListener,
              @Nullable String        query)
    {
      root.bind(contactResult, glideRequests, query);
      root.setOnClickListener(view -> eventListener.onContactClicked(contactResult));
    }

    void bind(@NonNull  DcMsg         messageResult,
              @NonNull  GlideRequests glideRequests,
              @NonNull  EventListener eventListener,
              @Nullable String        query)
    {
      root.bind(messageResult, glideRequests, query);
      root.setOnClickListener(view -> eventListener.onMessageClicked(messageResult));
    }

    void recycle() {
      root.unbind();
      root.setOnClickListener(null);
    }
  }

  public static class HeaderViewHolder extends RecyclerView.ViewHolder {

    private final TextView titleView;

    public HeaderViewHolder(View itemView) {
      super(itemView);
      titleView = itemView.findViewById(R.id.label);
    }

    public void bind(String text) {
      titleView.setText(text);
    }
  }
}
