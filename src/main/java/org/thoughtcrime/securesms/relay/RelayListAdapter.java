package org.thoughtcrime.securesms.relay;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import chat.delta.rpc.types.EnteredLoginParam;
import java.util.ArrayList;
import java.util.List;
import org.thoughtcrime.securesms.R;

public class RelayListAdapter extends RecyclerView.Adapter<RelayListAdapter.RelayViewHolder> {

  private List<EnteredLoginParam> relays = new ArrayList<>();
  private final OnRelayClickListener listener;

  public interface OnRelayClickListener {
    void onRelayClick(View view, EnteredLoginParam relay);

    void onRelayLongClick(View view, EnteredLoginParam relay);
  }

  public RelayListAdapter(OnRelayClickListener listener) {
    this.listener = listener;
  }

  public void setRelays(@Nullable List<EnteredLoginParam> relays) {
    this.relays = relays != null ? relays : new ArrayList<>();
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public RelayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.relay_list_item, parent, false);
    return new RelayViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull RelayViewHolder holder, int position) {
    EnteredLoginParam relay = relays.get(position);
    holder.bind(relay, listener);
  }

  @Override
  public int getItemCount() {
    return relays.size();
  }

  public static class RelayViewHolder extends RecyclerView.ViewHolder {
    private final TextView titleText;
    private final TextView subtitleText;

    public RelayViewHolder(@NonNull View itemView) {
      super(itemView);
      titleText = itemView.findViewById(R.id.title);
      subtitleText = itemView.findViewById(R.id.subtitle);
    }

    public void bind(EnteredLoginParam relay, OnRelayClickListener listener) {
      Context context = itemView.getContext();
      String[] parts = relay.addr.split("@");
      titleText.setText(parts.length == 2 ? parts[1] : parts[0]);

      String subtitle = parts.length == 2 ? parts[0] : "";
      subtitleText.setText(subtitle);

      itemView.setOnClickListener(
          v -> {
            if (listener != null) {
              listener.onRelayClick(v, relay);
            }
          });

      itemView.setOnLongClickListener(
          v -> {
            if (listener != null) {
              listener.onRelayLongClick(v, relay);
            }
            return true;
          });
    }
  }
}
