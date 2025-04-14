package org.thoughtcrime.securesms.accounts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.b44t.messenger.DcAccounts;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideRequests;

public class AccountSelectionListAdapter extends RecyclerView.Adapter<AccountSelectionListAdapter.AccountViewHolder>
{
  private final @NonNull AccountSelectionListFragment fragment;
  private final @NonNull DcAccounts           accounts;
  private @NonNull int[]                      accountList = new int[0];
  private int                                 selectedAccountId;
  private final LayoutInflater                li;
  private final ItemClickListener             clickListener;
  private final GlideRequests                 glideRequests;

  @Override
  public int getItemCount() {
    return accountList.length;
  }

  public static class AccountViewHolder extends RecyclerView.ViewHolder {
    AccountViewHolder(@NonNull  final View itemView,
                      @Nullable final ItemClickListener clickListener) {
      super(itemView);
      itemView.setOnClickListener(view -> {
        if (clickListener != null) {
          clickListener.onItemClick(getView());
        }
      });
    }

    public AccountSelectionListItem getView() {
      return (AccountSelectionListItem) itemView;
    }

    public void bind(@NonNull GlideRequests glideRequests, int accountId, DcContext dcContext, boolean selected, AccountSelectionListFragment fragment) {
      getView().bind(glideRequests, accountId, dcContext, selected, fragment);
    }

    public void unbind(@NonNull GlideRequests glideRequests) {
      getView().unbind(glideRequests);
    }
  }

  public AccountSelectionListAdapter(@NonNull  AccountSelectionListFragment fragment,
                                     @NonNull  GlideRequests glideRequests,
                                     @Nullable ItemClickListener clickListener)
  {
    super();
    Context context    = fragment.requireActivity();
    this.fragment      = fragment;
    this.accounts      = DcHelper.getAccounts(context);
    this.li            = LayoutInflater.from(context);
    this.glideRequests = glideRequests;
    this.clickListener = clickListener;
  }

  @NonNull
  @Override
  public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new AccountViewHolder(li.inflate(R.layout.account_selection_list_item, parent, false), clickListener);
  }

  @Override
  public void onBindViewHolder(@NonNull AccountViewHolder holder, int i) {
    int id = accountList[i];
    DcContext dcContext = accounts.getAccount(id);

    holder.unbind(glideRequests);
    holder.bind(glideRequests, id, dcContext, id == selectedAccountId, fragment);
  }

  public interface ItemClickListener {
    void onItemClick(AccountSelectionListItem item);
  }

  public void changeData(int[] ids, int selectedAccountId) {
    this.accountList = ids==null? new int[0] : ids;
    this.selectedAccountId = selectedAccountId;
    notifyDataSetChanged();
  }
}
