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
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcLot;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.ContactSelectionListItem;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.util.DateUtils;
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
  public static final int INFO_VERIFIED = 118;
  public static final int INFO_LAST_SEEN = 119;
  public static final int INFO_SEND_MESSAGE_BUTTON = 120;

  private final @NonNull Context              context;
  private final @NonNull Locale               locale;
  private final @NonNull DcContext            dcContext;

  private final @NonNull ArrayList<ItemData>  itemData = new ArrayList<>();
  private int                                 itemDataMemberCount;
  private DcChatlist                          itemDataSharedChats;
  private DcContact                           itemDataContact;
  private String                              itemDataStatusText;
  private boolean                             isMailingList;
  private boolean                             isBroadcast;
  private final Set<Integer>                  selectedMembers;

  private final LayoutInflater                layoutInflater;
  private final ItemClickListener             clickListener;
  private final GlideRequests                 glideRequests;

  static class ItemData {
    static final int CATEGORY_INFO = 1;
    static final int CATEGORY_SIGNATURE = 2;
    static final int CATEGORY_MEMBERS = 3;
    static final int CATEGORY_SHARED_CHATS = 4;
    final int type;
    final int contactId;
    final int chatlistIndex;
    final int settingsId;
    final String label;
    final int labelColor;
    final int iconLeft;

    ItemData(int type, int settingsId, String label, int labelColor, int iconLeft) {
      this(type, 0, 0, settingsId, label, labelColor, iconLeft);
    }

    ItemData(int type, int contactId, int chatlistIndex) {
      this(type, contactId, chatlistIndex, 0, null, 0, 0);
    }

    ItemData(int type, int contactId, int chatlistIndex, int settingsId, @Nullable String label, int labelColor, int iconLeft) {
      this.type          = type;
      this.contactId     = contactId;
      this.chatlistIndex = chatlistIndex;
      this.settingsId    = settingsId;
      this.label         = label;
      this.labelColor    = labelColor;
      this.iconLeft      = iconLeft;
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
    final TextView textView;
    HeaderViewHolder(View itemView) {
      super(itemView);
      textView = itemView.findViewById(R.id.label);
    }
  }

  @Override
  public ProfileSettingsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == ItemData.CATEGORY_MEMBERS) {
      final ContactSelectionListItem item = (ContactSelectionListItem)layoutInflater.inflate(R.layout.contact_selection_list_item, parent, false);
      item.setNoHeaderPadding();
      return new ViewHolder(item);
    }
    else if (viewType == ItemData.CATEGORY_SHARED_CHATS) {
      final ConversationListItem item = (ConversationListItem)layoutInflater.inflate(R.layout.conversation_list_item_view, parent, false);
      item.hideItemDivider();
      return new ViewHolder(item);
    }
    else if (viewType == ItemData.CATEGORY_SIGNATURE) {
      final ProfileStatusItem item = (ProfileStatusItem)layoutInflater.inflate(R.layout.profile_status_item, parent, false);
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
        if (isBroadcast) {
          name = context.getString(R.string.add_recipients);
        } else {
          name = context.getString(R.string.group_add_members);
        }
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

      conversationListItem.bind(DcHelper.getThreadRecord(context, summary, chat),
          itemDataSharedChats.getMsgId(chatlistIndex), summary, glideRequests,
          locale, Collections.emptySet(), false);
      conversationListItem.setOnClickListener(view -> clickListener.onSharedChatClicked(chatId));
    }
    else if(holder.itemView instanceof ProfileStatusItem) {
      ProfileStatusItem item = (ProfileStatusItem) holder.itemView;
      item.setOnLongClickListener(view -> {clickListener.onStatusLongClicked(); return true;});
      item.set(itemData.get(i).label);
    }
    else if(holder.itemView instanceof ProfileSettingsItem) {
      int settingsId = itemData.get(i).settingsId;
      ProfileSettingsItem profileSettingsItem = (ProfileSettingsItem) holder.itemView;
      profileSettingsItem.setOnClickListener(view -> clickListener.onSettingsClicked(settingsId));
      profileSettingsItem.set(itemData.get(i).label, itemData.get(i).labelColor, itemData.get(i).iconLeft);
    }
  }

  @Override
  public int getItemViewType(int i) {
    return itemData.get(i).type;
  }

  public interface ItemClickListener {
    void onSettingsClicked(int settingsId);
    void onStatusLongClicked();
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
      case ItemData.CATEGORY_MEMBERS:
        if (isMailingList) {
          txt = context.getString(R.string.contacts_headline);
        } else if (isBroadcast) {
          txt = context.getResources().getQuantityString(R.plurals.n_recipients, (int) itemDataMemberCount, (int) itemDataMemberCount);
        } else {
          txt = context.getResources().getQuantityString(R.plurals.n_members, (int) itemDataMemberCount, (int) itemDataMemberCount);
        }
        break;
      case ItemData.CATEGORY_SHARED_CHATS:
        txt = context.getString(R.string.profile_shared_chats);
        break;
      case ItemData.CATEGORY_INFO:
        txt = context.getString(R.string.info);
        break;
      case ItemData.CATEGORY_SIGNATURE:
        txt = context.getString(R.string.pref_default_status_label);
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

  @NonNull
  public String getStatusText() {
    return itemDataStatusText;
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
    itemDataStatusText = "";
    isMailingList = false;
    isBroadcast = false;

    if (memberList!=null) {
      itemDataMemberCount = memberList.length;
      if (dcChat.isBroadcast()) {
        isBroadcast = true;
      }

      if (dcChat.isMailingList()) {
        isMailingList = true;
      } else if (dcChat.canSend()) {
        itemData.add(new ItemData(ItemData.CATEGORY_MEMBERS, DcContact.DC_CONTACT_ID_ADD_MEMBER, 0));
        if (!isBroadcast) {
          itemData.add(new ItemData(ItemData.CATEGORY_MEMBERS, DcContact.DC_CONTACT_ID_QR_INVITE, 0));
        }
      }

      for (int value : memberList) {
        itemData.add(new ItemData(ItemData.CATEGORY_MEMBERS, value, 0));
      }
    }
    else if (sharedChats!=null && dcContact!=null) {
      boolean chatIsDeviceTalk = dcChat != null && dcChat.isDeviceTalk();

      itemDataContact = dcContact;
      if (!chatIsDeviceTalk) {
        if (dcContact.isVerified()) {
          int verifierId = dcContact.getVerifierId();

          String verifiedInfo;
          if (verifierId == DcContact.DC_CONTACT_ID_SELF) {
            verifiedInfo = context.getString(R.string.verified_by_you);
          } else if (verifierId != 0) {
            verifiedInfo = context.getString(R.string.verified_by, dcContext.getContact(verifierId).getAddr());
          } else {
            verifiedInfo = context.getString(R.string.verified);
          }
          itemData.add(new ItemData(ItemData.CATEGORY_INFO, INFO_VERIFIED, verifiedInfo, 0, R.drawable.ic_verified));
        }

        long lastSeenTimestamp = (itemDataContact!=null? itemDataContact.getLastSeen() : 0);
        String lastSeenTxt;
        if (lastSeenTimestamp == 0) {
          lastSeenTxt = context.getString(R.string.last_seen_unknown);
        }
        else {
          lastSeenTxt = context.getString(R.string.last_seen_at, DateUtils.getExtendedTimeSpanString(context, locale, lastSeenTimestamp));
        }
        itemData.add(new ItemData(ItemData.CATEGORY_INFO, INFO_LAST_SEEN, lastSeenTxt, 0, 0));


        itemData.add(new ItemData(ItemData.CATEGORY_INFO, INFO_SEND_MESSAGE_BUTTON, context.getString(R.string.send_message), R.color.delta_accent, 0));
      }

      itemDataStatusText = dcContact.getStatus();
      if (!itemDataStatusText.isEmpty()) {
        itemData.add(new ItemData(ItemData.CATEGORY_SIGNATURE, 0, itemDataStatusText, 0, 0));
      }

      itemDataSharedChats = sharedChats;
      if (!chatIsDeviceTalk) {
        int sharedChatsCnt = sharedChats.getCnt();
        for (int i = 0; i < sharedChatsCnt; i++) {
          itemData.add(new ItemData(ItemData.CATEGORY_SHARED_CHATS, 0, i));
        }
      }
    }

    notifyDataSetChanged();
  }
}
