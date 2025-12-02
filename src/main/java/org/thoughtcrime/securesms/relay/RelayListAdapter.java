package org.thoughtcrime.securesms.relay;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
  }

  public RelayListAdapter(OnRelayClickListener listener) {
    this.listener = listener;
  }

  public String getMainRelay() {
    return mainRelayAddr;
  }

  public void setRelays(List<EnteredLoginParam> relays, String mainRelayAddr) {
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
    private final ImageView mainIndicator;

    public RelayViewHolder(@NonNull View itemView) {
      super(itemView);
      titleText = itemView.findViewById(R.id.title);
      mainIndicator = itemView.findViewById(R.id.main_indicator);
    }

    public void bind(EnteredLoginParam relay, boolean isMain, OnRelayClickListener listener) {
      titleText.setText(relay.addr);
      mainIndicator.setVisibility(isMain ? View.VISIBLE : View.GONE);

      itemView.setOnClickListener(v -> {
        if (listener != null) {
          listener.onRelayClick(relay);
        }
      });
    }
  }
}
