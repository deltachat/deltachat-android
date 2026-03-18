package org.thoughtcrime.securesms.relay;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
  private String mainRelayAddr;

  public interface OnRelayClickListener {
    void onRelayClick(EnteredLoginParam relay);

    void onRelayLongClick(View view, EnteredLoginParam relay);
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
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.relay_list_item, parent, false);
    return new RelayViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull RelayViewHolder holder, int position) {
    EnteredLoginParam relay = relays.get(position);
    boolean isMain = relay.addr != null && relay.addr.equals(mainRelayAddr);
    boolean isUnpublished = true; // TODO: get correct state from jsonrpc
    holder.bind(relay, isMain, isUnpublished, listener);
  }

  @Override
  public int getItemCount() {
    return relays.size();
  }

  public static class RelayViewHolder extends RecyclerView.ViewHolder {
    private final TextView titleText;
    private final TextView subtitleText;
    private final ImageView mainIndicator;

    public RelayViewHolder(@NonNull View itemView) {
      super(itemView);
      titleText = itemView.findViewById(R.id.title);
      subtitleText = itemView.findViewById(R.id.subtitle);
      mainIndicator = itemView.findViewById(R.id.main_indicator);
    }

    public void bind(EnteredLoginParam relay, boolean isMain, boolean isUnpublished,  OnRelayClickListener listener) {
      Context context = itemView.getContext();
      String[] parts = relay.addr.split("@");
      titleText.setText(parts.length == 2 ? parts[1] : parts[0]);

      String subtitle = parts.length == 2 ? parts[0] : "";
      if (isMain) {
        subtitle += " · " + context.getString(R.string.used_for_sending);
      } else if (isUnpublished) {
        subtitle += " · " + context.getString(R.string.hidden_from_contacts);
      }
      subtitleText.setText(subtitle);

      mainIndicator.setVisibility(isMain ? View.VISIBLE : View.INVISIBLE);

      itemView.setOnClickListener(
          v -> {
            if (listener != null) {
              listener.onRelayClick(relay);
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
