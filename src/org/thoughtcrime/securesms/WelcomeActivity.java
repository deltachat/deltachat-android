package org.thoughtcrime.securesms;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcLot;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.qr.RegistrationQrActivity;
import org.thoughtcrime.securesms.service.GenericForegroundService;
import org.thoughtcrime.securesms.service.NotificationController;
import org.thoughtcrime.securesms.util.views.ProgressDialog;

import java.io.File;

public class WelcomeActivity extends BaseActionBarActivity implements DcEventCenter.DcEventDelegate {
    public static final String QR_ACCOUNT_EXTRA = "qr_account_extra";

    private boolean manualConfigure = true; // false: configure by QR account creation
    private ProgressDialog progressDialog = null;
    ApplicationDcContext dcContext;
    private NotificationController notificationController;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.welcome_activity);

        Button loginButton = findViewById(R.id.login_button);
        View scanQrButton = findViewById(R.id.scan_qr_button);
        View backupButton = findViewById(R.id.backup_button);

        loginButton.setOnClickListener((view) -> startRegistrationActivity());
        scanQrButton.setOnClickListener((view) -> startRegistrationQrActivity());
        backupButton.setOnClickListener((view) -> startImportBackup());

        dcContext = DcHelper.getContext(this);
        dcContext.eventCenter.addObserver(DcContext.DC_EVENT_CONFIGURE_PROGRESS, this);
        dcContext.eventCenter.addObserver(DcContext.DC_EVENT_IMEX_PROGRESS, this);
    }

    @Override
    public void onStart() {
        super.onStart();
        String qrAccount = getIntent().getStringExtra(QR_ACCOUNT_EXTRA);
        if (qrAccount!=null) {
            manualConfigure = false;
            startQrAccountCreation(qrAccount);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DcHelper.getContext(this).eventCenter.removeObservers(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private void startRegistrationActivity() {
        manualConfigure = true;
        Intent intent = new Intent(this, RegistrationActivity.class);
        startActivity(intent);
        // no finish() here, the back key should take the user back from RegistrationActivity to WelcomeActivity
    }

    private void startRegistrationQrActivity() {
        manualConfigure = false;
        new IntentIntegrator(this).setCaptureActivity(RegistrationQrActivity.class).initiateScan();
        // no finish() here, the back key should take the user back from RegistrationQrActivity to WelcomeActivity
    }

    @SuppressLint("InlinedApi")
    private void startImportBackup() {
        Permissions.with(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .ifNecessary()
                .withPermanentDenialDialog(getString(R.string.perm_explain_access_to_storage_denied))
                .onAllGranted(() -> {
                    File imexDir = DcHelper.getImexDir();
                    final String backupFile = dcContext.imexHasBackup(imexDir.getAbsolutePath());
                    if (backupFile != null) {
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.import_backup_title)
                                .setMessage(String.format(getResources().getString(R.string.import_backup_ask), backupFile))
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                        startImport(backupFile);
                                })
                                .show();
                    }
                    else {
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.import_backup_title)
                                .setMessage(String.format(getResources().getString(R.string.import_backup_no_backup_found), imexDir.getAbsolutePath()))
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    }
                })
                .execute();
    }

    private void startImport(final String backupFile)
    {
        notificationController = GenericForegroundService.startForegroundTask(this, getString(R.string.import_backup_title));
        if( progressDialog!=null ) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.one_moment));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(android.R.string.cancel), (dialog, which) -> {
            dcContext.stopOngoingProcess();
            notificationController.close();
        });
        progressDialog.show();

        dcContext.captureNextError();
        dcContext.imex(DcContext.DC_IMEX_IMPORT_BACKUP, backupFile);
    }

    private void startQrAccountCreation(String qrCode)
    {
        if (progressDialog!=null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.one_moment));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(android.R.string.cancel), (dialog, which) -> {
            dcContext.stopOngoingProcess();
        });
        progressDialog.show();

        dcContext.captureNextError();

        if (!dcContext.setConfigFromQr(qrCode)) {
            progressError(dcContext.getCapturedError());
        }

        // calling configure() results in
        // receiving multiple DC_EVENT_CONFIGURE_PROGRESS events
        dcContext.stopIo();
        dcContext.configure();
    }

    private void progressError(String data2) {
        progressDialog.dismiss();
        maybeShowConfigurationError(this, data2);
    }

    private void progressUpdate(int progress) {
        int percent = progress / 10;
        progressDialog.setMessage(getResources().getString(R.string.one_moment)+String.format(" %d%%", percent));
    }

    private void progressSuccess(boolean enterDisplayname) {
        dcContext.endCaptureNextError();
        progressDialog.dismiss();

        if (enterDisplayname) {
            Intent intent = new Intent(getApplicationContext(), CreateProfileActivity.class);
            intent.putExtra(CreateProfileActivity.FROM_WELCOME, true);
            startActivity(intent);
        } else {
            startActivity(new Intent(getApplicationContext(), ConversationListActivity.class));
        }
        finish();
    }

    public static void maybeShowConfigurationError(Activity activity, String data2) {
        if (data2 != null && !data2.isEmpty()) {
            AlertDialog d = new AlertDialog.Builder(activity)
                .setMessage(data2)
                .setPositiveButton(android.R.string.ok, null)
                .create();
            d.show();
            try {
                //noinspection ConstantConditions
                Linkify.addLinks((TextView) d.findViewById(android.R.id.message), Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
            } catch(NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handleEvent(DcEvent event) {
        int eventId = event.getId();

        if (eventId== DcContext.DC_EVENT_IMEX_PROGRESS ) {
            long progress = event.getData1Int();
            if (progress==0/*error/aborted*/) {
                progressError(dcContext.getCapturedError());
                notificationController.close();
            }
            else if (progress<1000/*progress in permille*/) {
                progressUpdate((int)progress);
                notificationController.setProgress(1000, progress, String.format(" %d%%", (int) progress / 10));
            }
            else if (progress==1000/*done*/) {
                dcContext.maybeStartIo();
                progressSuccess(false);
                notificationController.close();
            }
        }
        else if (manualConfigure && eventId==DcContext.DC_EVENT_CONFIGURE_PROGRESS) {
            long progress = event.getData1Int();
            if (progress==1000/*done*/) {
                dcContext.maybeStartIo();
                finish(); // remove ourself from the activity stack (finishAffinity is available in API 16, we're targeting API 14)
            }
        }
        else if (!manualConfigure && eventId==DcContext.DC_EVENT_CONFIGURE_PROGRESS) {
            long progress = event.getData1Int();
            if (progress==0/*error/aborted*/) {
                progressError(event.getData2Str());
            }
            else if (progress<1000/*progress in permille*/) {
                progressUpdate((int)progress);
            }
            else if (progress==1000/*done*/) {
                dcContext.maybeStartIo();
                progressSuccess(true);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==IntentIntegrator.REQUEST_CODE) {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (scanResult == null || scanResult.getFormatName() == null) {
                return; // aborted
            }
            String qrRaw = scanResult.getContents();
            DcLot qrParsed = dcContext.checkQr(qrRaw);
            switch (qrParsed.getState()) {
                case DcContext.DC_QR_ACCOUNT:
                    String domain = qrParsed.getText1();
                    new AlertDialog.Builder(this)
                            .setMessage(getString(R.string.qraccount_ask_create_and_login, domain))
                            .setPositiveButton(R.string.ok, (dialog, which) -> startQrAccountCreation(qrRaw))
                            .setNegativeButton(R.string.cancel, null)
                            .setCancelable(false)
                            .show();
                    break;

                default:
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.qraccount_qr_code_cannot_be_used)
                            .setPositiveButton(R.string.ok, null)
                            .show();
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        AccountManager accountManager = AccountManager.getInstance();
        if (accountManager.canRollbackAccountCreation(this)) {
            accountManager.rollbackAccountCreation(this);
        } else {
            super.onBackPressed();
        }
    }
}
