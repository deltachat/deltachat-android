package org.thoughtcrime.securesms.deltax;

import android.content.Context;
import android.content.res.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.deltax.module.PluginInfo;

public class PluginAdapter extends RecyclerView.Adapter<PluginAdapter.ViewHolder> {

  public interface PluginActionListener {
    void onToggle(PluginInfo plugin, boolean enabled);

    void onUninstall(PluginInfo plugin);

    void onOpen(PluginInfo plugin);

    void onItemClick(PluginInfo plugin);

    void onItemLongClick(PluginInfo plugin);
  }

  private final List<PluginInfo> plugins;
  private final PluginActionListener listener;
  private boolean selectionMode = false;
  private final Set<String> selected = new HashSet<>();

  public PluginAdapter(List<PluginInfo> plugins, PluginActionListener listener) {
    this.plugins = plugins;
    this.listener = listener;
  }

  public void setPlugins(List<PluginInfo> plugins) {
    this.plugins.clear();
    this.plugins.addAll(plugins);
  }

  public void setSelectionMode(boolean mode) {
    this.selectionMode = mode;
    if (!mode) selected.clear();
    notifyDataSetChanged();
  }

  public void setSelected(Set<String> ids) {
    selected.clear();
    if (ids != null) selected.addAll(ids);
    notifyDataSetChanged();
  }

  public Set<String> getSelectedIds() {
    return new HashSet<>(selected);
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plugin, parent, false);
    return new ViewHolder(v);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder h, int position) {
    PluginInfo p = plugins.get(position);
    Context ctx = h.itemView.getContext();
    DeltaX dx = DeltaX.getInstance(ctx);

    h.name.setText(p.manifest.name);
    h.meta.setText("v" + p.manifest.version + " · " + p.manifest.author);
    h.desc.setText(p.manifest.description != null ? p.manifest.description : "");

    boolean isSelected = selected.contains(p.getPackageName());
    if (selectionMode) {
      h.sw.setVisibility(View.GONE);
      h.uninstall.setVisibility(View.GONE);
      h.open.setVisibility(View.GONE);
      h.check.setVisibility(View.VISIBLE);
      h.check.setChecked(isSelected);
      if (isSelected) {
        h.root.setBackgroundColor(highlightColor(ctx));
      } else {
        h.root.setBackgroundResource(selectableBackground(ctx));
      }
    } else {
      h.check.setVisibility(View.GONE);
      h.sw.setVisibility(View.VISIBLE);
      h.uninstall.setVisibility(View.VISIBLE);
      boolean disabled = dx.isPluginDisabled(p.getPackageName());
      h.sw.setChecked(!disabled);
      h.sw.setOnCheckedChangeListener((buttonView, isChecked) -> listener.onToggle(p, isChecked));
      boolean hasPage = dx.hasInteractivePage(p.getPackageName());
      h.open.setVisibility(hasPage ? View.VISIBLE : View.GONE);
      h.root.setBackgroundResource(selectableBackground(ctx));
    }

    h.root.setOnClickListener(v -> listener.onItemClick(p));
    h.root.setOnLongClickListener(
        v -> {
          listener.onItemLongClick(p);
          return true;
        });
    h.uninstall.setOnClickListener(v -> listener.onUninstall(p));
    h.open.setOnClickListener(v -> listener.onOpen(p));
  }

  @Override
  public int getItemCount() {
    return plugins.size();
  }

  private static int selectableBackground(Context ctx) {
    TypedValue out = new TypedValue();
    ctx.getTheme().resolveAttribute(R.attr.selectableItemBackground, out, true);
    return out.resourceId;
  }

  private static int highlightColor(Context ctx) {
    TypedValue out = new TypedValue();
    ctx.getTheme().resolveAttribute(R.attr.colorControlHighlight, out, true);
    return out.data;
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    View root;
    TextView name;
    TextView meta;
    TextView desc;
    SwitchCompat sw;
    Button uninstall;
    Button open;
    CheckBox check;

    ViewHolder(View v) {
      super(v);
      root = v;
      name = v.findViewById(R.id.plugin_name);
      meta = v.findViewById(R.id.plugin_meta);
      desc = v.findViewById(R.id.plugin_desc);
      sw = v.findViewById(R.id.plugin_switch);
      uninstall = v.findViewById(R.id.plugin_uninstall);
      open = v.findViewById(R.id.plugin_open);
      check = v.findViewById(R.id.plugin_check);
    }
  }
}
