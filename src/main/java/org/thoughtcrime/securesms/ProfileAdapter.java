package org.thoughtcrime.securesms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcChatlist;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcLot;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.ContactSelectionListItem;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ProfileAdapter extends RecyclerView.Adapter
{
  public static final int ITEM_AVATAR = 10;
  public static final int ITEM_DIVIDER = 20;
  public static final int ITEM_SIGNATURE = 25;
  public static final int ITEM_ALL_MEDIA_BUTTON = 30;
  public static final int ITEM_SEND_MESSAGE_BUTTON = 35;
  public static final int ITEM_LAST_SEEN = 40;
  public static final int ITEM_INTRODUCED_BY = 45;
  public static final int ITEM_ADDRESS = 50;
  public static final int ITEM_HEADER = 53;
  public static final int ITEM_MEMBERS = 55;
  public static final int ITEM_SHARED_CHATS = 60;

  private final @NonNull Context              context;
  private final @NonNull Fragment             fragment;
  private final @NonNull DcContext            dcContext;
  private @Nullable DcChat                    dcChat;
  private @Nullable DcContact                 dcContact;

  private final @NonNull ArrayList<ItemData>  itemData = new ArrayList<>();
  private DcChatlist                          itemDataSharedChats;
  private String                              itemDataStatusText;
  private boolean                             isOutBroadcast;
  private int[]                               memberList;
  private final Set<Integer>                  selectedMembers;

  private final LayoutInflater                layoutInflater;
  private final ItemClickListener             clickListener;
  private final GlideRequests                 glideRequests;

  static class ItemData {
    final int viewType;
    final int contactId;
    final int chatlistIndex;
    final String label;
    final int icon;

    ItemData(int viewType, String label, int icon) {
      this(viewType, 0, 0, label, icon);
    }

    ItemData(int viewType, int contactId, int chatlistIndex) {
      this(viewType, contactId, chatlistIndex, null, 0);
    }

    private ItemData(int viewType, int contactId, int chatlistIndex, @Nullable String label, int icon) {
      this.viewType      = viewType;
      this.contactId     = contactId;
      this.chatlistIndex = chatlistIndex;
      this.label         = label;
      this.icon          = icon;
    }
  };

  public ProfileAdapter(@NonNull  Fragment fragment,
                        @NonNull  GlideRequests glideRequests,
                        @Nullable ItemClickListener clickListener)
  {
    super();
    this.fragment       = fragment;
    this.context        = fragment.requireContext();
    this.glideRequests  = glideRequests;
    this.clickListener  = clickListener;
    this.dcContext      = DcHelper.getContext(context);
    this.layoutInflater = LayoutInflater.from(context);
    this.selectedMembers= new HashSet<>();
  }

  @Override
  public int getItemCount() {
    return itemData.size();
  }

  @Override
  public int getItemViewType(int i) {
    return itemData.get(i).viewType;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    public ViewHolder(View itemView) {
      super(itemView);
    }
  }

  @NonNull
  @Override
  public ProfileAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == ITEM_HEADER) {
      final View item = LayoutInflater.from(context).inflate(R.layout.contact_selection_list_divider, parent, false);
      return new ViewHolder(item);
    } else if (viewType == ITEM_DIVIDER) {
      final View item = LayoutInflater.from(context).inflate(R.layout.profile_divider, parent, false);
      return new ViewHolder(item);
    } else if (viewType == ITEM_MEMBERS) {
      final ContactSelectionListItem item = (ContactSelectionListItem)layoutInflater.inflate(R.layout.contact_selection_list_item, parent, false);
      return new ViewHolder(item);
    } else if (viewType == ITEM_SHARED_CHATS) {
      final ConversationListItem item = (ConversationListItem)layoutInflater.inflate(R.layout.conversation_list_item_view, parent, false);
      item.hideItemDivider();
      return new ViewHolder(item);
    } else if (viewType == ITEM_SIGNATURE) {
      final ProfileStatusItem item = (ProfileStatusItem)layoutInflater.inflate(R.layout.profile_status_item, parent, false);
      return new ViewHolder(item);
    } else if (viewType == ITEM_AVATAR) {
      final ProfileAvatarItem item = (ProfileAvatarItem)layoutInflater.inflate(R.layout.profile_avatar_item, parent, false);
      return new ViewHolder(item);
    } else if (viewType == ITEM_ALL_MEDIA_BUTTON || viewType == ITEM_SEND_MESSAGE_BUTTON) {
      final ProfileTextItem item = (ProfileTextItem)layoutInflater.inflate(R.layout.profile_text_item_button, parent, false);
      return new ViewHolder(item);
    } else if (viewType == ITEM_LAST_SEEN || viewType == ITEM_INTRODUCED_BY || viewType == ITEM_ADDRESS) {
      final ProfileTextItem item = (ProfileTextItem)layoutInflater.inflate(R.layout.profile_text_item_small, parent, false);
      return new ViewHolder(item);
    } else {
      final ProfileTextItem item = (ProfileTextItem)layoutInflater.inflate(R.layout.profile_text_item, parent, false);
      return new ViewHolder(item);
    }
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
    ViewHolder holder = (ViewHolder) viewHolder;
    ItemData data = itemData.get(i);
    if (holder.itemView instanceof ContactSelectionListItem) {
      ContactSelectionListItem contactItem = (ContactSelectionListItem) holder.itemView;

      int contactId = data.contactId;
      DcContact dcContact = null;
      String label = null;
      String name;
      String addr = null;

      if (contactId == DcContact.DC_CONTACT_ID_ADD_MEMBER) {
        if (isOutBroadcast) {
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
      int chatlistIndex = data.chatlistIndex;

      int chatId = itemDataSharedChats.getChatId(chatlistIndex);
      DcChat chat = dcContext.getChat(chatId);
      DcLot summary = itemDataSharedChats.getSummary(chatlistIndex, chat);

      conversationListItem.bind(DcHelper.getThreadRecord(context, summary, chat),
        itemDataSharedChats.getMsgId(chatlistIndex), summary, glideRequests,
        Collections.emptySet(), false);
      conversationListItem.setOnClickListener(view -> clickListener.onSharedChatClicked(chatId));
    }
    else if(holder.itemView instanceof ProfileStatusItem) {
      ProfileStatusItem item = (ProfileStatusItem) holder.itemView;
      item.setOnLongClickListener(view -> {clickListener.onStatusLongClicked(); return true;});
      item.set(data.label);
    }
    else if(holder.itemView instanceof ProfileAvatarItem) {
      ProfileAvatarItem item = (ProfileAvatarItem) holder.itemView;
      item.setAvatarClickListener(view -> clickListener.onAvatarClicked());
      item.set(glideRequests, dcChat, dcContact, memberList);
    }
    else if(holder.itemView instanceof ProfileTextItem) {
      ProfileTextItem item = (ProfileTextItem) holder.itemView;
      item.setOnClickListener(view -> clickListener.onSettingsClicked(data.viewType));
      boolean tintIcon = data.viewType != ITEM_INTRODUCED_BY;
      item.set(data.label, data.icon, tintIcon);
      if (data.viewType == ITEM_LAST_SEEN || data.viewType == ITEM_ADDRESS) {
        int padding = (int)((float)context.getResources().getDimensionPixelSize(R.dimen.contact_list_normal_padding) * 1.2);
        item.setPadding(item.getPaddingLeft(), item.getPaddingTop(), item.getPaddingRight(), padding);
        if (data.viewType == ITEM_ADDRESS) {
          fragment.registerForContextMenu(item);
        }
      } else if (data.viewType == ITEM_INTRODUCED_BY) {
        int padding = context.getResources().getDimensionPixelSize(R.dimen.contact_list_normal_padding);
        item.setPadding(item.getPaddingLeft(), padding, item.getPaddingRight(), item.getPaddingBottom());
      } else if (data.viewType == ITEM_ALL_MEDIA_BUTTON && dcChat != null) {
        Util.runOnAnyBackgroundThread(() -> {
          String c = getAllMediaCountString(dcChat.getId());
          Util.runOnMain(() -> {
            item.setValue(c);
          });
        });
      }
    } else if (data.viewType == ITEM_HEADER) {
      TextView textView = holder.itemView.findViewById(R.id.label);
      textView.setText(data.label);
    }
  }

  public interface ItemClickListener {
    void onSettingsClicked(int settingsId);
    void onStatusLongClicked();
    void onSharedChatClicked(int chatId);
    void onMemberClicked(int contactId);
    void onMemberLongClicked(int contactId);
    void onAvatarClicked();
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
    this.dcChat = dcChat;
    this.dcContact = dcContact;
    itemData.clear();
    itemDataSharedChats = sharedChats;
    itemDataStatusText = "";
    isOutBroadcast = dcChat != null && dcChat.isOutBroadcast();
    boolean isMailingList = dcChat != null && dcChat.isMailingList();
    boolean isInBroadcast = dcChat != null && dcChat.isInBroadcast();
    boolean isSelfTalk = dcChat != null && dcChat.isSelfTalk();
    boolean isDeviceTalk = dcChat != null && dcChat.isDeviceTalk();
    this.memberList = memberList;

    itemData.add(new ItemData(ITEM_AVATAR, null, 0));

    if (isSelfTalk || dcContact != null && !dcContact.getStatus().isEmpty()) {
      itemDataStatusText = isSelfTalk ? context.getString(R.string.saved_messages_explain) : dcContact.getStatus();
      itemData.add(new ItemData(ITEM_SIGNATURE, itemDataStatusText, 0));
    } else {
      itemData.add(new ItemData(ITEM_DIVIDER, null, 0));
    }

    itemData.add(new ItemData(ITEM_ALL_MEDIA_BUTTON, context.getString(R.string.apps_and_media), R.drawable.ic_apps_24));

    if (dcContact != null && !isDeviceTalk && !isSelfTalk) {
      itemData.add(new ItemData(ITEM_SEND_MESSAGE_BUTTON, context.getString(R.string.send_message), R.drawable.ic_send_sms_white_24dp));
    }

    if (dcContact != null && !isDeviceTalk && !isSelfTalk) {
      long lastSeenTimestamp = dcContact.getLastSeen();
      String lastSeenTxt;
      if (lastSeenTimestamp == 0) {
        lastSeenTxt = context.getString(R.string.last_seen_unknown);
      }
      else {
        lastSeenTxt = context.getString(R.string.last_seen_at, DateUtils.getExtendedTimeSpanString(context, lastSeenTimestamp));
      }
      itemData.add(new ItemData(ITEM_LAST_SEEN, lastSeenTxt, 0));
    }

    if (memberList!=null && !isInBroadcast && !isMailingList) {
      itemData.add(new ItemData(ITEM_DIVIDER, null, 0));
      if (dcChat != null) {
        if (dcChat.canSend() && dcChat.isEncrypted()) {
          if (!isOutBroadcast) {
            itemData.add(new ItemData(ITEM_MEMBERS, DcContact.DC_CONTACT_ID_ADD_MEMBER, 0));
          }
          itemData.add(new ItemData(ITEM_MEMBERS, DcContact.DC_CONTACT_ID_QR_INVITE, 0));
        }
      }
      for (int value : memberList) {
        itemData.add(new ItemData(ITEM_MEMBERS, value, 0));
      }
    }

    if (!isDeviceTalk && sharedChats != null && sharedChats.getCnt() > 0) {
      itemData.add(new ItemData(ITEM_HEADER, context.getString(R.string.profile_shared_chats), 0));
      for (int i = 0; i < sharedChats.getCnt(); i++) {
        itemData.add(new ItemData(ITEM_SHARED_CHATS, 0, i));
      }
    }

    if (dcContact != null && !isDeviceTalk && !isSelfTalk) {
      int verifierId = dcContact.getVerifierId();
      if (verifierId != 0) {
        String introducedBy;
        if (verifierId == DcContact.DC_CONTACT_ID_SELF) {
          introducedBy = context.getString(R.string.verified_by_you);
        } else {
          introducedBy = context.getString(R.string.verified_by, dcContext.getContact(verifierId).getDisplayName());
        }
        itemData.add(new ItemData(ITEM_INTRODUCED_BY, introducedBy, dcContact.isVerified()? R.drawable.ic_verified : 0));
      } else if (dcContact.isVerified()) {
        String introducedBy = context.getString(R.string.verified_by_unknown);
        itemData.add(new ItemData(ITEM_INTRODUCED_BY, introducedBy, R.drawable.ic_verified));
      }

      if (dcContact != null) {
        itemData.add(new ItemData(ITEM_ADDRESS, dcContact.getAddr(), 0));
      }
    }

    notifyDataSetChanged();
  }

  public int ALL_MEDIA_COUNT_MAX = 500;
  public int getAllMediaCount(int chatId) {
    int c = dcContext.getChatMedia(chatId, DcMsg.DC_MSG_IMAGE, DcMsg.DC_MSG_GIF, DcMsg.DC_MSG_VIDEO).length;
    if (c < ALL_MEDIA_COUNT_MAX) {
      c += dcContext.getChatMedia(chatId, DcMsg.DC_MSG_AUDIO, DcMsg.DC_MSG_VOICE, 0).length;
    }
    if (c < ALL_MEDIA_COUNT_MAX) {
      c += dcContext.getChatMedia(chatId, DcMsg.DC_MSG_FILE, DcMsg.DC_MSG_WEBXDC, 0).length;
    }
    return c;
  }

  public String getAllMediaCountString(int chatId) {
    final int c = getAllMediaCount(chatId);
    if (c == 0) {
      return context.getString(R.string.none);
    } else if (c >= ALL_MEDIA_COUNT_MAX) {
      return ALL_MEDIA_COUNT_MAX + "+";
    } else {
      return c + "";
    }
  }
}
