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

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.annimon.stream.Stream;
import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.ConversationAdapter.HeaderViewHolder;
import org.thoughtcrime.securesms.attachments.DatabaseAttachment;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.MmsSmsColumns;
import org.thoughtcrime.securesms.database.MmsSmsDatabase;
import org.thoughtcrime.securesms.database.model.MessageRecord;
import org.thoughtcrime.securesms.database.model.MmsMessageRecord;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.mms.SlideDeck;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.Conversions;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.LRUCache;
import org.thoughtcrime.securesms.util.StickyHeaderDecoration;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.whispersystems.libsignal.util.guava.Optional;

import java.lang.ref.SoftReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
  private static final String TAG = ConversationAdapter.class.getSimpleName();
  private final Map<Integer,SoftReference<DcMsg>> messageRecordCache =
      Collections.synchronizedMap(new LRUCache<Integer,SoftReference<DcMsg>>(MAX_CACHE_SIZE)); // TODO: use the cache

  private static final int MESSAGE_TYPE_OUTGOING           = 0;
  private static final int MESSAGE_TYPE_INCOMING           = 1;
  private static final int MESSAGE_TYPE_UPDATE             = 2;
  private static final int MESSAGE_TYPE_AUDIO_OUTGOING     = 3;
  private static final int MESSAGE_TYPE_AUDIO_INCOMING     = 4;
  private static final int MESSAGE_TYPE_THUMBNAIL_OUTGOING = 5;
  private static final int MESSAGE_TYPE_THUMBNAIL_INCOMING = 6;
  private static final int MESSAGE_TYPE_DOCUMENT_OUTGOING  = 7;
  private static final int MESSAGE_TYPE_DOCUMENT_INCOMING  = 8;

  private final Set<DcMsg> batchSelected = Collections.synchronizedSet(new HashSet<DcMsg>());

  private final @Nullable ItemClickListener clickListener;
  private final @NonNull  GlideRequests     glideRequests;
  private final @NonNull  Locale            locale;
  private final @NonNull  Recipient         recipient;
  private final @NonNull  LayoutInflater    inflater;
  private final @NonNull  Context           context;
  private final @NonNull  Calendar          calendar;
  private final @NonNull  MessageDigest     digest;

  private ApplicationDcContext dcContext;
  private @NonNull int[]       dcMsgList = new int[0];
  private int                  recordToPulseHighlight;

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

  @Override
  public int getItemCount() {
    return dcMsgList.length;
  }

  private DcMsg getMsg(int position) {
    return dcContext.getMsg(dcMsgList[position]);
  }

  static class HeaderViewHolder extends RecyclerView.ViewHolder {
    TextView textView;

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
    void onItemLongClick(DcMsg item);
  }

  @SuppressWarnings("ConstantConditions")
  @VisibleForTesting
  ConversationAdapter(Context context) {
    try {
      this.glideRequests = null;
      this.locale        = null;
      this.clickListener = null;
      this.recipient     = null;
      this.inflater      = null;
      this.calendar      = null;
      this.context       = context;
      this.digest        = MessageDigest.getInstance("SHA1");
      this.dcContext     = DcHelper.getContext(context);
    } catch (NoSuchAlgorithmException nsae) {
      throw new AssertionError("SHA1 isn't supported!");
    }
  }

  public ConversationAdapter(@NonNull Context context,
                             @NonNull GlideRequests glideRequests,
                             @NonNull Locale locale,
                             @Nullable ItemClickListener clickListener,
                             @NonNull Recipient recipient) {
    try {
      this.glideRequests = glideRequests;
      this.locale = locale;
      this.clickListener = clickListener;
      this.recipient = recipient;
      this.context = context;
      this.inflater = LayoutInflater.from(context);
      this.calendar = Calendar.getInstance();
      this.digest = MessageDigest.getInstance("SHA1");
      this.dcContext     = DcHelper.getContext(context);

      setHasStableIds(true);
    } catch (NoSuchAlgorithmException nsae) {
      throw new AssertionError("SHA1 isn't supported!");
    }
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
    ConversationAdapter.ViewHolder holder = (ConversationAdapter.ViewHolder)viewHolder;
    Optional<DcMsg> previous = position <= 0? Optional.absent() : Optional.of(getMsg(position-1));
    Optional<DcMsg> next = position >= dcMsgList.length-1? Optional.absent() : Optional.of(getMsg(position+1));
    boolean pulseHighlight = dcMsgList[position] == recordToPulseHighlight;
    holder.getItem().bind(getMsg(position), previous, next, glideRequests, locale, batchSelected, recipient, pulseHighlight);
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    long start = System.currentTimeMillis();
    final V itemView = ViewUtil.inflate(inflater, parent, getLayoutForViewType(viewType));
    itemView.setOnClickListener(view -> {
      if (clickListener != null) {
        clickListener.onItemClick(itemView.getMessageRecord());
      }
    });
    itemView.setOnLongClickListener(view -> {
      if (clickListener != null) {
        clickListener.onItemLongClick(itemView.getMessageRecord());
      }
      return true;
    });
    itemView.setEventListener(clickListener);
    Log.w(TAG, "Inflate time: " + (System.currentTimeMillis() - start));
    return new ViewHolder(itemView);
  }

  private @LayoutRes int getLayoutForViewType(int viewType) {
    switch (viewType) {
      case MESSAGE_TYPE_AUDIO_OUTGOING:
      case MESSAGE_TYPE_THUMBNAIL_OUTGOING:
      case MESSAGE_TYPE_DOCUMENT_OUTGOING:
      case MESSAGE_TYPE_OUTGOING:        return R.layout.conversation_item_sent;
      case MESSAGE_TYPE_AUDIO_INCOMING:
      case MESSAGE_TYPE_THUMBNAIL_INCOMING:
      case MESSAGE_TYPE_DOCUMENT_INCOMING:
      case MESSAGE_TYPE_INCOMING:        return R.layout.conversation_item_received;
      case MESSAGE_TYPE_UPDATE:          return R.layout.conversation_item_update;
      default: throw new IllegalArgumentException("unsupported item view type given to ConversationAdapter");
    }
  }

  @Override
  public int getItemViewType(int i) {
    DcMsg messageRecord = getMsg(i);
    int type = messageRecord.getType();
    if (messageRecord.isUpdate()) {
      return MESSAGE_TYPE_UPDATE;
    } else if (type==DcMsg.DC_MSG_AUDIO || type==DcMsg.DC_MSG_VOICE) {
      if (messageRecord.isOutgoing()) return MESSAGE_TYPE_AUDIO_OUTGOING;
      else                            return MESSAGE_TYPE_AUDIO_INCOMING;
    } else if (type==DcMsg.DC_MSG_FILE) {
      if (messageRecord.isOutgoing()) return MESSAGE_TYPE_DOCUMENT_OUTGOING;
      else                            return MESSAGE_TYPE_DOCUMENT_INCOMING;
    } else if (type==DcMsg.DC_MSG_IMAGE || type==DcMsg.DC_MSG_GIF || type==DcMsg.DC_MSG_VIDEO) {
      if (messageRecord.isOutgoing()) return MESSAGE_TYPE_THUMBNAIL_OUTGOING;
      else                            return MESSAGE_TYPE_THUMBNAIL_INCOMING;
    } else if (messageRecord.isOutgoing()) {
      return MESSAGE_TYPE_OUTGOING;
    } else {
      return MESSAGE_TYPE_INCOMING;
    }
  }

  public int findLastSeenPosition(long lastSeen) {
    /* TODO -- we shoud do this without loading all messages in the chat
    if (lastSeen <= 0)     return -1;
    if (isActive())        return -1;

    int count = getItemCount();

    for (int i = 0;i<count;i++) {
      DcMsg msg = getMsg(i);
      if (msg.isOutgoing() || msg.getTimestamp() <= lastSeen) {
        return i;
      }
    }
    */

    return -1;
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

  public void pulseHighlightItem(int position) {
    if (position < getItemCount()) {
      recordToPulseHighlight = position;
      notifyItemChanged(position);
    }
  }

  protected boolean isFooterPosition(int position) {
//    return hasFooterView() && position == getItemCount() - 1;
    return false;
  }

  protected boolean isHeaderPosition(int position) {
//    return hasHeaderView() && position == 0;
    return false;
  }

  @Override
  public long getHeaderId(int position) {
    return -1; // no header Id.
//    if (isActive())      return -1;
//    if (isHeaderPosition(position)) return -1;
//    if (isFooterPosition(position)) return -1;
//    if (position >= getItemCount()) return -1;
//    if (position < 0)               return -1;
//
//    MessageRecord record = getRecordForPositionOrThrow(position);
//
//    calendar.setTime(new Date(record.getDateSent()));
//    return Util.hashCode(calendar.get(Calendar.YEAR), calendar.get(Calendar.DAY_OF_YEAR));
  }

  public boolean isActive() {
    return dcMsgList.length > 0;
  }

  public long getReceivedTimestamp(int position) {
    if (isActive())                 return 0;
    if (isHeaderPosition(position)) return 0;
    if (isFooterPosition(position)) return 0;
    if (position >= getItemCount()) return 0;
    if (position < 0)               return 0;

    DcMsg msg = getMsg(position);
    return msg.getTimestamp();
  }

  @NonNull
  public Context getContext() {
    return context;
  }

  @Override
  public HeaderViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
    return new HeaderViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.conversation_item_header, parent, false));
  }

  public HeaderViewHolder onCreateLastSeenViewHolder(ViewGroup parent) {
    return new HeaderViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.conversation_item_last_seen, parent, false));
  }

  @Override
  public void onBindHeaderViewHolder(HeaderViewHolder viewHolder, int position) {
    DcMsg msg = getMsg(position);
    viewHolder.setText(DateUtils.getRelativeDate(getContext(), locale, msg.getTimestamp()));
  }

  public void onBindLastSeenViewHolder(HeaderViewHolder viewHolder, int position) {
    viewHolder.setText(getContext().getResources().getQuantityString(R.plurals.ConversationAdapter_n_unread_messages, (position + 1), (position + 1)));
  }

  static class LastSeenHeader extends StickyHeaderDecoration {

    private final ConversationAdapter adapter;
    private final long                lastSeenTimestamp;

    LastSeenHeader(ConversationAdapter adapter, long lastSeenTimestamp) {
      super(adapter, false, false);
      this.adapter           = adapter;
      this.lastSeenTimestamp = lastSeenTimestamp;
    }

    @Override
    protected boolean hasHeader(RecyclerView parent, StickyHeaderAdapter stickyAdapter, int position) {
      if (!adapter.isActive()) {
        return false;
      }

      if (lastSeenTimestamp <= 0) {
        return false;
      }

      long currentRecordTimestamp  = adapter.getReceivedTimestamp(position);
      long previousRecordTimestamp = adapter.getReceivedTimestamp(position + 1);

      return currentRecordTimestamp > lastSeenTimestamp && previousRecordTimestamp < lastSeenTimestamp;
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

  public void changeData(@Nullable int[] dcMsgList) {
    this.dcMsgList = dcMsgList==null? new int[0] : dcMsgList;
    messageRecordCache.clear();
    notifyDataSetChanged();
  }

  public void setHeaderView(@Nullable View headerView) {
    // TODO: must not be implemented, remove calls to this function
  }

  public void addFastRecord(@NonNull MessageRecord record) {
    // TODO: i think this is not need, we simply should reload the view
  }

  public void releaseFastRecord(long id) {
      // TODO: i think this is not need, we simply should reload the view
  }
}

