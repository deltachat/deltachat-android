package org.thoughtcrime.securesms.qr;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.service.GenericForegroundService;
import org.thoughtcrime.securesms.service.NotificationController;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;

public class BackupProviderActivity extends AppCompatActivity {

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

    private TransferMode transferMode = TransferMode.RECEIVER_SCAN_QR;
    private TransferState transferState = TransferState.TRANSFER_UNKNOWN;

    private final DynamicTheme dynamicTheme = new DynamicTheme();
    private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

    NotificationController notificationController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dynamicTheme.onCreate(this);
        dynamicLanguage.onCreate(this);

        transferMode = TransferMode.fromInt(getIntent().getIntExtra(TRANSFER_MODE, TransferMode.INVALID.getInt()));
        if (transferMode == TransferMode.INVALID) {
          throw new RuntimeException("invalid transfer mode");
        }

        notificationController = GenericForegroundService.startForegroundTask(this, getString(R.string.multidevice_title));

        setContentView(R.layout.backup_provider_activity);

        ActionBar supportActionBar = getSupportActionBar();
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        supportActionBar.setTitle(R.string.multidevice_title);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            notificationController.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        dynamicTheme.onResume(this);
        dynamicLanguage.onResume(this);
    }

    @Override
    public void onBackPressed() {
        finishOrAskToFinish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case android.R.id.home:
                finishOrAskToFinish();
                return true;
        }

        return false;
    }

    public void setTransferState(TransferState transferState) {
        this.transferState = transferState;
    }

    private void finishOrAskToFinish() {
        switch (transferState) {
          case TRANSFER_ERROR:
          case TRANSFER_SUCCESS:
              finish();
              break;

          default:
              new AlertDialog.Builder(this)
                    .setMessage("Abort transfer?")
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> finish())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
                break;
        }
    }
}
