package org.thoughtcrime.securesms.deltax;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.deltax.module.PluginInfo;

public class PluginAdapter extends RecyclerView.Adapter<PluginAdapter.ViewHolder> {

  public interface PluginActionListener {
    void onToggle(PluginInfo plugin, boolean enabled);

    void onUninstall(PluginInfo plugin);
  }

  private final List<PluginInfo> plugins;
  private final PluginActionListener listener;

  public PluginAdapter(List<PluginInfo> plugins, PluginActionListener listener) {
    this.plugins = plugins;
    this.listener = listener;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plugin, parent, false);
    return new ViewHolder(v);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder h, int position) {
    PluginInfo p = plugins.get(position);
    h.name.setText(p.manifest.name);
    h.meta.setText("v" + p.manifest.version + " · " + p.manifest.author);
    h.desc.setText(p.manifest.description != null ? p.manifest.description : "");
    boolean disabled = DeltaX.getInstance(h.itemView.getContext()).isPluginDisabled(p.getPackageName());
    h.sw.setChecked(!disabled);
    h.sw.setOnCheckedChangeListener((buttonView, isChecked) -> listener.onToggle(p, isChecked));
    h.uninstall.setOnClickListener(v -> listener.onUninstall(p));
  }

  @Override
  public int getItemCount() {
    return plugins.size();
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    TextView name;
    TextView meta;
    TextView desc;
    Switch sw;
    Button uninstall;

    ViewHolder(View v) {
      super(v);
      name = v.findViewById(R.id.plugin_name);
      meta = v.findViewById(R.id.plugin_meta);
      desc = v.findViewById(R.id.plugin_desc);
      sw = v.findViewById(R.id.plugin_switch);
      uninstall = v.findViewById(R.id.plugin_uninstall);
    }
  }
}
