package org.thoughtcrime.securesms.proxy;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.thoughtcrime.securesms.R;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ProxyListAdapter extends BaseAdapter {
  @NonNull  private final Context context;
  @NonNull  private final List<String> proxies = new LinkedList<>();
  @Nullable private ItemClickListener itemClickListener;

  public ProxyListAdapter(@NonNull Context context)
  {
    this.context = context;
  }

  public void changeData(String newProxies) {
    proxies.clear();
    if (!TextUtils.isEmpty(newProxies)) {
      Collections.addAll(proxies, newProxies.split("\n"));
    }
    notifyDataSetChanged();
  }

  @Override
  public int getCount() {
    return proxies.size();
  }

  @Override
  public Object getItem(int position) {
    return proxies.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(final int position, View v, final ViewGroup parent) {
    if (v == null) {
      v = LayoutInflater.from(context).inflate(R.layout.selected_contact_list_item, parent, false);
    }

    final String proxy = (String)getItem(position);
    TextView name = v.findViewById(R.id.name);
    name.setText(proxy);
    v.setOnClickListener(view -> {
      if (itemClickListener != null) {
        itemClickListener.onItemClick(proxy);
      }
    });

    return v;
  }

  public void setItemClickListener(@Nullable ItemClickListener listener) {
    itemClickListener = listener;
  }

  public interface ItemClickListener {
    void onItemClick(String proxyUrl);
  }
}
