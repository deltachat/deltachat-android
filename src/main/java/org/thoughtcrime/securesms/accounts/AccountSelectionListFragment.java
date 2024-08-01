package org.thoughtcrime.securesms.accounts;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.b44t.messenger.DcAccounts;

import org.thoughtcrime.securesms.ConnectivityActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.util.ViewUtil;

import static com.b44t.messenger.DcContact.DC_CONTACT_ID_ADD_ACCOUNT;

public class AccountSelectionListFragment extends DialogFragment
{
  private RecyclerView recyclerView;

  @Override
  public void onActivityCreated(Bundle icicle) {
    super.onActivityCreated(icicle);
    AccountSelectionListAdapter adapter = new AccountSelectionListAdapter(this,
            GlideApp.with(getActivity()),
            new ListClickListener());
    recyclerView.setAdapter(adapter);

    DcAccounts accounts = DcHelper.getAccounts(getActivity());
    int[] accountIds = accounts.getAll();

    int[] ids = new int[accountIds.length + 1];
    ids[0] = DC_CONTACT_ID_ADD_ACCOUNT;
    int j = 0;
    for (int accountId : accountIds) {
      ids[++j] = accountId;
    }
    adapter.changeData(ids, accounts.getSelectedAccount().getAccountId());
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
            .setTitle(R.string.switch_account)
            .setNeutralButton(R.string.connectivity, ((dialog, which) -> {
              startActivity(new Intent(getActivity(), ConnectivityActivity.class));
            }))
            .setNegativeButton(R.string.cancel, null);

    LayoutInflater inflater = getActivity().getLayoutInflater();
    View view = inflater.inflate(R.layout.account_selection_list_fragment, null);
    recyclerView = ViewUtil.findById(view, R.id.recycler_view);
    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

    return builder.setView(view).create();
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    requireActivity().getMenuInflater().inflate(R.menu.account_item_context, menu);

    // hack to make onContextItemSelected() work with DialogFragment,
    // see https://stackoverflow.com/questions/15929026/oncontextitemselected-does-not-get-called-in-a-dialogfragment
    MenuItem.OnMenuItemClickListener listener = item -> {
      onContextItemSelected(item, v);
      return true;
    };
    for (int i = 0, n = menu.size(); i < n; i++) {
      menu.getItem(i).setOnMenuItemClickListener(listener);
    }
    // /hack
  }

  private void onContextItemSelected(MenuItem item, View view) {
    switch (item.getItemId()) {
    case R.id.delete:
      onDeleteAccount(((AccountSelectionListItem) view).getAccountId());
      break;
    }
  }

  private void onDeleteAccount(int accountId) {
    Activity activity = getActivity();
    AccountSelectionListFragment.this.dismiss();
    if (activity == null) return;
    DcAccounts accounts = DcHelper.getAccounts(activity);
    new AlertDialog.Builder(activity)
      .setTitle(accounts.getAccount(accountId).getNameNAddr())
      .setMessage(R.string.forget_login_confirmation_desktop)
      .setNegativeButton(R.string.cancel, (dialog, which) -> AccountManager.getInstance().showSwitchAccountMenu(activity))
      .setPositiveButton(R.string.ok, (dialog2, which2) -> {
          DcHelper.getNotificationCenter(activity).removeAllNotifiations(accountId);
          accounts.removeAccount(accountId);
          AccountManager.getInstance().showSwitchAccountMenu(activity);
      })
      .show();
  }

  private class ListClickListener implements AccountSelectionListAdapter.ItemClickListener {

    @Override
    public void onItemClick(AccountSelectionListItem contact) {
      Activity activity = getActivity();
      AccountSelectionListFragment.this.dismiss();
      int accountId = contact.getAccountId();
      if (accountId == DC_CONTACT_ID_ADD_ACCOUNT) {
        AccountManager.getInstance().switchAccountAndStartActivity(activity, 0);
      } else if (accountId != DcHelper.getAccounts(activity).getSelectedAccount().getAccountId()) {
        AccountManager.getInstance().switchAccountAndStartActivity(activity, accountId);
      }
    }
  }

}
