package org.thoughtcrime.securesms.util;

import android.content.Context;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.AvatarImageView;
import org.thoughtcrime.securesms.components.emoji.EmojiTextView;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.b44t.messenger.DcContact.DC_CONTACT_ID_SELF;
import static com.b44t.messenger.DcContact.DC_CONTACT_ID_ADD_MEMBER;

public class SelectedContactsAdapter extends BaseAdapter {
  @NonNull  private final Context                context;
  @Nullable private ItemClickListener            itemClickListener;
  @NonNull  private final List<Integer>          contacts = new LinkedList<>();
  private final boolean                          isBroadcast;
  @NonNull  private final DcContext              dcContext;
  @NonNull  private final GlideRequests          glideRequests;

  public SelectedContactsAdapter(@NonNull Context context,
                                   @NonNull  GlideRequests glideRequests,
                                   boolean isBroadcast)
  {
    this.context       = context;
    this.glideRequests = glideRequests;
    this.isBroadcast   = isBroadcast;
    this.dcContext     = DcHelper.getContext(context);
  }

  public void changeData(Collection<Integer> contactIds) {
    contacts.clear();
    contacts.add(DC_CONTACT_ID_ADD_MEMBER);
    if (contactIds != null) {
      for (int id : contactIds) {
        if (id != DC_CONTACT_ID_SELF) {
          contacts.add(id);
        }
      }
    }
    if (!isBroadcast) {
      contacts.add(DC_CONTACT_ID_SELF);
    }
    notifyDataSetChanged();
  }

  public void remove(@NonNull Integer contactId) {
    if (contacts.remove(contactId)) {
      notifyDataSetChanged();
    }
  }

  public Set<Integer> getContacts() {
    final Set<Integer> set = new HashSet<>(contacts.size()-1);
    for (int i = 1; i < contacts.size(); i++) {
      set.add(contacts.get(i));
    }
    return set;
  }

  @Override
  public int getCount() {
    return contacts.size();
  }

  @Override
  public Object getItem(int position) {
    return contacts.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(final int position, View v, final ViewGroup parent) {
    if (v == null) {
      v = LayoutInflater.from(context).inflate(R.layout.selected_contact_list_item, parent, false);
    }

    AvatarImageView avatar = v.findViewById(R.id.contact_photo_image);
    EmojiTextView   name   = v.findViewById(R.id.name);
    TextView        phone  = v.findViewById(R.id.phone);
    ImageButton     delete = v.findViewById(R.id.delete);

    final int contactId = (int)getItem(position);
    final boolean modifiable = contactId != DC_CONTACT_ID_ADD_MEMBER && contactId != DC_CONTACT_ID_SELF;
    Recipient recipient = null;
    boolean verified = false;

    if(contactId == DcContact.DC_CONTACT_ID_ADD_MEMBER) {
      name.setText(context.getString(isBroadcast? R.string.add_recipients : R.string.group_add_members));
      name.setTypeface(null, Typeface.BOLD);
      phone.setVisibility(View.GONE);
    } else {
      DcContact dcContact = dcContext.getContact(contactId);
      recipient = new Recipient(context, dcContact);
      name.setText(dcContact.getDisplayName());
      name.setTypeface(null, Typeface.NORMAL);
      phone.setText(dcContact.getAddr());
      phone.setVisibility(View.VISIBLE);
      verified = dcContact.isVerified();
    }

    avatar.clear(glideRequests);
    avatar.setAvatar(glideRequests, recipient, false);
    name.setCompoundDrawablesWithIntrinsicBounds(0, 0, verified? R.drawable.ic_verified : 0, 0);
    delete.setVisibility(modifiable ? View.VISIBLE : View.GONE);
    delete.setColorFilter(DynamicTheme.isDarkTheme(context)? Color.WHITE : Color.BLACK);
    delete.setOnClickListener(view -> {
      if (itemClickListener != null) {
        itemClickListener.onItemDeleteClick(contacts.get(position));
      }
    });
    v.setOnClickListener(view -> {
      if (itemClickListener != null) {
        itemClickListener.onItemClick(contacts.get(position));
      }
    });

    return v;
  }

  public void setItemClickListener(@Nullable ItemClickListener listener) {
    itemClickListener = listener;
  }

  public interface ItemClickListener {
    void onItemClick(int contactId);
    void onItemDeleteClick(int contactId);
  }
}
