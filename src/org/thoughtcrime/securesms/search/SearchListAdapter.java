package org.thoughtcrime.securesms.search;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.b44t.messenger.DcChatlist;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.ConversationListItem;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.model.ThreadRecord;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.search.model.SearchResult;
import org.thoughtcrime.securesms.util.StickyHeaderDecoration;

import java.util.Collections;
import java.util.Locale;

class SearchListAdapter extends    RecyclerView.Adapter<SearchListAdapter.SearchResultViewHolder>
                        implements StickyHeaderDecoration.StickyHeaderAdapter<SearchListAdapter.HeaderViewHolder>
{
  private static final int TYPE_CHATS         = 1;
  private static final int TYPE_CONTACTS      = 2;
  private static final int TYPE_MESSAGES      = 3;

  private final GlideRequests glideRequests;
  private final EventListener eventListener;
  private final Locale        locale;

  @NonNull
  private SearchResult searchResult = SearchResult.EMPTY;

  Context              context;
  ApplicationDcContext dcContext;

  SearchListAdapter(Context                context,
                    @NonNull GlideRequests glideRequests,
                    @NonNull EventListener eventListener,
                    @NonNull Locale        locale)
  {
    this.glideRequests = glideRequests;
    this.eventListener = eventListener;
    this.locale        = locale;
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
      holder.bind(context, conversationResult, glideRequests, eventListener, locale, searchResult.getQuery());
      return;
    }

    DcContact contactResult = getContactResult(position);

    if (contactResult != null) {
      holder.bind(contactResult, glideRequests, eventListener, locale, searchResult.getQuery());
      return;
    }

    DcMsg messageResult = getMessageResult(position);

    if (messageResult != null) {
      holder.bind(messageResult, glideRequests, eventListener, locale, searchResult.getQuery());
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
        break;
    }
    viewHolder.bind(context.getResources().getQuantityString(textId, count, count));
  }

  void updateResults(@NonNull SearchResult result) {
    this.searchResult = result;
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
              @NonNull  Locale        locale,
              @Nullable String        query)
    {
      ApplicationDcContext dcContext = DcHelper.getContext(context);
      ThreadRecord threadRecord = dcContext.getThreadRecord(chatlistItem.summary, dcContext.getChat(chatlistItem.chatId));
      root.bind(threadRecord, chatlistItem.msgId, chatlistItem.summary, glideRequests, locale, Collections.emptySet(), false, query);
      root.setOnClickListener(view -> eventListener.onConversationClicked(chatlistItem));
    }

    void bind(@NonNull  DcContact     contactResult,
              @NonNull  GlideRequests glideRequests,
              @NonNull  EventListener eventListener,
              @NonNull  Locale        locale,
              @Nullable String        query)
    {
      root.bind(contactResult, glideRequests, locale, query);
      root.setOnClickListener(view -> eventListener.onContactClicked(contactResult));
    }

    void bind(@NonNull  DcMsg         messageResult,
              @NonNull  GlideRequests glideRequests,
              @NonNull  EventListener eventListener,
              @NonNull  Locale        locale,
              @Nullable String        query)
    {
      root.bind(messageResult, glideRequests, locale, query);
      root.setOnClickListener(view -> eventListener.onMessageClicked(messageResult));
    }

    void recycle() {
      root.unbind();
      root.setOnClickListener(null);
    }
  }

  public static class HeaderViewHolder extends RecyclerView.ViewHolder {

    private TextView titleView;

    public HeaderViewHolder(View itemView) {
      super(itemView);
      titleView = itemView.findViewById(R.id.label);
    }

    public void bind(String text) {
      titleView.setText(text);
    }
  }
}
