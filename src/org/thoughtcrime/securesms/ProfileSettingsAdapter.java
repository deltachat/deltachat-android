package org.thoughtcrime.securesms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcChatlist;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcLot;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.ContactSelectionListItem;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.util.StickyHeaderDecoration.StickyHeaderAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ProfileSettingsAdapter extends RecyclerView.Adapter
                                    implements StickyHeaderAdapter<ProfileSettingsAdapter.HeaderViewHolder>
{
  public static final int SETTING_CONTACT_ADDR = 110;
  public static final int SETTING_NEW_CHAT = 120;

  private final @NonNull Context              context;
  private final @NonNull Locale               locale;
  private final @NonNull ApplicationDcContext dcContext;

  private @NonNull ArrayList<ItemData>        itemData = new ArrayList<>();
  private int                                 itemDataMemberCount;
  private DcChatlist                          itemDataSharedChats;
  private DcContact                           itemDataContact;
  private boolean                             isMailingList;
  private final Set<Integer>                  selectedMembers;

  private final LayoutInflater                layoutInflater;
  private final ItemClickListener             clickListener;
  private final GlideRequests                 glideRequests;

  static class ItemData {
    static final int TYPE_PRIMARY_SETTING = 1;
    static final int TYPE_MEMBER = 2;
    static final int TYPE_SHARED_CHAT = 3;
    int type;
    int contactId;
    int chatlistIndex;
    int settingsId;
    String label;

    ItemData(int type, int settingsId, String label) {
      this(type, 0, 0, settingsId, label);
    }

    ItemData(int type, int contactId, int chatlistIndex) {
      this(type, contactId, chatlistIndex, 0, null);
    }

    ItemData(int type, int contactId, int chatlistIndex, int settingsId, @Nullable String label) {
      this.type          = type;
      this.contactId     = contactId;
      this.chatlistIndex = chatlistIndex;
      this.settingsId    = settingsId;
      this.label         = label;
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
    this.selectedMembers= new HashSet<>();
  }

  @Override
  public int getItemCount() {
    return itemData.size();
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
      textView = itemView.findViewById(R.id.label);
    }
  }

  @Override
  public ProfileSettingsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == ItemData.TYPE_MEMBER) {
      final ContactSelectionListItem item = (ContactSelectionListItem)layoutInflater.inflate(R.layout.contact_selection_list_item, parent, false);
      item.setNoHeaderPadding();
      return new ViewHolder(item);
    }
    else if (viewType == ItemData.TYPE_SHARED_CHAT) {
      final ConversationListItem item = (ConversationListItem)layoutInflater.inflate(R.layout.conversation_list_item_view, parent, false);
      item.hideItemDivider();
      return new ViewHolder(item);
    }
    else {
      final ProfileSettingsItem item = (ProfileSettingsItem)layoutInflater.inflate(R.layout.profile_settings_item, parent, false);
      return new ViewHolder(item);
    }
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
    ViewHolder holder = (ViewHolder) viewHolder;
    if (holder.itemView instanceof ContactSelectionListItem) {
      ContactSelectionListItem contactItem = (ContactSelectionListItem) holder.itemView;

      int contactId = itemData.get(i).contactId;
      DcContact dcContact = null;
      String label = null;
      String name;
      String addr = null;

      if (contactId == DcContact.DC_CONTACT_ID_ADD_MEMBER) {
        name = context.getString(R.string.group_add_members);
      }
      else if (contactId == DcContact.DC_CONTACT_ID_QR_INVITE) {
        name = context.getString(R.string.qrshow_title);
      }
      else {
        dcContact = dcContext.getContact(contactId);
        name = dcContact.getDisplayName();
        addr = dcContact.getAddr();
      }

      contactItem.unbind(glideRequests);
      contactItem.set(glideRequests, contactId, dcContact, name, addr, label, false, true);
      contactItem.setSelected(selectedMembers.contains(contactId));
      contactItem.setOnClickListener(view -> clickListener.onMemberClicked(contactId));
      contactItem.setOnLongClickListener(view -> {clickListener.onMemberLongClicked(contactId); return true;});
    }
    else if (holder.itemView instanceof ConversationListItem) {
      ConversationListItem conversationListItem = (ConversationListItem) holder.itemView;
      int chatlistIndex = itemData.get(i).chatlistIndex;

      int chatId = itemDataSharedChats.getChatId(chatlistIndex);
      DcChat chat = dcContext.getChat(chatId);
      DcLot summary = itemDataSharedChats.getSummary(chatlistIndex, chat);

      conversationListItem.bind(dcContext.getThreadRecord(summary, chat),
          itemDataSharedChats.getMsgId(chatlistIndex), summary, glideRequests,
          locale, Collections.emptySet(), false);
      conversationListItem.setOnClickListener(view -> clickListener.onSharedChatClicked(chatId));
    }
    else if(holder.itemView instanceof ProfileSettingsItem) {
      int settingsId = itemData.get(i).settingsId;
      ProfileSettingsItem profileSettingsItem = (ProfileSettingsItem) holder.itemView;
      profileSettingsItem.setOnClickListener(view -> clickListener.onSettingsClicked(settingsId));
      profileSettingsItem.set(itemData.get(i).label);
    }
  }

  @Override
  public int getItemViewType(int i) {
    return itemData.get(i).type;
  }

  public interface ItemClickListener {
    void onSettingsClicked(int settingsId);
    void onSharedChatClicked(int chatId);
    void onMemberClicked(int contactId);
    void onMemberLongClicked(int contactId);
  }

  @Override
  public long getHeaderId(int position) {
    return getItemViewType(position);
  }

  @Override
  public HeaderViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
    return new HeaderViewHolder(LayoutInflater.from(context).inflate(R.layout.contact_selection_list_divider, parent, false));
  }

  @Override
  public void onBindHeaderViewHolder(HeaderViewHolder viewHolder, int position) {
    String txt = "";
    switch(getItemViewType(position)) {
      case ItemData.TYPE_MEMBER:
        if (isMailingList) {
          txt = context.getString(R.string.contacts_headline);
        } else {
          txt = context.getResources().getQuantityString(R.plurals.n_members, (int) itemDataMemberCount, (int) itemDataMemberCount);
        }
        break;
      case ItemData.TYPE_SHARED_CHAT:
        txt = context.getString(R.string.profile_shared_chats);
        break;
      case ItemData.TYPE_PRIMARY_SETTING:
        if(itemDataContact!=null && itemDataContact.isVerified()) {
          txt = context.getString(R.string.verified_contact);
        }
        else {
          txt = context.getString(R.string.contact);
        }
        break;
      default:
        txt = context.getString(R.string.menu_settings);
        break;
    }
    viewHolder.textView.setText(txt);
  }

  public void toggleMemberSelection(int contactId) {
    if (!selectedMembers.remove(contactId)) {
      selectedMembers.add(contactId);
    }
    notifyDataSetChanged();
  }

  @NonNull
  public Collection<Integer> getSelectedMembers() {
    return new HashSet<>(selectedMembers);
  }

  public int getSelectedMembersCount() {
    return selectedMembers.size();
  }

  public void clearSelection() {
    selectedMembers.clear();
    notifyDataSetChanged();
  }

  public void changeData(@Nullable int[] memberList, @Nullable DcContact dcContact, @Nullable DcChatlist sharedChats, @Nullable DcChat dcChat) {
    itemData.clear();
    itemDataMemberCount = 0;
    itemDataSharedChats = null;
    itemDataContact = null;
    isMailingList = false;

    if (memberList!=null) {
      itemDataMemberCount = memberList.length;
      if (dcChat.isMailingList()) {
        isMailingList = true;
      } else {
        itemData.add(new ItemData(ItemData.TYPE_MEMBER, DcContact.DC_CONTACT_ID_ADD_MEMBER, 0));
        itemData.add(new ItemData(ItemData.TYPE_MEMBER, DcContact.DC_CONTACT_ID_QR_INVITE, 0));
      }
      for (int value : memberList) {
        itemData.add(new ItemData(ItemData.TYPE_MEMBER, value, 0));
      }
    }
    else if (sharedChats!=null && dcContact!=null) {
      itemDataContact = dcContact;
      itemData.add(new ItemData(ItemData.TYPE_PRIMARY_SETTING, SETTING_CONTACT_ADDR,dcContact.getAddr()));
      itemData.add(new ItemData(ItemData.TYPE_PRIMARY_SETTING, SETTING_NEW_CHAT, context.getString(R.string.send_message)));
      itemDataSharedChats = sharedChats;
      int sharedChatsCnt = sharedChats.getCnt();
      for (int i = 0; i < sharedChatsCnt; i++) {
        itemData.add(new ItemData(ItemData.TYPE_SHARED_CHAT, 0, i));
      }
    }

    notifyDataSetChanged();
  }
}
