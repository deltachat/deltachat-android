package org.thoughtcrime.securesms;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcChatlist;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcLot;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.ContactSelectionListItem;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.util.LRUCache;
import org.thoughtcrime.securesms.util.StickyHeaderDecoration.StickyHeaderAdapter;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public class ProfileSettingsAdapter extends RecyclerView.Adapter
                                    implements StickyHeaderAdapter<ProfileSettingsAdapter.HeaderViewHolder>
{
  private static final int MAX_CACHE_SIZE = 100;
  private final Map<Integer,SoftReference<DcContact>> recordCache =
          Collections.synchronizedMap(new LRUCache<>(MAX_CACHE_SIZE));

  private final @NonNull Context              context;
  private final @NonNull Locale               locale;
  private final @NonNull ApplicationDcContext dcContext;

  private @NonNull ArrayList<ItemData>        itemData = new ArrayList<>();
  private int                                 itemDataMemberCount;
  private DcChatlist                          itemDataSharedChats;

  private final LayoutInflater                layoutInflater;
  private final ItemClickListener             clickListener;
  private final GlideRequests                 glideRequests;

  static class ItemData {
    static final int TYPE_MEMBER = 0;
    static final int TYPE_SHARED_CHAT = 1;
    int type;
    int contactId;
    int chatlistIndex;
    int settingsId;

    ItemData(int type, int contactId, int chatlistIndex, int settingsId) {
      this.type          = type;
      this.contactId     = contactId;
      this.chatlistIndex = chatlistIndex;
      this.settingsId    = settingsId;
    }
  };

  public ProfileSettingsAdapter(@NonNull  Context context,
                                @NonNull  GlideRequests glideRequests,
                                @NonNull  Locale locale,
                                @Nullable ItemClickListener clickListener)
  {
    super();
    this.context        = context;
    this.glideRequests  = glideRequests;
    this.locale         = locale;
    this.clickListener  = clickListener;
    this.dcContext      = DcHelper.getContext(context);
    this.layoutInflater = LayoutInflater.from(context);
  }

  @Override
  public int getItemCount() {
    return itemData.size();
  }

  private @NonNull DcContact getContact(int position) {
    if(position<0 || position>=itemData.size() || itemData.get(position).type!=ItemData.TYPE_MEMBER) {
      return new DcContact(0);
    }

    final SoftReference<DcContact> reference = recordCache.get(position);
    if (reference != null) {
      final DcContact fromCache = reference.get();
      if (fromCache != null) {
        return fromCache;
      }
    }

    final DcContact fromDb = dcContext.getContact(itemData.get(position).contactId);
    recordCache.put(position, new SoftReference<>(fromDb));
    return fromDb;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    public ViewHolder(View itemView) {
      super(itemView);
    }
  }

  static class HeaderViewHolder extends RecyclerView.ViewHolder {
    TextView textView;
    HeaderViewHolder(View itemView) {
      super(itemView);
      textView = itemView.findViewById(R.id.text);
    }
  }

  @Override
  public ProfileSettingsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == ItemData.TYPE_MEMBER) {
      final ContactSelectionListItem item = (ContactSelectionListItem)layoutInflater.inflate(R.layout.contact_selection_list_item, parent, false);
      item.setNoHeaderPadding();
      return new ViewHolder(item);
    }
    else {
      final ConversationListItem item = (ConversationListItem)layoutInflater.inflate(R.layout.conversation_list_item_view, parent, false);
      item.hideItemDivider();
      return new ViewHolder(item);
    }
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
    ViewHolder holder = (ViewHolder) viewHolder;
    if (holder.itemView instanceof ContactSelectionListItem) {
      ContactSelectionListItem contactItem = (ContactSelectionListItem) holder.itemView;

      int id = itemData.get(i).contactId;
      DcContact dcContact = null;
      String label = null;
      String name;
      String addr = null;

      if (id == DcContact.DC_CONTACT_ID_ADD_MEMBER) {
        name = context.getString(R.string.group_add_members);
      }
      else if (id == DcContact.DC_CONTACT_ID_QR_INVITE) {
        name = context.getString(R.string.qrshow_title);
      }
      else {
        dcContact = getContact(i);
        name = dcContact.getDisplayName();
        addr = dcContact.getAddr();
      }

      contactItem.set(glideRequests, id, dcContact, name, addr, label, false, true);
    }
    else if (holder.itemView instanceof ConversationListItem) {
      ConversationListItem conversationListItem = (ConversationListItem) holder.itemView;
      int chatlistIndex = itemData.get(i).chatlistIndex;

      DcChat chat = dcContext.getChat(itemDataSharedChats.getChatId(chatlistIndex));
      DcLot summary = itemDataSharedChats.getSummary(chatlistIndex, chat);

      conversationListItem.bind(dcContext.getThreadRecord(summary, chat),
          itemDataSharedChats.getMsgId(chatlistIndex), summary, glideRequests,
          locale, Collections.emptySet(), false);

    }
  }

  @Override
  public int getItemViewType(int i) {
    return itemData.get(i).type;
  }

  public interface ItemClickListener {
    void onItemClick(ContactSelectionListItem item, boolean handleActionMode);
    void onItemLongClick(ContactSelectionListItem view);
  }

  @Override
  public long getHeaderId(int position) {
    return getItemViewType(position);
  }

  @Override
  public HeaderViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
    return new HeaderViewHolder(LayoutInflater.from(context).inflate(R.layout.profile_item_header, parent, false));
  }

  @Override
  public void onBindHeaderViewHolder(HeaderViewHolder viewHolder, int position) {
    String txt = "";
    switch(getItemViewType(position)) {
      case ItemData.TYPE_MEMBER:
        txt = context.getResources().getQuantityString(R.plurals.n_members, (int) itemDataMemberCount, (int) itemDataMemberCount);
        break;
      case ItemData.TYPE_SHARED_CHAT:
        txt = context.getString(R.string.profile_shared_chats);
        break;
      default:
        txt = "";
        break;
    }
    viewHolder.textView.setText(txt);
  }


  public void changeData(@Nullable int[] memberList, @Nullable DcChatlist sharedChats) {
    itemData.clear();
    itemDataMemberCount = 0;
    itemDataSharedChats = null;

    if (memberList!=null) {
      itemDataMemberCount = memberList.length;
      itemData.add(new ItemData(ItemData.TYPE_MEMBER, DcContact.DC_CONTACT_ID_ADD_MEMBER, 0, 0));
      itemData.add(new ItemData(ItemData.TYPE_MEMBER, DcContact.DC_CONTACT_ID_QR_INVITE, 0, 0));
      for (int i = 0; i < memberList.length; i++) {
        itemData.add(new ItemData(ItemData.TYPE_MEMBER, memberList[i], 0, 0));
      }
    }
    else if (sharedChats!=null) {
      itemDataSharedChats = sharedChats;
      int sharedChatsCnt = sharedChats.getCnt();
      for (int i = 0; i < sharedChatsCnt; i++) {
        itemData.add(new ItemData(ItemData.TYPE_SHARED_CHAT, 0, i, 0));
      }
    }

    recordCache.clear();
    notifyDataSetChanged();
  }
}
