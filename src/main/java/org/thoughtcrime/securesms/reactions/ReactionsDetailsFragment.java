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

import org.thoughtcrime.securesms.ProfileActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.util.Pair;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;
import chat.delta.rpc.types.Reactions;

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
            .setTitle(R.string.reactions)
            .setNegativeButton(R.string.ok, null);
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
      final Reactions reactions = DcHelper.getRpc(requireActivity()).getMessageReactions(accId, msgId);
      ArrayList<Pair<Integer, String>> contactsReactions = new ArrayList<>();
      if (reactions != null) {
        Map<String, List<String>> reactionsByContact = reactions.reactionsByContact;
        List<String> selfReactions = reactionsByContact.remove(String.valueOf(DcContact.DC_CONTACT_ID_SELF));
        for (String contact: reactionsByContact.keySet()) {
          for (String reaction: reactionsByContact.get(contact)) {
            contactsReactions.add(new Pair<>(Integer.parseInt(contact), reaction));
          }
        }
        if (selfReactions != null) {
          for (String reaction: selfReactions) {
            contactsReactions.add(new Pair<>(DcContact.DC_CONTACT_ID_SELF, reaction));
          }
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

  private String getSelfReaction(Rpc rpc, int accId) {
    String result = null;
    try {
      final Reactions reactions = rpc.getMessageReactions(accId, msgId);
      if (reactions != null) {
        final Map<String, List<String>> reactionsByContact = reactions.reactionsByContact;
        final List<String> selfReactions = reactionsByContact.get(String.valueOf(DcContact.DC_CONTACT_ID_SELF));
        if (selfReactions != null && !selfReactions.isEmpty()) {
          result = selfReactions.get(0);
        }
      }
    } catch(RpcException e) {
      e.printStackTrace();
    }
    return result;
  }

  private void sendReaction(final String reaction) {
    Rpc rpc = DcHelper.getRpc(requireActivity());
    DcContext dcContext = DcHelper.getContext(requireActivity());
    int accId = dcContext.getAccountId();

    try {
      if (reaction == null || reaction.equals(getSelfReaction(rpc, accId))) {
        rpc.sendReaction(accId, msgId, Collections.singletonList(""));
      } else {
        rpc.sendReaction(accId, msgId, Collections.singletonList(reaction));
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
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

    @Override
    public void onReactionClick(ReactionRecipientItem item) {
      sendReaction(item.getReaction());
      ReactionsDetailsFragment.this.dismiss();
    }
  }

}
