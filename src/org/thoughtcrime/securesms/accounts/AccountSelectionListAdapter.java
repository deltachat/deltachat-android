package org.thoughtcrime.securesms.accounts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.b44t.messenger.DcAccounts;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideRequests;

public class AccountSelectionListAdapter extends RecyclerView.Adapter
{
  private final static String TAG = AccountSelectionListAdapter.class.getSimpleName();

  private final @NonNull Context              context;
  private final @NonNull DcAccounts           accounts;
  private @NonNull int[]                      accountList = new int[0];
  private final LayoutInflater                li;
  private final ItemClickListener             clickListener;
  private final GlideRequests                 glideRequests;

  @Override
  public int getItemCount() {
    return accountList.length;
  }

  public abstract static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View itemView) {
      super(itemView);
    }

    public abstract void bind(@NonNull GlideRequests glideRequests, int accountId, DcContact self, String name, String addr, ItemClickListener listener);
    public abstract void unbind(@NonNull GlideRequests glideRequests);
    }

  public class AccountViewHolder extends ViewHolder {

    AccountViewHolder(@NonNull  final View itemView) {
      super(itemView);
    }

    public AccountSelectionListItem getView() {
      return (AccountSelectionListItem) itemView;
    }

    public void bind(@NonNull GlideRequests glideRequests, int accountId, DcContact self, String name, String addr, ItemClickListener listener) {
      getView().bind(glideRequests, accountId, self, name, addr, listener);
    }

    @Override
    public void unbind(@NonNull GlideRequests glideRequests) {
      getView().unbind(glideRequests);
    }
  }

  public AccountSelectionListAdapter(@NonNull  Context context,
                                     @NonNull  GlideRequests glideRequests,
                                     @Nullable ItemClickListener clickListener)
  {
    super();
    this.context       = context;
    this.accounts      = DcHelper.getAccounts(context);
    this.li            = LayoutInflater.from(context);
    this.glideRequests = glideRequests;
    this.clickListener = clickListener;
  }

  @Override
  public AccountSelectionListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new AccountViewHolder(li.inflate(R.layout.account_selection_list_item, parent, false));
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {

    int id = accountList[i];
    DcContact dcContact = null;
    String name;
    String addr = null;

    if (id == DcContact.DC_CONTACT_ID_ADD_ACCOUNT) {
      name = context.getString(R.string.add_account);
    } else {
      DcContext dcContext = accounts.getAccount(id);
      dcContact = dcContext.getContact(DcContact.DC_CONTACT_ID_SELF);
      addr = dcContact.getAddr();
      name = dcContext.getConfig("displayname");
      if (name == "") {
        name = addr;
      }
    }

    ViewHolder holder = (ViewHolder) viewHolder;
    holder.unbind(glideRequests);
    holder.bind(glideRequests, id, dcContact, name, addr, clickListener);
  }

  public interface ItemClickListener {
    void onItemClick(AccountSelectionListItem item);
    void onDeleteButtonClick(int accountId);
  }

  public void changeData(int[] ids) {
    this.accountList = ids==null? new int[0] : ids;
    notifyDataSetChanged();
  }
}
