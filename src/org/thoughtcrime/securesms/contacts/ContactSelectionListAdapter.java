/**
 * Copyright (C) 2014 Open Whisper Systems
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
package org.thoughtcrime.securesms.contacts;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.b44t.messenger.DcChatlist;
import com.b44t.messenger.DcContact;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.RecyclerViewFastScroller.FastScrollAdapter;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcContactsLoader;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.ContactSelectionListAdapter.HeaderViewHolder;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.util.StickyHeaderDecoration.StickyHeaderAdapter;
import org.thoughtcrime.securesms.util.Util;

import java.util.HashSet;
import java.util.Set;

/**
 * List adapter to display all contacts and their related information
 *
 * @author Jake McGinty
 */
public class ContactSelectionListAdapter extends RecyclerView.Adapter
                                         implements FastScrollAdapter,
                                                    StickyHeaderAdapter<HeaderViewHolder>
{
  private final static String TAG = ContactSelectionListAdapter.class.getSimpleName();

  private static final int VIEW_TYPE_CONTACT = 0;
  private static final int VIEW_TYPE_DIVIDER = 1;

  private final static int STYLE_ATTRIBUTES[] = new int[]{R.attr.contact_selection_push_user,
                                                          R.attr.contact_selection_lay_user};

  private final @NonNull Context              context;
  private final @NonNull ApplicationDcContext dcContext;
  private @NonNull int[]                      dcContactList = new int[0];
  private String                              query;
  private final boolean                       multiSelect;
  private final LayoutInflater                li;
  private final TypedArray                    drawables;
  private final ItemClickListener             clickListener;
  private final GlideRequests                 glideRequests;
  private final Set<String>                   selectedContacts = new HashSet<>(); // TODO: maybe better use contact-id here

  public abstract static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View itemView) {
      super(itemView);
    }

    public abstract void bind(@NonNull GlideRequests glideRequests, int type, DcContact contact, String name, String number, String label, int color, boolean multiSelect);
    public abstract void unbind(@NonNull GlideRequests glideRequests);
    public abstract void setChecked(boolean checked);
  }

  public static class ContactViewHolder extends ViewHolder {
    ContactViewHolder(@NonNull  final View itemView,
                      @Nullable final ItemClickListener clickListener)
    {
      super(itemView);
      itemView.setOnClickListener(v -> {
        if (clickListener != null) clickListener.onItemClick(getView());
      });
    }

    public ContactSelectionListItem getView() {
      return (ContactSelectionListItem) itemView;
    }

    public void bind(@NonNull GlideRequests glideRequests, int type, DcContact contact, String name, String addr, String label, int color, boolean multiSelect) {
      getView().set(glideRequests, type, contact, name, addr, label, color, multiSelect);
    }

    @Override
    public void unbind(@NonNull GlideRequests glideRequests) {
      getView().unbind(glideRequests);
    }

    @Override
    public void setChecked(boolean checked) {
      getView().setChecked(checked);
    }
  }

  public static class DividerViewHolder extends ViewHolder {

    private final TextView label;

    DividerViewHolder(View itemView) {
      super(itemView);
      this.label = itemView.findViewById(R.id.label);
    }

    @Override
    public void bind(@NonNull GlideRequests glideRequests, int type, DcContact contact, String name, String number, String label, int color, boolean multiSelect) {
      this.label.setText(name);
    }

    @Override
    public void unbind(@NonNull GlideRequests glideRequests) {}

    @Override
    public void setChecked(boolean checked) {}
  }

  static class HeaderViewHolder extends RecyclerView.ViewHolder {
    HeaderViewHolder(View itemView) {
      super(itemView);
    }
  }

  public ContactSelectionListAdapter(@NonNull  Context context,
                                     @NonNull  GlideRequests glideRequests,
                                     @Nullable ItemClickListener clickListener,
                                     boolean multiSelect)
  {
    super();
    this.context       = context;
    this.dcContext     = DcHelper.getContext(context);
    this.li            = LayoutInflater.from(context);
    this.glideRequests = glideRequests;
    this.drawables     = context.obtainStyledAttributes(STYLE_ATTRIBUTES);
    this.multiSelect   = multiSelect;
    this.clickListener = clickListener;
  }

  @Override
  public int getItemCount() {
    return dcContactList.length;
  }

  @Override
  public long getHeaderId(int i) {
    return -1;
    /* TODO: what should be done here?
    if (!isActiveCursor()) return -1;

    int contactType = getContactType(i);

    if (contactType == ContactsDatabase.DIVIDER_TYPE) return -1;
    return Util.hashCode(getHeaderString(i), getContactType(i));
    */
  }

  @Override
  public ContactSelectionListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == VIEW_TYPE_CONTACT) {
      return new ContactViewHolder(li.inflate(R.layout.contact_selection_list_item, parent, false), clickListener);
    } else {
      return new DividerViewHolder(li.inflate(R.layout.contact_selection_list_divider, parent, false));
    }
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {

    int       id        = dcContactList[i];
    int       color     = drawables.getColor(0, 0xff000000);
    DcContact dcContact = null;
    String    label     = null;
    String    name;
    String    addr = null;

    if(id==DcContact.DC_CONTACT_ID_NEW_CONTACT) {
      name = context.getString(R.string.contact_selection_list__new_contact);
      if(dcContext.mayBeValidAddr(query)) {
        addr = query == null ? "" : query;
        label = "\u2026";
      }
      else {
        addr = context.getString(R.string.contact_selection_list__type_email_above);
      }
    }
    else if(id==DcContact.DC_CONTACT_ID_NEW_GROUP) {
      name = context.getString(R.string.contact_selection_list__new_group);
    }
    else if(id==DcContact.DC_CONTACT_ID_NEW_VERIFIED_GROUP) {
      name = context.getString(R.string.contact_selection_list__new_verified_group);
    }
    else {
      dcContact = dcContext.getContact(id);
      name      = dcContact.getDisplayName();
      addr      = dcContact.getAddr();
    }

    ViewHolder holder = (ViewHolder)viewHolder;
    holder.unbind(glideRequests);
    holder.bind(glideRequests, id, dcContact, name, addr, label, color, multiSelect);
    holder.setChecked(selectedContacts.contains(addr));
  }

  @Override
  public int getItemViewType(int i) {
    return VIEW_TYPE_CONTACT;
  }

  @Override
  public HeaderViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
    return new HeaderViewHolder(LayoutInflater.from(context).inflate(R.layout.contact_selection_recyclerview_header, parent, false));
  }

  @Override
  public void onBindHeaderViewHolder(HeaderViewHolder viewHolder, int position) {
    ((TextView)viewHolder.itemView).setText(getSpannedHeaderString(position));
  }

  @Override
  public CharSequence getBubbleText(int position) {
    return getHeaderString(position);
  }

  public Set<String> getSelectedContacts() {
    return selectedContacts;
  }

  private CharSequence getSpannedHeaderString(int position) {
    final String headerString = getHeaderString(position);
    /* TODO: what should be done here?
    if (isPush(position)) {
      SpannableString spannable = new SpannableString(headerString);
      spannable.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.signal_primary)), 0, headerString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      return spannable;
    } else*/ {
      return headerString;
    }
  }

  private @NonNull String getHeaderString(int position) {
    return " ";
    /* TODO: what should be done here?
    int contactType = getContactType(position);

    if (contactType == ContactsDatabase.RECENT_TYPE || contactType == ContactsDatabase.DIVIDER_TYPE) {
      return " ";
    }

    Cursor cursor = getCursorAtPositionOrThrow(position);
    String letter = cursor.getString(cursor.getColumnIndexOrThrow(ContactsDatabase.NAME_COLUMN));

    if (!TextUtils.isEmpty(letter)) {
      String firstChar = letter.trim().substring(0, 1).toUpperCase();
      if (Character.isLetterOrDigit(firstChar.codePointAt(0))) {
        return firstChar;
      }
    }

    return "#";
    */
  }

  public interface ItemClickListener {
    void onItemClick(ContactSelectionListItem item);
  }

  public void changeData(DcContactsLoader.Ret loaderRet) {
    this.dcContactList = loaderRet==null? new int[0] : loaderRet.ids;
    this.query = loaderRet==null? null : loaderRet.query;
    notifyDataSetChanged();
  }
}
