package org.thoughtcrime.securesms;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.b44t.messenger.DcContact;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcContactsLoader;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.ContactSelectionListItem;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.util.LRUCache;
import org.thoughtcrime.securesms.util.StickyHeaderDecoration.StickyHeaderAdapter;
import org.thoughtcrime.securesms.util.Util;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProfileSettingsAdapter extends RecyclerView.Adapter
                                    implements StickyHeaderAdapter<ProfileSettingsAdapter.HeaderViewHolder>
{
  private final static String TAG = ProfileSettingsAdapter.class.getSimpleName();

  private static final int VIEW_TYPE_CONTACT = 0;
  private static final int VIEW_TYPE_DIVIDER = 1;

  private final static int STYLE_ATTRIBUTES[] = new int[]{R.attr.contact_selection_push_user,
                                                          R.attr.contact_selection_lay_user};

  private static final int MAX_CACHE_SIZE = 100;

  private final Map<Integer,SoftReference<DcContact>> recordCache =
          Collections.synchronizedMap(new LRUCache<Integer,SoftReference<DcContact>>(MAX_CACHE_SIZE));

  private final @NonNull Context              context;
  private final @NonNull ApplicationDcContext dcContext;
  private @NonNull int[]                      dcContactList = new int[0];
  private final boolean                       multiSelect;
  private final boolean                       longPressSelect;
  private final LayoutInflater                li;
  private final ItemClickListener             clickListener;
  private final GlideRequests                 glideRequests;
  private final Set<String>                   selectedContacts = new HashSet<>(); // TODO: maybe better use contact-id here
  private final SparseIntArray                actionModeSelection = new SparseIntArray();

  public ProfileSettingsAdapter(@NonNull  Context context,
                                @NonNull  GlideRequests glideRequests,
                                @Nullable ItemClickListener clickListener,
                                boolean multiSelect,
                                boolean longPressSelect)
  {
    super();
    this.context       = context;
    this.dcContext     = DcHelper.getContext(context);
    this.li            = LayoutInflater.from(context);
    this.glideRequests = glideRequests;
    this.multiSelect   = multiSelect;
    this.clickListener = clickListener;
    this.longPressSelect = longPressSelect;
  }

  @Override
  public int getItemCount() {
    return dcContactList.length;
  }

  private @NonNull DcContact getContact(int position) {
    if(position<0 || position>=dcContactList.length) {
      return new DcContact(0);
    }

    final SoftReference<DcContact> reference = recordCache.get(position);
    if (reference != null) {
      final DcContact fromCache = reference.get();
      if (fromCache != null) {
        return fromCache;
      }
    }

    final DcContact fromDb = dcContext.getContact(dcContactList[position]);
    recordCache.put(position, new SoftReference<>(fromDb));
    return fromDb;
  }

    public void resetActionModeSelection() {
      actionModeSelection.clear();
      notifyDataSetChanged();
    }

    public void selectAll() {
        actionModeSelection.clear();
        for(int index = 0; index < dcContactList.length; index++) {
          int value = dcContactList[index];
          if (value > 0) {
            actionModeSelection.put(index, value);
          }
        }
      notifyDataSetChanged();
    }

  private boolean isActionModeEnabled() {
    return actionModeSelection.size() != 0;
  }

  public abstract static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View itemView) {
      super(itemView);
    }

    public abstract void bind(@NonNull GlideRequests glideRequests, int type, DcContact contact, String name, String number, String label, boolean multiSelect, boolean enabled);
    public abstract void unbind(@NonNull GlideRequests glideRequests);
    public abstract void setChecked(boolean checked);
    public abstract void setSelected(boolean enabled);
    public abstract void setEnabled(boolean enabled);
  }

  public class ContactViewHolder extends ViewHolder {

    ContactViewHolder(@NonNull  final View itemView,
                      @Nullable final ItemClickListener clickListener) {
      super(itemView);
      itemView.setOnClickListener(view -> {
        if (clickListener != null) {
          if (isActionModeEnabled()) {
            toggleSelection();
            clickListener.onItemClick(getView(), true);
          } else {
            clickListener.onItemClick(getView(), false);
          }
        }
      });
      itemView.setOnLongClickListener(view -> {
          if (clickListener != null) {
            int contactId = getContactId(getAdapterPosition());
            if (contactId > 0) {
              toggleSelection();
              clickListener.onItemLongClick(getView());
            }
          }
          return true;
      });
    }

    private int getContactId(int adapterPosition) {
      return ProfileSettingsAdapter.this.dcContactList[adapterPosition];
    }

    private void toggleSelection() {
      if (!longPressSelect) {
        return;
      }
      int adapterPosition = getAdapterPosition();
      int contactId = getContactId(adapterPosition);
      boolean enabled = actionModeSelection.indexOfKey(adapterPosition) > -1;
      if (enabled) {
        ProfileSettingsAdapter.this.actionModeSelection.delete(adapterPosition);
        } else {
        ProfileSettingsAdapter.this.actionModeSelection.put(adapterPosition, contactId);
      }
      notifyDataSetChanged();
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
    public void setSelected(boolean enabled) {
      getView().setSelected(enabled);
    }

    @Override
    public void setEnabled(boolean enabled) {
      getView().setEnabled(enabled);
    }
  }

  public static class DividerViewHolder extends ViewHolder {

    private final TextView label;

    DividerViewHolder(View itemView) {
      super(itemView);
      this.label = itemView.findViewById(R.id.label);
    }

    @Override
    public void bind(@NonNull GlideRequests glideRequests, int type, DcContact contact, String name, String number, String label, boolean multiSelect, boolean enabled) {
      this.label.setText(name);
    }

    @Override
    public void unbind(@NonNull GlideRequests glideRequests) {}

    @Override
    public void setChecked(boolean checked) {}

    @Override
    public void setSelected(boolean enabled) {
    }

    @Override
    public void setEnabled(boolean enabled) {

    }
  }

  static class HeaderViewHolder extends RecyclerView.ViewHolder {
    HeaderViewHolder(View itemView) {
      super(itemView);
    }
  }

  @Override
  public ProfileSettingsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == VIEW_TYPE_CONTACT) {
      return new ContactViewHolder(li.inflate(R.layout.contact_selection_list_item, parent, false), clickListener);
    } else {
      return new DividerViewHolder(li.inflate(R.layout.contact_selection_list_divider, parent, false));
    }
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {

    int id = dcContactList[i];
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
    boolean enabled = true;
    if (dcContact == null) {
      holder.setSelected(false);
      holder.setEnabled(!isActionModeEnabled());
      if (isActionModeEnabled()) {
        enabled = false;
      }
    } else {
      boolean selected = actionModeSelection.indexOfValue(id) > -1;
      holder.setSelected(selected);
    }
    holder.bind(glideRequests, id, dcContact, name, addr, label, itemMultiSelect, enabled);
    holder.setChecked(selectedContacts.contains(addr));
  }

  @Override
  public int getItemViewType(int i) {
    return VIEW_TYPE_CONTACT;
  }

  public Set<String> getSelectedContacts() {
    return selectedContacts;
  }

  public SparseIntArray getActionModeSelection() {
    return actionModeSelection;
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
    if(contactList!=null) {
      final int additionalItems = 1;
      dcContactList = new int[contactList.length + additionalItems];
      dcContactList[0] = DcContact.DC_CONTACT_ID_NEW_CONTACT;
      for (int i = 0; i < contactList.length; i++) {
        dcContactList[i + additionalItems] = contactList[i];
      }
    }
    else {
      dcContactList = new int[0];
    }

    recordCache.clear();
    notifyDataSetChanged();
  }
}
