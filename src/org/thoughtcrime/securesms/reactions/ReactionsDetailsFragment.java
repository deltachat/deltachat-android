package org.thoughtcrime.securesms.reactions;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.rpc.RpcException;

import org.thoughtcrime.securesms.ProfileActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.util.Pair;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.ArrayList;
import java.util.Map;

public class ReactionsDetailsFragment extends DialogFragment implements DcEventCenter.DcEventDelegate {
  private static final String TAG = ReactionsDetailsFragment.class.getSimpleName();

  private RecyclerView recyclerView;
  private ReactionRecipientsAdapter adapter;
  private final int msgId;

  public ReactionsDetailsFragment(int msgId) {
    super();
    this.msgId = msgId;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    adapter = new ReactionRecipientsAdapter(requireActivity(), GlideApp.with(requireActivity()), new ListClickListener());

    LayoutInflater inflater = requireActivity().getLayoutInflater();
    View view = inflater.inflate(R.layout.reactions_details_fragment, null);
    recyclerView = ViewUtil.findById(view, R.id.recycler_view);
    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

    recyclerView.setAdapter(adapter);

    refreshData();

    DcEventCenter eventCenter = DcHelper.getEventCenter(requireContext());
    eventCenter.addObserver(DcContext.DC_EVENT_REACTIONS_CHANGED, this);

    AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity())
            .setTitle(R.string.reactions_details_title)
            .setNegativeButton(R.string.cancel, null);
    return builder.setView(view).create();
  }

  @Override
  public void onDestroy() {
    Log.i(TAG, "onDestroy()");
    super.onDestroy();
    DcHelper.getEventCenter(requireActivity()).removeObservers(this);
  }

  @Override
  public void handleEvent(@NonNull DcEvent event) {
    if (event.getId() == DcContext.DC_EVENT_REACTIONS_CHANGED) {
      if (event.getData2Int() == msgId) {
        refreshData();
      }
    }
  }

  private void refreshData() {
    if (recyclerView == null) return;

    int accId = DcHelper.getContext(requireActivity()).getAccountId();
    try {
      Map<Integer, String[]> reactionsByContact = DcHelper.getRpc(requireActivity()).getMsgReactions(accId, msgId).getReactionsByContact();
      ArrayList<Pair<Integer, String>> contactsReactions = new ArrayList<>();
      String[] selfReactions = reactionsByContact.remove(DcContact.DC_CONTACT_ID_SELF);
      for (Integer contact: reactionsByContact.keySet()) {
        for (String reaction: reactionsByContact.get(contact)) {
          contactsReactions.add(new Pair<>(contact, reaction));
        }
      }
      if (selfReactions != null) {
        for (String reaction: selfReactions) {
          contactsReactions.add(new Pair<>(DcContact.DC_CONTACT_ID_SELF, reaction));
        }
      }
      adapter.changeData(contactsReactions);
    } catch (RpcException e) {
      e.printStackTrace();
    }
  }

  private void openConversation(int contactId) {
    Intent intent = new Intent(getContext(), ProfileActivity.class);
    intent.putExtra(ProfileActivity.CONTACT_ID_EXTRA, contactId);
    requireContext().startActivity(intent);
  }

  private class ListClickListener implements ReactionRecipientsAdapter.ItemClickListener {

    @Override
    public void onItemClick(ReactionRecipientItem item) {
        int contactId = item.getContactId();
        if (contactId != DcContact.DC_CONTACT_ID_SELF) {
          ReactionsDetailsFragment.this.dismiss();
          openConversation(contactId);
        }
    }
  }

}
