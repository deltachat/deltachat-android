package org.thoughtcrime.securesms.qr;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.BaseActionBarActivity;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.LogViewActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.service.GenericForegroundService;
import org.thoughtcrime.securesms.service.NotificationController;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

public class BackupTransferActivity extends BaseActionBarActivity {

    private final static String TAG = BackupTransferActivity.class.getSimpleName();

    public enum TransferMode {
        INVALID(0),
        SENDER_SHOW_QR(1),
        RECEIVER_SCAN_QR(2);
        private final int i;
        TransferMode(int i) { this.i = i; }
        public int getInt() { return i; }
        public static TransferMode fromInt(int i) { return values()[i]; }
    };

    public enum TransferState {
        TRANSFER_UNKNOWN,
        TRANSFER_ERROR,
        TRANSFER_SUCCESS;
    };

    public static final String TRANSFER_MODE = "transfer_mode";
    public static final String QR_CODE = "qr_code";

    private TransferMode transferMode = TransferMode.RECEIVER_SCAN_QR;
    private TransferState transferState = TransferState.TRANSFER_UNKNOWN;

    NotificationController notificationController;
    private boolean notificationControllerClosed = false;
    public boolean warnAboutCopiedQrCodeOnAbort = false;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        transferMode = TransferMode.fromInt(getIntent().getIntExtra(TRANSFER_MODE, TransferMode.INVALID.getInt()));
        if (transferMode == TransferMode.INVALID) {
          throw new RuntimeException("bad transfer mode");
        }

        DcHelper.getAccounts(this).stopIo();

        String title = getString(transferMode == TransferMode.RECEIVER_SCAN_QR ? R.string.multidevice_receiver_title : R.string.multidevice_title);
        notificationController = GenericForegroundService.startForegroundTask(this, title);

        setContentView(R.layout.backup_provider_activity);

        switch(transferMode) {
            case SENDER_SHOW_QR:
                initFragment(R.id.backup_provider_fragment, new BackupProviderFragment(), icicle);
                break;

          case RECEIVER_SCAN_QR:
                initFragment(R.id.backup_provider_fragment, new BackupReceiverFragment(), icicle);
                break;
        }

        ActionBar supportActionBar = getSupportActionBar();
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        supportActionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        supportActionBar.setTitle(title);

        // add padding to avoid content hidden behind system bars
        ViewUtil.applyWindowInsets(findViewById(R.id.backup_provider_fragment));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!notificationControllerClosed) {
            notificationController.close();
        }
        DcHelper.getAccounts(this).startIo();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.backup_transfer_menu, menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        finishOrAskToFinish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);

      int itemId = item.getItemId();
      if (itemId == android.R.id.home) {
        finishOrAskToFinish();
        return true;
      } else if (itemId == R.id.troubleshooting) {
        DcHelper.openHelp(this, "#multiclient");
        return true;
      } else if (itemId == R.id.view_log_button) {
        startActivity(new Intent(this, LogViewActivity.class));
        return true;
      }

        return false;
    }

    public void setTransferState(TransferState transferState) {
        this.transferState = transferState;
    }

    public void setTransferError(@NonNull String errorContext) {
        if (this.transferState != TransferState.TRANSFER_ERROR) {
            this.transferState = TransferState.TRANSFER_ERROR;

            String lastError = DcHelper.getContext(this).getLastError();
            if (lastError.isEmpty()) {
                lastError = "<last error not set>";
            }

            String error = errorContext;
            if (!error.isEmpty()) {
                error += ": ";
            }
            error += lastError;

            new AlertDialog.Builder(this)
                .setMessage(error)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(false)
                .show();
        }
    }

    private void finishOrAskToFinish() {
        switch (transferState) {
          case TRANSFER_ERROR:
          case TRANSFER_SUCCESS:
              doFinish();
              break;

          default:
              String msg = getString(R.string.multidevice_abort);
              if (warnAboutCopiedQrCodeOnAbort) {
                  msg += "\n\n" + getString(R.string.multidevice_abort_will_invalidate_copied_qr);
              }
              new AlertDialog.Builder(this)
                    .setMessage(msg)
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> doFinish())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
              break;
        }
    }

    public void doFinish() {
        // the permanent notification will prevent other activities to be started and kill BackupTransferActivity;
        // close it before starting other activities
        notificationController.close();
        notificationControllerClosed = true;

        if (transferMode == TransferMode.RECEIVER_SCAN_QR && transferState == TransferState.TRANSFER_SUCCESS) {
            startActivity(new Intent(getApplicationContext(), ConversationListActivity.class));
        } else if (transferMode == TransferMode.SENDER_SHOW_QR) {
            // restart the activities that were removed when BackupTransferActivity was started at (**2)
            // (we removed the activity backstack as otherwise a tap on the Delta Chat icon on the home screen would
            // call onNewIntent() which cannot be aborted and will kill BackupTransferActivity.
            // if all activities are removed, onCreate() will be called and that can be aborted, so that
            // a tap in the home icon just opens BackupTransferActivity.
            // (the user can leave Delta Chat during backup transfer :)
            // a proper fix would maybe to not rely onNewIntent() at all - but that would require more refactorings
            // and needs lots if testing in complicated areas (share ...))
            startActivity(new Intent(getApplicationContext(), ConversationListActivity.class));
            startActivity(new Intent(this, ApplicationPreferencesActivity.class));
            overridePendingTransition(0, 0);
        }
        finish();
    }

    public static void appendSSID(Activity activity, final TextView textView) {
        if (textView != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            new Thread(() -> {
                try {
                    // depending on the android version, getting the SSID requires none, all or one of
                    // ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION, NEARBY_WIFI_DEVICES, ACCESS_WIFI_STATE, ACCESS_NETWORK_STATE and maybe even more.
                    final WifiManager wifiManager = (WifiManager)activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (wifiManager.isWifiEnabled()) {
                        final WifiInfo info = wifiManager.getConnectionInfo();
                        final String ssid = info.getSSID();
                        Log.i(TAG, "wifi ssid: "+ssid);
                        if (!ssid.equals("<unknown ssid>")) { // "<unknown ssid>" may be returned on insufficient rights
                            Util.runOnMain(() -> {
                                textView.setText(textView.getText() + " (" + ssid + ")");
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
