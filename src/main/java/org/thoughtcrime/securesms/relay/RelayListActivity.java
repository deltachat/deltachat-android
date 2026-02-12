package org.thoughtcrime.securesms.relay;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;

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
import org.thoughtcrime.securesms.util.ScreenLockUtil;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.List;

import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;
import chat.delta.rpc.types.EnteredLoginParam;

public class RelayListActivity extends BaseActionBarActivity
  implements RelayListAdapter.OnRelayClickListener, DcEventCenter.DcEventDelegate {

  private static final String TAG = RelayListActivity.class.getSimpleName();
  public static final String EXTRA_QR_DATA = "qr_data";

  private RelayListAdapter adapter;
  private Rpc rpc;
  private int accId;

  /** QR provided via Intent extras needs to be saved to pass it to QrCodeHandler when authorization finishes */
  private String qrData = null;

  /** Relay selected for context menu via onRelayLongClick() */
  private EnteredLoginParam contextMenuRelay = null;

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

    qrData = getIntent().getStringExtra(EXTRA_QR_DATA);
    if (qrData != null) {
      // when the activity is opened with a QR data, we need to ask for authorization first
      boolean result = ScreenLockUtil.applyScreenLock(this, getString(R.string.add_transport), getString(R.string.enter_system_secret_to_continue), ScreenLockUtil.REQUEST_CODE_CONFIRM_CREDENTIALS);
      if (!result) {
        new QrCodeHandler(this).handleOnlyAddRelayQr(qrData);
      }
    }

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
    eventCenter.addObserver(DcContext.DC_EVENT_TRANSPORTS_MODIFIED, this);
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
  public void onRelayLongClick(View view, EnteredLoginParam relay) {
    contextMenuRelay = relay;
    registerForContextMenu(view);
    openContextMenu(view);
    unregisterForContextMenu(view);
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    getMenuInflater().inflate(R.menu.relay_item_context, menu);

    boolean nonNullAddr = contextMenuRelay != null && contextMenuRelay.addr != null;
    boolean isMain = nonNullAddr && contextMenuRelay.addr.equals(adapter.getMainRelay());
    menu.findItem(R.id.menu_delete_relay).setVisible(!isMain);
  }

  @Override
  public void onContextMenuClosed(android.view.Menu menu) {
    super.onContextMenuClosed(menu);
    contextMenuRelay = null;
  }

  @Override
  public boolean onContextItemSelected(@NonNull MenuItem item) {
    if (contextMenuRelay == null) return super.onContextItemSelected(item);

    int itemId = item.getItemId();
    if (itemId == R.id.menu_edit_relay) {
      onRelayEdit(contextMenuRelay);
      contextMenuRelay = null;
      return true;
    } else if (itemId == R.id.menu_delete_relay) {
      onRelayDelete(contextMenuRelay);
      contextMenuRelay = null;
      return true;
    }

    return super.onContextItemSelected(item);
  }

  private void onRelayEdit(EnteredLoginParam relay) {
    Intent intent = new Intent(this, EditRelayActivity.class);
    intent.putExtra(EditRelayActivity.EXTRA_ADDR, relay.addr);
    startActivity(intent);
  }

  private void onRelayDelete(EnteredLoginParam relay) {
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
    if (resultCode != RESULT_OK) {
      // if user canceled unlocking, then finish
      if (requestCode == ScreenLockUtil.REQUEST_CODE_CONFIRM_CREDENTIALS) finish();
      return;
    }

    QrCodeHandler qrCodeHandler = new QrCodeHandler(this);
    if (requestCode == IntentIntegrator.REQUEST_CODE) {
      IntentResult scanResult = IntentIntegrator.parseActivityResult(resultCode, data);
      qrCodeHandler.handleOnlyAddRelayQr(scanResult.getContents());
    } else if (requestCode == ScreenLockUtil.REQUEST_CODE_CONFIRM_CREDENTIALS) {
      // user authorized, then proceed to handle the QR data
      if (qrData != null) {
        qrCodeHandler.handleOnlyAddRelayQr(qrData);
        qrData = null;
      }
    }
  }

  @Override
  public void handleEvent(@NonNull DcEvent event) {
    int eventId = event.getId();
    if (eventId == DcContext.DC_EVENT_CONFIGURE_PROGRESS) {
      if (event.getData1Int() == 1000) loadRelays();
    } else if (eventId == DcContext.DC_EVENT_TRANSPORTS_MODIFIED) {
      loadRelays();
    }
  }
}
