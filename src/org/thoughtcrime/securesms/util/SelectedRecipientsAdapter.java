package org.thoughtcrime.securesms.util;

import android.content.Context;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.b44t.messenger.DcContact;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.guava.Optional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.b44t.messenger.DcContact.DC_CONTACT_ID_SELF;

public class SelectedRecipientsAdapter extends BaseAdapter {
  @NonNull  private Context                    context;
  @Nullable private OnRecipientDeletedListener onRecipientDeletedListener;
  @NonNull  private List<RecipientWrapper>     recipients;
  @NonNull  private final ApplicationDcContext dcContext;

  public SelectedRecipientsAdapter(@NonNull Context context) {
    this(context, Collections.<Recipient>emptyList());
  }

  public SelectedRecipientsAdapter(@NonNull Context context,
                                   @NonNull Collection<Recipient> existingRecipients)
  {
    this.context    = context;
    this.dcContext  = DcHelper.getContext(context);
    this.recipients = wrapExistingMembers(existingRecipients);
  }

  public void add(@NonNull Recipient recipient, boolean isPush) {
    if (!find(recipient).isPresent()) {
      boolean isModifiable = true;
      if (recipient.getAddress().getDcContactId() == DC_CONTACT_ID_SELF) {
        isModifiable = false;
      }
      RecipientWrapper wrapper = new RecipientWrapper(recipient, isModifiable, isPush);
      this.recipients.add(0, wrapper);
      notifyDataSetChanged();
    }
  }

  public Optional<RecipientWrapper> find(@NonNull Recipient recipient) {
    RecipientWrapper found = null;
    for (RecipientWrapper wrapper : recipients) {
      if (wrapper.getRecipient().equals(recipient)) found = wrapper;
    }
    return Optional.fromNullable(found);
  }

  public void remove(@NonNull Recipient recipient) {
    Optional<RecipientWrapper> match = find(recipient);
    if (match.isPresent()) {
      recipients.remove(match.get());
      notifyDataSetChanged();
    }
  }

  public void clear() {
    recipients.clear();
    notifyDataSetChanged();
  }

  public Set<Recipient> getRecipients() {
    final Set<Recipient> recipientSet = new HashSet<>(recipients.size());
    for (RecipientWrapper wrapper : recipients) {
      recipientSet.add(wrapper.getRecipient());
    }
    return recipientSet;
  }

  @Override
  public int getCount() {
    return recipients.size();
  }

  @Override
  public Object getItem(int position) {
    return recipients.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(final int position, View v, final ViewGroup parent) {
    if (v == null) {
      v = LayoutInflater.from(context).inflate(R.layout.selected_recipient_list_item, parent, false);
    }

    final RecipientWrapper rw         = (RecipientWrapper)getItem(position);
    final Recipient        p          = rw.getRecipient();
    final boolean          modifiable = rw.isModifiable();
    DcContact              dcContact  = new DcContact(0);

    if(p.getAddress().isDcContact()) {
      dcContact = dcContext.getContact(p.getAddress().getDcContactId());
    }

    TextView    name   = v.findViewById(R.id.name);
    TextView    phone  = v.findViewById(R.id.phone);
    ImageButton delete = v.findViewById(R.id.delete);

    name.setText(dcContact.getDisplayName());
    phone.setText(dcContact.getAddr());
    delete.setVisibility(modifiable ? View.VISIBLE : View.GONE);
    delete.setColorFilter(DynamicTheme.isDarkTheme(context)? Color.WHITE : Color.BLACK);
    delete.setOnClickListener(view -> {
      if (onRecipientDeletedListener != null) {
        onRecipientDeletedListener.onRecipientDeleted(recipients.get(position).getRecipient());
      }
    });

    return v;
  }

  private static List<RecipientWrapper> wrapExistingMembers(Collection<Recipient> recipients) {
    final LinkedList<RecipientWrapper> wrapperList = new LinkedList<>();
    for (Recipient recipient : recipients) {
      boolean isModifiable = true;
      if (recipient.getAddress().getDcContactId() == DC_CONTACT_ID_SELF) {
        isModifiable = false;
      }
      wrapperList.add(new RecipientWrapper(recipient, isModifiable, true));
    }
    return wrapperList;
  }

  public void setOnRecipientDeletedListener(@Nullable OnRecipientDeletedListener listener) {
    onRecipientDeletedListener = listener;
  }

  public interface OnRecipientDeletedListener {
    void onRecipientDeleted(Recipient recipient);
  }

  public static class RecipientWrapper {
    private final Recipient recipient;
    private final boolean   modifiable;
    private final boolean   push;

    public RecipientWrapper(final @NonNull Recipient recipient,
                            final boolean modifiable,
                            final boolean push)
    {
      this.recipient  = recipient;
      this.modifiable = modifiable;
      this.push       = push;
    }

    public @NonNull Recipient getRecipient() {
      return recipient;
    }

    public boolean isModifiable() {
      return modifiable;
    }

    public boolean isPush() {
      return push;
    }
  }
}