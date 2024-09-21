package org.thoughtcrime.securesms.proxy;

import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_PROXY_ENABLED;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcLot;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ProxyListAdapter extends BaseAdapter {
  @NonNull  private final Context context;
  @NonNull  private final DcContext dcContext;
  @NonNull  private final List<String> proxies = new LinkedList<>();
  @Nullable private ItemClickListener itemClickListener;

  public ProxyListAdapter(@NonNull Context context)
  {
    this.context = context;
    this.dcContext = DcHelper.getContext(context);
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
      v = LayoutInflater.from(context).inflate(R.layout.proxy_list_item, parent, false);
    }

    TextView host = v.findViewById(R.id.host);
    TextView protocol = v.findViewById(R.id.protocol);
    ImageView checkmark = v.findViewById(R.id.checkmark);
    TextView status = v.findViewById(R.id.status);

    final String proxyUrl = (String)getItem(position);
    final DcLot qrParsed = dcContext.checkQr(proxyUrl);
    if (qrParsed.getState() == DcContext.DC_QR_PROXY) {
      host.setText(qrParsed.getText1());
      protocol.setText(proxyUrl.split(":", 2)[0]);
    } else {
      host.setText(proxyUrl);
      protocol.setText(R.string.unknown);
    }
    if (position == 0 && DcHelper.getInt(context, CONFIG_PROXY_ENABLED) == 1) {
      checkmark.setVisibility(View.VISIBLE);
      status.setVisibility(View.VISIBLE);
      status.setText(DcHelper.getConnectivitySummary(context, R.string.connectivity_connected));
    } else {
      checkmark.setVisibility(View.GONE);
      status.setVisibility(View.GONE);
    }
    v.setOnClickListener(view -> {
      if (itemClickListener != null) {
        itemClickListener.onItemClick(proxyUrl);
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
