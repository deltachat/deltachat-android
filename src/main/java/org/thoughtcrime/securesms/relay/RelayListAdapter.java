package org.thoughtcrime.securesms.relay;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import org.thoughtcrime.securesms.R;

import java.util.ArrayList;
import java.util.List;

import chat.delta.rpc.types.EnteredLoginParam;

public class RelayListAdapter extends RecyclerView.Adapter<RelayListAdapter.RelayViewHolder> {

  private List<EnteredLoginParam> relays = new ArrayList<>();
  private final OnRelayClickListener listener;
  private String mainRelayAddr;

  public interface OnRelayClickListener {
    void onRelayClick(EnteredLoginParam relay);
    void onRelayEdit(EnteredLoginParam relay);
    void onRelayDelete(EnteredLoginParam relay);
  }

  public RelayListAdapter(OnRelayClickListener listener) {
    this.listener = listener;
  }

  public String getMainRelay() {
    return mainRelayAddr;
  }

  public void setRelays(@Nullable List<EnteredLoginParam> relays, String mainRelayAddr) {
    this.relays = relays != null ? relays : new ArrayList<>();
    this.mainRelayAddr = mainRelayAddr;
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public RelayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
      .inflate(R.layout.relay_list_item, parent, false);
    return new RelayViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull RelayViewHolder holder, int position) {
    EnteredLoginParam relay = relays.get(position);
    boolean isMain = relay.addr != null && relay.addr.equals(mainRelayAddr);
    holder.bind(relay, isMain, listener);
  }

  @Override
  public int getItemCount() {
    return relays.size();
  }

  public static class RelayViewHolder extends RecyclerView.ViewHolder {
    private final TextView titleText;
    private final TextView subtitleText;
    private final ImageView mainIndicator;
    private final ImageView editButton;
    private final ImageView deleteButton;

    public RelayViewHolder(@NonNull View itemView) {
      super(itemView);
      titleText = itemView.findViewById(R.id.title);
      subtitleText = itemView.findViewById(R.id.subtitle);
      mainIndicator = itemView.findViewById(R.id.main_indicator);
      editButton = itemView.findViewById(R.id.edit_button);
      deleteButton = itemView.findViewById(R.id.delete_button);
    }

    public void bind(EnteredLoginParam relay, boolean isMain, OnRelayClickListener listener) {
      String[] parts = relay.addr.split("@");
      titleText.setText(parts.length == 2? parts[1] : parts[0]);
      subtitleText.setText(parts.length == 2? parts[0] : "");
      mainIndicator.setVisibility(isMain ? View.VISIBLE : View.INVISIBLE);
      deleteButton.setVisibility(isMain ? View.GONE : View.VISIBLE);

      itemView.setOnClickListener(v -> {
        if (listener != null) {
          listener.onRelayClick(relay);
        }
      });

      editButton.setOnClickListener(v -> {
        if (listener != null) {
          listener.onRelayEdit(relay);
        }
      });

      deleteButton.setOnClickListener(v -> {
        if (listener != null) {
          listener.onRelayDelete(relay);
        }
      });
    }
  }
}
