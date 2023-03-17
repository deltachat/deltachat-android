package org.thoughtcrime.securesms.qr;

import static org.thoughtcrime.securesms.PassphraseRequiredActionBarActivity.LOCALE_EXTRA;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import org.thoughtcrime.securesms.BaseActionBarActivity;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.service.GenericForegroundService;
import org.thoughtcrime.securesms.service.NotificationController;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;

import java.util.Locale;

public class BackupTransferActivity extends BaseActionBarActivity {

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

    private final DynamicTheme dynamicTheme = new DynamicTheme();
    private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

    NotificationController notificationController;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        dynamicTheme.onCreate(this);
        dynamicLanguage.onCreate(this);

        transferMode = TransferMode.fromInt(getIntent().getIntExtra(TRANSFER_MODE, TransferMode.INVALID.getInt()));
        if (transferMode == TransferMode.INVALID) {
          throw new RuntimeException("bad transfer mode");
        }

        DcHelper.getAccounts(this).stopIo();

        notificationController = GenericForegroundService.startForegroundTask(this, getString(R.string.multidevice_title));

        setContentView(R.layout.backup_provider_activity);

        switch(transferMode) {
            case SENDER_SHOW_QR:
                initFragment(android.R.id.content, new BackupProviderFragment(), dynamicLanguage.getCurrentLocale(), icicle);
                break;

          case RECEIVER_SCAN_QR:
                initFragment(android.R.id.content, new BackupReceiverFragment(), dynamicLanguage.getCurrentLocale(), icicle);
                break;
        }

        ActionBar supportActionBar = getSupportActionBar();
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        supportActionBar.setTitle(R.string.multidevice_title);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            notificationController.close();
            DcHelper.getAccounts(this).startIo();
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
              doFinish();
              break;

          default:
              new AlertDialog.Builder(this)
                    .setMessage("Abort transfer?")
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> doFinish())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
                break;
        }
    }

    private void doFinish() {
        if (transferMode == TransferMode.RECEIVER_SCAN_QR && transferState == TransferState.TRANSFER_SUCCESS) {
            startActivity(new Intent(getApplicationContext(), ConversationListActivity.class));
        }
        finish();
    }

    protected <T extends Fragment> T initFragment(@IdRes int target,
                                                  @NonNull T fragment,
                                                  @Nullable Locale locale,
                                                  @Nullable Bundle extras)
    {
        Bundle args = new Bundle();
        args.putSerializable(LOCALE_EXTRA, locale);
        if (extras != null) {
            args.putAll(extras);
        }
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction().replace(target, fragment).commitAllowingStateLoss();
        return fragment;
    }
}
