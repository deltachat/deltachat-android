package org.thoughtcrime.securesms;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcLot;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.AttachmentManager;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.qr.BackupTransferActivity;
import org.thoughtcrime.securesms.qr.RegistrationQrActivity;
import org.thoughtcrime.securesms.service.GenericForegroundService;
import org.thoughtcrime.securesms.service.NotificationController;
import org.thoughtcrime.securesms.util.StorageUtil;
import org.thoughtcrime.securesms.util.StreamUtil;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.views.ProgressDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class WelcomeActivity extends BaseActionBarActivity implements DcEventCenter.DcEventDelegate {
    public static final String BACKUP_QR_EXTRA = "backup_qr_extra";
    public static final int PICK_BACKUP = 20574;
    private final static String TAG = WelcomeActivity.class.getSimpleName();
    public static final String TMP_BACKUP_FILE = "tmp-backup-file";
    public static final String DC_REQUEST_ACCOUNT_DATA = "chat.delta.DC_REQUEST_ACCOUNT_DATA";

    private ProgressDialog progressDialog = null;
    private boolean imexUserAborted;
    DcContext dcContext;
    private NotificationController notificationController;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.welcome_activity);

        Button signUpButton = findViewById(R.id.signup_button);
        Button signInButton = findViewById(R.id.signin_button);

        View view = View.inflate(this, R.layout.login_options_view, null);
        AlertDialog signInDialog = new AlertDialog.Builder(this)
          .setView(view)
          .setTitle(R.string.onboarding_alternative_logins)
          .setNegativeButton(R.string.cancel, null)
          .create();
        view.findViewById(R.id.add_as_second_device_button).setOnClickListener((v) -> {
          startAddAsSecondDeviceActivity();
          signInDialog.dismiss();
        });
        view.findViewById(R.id.backup_button).setOnClickListener((v) -> {
          startImportBackup();
          signInDialog.dismiss();
        });
        view.findViewById(R.id.login_button).setOnClickListener((v) -> {
          startRegistrationActivity();
          signInDialog.dismiss();
        });

        signUpButton.setOnClickListener((v) -> startInstantOnboardingActivity());
        signInButton.setOnClickListener((v) -> signInDialog.show());

        registerForEvents();
        initializeActionBar();

        if (!DcHelper.hasAnyConfiguredContext(this)) {
          Intent intent = new Intent();
          // Since android API 26 only explicit broadcasts are allowed for IPC with a few exceptions.
          // As a result we have to send for each companion app we want to support an intent with a
          // specified package
          intent.setPackage("chat.delta.androidyggmail");
          intent.setAction(DC_REQUEST_ACCOUNT_DATA);
          sendBroadcast(intent);
        }
    }

    protected void initializeActionBar() {
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar == null) throw new AssertionError();

        boolean canGoBack = AccountManager.getInstance().canRollbackAccountCreation(this);
        supportActionBar.setDisplayHomeAsUpEnabled(canGoBack);
        getSupportActionBar().setTitle(canGoBack? R.string.add_account : R.string.welcome_desktop);
    }

    private void registerForEvents() {
        dcContext = DcHelper.getContext(this);
        DcHelper.getEventCenter(this).addObserver(DcContext.DC_EVENT_IMEX_PROGRESS, this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        }

        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        String backupQr = getIntent().getStringExtra(BACKUP_QR_EXTRA);
        if (backupQr != null) {
            getIntent().removeExtra(BACKUP_QR_EXTRA);
            startBackupTransfer(backupQr);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DcHelper.getEventCenter(this).removeObservers(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private void startRegistrationActivity() {
        Intent intent = new Intent(this, RegistrationActivity.class);
        startActivity(intent);
    }

    private void startInstantOnboardingActivity() {
        Intent intent = new Intent(this, InstantOnboardingActivity.class);
        intent.putExtra(InstantOnboardingActivity.FROM_WELCOME, true);
        startActivity(intent);
    }

    private void startAddAsSecondDeviceActivity() {
        new IntentIntegrator(this).setCaptureActivity(RegistrationQrActivity.class)
          .addExtra(RegistrationQrActivity.ADD_AS_SECOND_DEVICE_EXTRA, true)
          .initiateScan();
    }

    @SuppressLint("InlinedApi")
    private void startImportBackup() {
        Permissions.with(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .alwaysGrantOnSdk30()
                .ifNecessary()
                .withPermanentDenialDialog(getString(R.string.perm_explain_access_to_storage_denied))
                .onAllGranted(() -> {
                    File imexDir = DcHelper.getImexDir();
                    if (Build.VERSION.SDK_INT >= 30) {
                        AttachmentManager.selectMediaType(this, "application/x-tar", null, PICK_BACKUP, StorageUtil.getDownloadUri());
                    } else {
                        final String backupFile = dcContext.imexHasBackup(imexDir.getAbsolutePath());
                        if (backupFile != null) {


                            View gl = View.inflate(this, R.layout.dialog_with_checkbox, null);
                            CheckBox encryptCheckbox = gl.findViewById(R.id.dialog_checkbox);
                            TextView msg = gl.findViewById(R.id.dialog_message);

                            // If we'd use both `setMessage()` and `setView()` on the same AlertDialog, on small screens the
                            // "OK" and "Cancel" buttons would not be show. So, put the message into our custom view:
                            msg.setText(String.format(getResources().getString(R.string.import_backup_ask), backupFile) );
                            encryptCheckbox.setText("Encrypt database (highly experimental, use at your own risk)");
                            int[]      tintAttr   = new int[]{android.R.attr.textColorSecondary};
                            TypedArray typedArray = obtainStyledAttributes(tintAttr);
                            int        color      = typedArray.getColor(0, Color.GRAY);
                            typedArray.recycle();
                            encryptCheckbox.setTextColor(color);

                            new AlertDialog.Builder(this)
                                    .setTitle(R.string.import_backup_title)
                                    .setView(gl)
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .setPositiveButton(android.R.string.ok, (dialog, which) -> startImport(backupFile, null, encryptCheckbox.isChecked()))
                                    .show();
                        }
                        else {
                            new AlertDialog.Builder(this)
                                    .setTitle(R.string.import_backup_title)
                                    .setMessage(String.format(getResources().getString(R.string.import_backup_no_backup_found), imexDir.getAbsolutePath()))
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show();
                        }
                    }
                })
                .execute();
    }

    private void startImport(@Nullable final String backupFile, final @Nullable Uri backupFileUri, boolean encrypt)
    {
        notificationController = GenericForegroundService.startForegroundTask(this, getString(R.string.import_backup_title));

        if (encrypt) {
            AccountManager accountManager = AccountManager.getInstance();

            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog = null;
            }

            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.one_moment));
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(false);
            progressDialog.show();

            Util.runOnBackground(() -> {
                DcHelper.getEventCenter(this).removeObservers(this);
                accountManager.switchToEncrypted(this);
                // Event center changed, register for events again
                registerForEvents();
                Util.runOnMain(() -> continueStartBackup(backupFile, backupFileUri));
            });
        } else {
            continueStartBackup(backupFile, backupFileUri);
        }

    }

    private void continueStartBackup(String backupFile, Uri backupFileUri) {
        if( progressDialog!=null ) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        imexUserAborted = false;
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.one_moment));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(android.R.string.cancel), (dialog, which) -> {
            imexUserAborted = true;
            dcContext.stopOngoingProcess();
            notificationController.close();
            cleanupTempBackupFile();
        });
        progressDialog.show();

        Util.runOnBackground(() -> {
            String file = backupFile;
            if (backupFile == null) {
                try {
                    file = copyToCacheDir(backupFileUri).getAbsolutePath();
                } catch (IOException e) {
                    e.printStackTrace();
                    notificationController.close();
                    cleanupTempBackupFile();
                    return;
                }
            }

            DcHelper.getEventCenter(this).captureNextError();
            dcContext.imex(DcContext.DC_IMEX_IMPORT_BACKUP, file);
        });
    }

    private File copyToCacheDir(Uri uri) throws IOException {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            File file = File.createTempFile(TMP_BACKUP_FILE, ".tmp", getCacheDir());
            try (OutputStream outputStream = new FileOutputStream(file)) {
                StreamUtil.copy(inputStream, outputStream);
            }
            return file;
        }
    }

    private void startBackupTransfer(String qrCode)
    {
        if (progressDialog!=null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        Intent intent = new Intent(this, BackupTransferActivity.class);
        intent.putExtra(BackupTransferActivity.TRANSFER_MODE, BackupTransferActivity.TransferMode.RECEIVER_SCAN_QR.getInt());
        intent.putExtra(BackupTransferActivity.QR_CODE, qrCode);
        startActivity(intent);
    }

    private void progressError(String data2) {
        progressDialog.dismiss();
        maybeShowConfigurationError(this, data2);
    }

    private void progressUpdate(int progress) {
        int percent = progress / 10;
        progressDialog.setMessage(getResources().getString(R.string.one_moment)+String.format(" %d%%", percent));
    }

    private void progressSuccess() {
        DcHelper.getEventCenter(this).endCaptureNextError();
        progressDialog.dismiss();
        Intent intent = new Intent(getApplicationContext(), ConversationListActivity.class);
        intent.putExtra(ConversationListActivity.FROM_WELCOME, true);
        startActivity(intent);
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
    public void handleEvent(@NonNull DcEvent event) {
        int eventId = event.getId();

        if (eventId== DcContext.DC_EVENT_IMEX_PROGRESS ) {
            long progress = event.getData1Int();
            if (progressDialog == null || notificationController == null) {
                // IMEX runs in BackupTransferActivity
                if (progress == 1000) {
                    finish();  // transfer done - remove ourself from the activity stack (finishAffinity is available in API 16, we're targeting API 14)
                }
                return;
            }
            if (progress==0/*error/aborted*/) {
                if (!imexUserAborted) {
                  progressError(dcContext.getLastError());
                }
                notificationController.close();
                cleanupTempBackupFile();
            }
            else if (progress<1000/*progress in permille*/) {
                progressUpdate((int)progress);
                notificationController.setProgress(1000, progress, String.format(" %d%%", (int) progress / 10));
            }
            else if (progress==1000/*done*/) {
                DcHelper.getAccounts(this).startIo();
                progressSuccess();
                notificationController.close();
                cleanupTempBackupFile();
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
                case DcContext.DC_QR_BACKUP:
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.multidevice_receiver_title)
                            .setMessage(R.string.multidevice_receiver_scanning_ask)
                            .setPositiveButton(R.string.perm_continue, (dialog, which) -> startBackupTransfer(qrRaw))
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
        } else if (requestCode == PICK_BACKUP) {
            Uri uri = (data != null ? data.getData() : null);
            if (uri == null) {
                Log.e(TAG, " Can't import null URI");
                return;
            }
            startImport(null, uri, false);
        }
    }

    private void cleanupTempBackupFile() {
        try {
            File[] files = getCacheDir().listFiles((dir, name) -> name.startsWith(TMP_BACKUP_FILE));
            for (File file : files) {
                if (file.getName().endsWith("tmp")) {
                    Log.i(TAG, "Deleting temp backup file " + file);
                    file.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
