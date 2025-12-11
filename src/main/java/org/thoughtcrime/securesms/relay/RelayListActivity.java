package org.thoughtcrime.securesms.relay;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.thoughtcrime.securesms.BaseActionBarActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.registration.PulsingFloatingActionButton;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.qr.QrActivity;
import org.thoughtcrime.securesms.qr.QrCodeHandler;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.List;

import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;
import chat.delta.rpc.types.EnteredLoginParam;
import chat.delta.rpc.types.SecurejoinSource;
import chat.delta.rpc.types.SecurejoinUiPath;

public class RelayListActivity extends BaseActionBarActivity
  implements RelayListAdapter.OnRelayClickListener, DcEventCenter.DcEventDelegate {

  private static final String TAG = RelayListActivity.class.getSimpleName();
  public static final String EXTRA_QR_DATA = "qr_data";

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
    PulsingFloatingActionButton fabAdd = findViewById(R.id.fab_add_relay);

    // add padding to avoid content hidden behind system bars
    ViewUtil.applyWindowInsets(recyclerView);
    // Apply insets to prevent fab from being covered by system bars
    ViewUtil.applyWindowInsetsAsMargin(fabAdd);

    fabAdd.setOnClickListener(v -> {
      new IntentIntegrator(this).setCaptureActivity(QrActivity.class).addExtra(QrActivity.EXTRA_SCAN_RELAY, true).initiateScan();
    });

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

    DcEventCenter eventCenter = DcHelper.getEventCenter(this);
    eventCenter.addObserver(DcContext.DC_EVENT_CONFIGURE_PROGRESS, this);

    String qrdata = getIntent().getStringExtra(EXTRA_QR_DATA);
    if (qrdata != null) {
      QrCodeHandler qrCodeHandler = new QrCodeHandler(this);
      qrCodeHandler.handleQrData(qrdata, SecurejoinSource.Unknown, SecurejoinUiPath.Unknown);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    DcHelper.getEventCenter(this).removeObservers(this);
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
  public void onRelayEdit(EnteredLoginParam relay) {
    Intent intent = new Intent(this, EditRelayActivity.class);
    intent.putExtra(EditRelayActivity.EXTRA_ADDR, relay.addr);
    startActivity(intent);
  }

  @Override
  public void onRelayDelete(EnteredLoginParam relay) {
    new AlertDialog.Builder(this)
      .setTitle(R.string.remove_transport)
      .setMessage(getString(R.string.confirm_remove_transport, relay.addr))
      .setPositiveButton(R.string.ok, (dialog, which) -> {
        try {
          rpc.deleteTransport(accId, relay.addr);
          loadRelays();
        } catch (RpcException e) {
          Log.e(TAG, "RPC.deleteTransport() failed", e);
        }
      })
      .setNegativeButton(R.string.cancel, null)
      .show();
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == IntentIntegrator.REQUEST_CODE) {
      IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
      QrCodeHandler qrCodeHandler = new QrCodeHandler(this);
      qrCodeHandler.onScanPerformed(scanResult, SecurejoinUiPath.Unknown);
    }
  }

  @Override
  public void handleEvent(@NonNull DcEvent event) {
    int eventId = event.getId();
    if (eventId == DcContext.DC_EVENT_CONFIGURE_PROGRESS && event.getData1Int() == 1000) {
      loadRelays();
    }
  }
}
