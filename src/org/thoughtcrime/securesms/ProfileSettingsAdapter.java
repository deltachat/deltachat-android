package org.thoughtcrime.securesms;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.b44t.messenger.DcContact;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.ContactSelectionListItem;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.util.LRUCache;
import org.thoughtcrime.securesms.util.StickyHeaderDecoration.StickyHeaderAdapter;
import org.thoughtcrime.securesms.util.Util;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public class ProfileSettingsAdapter extends RecyclerView.Adapter
                                    implements StickyHeaderAdapter<ProfileSettingsAdapter.HeaderViewHolder>
{
  private static final int MAX_CACHE_SIZE = 100;
  private final Map<Integer,SoftReference<DcContact>> recordCache =
          Collections.synchronizedMap(new LRUCache<>(MAX_CACHE_SIZE));

  private final @NonNull Context              context;
  private final @NonNull ApplicationDcContext dcContext;
  private @NonNull ArrayList<ItemData>        itemData = new ArrayList<>();
  private final boolean                       multiSelect;
  private final LayoutInflater                li;
  private final ItemClickListener             clickListener;
  private final GlideRequests                 glideRequests;

  static class ItemData {
    static final int TYPE_MEMBER = 0;
    static final int TYPE_SHARED_CHAT = 1;
    int type;
    int contactId;
    int chatId;
    int settingsId;

    ItemData(int type, int contactId, int chatId, int settingsId) {
      this.type = type;
      this.contactId = contactId;
      this.chatId = chatId;
      this.settingsId = settingsId;
    }
  };

  public ProfileSettingsAdapter(@NonNull  Context context,
                                @NonNull  GlideRequests glideRequests,
                                @Nullable ItemClickListener clickListener,
                                boolean multiSelect)
  {
    super();
    this.context       = context;
    this.dcContext     = DcHelper.getContext(context);
    this.li            = LayoutInflater.from(context);
    this.glideRequests = glideRequests;
    this.multiSelect   = multiSelect;
    this.clickListener = clickListener;
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

  public abstract static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View itemView) {
      super(itemView);
    }

    public abstract void bind(@NonNull GlideRequests glideRequests, int type, DcContact contact, String name, String number, String label, boolean multiSelect, boolean enabled);
    public abstract void unbind(@NonNull GlideRequests glideRequests);
    public abstract void setChecked(boolean checked);
    public abstract void setEnabled(boolean enabled);
  }

  public class ContactViewHolder extends ViewHolder {

    ContactViewHolder(@NonNull  final View itemView,
                      @Nullable final ItemClickListener clickListener) {
      super(itemView);
      itemView.setOnClickListener(view -> {
        if (clickListener != null) {
            clickListener.onItemClick(getView(), false);
        }
      });
      itemView.setOnLongClickListener(view -> {
          if (clickListener != null) {
            int contactId = getContactId(getAdapterPosition());
            if (contactId > 0) {
              clickListener.onItemLongClick(getView());
            }
          }
          return true;
      });
    }

    private int getContactId(int adapterPosition) {
      return itemData.get(adapterPosition).contactId;
    }

    public ContactSelectionListItem getView() {
      return (ContactSelectionListItem) itemView;
    }

    public void bind(@NonNull GlideRequests glideRequests, int type, DcContact contact, String name, String addr, String label, boolean multiSelect, boolean enabled) {
      getView().set(glideRequests, type, contact, name, addr, label, multiSelect, enabled);
    }

    @Override
    public void unbind(@NonNull GlideRequests glideRequests) {
      getView().unbind(glideRequests);
    }

    @Override
    public void setChecked(boolean checked) {
      getView().setChecked(checked);
    }

    @Override
    public void setEnabled(boolean enabled) {
      getView().setEnabled(enabled);
    }
  }

  static class HeaderViewHolder extends RecyclerView.ViewHolder {
    HeaderViewHolder(View itemView) {
      super(itemView);
    }
  }

  @Override
  public ProfileSettingsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == ItemData.TYPE_MEMBER) {
      return new ContactViewHolder(li.inflate(R.layout.contact_selection_list_item, parent, false), clickListener);
    }
    return null;
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {

    int id = itemData.get(i).contactId;
    DcContact dcContact = null;
    String label = null;
    String name;
    String addr = null;
    boolean itemMultiSelect = multiSelect;

    if (id == DcContact.DC_CONTACT_ID_NEW_CONTACT) {
      name = context.getString(R.string.group_add_members);
      itemMultiSelect = false;
    } else if (id == DcContact.DC_CONTACT_ID_NEW_GROUP) {
      name = context.getString(R.string.menu_new_group);
    } else if (id == DcContact.DC_CONTACT_ID_NEW_VERIFIED_GROUP) {
      name = context.getString(R.string.qrshow_title);
    } else {
      dcContact = getContact(i);
      name = dcContact.getDisplayName();
      addr = dcContact.getAddr();
    }

    ViewHolder holder = (ViewHolder) viewHolder;
    holder.unbind(glideRequests);
    holder.bind(glideRequests, id, dcContact, name, addr, label, itemMultiSelect, true);
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
    if (position < 0 || position >= getItemCount()) {
      return -1;
    }

    return Util.hashCode(getHeaderString(position));
  }

  private @NonNull String getHeaderString(int position) {
    DcContact dcContact = getContact(position);
    String name = dcContact.getDisplayName();
    if (!TextUtils.isEmpty(name)) {
      String firstChar = name.trim().substring(0, 1).toUpperCase();
      if (Character.isLetterOrDigit(firstChar.codePointAt(0))) {
        return firstChar;
      }
    }
    return "";
  }

  @Override
  public HeaderViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
    return new HeaderViewHolder(LayoutInflater.from(context).inflate(R.layout.contact_selection_recyclerview_header, parent, false));
  }

  @Override
  public void onBindHeaderViewHolder(HeaderViewHolder viewHolder, int position) {
    ((TextView)viewHolder.itemView).setText(getHeaderString(position));
  }


  public void changeData(@Nullable int[] contactList) {
    itemData.clear();
    if(contactList!=null) {
      itemData.add(new ItemData(ItemData.TYPE_MEMBER, DcContact.DC_CONTACT_ID_NEW_CONTACT, 0, 0));
      for (int i = 0; i < contactList.length; i++) {
        itemData.add(new ItemData(ItemData.TYPE_MEMBER, contactList[i], 0, 0));
      }
    }

    recordCache.clear();
    notifyDataSetChanged();
  }
}
