package org.thoughtcrime.securesms.messagedetails;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;
import chat.delta.rpc.types.Message;
import chat.delta.rpc.types.MessageReadReceipt;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.thoughtcrime.securesms.ProfileActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.ViewUtil;

public class MessageInfoDialogFragment extends DialogFragment
    implements DcEventCenter.DcEventDelegate {

  private static final String TAG = "MessageInfoDialogFrag";
  private static final String ARG_MSG_ID = "msg_id";

  private static final DiffUtil.ItemCallback<MessageReadReceipt> DIFF_CALLBACK =
      new DiffUtil.ItemCallback<MessageReadReceipt>() {
        @Override
        public boolean areItemsTheSame(
            @NonNull MessageReadReceipt oldItem, @NonNull MessageReadReceipt newItem) {
          return oldItem.contactId != null && oldItem.contactId.equals(newItem.contactId);
        }

        @Override
        public boolean areContentsTheSame(
            @NonNull MessageReadReceipt oldItem, @NonNull MessageReadReceipt newItem) {
          return areItemsTheSame(oldItem, newItem)
              && (Objects.equals(oldItem.timestamp, newItem.timestamp));
        }
      };

  private TextView sentTimeView;
  private View receivedRow;
  private TextView receivedTimeView;
  private View readBySection;
  private TextView readByLabelView;
  private TextView viewsRow;
  private RecyclerView receiptsList;
  private ReadReceiptsAdapter receiptsAdapter;
  private int msgId;

  public static MessageInfoDialogFragment newInstance(int msgId) {
    MessageInfoDialogFragment fragment = new MessageInfoDialogFragment();
    Bundle args = new Bundle();
    args.putInt(ARG_MSG_ID, msgId);
    fragment.setArguments(args);
    return fragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    msgId = getArguments() != null ? getArguments().getInt(ARG_MSG_ID, 0) : 0;

    View view = View.inflate(requireActivity(), R.layout.message_info_dialog, null);
    sentTimeView = view.findViewById(R.id.message_info_sent_time);
    receivedRow = view.findViewById(R.id.message_info_received_row);
    receivedTimeView = view.findViewById(R.id.message_info_received_time);
    readBySection = view.findViewById(R.id.message_info_read_by_section);
    readByLabelView = view.findViewById(R.id.message_info_read_by_label);
    viewsRow = view.findViewById(R.id.message_info_views_row);
    receiptsList = view.findViewById(R.id.message_info_receipts);

    receiptsAdapter = new ReadReceiptsAdapter(GlideApp.with(this));
    receiptsList.setLayoutManager(new LinearLayoutManager(requireActivity()));
    receiptsList.setAdapter(receiptsAdapter);

    ImageButton overflowButton = view.findViewById(R.id.message_info_overflow);
    overflowButton.setOnClickListener(v -> showDeveloperMenu(overflowButton));
    ImageButton closeButton = view.findViewById(R.id.message_info_close);
    closeButton.setOnClickListener(v -> dismiss());

    int iconColor = MaterialColors.getColor(view, android.R.attr.textColorPrimary);
    overflowButton.setColorFilter(iconColor);
    closeButton.setColorFilter(iconColor);

    setCompoundIcon(
        readByLabelView,
        R.drawable.ic_delivery_status_read,
        ContextCompat.getColor(requireActivity(), R.color.green_500));
    setCompoundIcon(
        viewsRow,
        R.drawable.ic_baseline_eye,
        MaterialColors.getColor(view, android.R.attr.textColorSecondary));

    DcEventCenter eventCenter = DcHelper.getEventCenter(requireContext());
    eventCenter.addObserver(DcContext.DC_EVENT_MSG_READ, this);
    eventCenter.addObserver(DcContext.DC_EVENT_MSG_READ_COUNT_CHANGED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_MSG_DELETED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_MSGS_CHANGED, this);

    refreshData();

    return new AlertDialog.Builder(requireActivity()).setView(view).create();
  }

  @Override
  public void onStart() {
    super.onStart();
    Dialog dialog = getDialog();
    if (dialog == null || dialog.getWindow() == null) {
      return;
    }
    MaterialShapeDrawable background = new MaterialShapeDrawable();
    background.setShapeAppearanceModel(
        background.getShapeAppearanceModel().toBuilder()
            .setAllCorners(CornerFamily.ROUNDED, ViewUtil.dpToPx(18))
            .build());

    TypedValue typedValue = new TypedValue();
    if (!requireContext()
        .getTheme()
        .resolveAttribute(R.attr.dialog_background_color, typedValue, true)) {
      requireContext()
          .getTheme()
          .resolveAttribute(android.R.attr.colorBackground, typedValue, true);
    }
    background.setFillColor(ColorStateList.valueOf(typedValue.data));
    dialog.getWindow().setBackgroundDrawable(background);
  }

  @Override
  public void onDestroy() {
    DcHelper.getEventCenter(requireActivity()).removeObservers(this);
    super.onDestroy();
  }

  @Override
  public void handleEvent(@NonNull DcEvent event) {
    int eventMsgId = event.getData2Int();
    switch (event.getId()) {
      case DcContext.DC_EVENT_MSG_DELETED:
        if (eventMsgId == msgId) dismiss();
        break;
      case DcContext.DC_EVENT_MSG_READ:
      case DcContext.DC_EVENT_MSG_READ_COUNT_CHANGED:
        if (eventMsgId == msgId) refreshData();
        break;
      case DcContext.DC_EVENT_MSGS_CHANGED:
        if (eventMsgId == 0 || eventMsgId == msgId) refreshData();
        break;
      default:
        break;
    }
  }

  private void refreshData() {
    DcContext dcContext = DcHelper.getContext(requireActivity());
    int accId = dcContext.getAccountId();
    Rpc rpc = DcHelper.getRpc(requireActivity());
    try {
      Message message = rpc.getMessage(accId, msgId);
      boolean outgoing = message.fromId != null && message.fromId == DcContact.DC_CONTACT_ID_SELF;
      boolean hasReceived =
          !outgoing && message.receivedTimestamp != null && message.receivedTimestamp != 0;

      sentTimeView.setText(
          DateUtils.getAbsoluteDateTime(
              requireContext(), (message.timestamp != null ? message.timestamp : 0) * 1000L));
      receivedRow.setVisibility(hasReceived ? View.VISIBLE : View.GONE);
      if (hasReceived) {
        receivedTimeView.setText(
            DateUtils.getAbsoluteDateTime(requireContext(), message.receivedTimestamp * 1000L));
      }

      List<MessageReadReceipt> receipts;
      if (outgoing) {
        receipts = rpc.getMessageReadReceipts(accId, msgId);
        Collections.sort(receipts, (lhs, rhs) -> Long.compare(readTime(rhs), readTime(lhs)));
      } else {
        receipts = Collections.emptyList();
      }
      receiptsAdapter.submitList(receipts);

      boolean isOutBroadcast =
          outgoing && message.chatId != null && dcContext.getChat(message.chatId).isOutBroadcast();
      if (isOutBroadcast) {
        // In channels, receipts is counter only; individual readers are not listed.
        readBySection.setVisibility(View.GONE);
        viewsRow.setVisibility(View.VISIBLE);
        viewsRow.setText(String.valueOf(receipts.size()));
      } else {
        viewsRow.setVisibility(View.GONE);
        readBySection.setVisibility(outgoing && !receipts.isEmpty() ? View.VISIBLE : View.GONE);
      }
    } catch (RpcException e) {
      Log.i(TAG, "Message " + msgId + " no longer available: " + e);
      dismiss();
    }
  }

  private static long readTime(MessageReadReceipt receipt) {
    return receipt.timestamp != null ? receipt.timestamp : 0;
  }

  private void setCompoundIcon(TextView view, int iconRes, int tint) {
    Drawable icon = ContextCompat.getDrawable(requireActivity(), iconRes).mutate();
    icon.setTint(tint);
    int size = Math.round(18 * view.getResources().getDisplayMetrics().density);
    icon.setBounds(0, 0, size, size);
    view.setCompoundDrawablesRelative(icon, null, null, null);
  }

  private void showDeveloperMenu(View anchor) {
    PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
    popup.getMenu().add(R.string.global_menu_view_developer_tools_desktop);
    popup.setOnMenuItemClickListener(
        item -> {
          showDeveloperView();
          return true;
        });
    popup.show();
  }

  private void showDeveloperView() {
    View view = View.inflate(requireActivity(), R.layout.message_details_view, null);
    TextView detailsText = view.findViewById(R.id.details_text);
    detailsText.setText(DcHelper.getContext(requireContext()).getMsgInfo(msgId));

    new AlertDialog.Builder(requireActivity())
        .setView(view)
        .setPositiveButton(android.R.string.ok, null)
        .create()
        .show();
  }

  private class ReadReceiptsAdapter
      extends ListAdapter<MessageReadReceipt, ReadReceiptsAdapter.ReadReceiptViewHolder> {

    private final GlideRequests glideRequests;

    ReadReceiptsAdapter(GlideRequests glideRequests) {
      super(DIFF_CALLBACK);
      this.glideRequests = glideRequests;
    }

    @NonNull
    @Override
    public ReadReceiptViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      ReadReceiptItem item =
          (ReadReceiptItem)
              LayoutInflater.from(parent.getContext())
                  .inflate(R.layout.read_receipt_row, parent, false);
      return new ReadReceiptViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull ReadReceiptViewHolder holder, int position) {
      holder.unbind(glideRequests);
      holder.bind(glideRequests, getItem(position));
    }

    class ReadReceiptViewHolder extends RecyclerView.ViewHolder {

      private final ReadReceiptItem item;

      ReadReceiptViewHolder(@NonNull ReadReceiptItem item) {
        super(item);
        this.item = item;
        itemView.setOnClickListener(
            v -> {
              int position = getBindingAdapterPosition();
              if (position == RecyclerView.NO_POSITION) return;
              Integer contactId = getItem(position).contactId;
              if (contactId != null && contactId != DcContact.DC_CONTACT_ID_SELF) {
                dismiss();
                Intent intent = new Intent(getContext(), ProfileActivity.class);
                intent.putExtra(ProfileActivity.CONTACT_ID_EXTRA, contactId);
                requireContext().startActivity(intent);
              }
            });
      }

      void bind(GlideRequests glideRequests, MessageReadReceipt receipt) {
        item.bind(
            glideRequests,
            receipt.contactId != null ? receipt.contactId : 0,
            receipt.timestamp != null ? receipt.timestamp : 0);
      }

      void unbind(GlideRequests glideRequests) {
        item.unbind(glideRequests);
      }
    }
  }
}
