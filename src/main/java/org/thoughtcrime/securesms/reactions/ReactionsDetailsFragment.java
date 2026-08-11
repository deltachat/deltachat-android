package org.thoughtcrime.securesms.reactions;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;
import chat.delta.rpc.types.Reaction;
import chat.delta.rpc.types.Reactions;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.thoughtcrime.securesms.ProfileActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.util.Pair;
import org.thoughtcrime.securesms.util.ViewUtil;

public class ReactionsDetailsFragment extends BottomSheetDialogFragment
    implements DcEventCenter.DcEventDelegate {
  private static final String TAG = "ReactionsDetailsFragment";
  private static final String ARG_MSG_ID = "msg_id";
  private static final String ARG_IS_BROADCAST = "is_broadcast";

  private RecyclerView recyclerView;
  private ChipGroup pillsContainer;
  private TextView titleView;
  private ReactionRecipientsAdapter adapter;
  private int msgId;
  private boolean isBroadcast;

  public static ReactionsDetailsFragment newInstance(int msgId, boolean isBroadcast) {
    ReactionsDetailsFragment fragment = new ReactionsDetailsFragment();
    Bundle args = new Bundle();
    args.putInt(ARG_MSG_ID, msgId);
    args.putBoolean(ARG_IS_BROADCAST, isBroadcast);
    fragment.setArguments(args);
    return fragment;
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.reactions_details_fragment, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    msgId = getArguments() != null ? getArguments().getInt(ARG_MSG_ID, 0) : 0;
    isBroadcast = getArguments() != null && getArguments().getBoolean(ARG_IS_BROADCAST, false);

    adapter =
        new ReactionRecipientsAdapter(
            requireActivity(), GlideApp.with(requireActivity()), new ListClickListener());

    recyclerView = view.findViewById(R.id.recycler_view);
    pillsContainer = view.findViewById(R.id.reactions_pills_container);
    titleView = view.findViewById(R.id.reactions_title);

    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    recyclerView.setAdapter(adapter);
    recyclerView.setVisibility(isBroadcast ? View.GONE : View.VISIBLE);

    refreshData();

    DcEventCenter eventCenter = DcHelper.getEventCenter(requireContext());
    eventCenter.addObserver(DcContext.DC_EVENT_REACTIONS_CHANGED, this);
  }

  @Override
  public void onStart() {
    super.onStart();
    BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
    if (dialog != null) {
      View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
      if (bottomSheet != null) {
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        MaterialShapeDrawable shapeDrawable = new MaterialShapeDrawable();
        shapeDrawable.setShapeAppearanceModel(
            shapeDrawable.getShapeAppearanceModel().toBuilder()
                .setTopLeftCorner(CornerFamily.ROUNDED, ViewUtil.dpToPx(18))
                .setTopRightCorner(CornerFamily.ROUNDED, ViewUtil.dpToPx(18))
                .build());

        TypedValue typedValue = new TypedValue();
        if (requireContext()
            .getTheme()
            .resolveAttribute(R.attr.dialog_background_color, typedValue, true)) {
          shapeDrawable.setFillColor(ColorStateList.valueOf(typedValue.data));
        } else {
          shapeDrawable.setFillColor(
              ColorStateList.valueOf(getResources().getColor(R.color.background_material_light)));
        }

        bottomSheet.setBackground(shapeDrawable);
        bottomSheet.setClipToOutline(true);
      }
    }
  }

  private static View buildPill(
      @NonNull Context context, @NonNull ViewGroup parent, @NonNull Reaction reaction) {
    View root = LayoutInflater.from(context).inflate(R.layout.reaction_pill, parent, false);
    AppCompatTextView emojiView = root.findViewById(R.id.reaction_pill_emoji);
    TextView countView = root.findViewById(R.id.reaction_pill_count);

    emojiView.setText(reaction.emoji);
    countView.setText(String.valueOf(reaction.count));

    if (reaction.isFromSelf) {
      root.setBackground(
          ContextCompat.getDrawable(context, R.drawable.reaction_pill_background_selected));
      countView.setTextColor(
          ContextCompat.getColor(context, R.color.reaction_pill_text_color_selected));
    } else {
      root.setBackground(ContextCompat.getDrawable(context, R.drawable.reaction_pill_background));
    }

    return root;
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
    if (recyclerView == null || pillsContainer == null || titleView == null) return;

    int accId = DcHelper.getContext(requireActivity()).getAccountId();
    try {
      final Reactions reactions =
          DcHelper.getRpc(requireActivity()).getMessageReactions(accId, msgId);
      ArrayList<Pair<Integer, String>> contactsReactions = new ArrayList<>();

      if (reactions != null) {
        pillsContainer.removeAllViews();
        int totalCount = 0;
        for (Reaction reaction : reactions.reactions) {
          View pill = buildPill(requireContext(), pillsContainer, reaction);
          final String emoji = reaction.emoji;
          pill.setOnClickListener(
              view -> {
                sendReaction(emoji);
              });
          pillsContainer.addView(pill);
          totalCount += reaction.count;
        }
        titleView.setText(
            getResources().getQuantityString(R.plurals.n_reactions, totalCount, totalCount));

        // Display contacts list only if chat is not a channel
        if (!isBroadcast) {
          Map<String, List<String>> reactionsByContact = reactions.reactionsByContact;
          List<String> selfReactions =
              reactionsByContact.remove(String.valueOf(DcContact.DC_CONTACT_ID_SELF));
          if (selfReactions != null) {
            for (String reaction : selfReactions) {
              contactsReactions.add(new Pair<>(DcContact.DC_CONTACT_ID_SELF, reaction));
            }
          }
          for (String contact : reactionsByContact.keySet()) {
            for (String reaction : reactionsByContact.get(contact)) {
              contactsReactions.add(new Pair<>(Integer.parseInt(contact), reaction));
            }
          }
          adapter.changeData(contactsReactions);
        }
      } else {
        dismiss();
      }
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
        final List<String> selfReactions =
            reactionsByContact.get(String.valueOf(DcContact.DC_CONTACT_ID_SELF));
        if (selfReactions != null && !selfReactions.isEmpty()) {
          result = selfReactions.get(0);
        }
      }
    } catch (RpcException e) {
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
    } catch (Exception e) {
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
  }
}
