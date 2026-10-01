/**
 * Copyright (C) 2014 Open Whisper Systems
 *
 * <p>This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * <p>You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.contacts;

import android.content.Context;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcContactsLoader;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.search.QrInviteData;
import org.thoughtcrime.securesms.util.LRUCache;

/**
 * List adapter to display all contacts and their related information
 *
 * @author Jake McGinty
 */
public class ContactSelectionListAdapter
    extends RecyclerView.Adapter<ContactSelectionListAdapter.ViewHolder> {
  private static final int VIEW_TYPE_CONTACT = 0;
  private static final int MAX_CACHE_SIZE = 100;

  private final Map<Integer, SoftReference<DcContact>> recordCache =
      Collections.synchronizedMap(new LRUCache<Integer, SoftReference<DcContact>>(MAX_CACHE_SIZE));

  private final @NonNull Context context;
  private final @NonNull DcContext dcContext;
  private @NonNull int[] dcContactList = new int[0];
  private @Nullable QrInviteData qrInviteData = null;
  private final boolean multiSelect;
  private final boolean longPressSelect;
  private final LayoutInflater li;
  private final ItemClickListener clickListener;
  private final GlideRequests glideRequests;
  private final Set<Integer> selectedContacts = new HashSet<>();
  private final SparseIntArray actionModeSelection = new SparseIntArray();

  @Override
  public int getItemCount() {
    return dcContactList.length;
  }

  private @NonNull DcContact getContact(int position) {
    if (position < 0 || position >= dcContactList.length) {
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
    for (int index = 0; index < dcContactList.length; index++) {
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

  public class ViewHolder extends RecyclerView.ViewHolder {

    ViewHolder(@NonNull final View itemView, @Nullable final ItemClickListener clickListener) {
      super(itemView);
      itemView.setOnClickListener(
          view -> {
            if (clickListener != null) {
              if (isActionModeEnabled()) {
                toggleSelection();
                clickListener.onItemClick(getView(), true);
              } else {
                clickListener.onItemClick(getView(), false);
              }
            }
          });
      itemView.setOnLongClickListener(
          view -> {
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
      return ContactSelectionListAdapter.this.dcContactList[adapterPosition];
    }

    private void toggleSelection() {
      if (!longPressSelect) {
        return;
      }
      int adapterPosition = getBindingAdapterPosition();
      if (adapterPosition < 0) return;
      int contactId = getContactId(adapterPosition);
      boolean enabled = actionModeSelection.indexOfKey(adapterPosition) > -1;
      if (enabled) {
        ContactSelectionListAdapter.this.actionModeSelection.delete(adapterPosition);
      } else {
        ContactSelectionListAdapter.this.actionModeSelection.put(adapterPosition, contactId);
      }
      notifyDataSetChanged();
    }

    public ContactSelectionListItem getView() {
      return (ContactSelectionListItem) itemView;
    }
  }

  public ContactSelectionListAdapter(
      @NonNull Context context,
      @NonNull GlideRequests glideRequests,
      @Nullable ItemClickListener clickListener,
      boolean multiSelect,
      boolean longPressSelect) {
    super();
    this.context = context;
    this.dcContext = DcHelper.getContext(context);
    this.li = LayoutInflater.from(context);
    this.glideRequests = glideRequests;
    this.multiSelect = multiSelect;
    this.clickListener = clickListener;
    this.longPressSelect = longPressSelect;
  }

  @NonNull
  @Override
  public ContactSelectionListAdapter.ViewHolder onCreateViewHolder(
      @NonNull ViewGroup parent, int viewType) {
    return new ViewHolder(
        li.inflate(R.layout.contact_selection_list_item, parent, false), clickListener);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
    ContactSelectionListItem item = viewHolder.getView();
    item.unbind(glideRequests);
    int id = dcContactList[i];

    if (id == DcContact.DC_CONTACT_ID_INVITE_LINK && qrInviteData != null) {
      item.setSelected(false);
      item.setEnabled(!isActionModeEnabled());
      item.setQrInviteData(qrInviteData, DcContact.DC_CONTACT_ID_INVITE_LINK, glideRequests);
      return;
    }

    String title = null;
    if (id == DcContact.DC_CONTACT_ID_NEW_CLASSIC_CONTACT) {
      title = context.getString(R.string.menu_new_classic_contact);
    } else if (id == DcContact.DC_CONTACT_ID_NEW_GROUP) {
      title = context.getString(R.string.menu_new_group);
    } else if (id == DcContact.DC_CONTACT_ID_NEW_UNENCRYPTED_GROUP) {
      title = context.getString(R.string.new_email);
    } else if (id == DcContact.DC_CONTACT_ID_NEW_BROADCAST) {
      title = context.getString(R.string.new_channel);
    } else if (id == DcContact.DC_CONTACT_ID_QR_INVITE) {
      title = context.getString(R.string.menu_new_contact);
    }

    if (title == null) { // normal contact
      DcContact dcContact = getContact(i);
      item.setSelected(actionModeSelection.indexOfValue(id) >= 0);
      item.setChecked(selectedContacts.contains(id));
      boolean enabled = !(dcContact.getId() == DcContact.DC_CONTACT_ID_SELF && multiSelect);
      item.setEnabled(enabled);
      item.setContact(glideRequests, dcContact, multiSelect);
    } else { // special action/button
      item.setSelected(false);
      item.setChecked(false);
      item.setEnabled(!isActionModeEnabled());
      item.setSpecial(glideRequests, id, title);
    }
  }

  @Override
  public int getItemViewType(int i) {
    return VIEW_TYPE_CONTACT;
  }

  public Set<Integer> getSelectedContacts() {
    return selectedContacts;
  }

  public SparseIntArray getActionModeSelection() {
    return actionModeSelection;
  }

  public @Nullable QrInviteData getQrInviteData() {
    return qrInviteData;
  }

  public interface ItemClickListener {
    void onItemClick(ContactSelectionListItem item, boolean handleActionMode);

    void onItemLongClick(ContactSelectionListItem view);
  }

  public void changeData(DcContactsLoader.Ret loaderRet) {
    this.dcContactList = loaderRet == null ? new int[0] : loaderRet.ids;
    this.qrInviteData = loaderRet == null ? null : loaderRet.qrInviteData;
    recordCache.clear();
    notifyDataSetChanged();
  }
}
