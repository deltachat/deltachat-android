package org.thoughtcrime.securesms.relay;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.thoughtcrime.securesms.BaseActionBarActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.List;

import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;
import chat.delta.rpc.types.EnteredLoginParam;

public class RelayListActivity extends BaseActionBarActivity
  implements RelayListAdapter.OnRelayClickListener {

  private static final String TAG = RelayListActivity.class.getSimpleName();

  private RelayListAdapter adapter;
  private Rpc rpc;
  private int accId;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_relay_list);

    rpc = DcHelper.getRpc(this);
    accId = DcHelper.getContext(this).getAccountId();

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setTitle(R.string.transports);
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    RecyclerView recyclerView = findViewById(R.id.relay_list);

    // add padding to avoid content hidden behind system bars
    ViewUtil.applyWindowInsets(recyclerView);

    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    // Add the default divider (uses the theme’s `android.R.attr.listDivider`)
    DividerItemDecoration divider = new DividerItemDecoration(
      recyclerView.getContext(),
      layoutManager.getOrientation());
    recyclerView.addItemDecoration(divider);
    recyclerView.setLayoutManager(layoutManager);

    adapter = new RelayListAdapter(this);
    recyclerView.setAdapter(adapter);

    loadRelays();
  }

  }

  private void loadRelays() {
    Util.runOnAnyBackgroundThread(() -> {
      String mainRelayAddr = "";
      try {
        mainRelayAddr = rpc.getConfig(accId, DcHelper.CONFIG_CONFIGURED_ADDRESS);
      } catch (RpcException e) {
        Log.e(TAG, "RPC.getConfig() failed", e);
      }
      String finalMainRelayAddr = mainRelayAddr;

      try {
        List<EnteredLoginParam> relays = rpc.listTransports(accId);

        Util.runOnMain(() -> adapter.setRelays(relays, finalMainRelayAddr));
      } catch (RpcException e) {
        Log.e(TAG, "RPC.listTransports() failed", e);
        Util.runOnMain(() -> adapter.setRelays(null, finalMainRelayAddr));
      }
    });
  }

  @Override
  public void onRelayClick(EnteredLoginParam relay) {
    if (relay.addr != null && !relay.addr.equals(adapter.getMainRelay())) {
      Util.runOnAnyBackgroundThread(() -> {
        try {
          rpc.setConfig(accId, DcHelper.CONFIG_CONFIGURED_ADDRESS, relay.addr);
        } catch (RpcException e) {
          Log.e(TAG, "RPC.setConfig() failed", e);
        }

        loadRelays();
      });
    }
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
