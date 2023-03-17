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

    public enum TransferState {
        TRANSFER_UNKNOWN, TRANSFER_ERROR, TRANSFER_SUCCESS;
    };

    private TransferState transferState = TransferState.TRANSFER_UNKNOWN;

    private final DynamicTheme dynamicTheme = new DynamicTheme();
    private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

    NotificationController notificationController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dynamicTheme.onCreate(this);
        dynamicLanguage.onCreate(this);
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
                    .setCancelable(false)
                    .show();
                break;
        }
    }
}
