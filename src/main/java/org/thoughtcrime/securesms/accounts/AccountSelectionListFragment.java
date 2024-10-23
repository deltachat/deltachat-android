package org.thoughtcrime.securesms.accounts;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.b44t.messenger.DcAccounts;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.rpc.Rpc;
import com.b44t.messenger.rpc.RpcException;

import org.thoughtcrime.securesms.ConnectivityActivity;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.AvatarView;
import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

import static com.b44t.messenger.DcContact.DC_CONTACT_ID_ADD_ACCOUNT;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_PRIVATE_TAG;

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
    int j = 0;
    for (int accountId : accountIds) {
      ids[j++] = accountId;
    }
    ids[j] = DC_CONTACT_ID_ADD_ACCOUNT;
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

    AccountSelectionListItem listItem = (AccountSelectionListItem) v;
    int accountId = listItem.getAccountId();
    DcAccounts dcAccounts = DcHelper.getAccounts(getActivity());

    Util.redMenuItem(menu, R.id.delete);

    if (dcAccounts.getAccount(accountId).isMuted()) {
      menu.findItem(R.id.menu_mute_notifications).setTitle(R.string.menu_unmute);
    }

    // hack to make onContextItemSelected() work with DialogFragment,
    // see https://stackoverflow.com/questions/15929026/oncontextitemselected-does-not-get-called-in-a-dialogfragment
    MenuItem.OnMenuItemClickListener listener = item -> {
      onContextItemSelected(item, accountId);
      return true;
    };
    for (int i = 0, n = menu.size(); i < n; i++) {
      menu.getItem(i).setOnMenuItemClickListener(listener);
    }
    // /hack
  }

  private void onContextItemSelected(MenuItem item, int accountId) {
    switch (item.getItemId()) {
    case R.id.delete:
      onDeleteAccount(accountId);
      break;
    case R.id.menu_mute_notifications:
      onToggleMute(accountId);
      break;
    case R.id.menu_set_tag:
      onSetTag(accountId);
      break;
    }
  }

  private void onSetTag(int accountId) {
    Activity activity = getActivity();
    if (activity == null) return;
    AccountSelectionListFragment.this.dismiss();

    DcContext dcContext = DcHelper.getAccounts(activity).getAccount(accountId);
    View view = View.inflate(activity, R.layout.single_line_input, null);
    EditText inputField = view.findViewById(R.id.input_field);
    inputField.setHint(R.string.profile_tag_hint);
    inputField.setText(dcContext.getConfig(CONFIG_PRIVATE_TAG));

    new AlertDialog.Builder(activity)
      .setTitle(R.string.profile_tag)
      .setMessage(R.string.profile_tag_explain)
      .setView(view)
      .setPositiveButton(android.R.string.ok, (d, b) -> {
        String newTag = inputField.getText().toString().trim();
        dcContext.setConfig(CONFIG_PRIVATE_TAG, newTag);
        AccountManager.getInstance().showSwitchAccountMenu(activity);
      })
      .setNegativeButton(R.string.cancel, (d, b) -> AccountManager.getInstance().showSwitchAccountMenu(activity))
      .show();
  }

  private void onDeleteAccount(int accountId) {
    Activity activity = getActivity();
    AccountSelectionListFragment.this.dismiss();
    if (activity == null) return;
    DcAccounts accounts = DcHelper.getAccounts(activity);
    Rpc rpc = DcHelper.getRpc(activity);

    View dialogView = View.inflate(activity, R.layout.dialog_delete_profile, null);
    AvatarView avatar = dialogView.findViewById(R.id.avatar);
    TextView nameView = dialogView.findViewById(R.id.name);
    TextView addrView = dialogView.findViewById(R.id.address);
    TextView sizeView = dialogView.findViewById(R.id.size_label);
    TextView description = dialogView.findViewById(R.id.description);
    DcContext dcContext = accounts.getAccount(accountId);
    String name = dcContext.getConfig("displayname");
    DcContact contact = dcContext.getContact(DcContact.DC_CONTACT_ID_SELF);
    if (TextUtils.isEmpty(name)) {
      name = contact.getAddr();
    }
    Recipient recipient = new Recipient(getContext(), contact, name);
    avatar.setAvatar(GlideApp.with(activity), recipient, false);
    nameView.setText(name);
    addrView.setText(contact.getAddr());
    try {
      sizeView.setText(Util.getPrettyFileSize(rpc.getAccountFileSize(accountId)));
    } catch (RpcException e) {
      e.printStackTrace();
    }
    description.setText(activity.getString(R.string.delete_account_explain_with_name, name));

    AlertDialog dialog = new AlertDialog.Builder(activity)
      .setTitle(R.string.delete_account)
      .setView(dialogView)
      .setNegativeButton(R.string.cancel, (d, which) -> AccountManager.getInstance().showSwitchAccountMenu(activity))
      .setPositiveButton(R.string.delete, (d2, which2) -> {
          boolean selected = accountId == accounts.getSelectedAccount().getAccountId();
          DcHelper.getNotificationCenter(activity).removeAllNotifications(accountId);
          accounts.removeAccount(accountId);
          if (selected) {
            DcContext selAcc = accounts.getSelectedAccount();
            AccountManager.getInstance().switchAccountAndStartActivity(activity, selAcc.isOk()? selAcc.getAccountId() : 0);
          } else {
            AccountManager.getInstance().showSwitchAccountMenu(activity);
          }

          // title update needed to show "Delta Chat" in case there is only one profile left
          if (activity instanceof ConversationListActivity) {
            ((ConversationListActivity)activity).refreshTitle();
          }
      })
      .show();
    Util.redPositiveButton(dialog);
  }

  private void onToggleMute(int accountId) {
    DcAccounts dcAccounts = DcHelper.getAccounts(getActivity());
    DcContext dcContext = dcAccounts.getAccount(accountId);
    dcContext.setMuted(!dcContext.isMuted());
    recyclerView.getAdapter().notifyDataSetChanged();
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
