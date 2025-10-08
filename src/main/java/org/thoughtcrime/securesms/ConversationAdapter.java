/*
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms;

import static org.thoughtcrime.securesms.ConversationItem.PULSE_HIGHLIGHT_MILLIS;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.ConversationAdapter.HeaderViewHolder;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.LRUCache;
import org.thoughtcrime.securesms.util.StickyHeaderDecoration;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.lang.ref.SoftReference;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A DC adapter for a conversation thread.  Ultimately
 * used by ConversationActivity to display a conversation
 * thread in a ListActivity.
 *
 * @author Moxie Marlinspike
 *
 */
public class ConversationAdapter <V extends View & BindableConversationItem>
    extends RecyclerView.Adapter
  implements StickyHeaderDecoration.StickyHeaderAdapter<HeaderViewHolder>
{

  private static final int MAX_CACHE_SIZE = 40;
  private final Map<Integer,SoftReference<DcMsg>> recordCache =
      Collections.synchronizedMap(new LRUCache<Integer,SoftReference<DcMsg>>(MAX_CACHE_SIZE));

  private static final int MESSAGE_TYPE_OUTGOING           = 0;
  private static final int MESSAGE_TYPE_INCOMING           = 1;
  private static final int MESSAGE_TYPE_INFO               = 2;
  private static final int MESSAGE_TYPE_AUDIO_OUTGOING     = 3;
  private static final int MESSAGE_TYPE_AUDIO_INCOMING     = 4;
  private static final int MESSAGE_TYPE_THUMBNAIL_OUTGOING = 5;
  private static final int MESSAGE_TYPE_THUMBNAIL_INCOMING = 6;
  private static final int MESSAGE_TYPE_DOCUMENT_OUTGOING  = 7;
  private static final int MESSAGE_TYPE_DOCUMENT_INCOMING  = 8;
  private static final int MESSAGE_TYPE_STICKER_INCOMING   = 9;
  private static final int MESSAGE_TYPE_STICKER_OUTGOING   = 10;

  private final Set<DcMsg> batchSelected = Collections.synchronizedSet(new HashSet<DcMsg>());

  private final @Nullable ItemClickListener clickListener;
  private final @NonNull  GlideRequests     glideRequests;
  private final @NonNull  Recipient         recipient;
  private final @NonNull  LayoutInflater    inflater;
  private final @NonNull  Context           context;
  private final @NonNull  Calendar          calendar;

  private final DcContext      dcContext;
  private @NonNull DcChat      dcChat;
  private @NonNull int[]       dcMsgList = new int[0];
  private int                  positionToPulseHighlight = -1;
  private int                  positionCurrentlyPulseHighlighting = -1;
  private long                 pulseHighlightingSince = -1;
  private int                  lastSeenPosition = -1;
  private long                 lastSeen = -1;

  protected static class ViewHolder extends RecyclerView.ViewHolder {
    public <V extends View & BindableConversationItem> ViewHolder(final @NonNull V itemView) {
      super(itemView);
    }

    @SuppressWarnings("unchecked")
    public <V extends View & BindableConversationItem> V getView() {
      return (V)itemView;
    }

    public BindableConversationItem getItem() {
      return getView();
    }
  }


  public boolean isActive() {
    return dcMsgList.length > 0;
  }

  public @NonNull DcChat getChat(){
    return dcChat;
  }


  public void setLastSeen(long timestamp) {
    lastSeen = timestamp;
  }

  public void updateLastSeenPosition() {
    this.lastSeenPosition = findLastSeenPosition(lastSeen);
  }

  void setLastSeenPosition(int pos) {
    lastSeenPosition = pos;
  }

  public int getLastSeenPosition() {
    return lastSeenPosition;
  }

  @Override
  public int getItemCount() {
    return dcMsgList.length;
  }

  @Override
  public long getItemId(int position) {
    if (position<0 || position>=dcMsgList.length) {
      return 0;
    }
    return dcMsgList[dcMsgList.length-1-position];
  }

  public @NonNull DcMsg getMsg(int position) {
    if(position<0 || position>=dcMsgList.length) {
      return new DcMsg(0);
    }

    final SoftReference<DcMsg> reference = recordCache.get(position);
    if (reference != null) {
      final DcMsg fromCache = reference.get();
      if (fromCache != null) {
        return fromCache;
      }
    }

    final DcMsg fromDb = dcContext.getMsg((int)getItemId(position));
    recordCache.put(position, new SoftReference<>(fromDb));
    return fromDb;
  }

  /**
   * Returns the position of the message with msgId in the chat list, counted from the top
   */
  public int msgIdToPosition(int msgId) {
    for(int i=0; i<dcMsgList.length; i++ ) {
      if(dcMsgList[i]==msgId) {
        return dcMsgList.length - 1 - i;
      }
    }
    return -1;
  }

  static class HeaderViewHolder extends RecyclerView.ViewHolder {
    final TextView textView;

    HeaderViewHolder(View itemView) {
      super(itemView);
      textView = ViewUtil.findById(itemView, R.id.text);
    }

    HeaderViewHolder(TextView textView) {
      super(textView);
      this.textView = textView;
    }

    public void setText(CharSequence text) {
      textView.setText(text);
    }
  }


  interface ItemClickListener extends BindableConversationItem.EventListener {
    void onItemClick(DcMsg item);
    void onItemLongClick(DcMsg item, View view);
  }

  public ConversationAdapter(@NonNull Context context,
                             @NonNull DcChat dcChat,
                             @NonNull GlideRequests glideRequests,
                             @Nullable ItemClickListener clickListener,
                             @NonNull Recipient recipient) {
    this.dcChat = dcChat;
    this.glideRequests = glideRequests;
    this.clickListener = clickListener;
    this.recipient = recipient;
    this.context = context;
    this.inflater = LayoutInflater.from(context);
    this.calendar = Calendar.getInstance();
    this.dcContext     = DcHelper.getContext(context);

    setHasStableIds(true);
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
    ConversationAdapter.ViewHolder holder = (ConversationAdapter.ViewHolder)viewHolder;

    long now = System.currentTimeMillis();
    if (position == positionToPulseHighlight) {
      positionToPulseHighlight = -1;
      positionCurrentlyPulseHighlighting = position;
      pulseHighlightingSince = now;
    }

    long elapsed = now - pulseHighlightingSince;
    boolean pulseHighlight = (positionCurrentlyPulseHighlighting == position && elapsed < PULSE_HIGHLIGHT_MILLIS);

    holder.getItem().bind(getMsg(position), dcChat, glideRequests, batchSelected, recipient, pulseHighlight);
  }

  @Override
  public void onViewRecycled(@NonNull RecyclerView.ViewHolder viewHolder) {
    if (viewHolder.itemView instanceof  ConversationItem) {
      ConversationSwipeAnimationHelper.update((ConversationItem) viewHolder.itemView, 0, 1);
    }
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final V itemView = ViewUtil.inflate(inflater, parent, getLayoutForViewType(viewType));
    itemView.setOnClickListener(view -> {
      if (clickListener != null) {
        clickListener.onItemClick(itemView.getMessageRecord());
      }
    });
    itemView.setOnLongClickListener(view -> {
      if (clickListener != null) {
        clickListener.onItemLongClick(itemView.getMessageRecord(), view);
      }
      return true;
    });
    itemView.setEventListener(clickListener);
    return new ViewHolder(itemView);
  }

  private @LayoutRes int getLayoutForViewType(int viewType) {
    switch (viewType) {
      case MESSAGE_TYPE_AUDIO_OUTGOING:
      case MESSAGE_TYPE_THUMBNAIL_OUTGOING:
      case MESSAGE_TYPE_DOCUMENT_OUTGOING:
      case MESSAGE_TYPE_STICKER_OUTGOING:
      case MESSAGE_TYPE_OUTGOING:        return R.layout.conversation_item_sent;
      case MESSAGE_TYPE_AUDIO_INCOMING:
      case MESSAGE_TYPE_THUMBNAIL_INCOMING:
      case MESSAGE_TYPE_DOCUMENT_INCOMING:
      case MESSAGE_TYPE_STICKER_INCOMING:
      case MESSAGE_TYPE_INCOMING:        return R.layout.conversation_item_received;
      case MESSAGE_TYPE_INFO:            return R.layout.conversation_item_update;
      default: throw new IllegalArgumentException("unsupported item view type given to ConversationAdapter");
    }
  }

  @Override
  public int getItemViewType(int i) {
    DcMsg dcMsg = getMsg(i);
    int type = dcMsg.getType();
    if (dcMsg.isInfo()) {
      return MESSAGE_TYPE_INFO;
    }
    else if (type==DcMsg.DC_MSG_AUDIO || type==DcMsg.DC_MSG_VOICE) {
      return dcMsg.isOutgoing()? MESSAGE_TYPE_AUDIO_OUTGOING : MESSAGE_TYPE_AUDIO_INCOMING;
    }
    else if (type==DcMsg.DC_MSG_FILE) {
      return dcMsg.isOutgoing()? MESSAGE_TYPE_DOCUMENT_OUTGOING : MESSAGE_TYPE_DOCUMENT_INCOMING;
    }
    else if (type==DcMsg.DC_MSG_IMAGE || type==DcMsg.DC_MSG_GIF || type==DcMsg.DC_MSG_VIDEO) {
      return dcMsg.isOutgoing()? MESSAGE_TYPE_THUMBNAIL_OUTGOING : MESSAGE_TYPE_THUMBNAIL_INCOMING;
    }
    else if (type == DcMsg.DC_MSG_STICKER) {
      return dcMsg.isOutgoing()? MESSAGE_TYPE_STICKER_OUTGOING : MESSAGE_TYPE_STICKER_INCOMING;
    }
    else {
      return dcMsg.isOutgoing()? MESSAGE_TYPE_OUTGOING : MESSAGE_TYPE_INCOMING;
    }
  }

  public void toggleSelection(DcMsg messageRecord) {
    if (!batchSelected.remove(messageRecord)) {
      batchSelected.add(messageRecord);
    }
  }

  public void clearSelection() {
    batchSelected.clear();
  }

  public Set<DcMsg> getSelectedItems() {
    return Collections.unmodifiableSet(new HashSet<>(batchSelected));
  }

  public int[] getMessageIds() {
    return dcMsgList;
  }

  public void pulseHighlightItem(int position) {
    if (position>=0 && position < getItemCount()) {
      positionToPulseHighlight = position;
      notifyItemChanged(position);
    }
  }

  public long getSortTimestamp(int position) {
    if (!isActive())                return 0;
    if (position >= getItemCount()) return 0;
    if (position < 0)               return 0;

    DcMsg msg = getMsg(position);
    return msg.getSortTimestamp();
  }

  @NonNull
  public Context getContext() {
    return context;
  }

  @Override
  public long getHeaderId(int position) {
    if (position >= getItemCount()) return -1;
    if (position < 0)               return -1;

    calendar.setTime(new Date(getSortTimestamp(position)));
    return Util.hashCode(calendar.get(Calendar.YEAR), calendar.get(Calendar.DAY_OF_YEAR));
  }

  @Override
  public HeaderViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
    return new HeaderViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.conversation_item_header, parent, false));
  }

  /**
   * date header view
   */
  @Override
  public void onBindHeaderViewHolder(HeaderViewHolder viewHolder, int position) {
    viewHolder.setText(DateUtils.getRelativeDate(getContext(), getSortTimestamp(position)));
  }


  public void changeData(@Nullable int[] dcMsgList) {
    // should be called when there are new messages
    this.dcMsgList = dcMsgList == null ? new int[0] : dcMsgList;
    reloadData();
  }

  public void reloadChat() {
    // should be called when the chat was modified
    dcChat = dcContext.getChat(dcChat.getId());
  }

  private void reloadData() {
    // should be called when some items in a message are changed, eg. seen-state
    recordCache.clear();
    updateLastSeenPosition();
    notifyDataSetChanged();
  }

  private int findLastSeenPosition(long lastSeen) {
    if (lastSeen <= 0)     return -1;
    if (!isActive())       return -1;

    int count = getItemCount();


    for (int i = 0; i < count; i++) {
      DcMsg msg = getMsg(i);
      if (msg.isOutgoing() || msg.getTimestamp() <= lastSeen) {
        return i - 1;
      }
    }

    return -1;
  }

  public HeaderViewHolder onCreateLastSeenViewHolder(ViewGroup parent) {
    return new HeaderViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.conversation_item_last_seen, parent, false));
  }

  public void onBindLastSeenViewHolder(HeaderViewHolder viewHolder, int position) {
    viewHolder.setText(getContext().getResources().getQuantityString(R.plurals.chat_n_new_messages, (position + 1), (position + 1)));
  }

  static class LastSeenHeader extends StickyHeaderDecoration {
    private final ConversationAdapter adapter;

    LastSeenHeader(ConversationAdapter adapter) {
      super(adapter, false, false);
      this.adapter           = adapter;
    }

    @Override
    protected boolean hasHeader(RecyclerView parent, StickyHeaderAdapter stickyAdapter, int position) {
      return  adapter.isActive() && position == adapter.getLastSeenPosition();
    }

    @Override
    protected int getHeaderTop(RecyclerView parent, View child, View header, int adapterPos, int layoutPos) {
      return parent.getLayoutManager().getDecoratedTop(child);
    }

    @Override
    protected HeaderViewHolder getHeader(RecyclerView parent, StickyHeaderAdapter stickyAdapter, int position) {
      HeaderViewHolder viewHolder = adapter.onCreateLastSeenViewHolder(parent);
      adapter.onBindLastSeenViewHolder(viewHolder, position);

      int widthSpec  = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY);
      int heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.UNSPECIFIED);

      int childWidth  = ViewGroup.getChildMeasureSpec(widthSpec, parent.getPaddingLeft() + parent.getPaddingRight(), viewHolder.itemView.getLayoutParams().width);
      int childHeight = ViewGroup.getChildMeasureSpec(heightSpec, parent.getPaddingTop() + parent.getPaddingBottom(), viewHolder.itemView.getLayoutParams().height);

      viewHolder.itemView.measure(childWidth, childHeight);
      viewHolder.itemView.layout(0, 0, viewHolder.itemView.getMeasuredWidth(), viewHolder.itemView.getMeasuredHeight());

      return viewHolder;
    }
  }
}
