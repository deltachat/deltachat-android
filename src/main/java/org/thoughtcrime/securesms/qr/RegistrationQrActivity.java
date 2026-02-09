package org.thoughtcrime.securesms.qr;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcLot;
import com.journeyapps.barcodescanner.CompoundBarcodeView;

import org.thoughtcrime.securesms.BaseActionBarActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

public class RegistrationQrActivity extends BaseActionBarActivity {

    public static final String ADD_AS_SECOND_DEVICE_EXTRA = "add_as_second_device";
    public static final String QRDATA_EXTRA = "qrdata";

    private CustomCaptureManager capture;

    private CompoundBarcodeView barcodeScannerView;

    private DcContext dcContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean addAsAnotherDevice = getIntent().getBooleanExtra(ADD_AS_SECOND_DEVICE_EXTRA, false);
        if (addAsAnotherDevice) {
            setContentView(R.layout.activity_registration_2nd_device_qr);
            getSupportActionBar().setTitle(R.string.multidevice_receiver_title);
        } else {
            setContentView(R.layout.activity_registration_qr);
            getSupportActionBar().setTitle(R.string.scan_invitation_code);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // add padding to avoid content hidden behind system bars
        ViewUtil.applyWindowInsets(findViewById(R.id.layout_container), true, false, true, true);

        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner);
        barcodeScannerView.setStatusText(getString(R.string.qrscan_hint) + "\n ");

        View sameNetworkHint = findViewById(R.id.same_network_hint);
        if (sameNetworkHint != null) {
            BackupTransferActivity.appendSSID(this, findViewById(R.id.same_network_hint));
        }

        if (savedInstanceState != null) {
            init(barcodeScannerView, getIntent(), savedInstanceState);
        }

        Permissions.with(this)
                .request(Manifest.permission.CAMERA)
                .ifNecessary()
                .withPermanentDenialDialog(getString(R.string.perm_explain_access_to_camera_denied))
                .onAnyResult(this::handleQrScanWithPermissions)
                .onAnyDenied(this::handleQrScanWithDeniedPermission)
                .execute();

        dcContext = DcHelper.getContext(this);
    }

    private void handleQrScanWithPermissions() {
        init(barcodeScannerView, getIntent(), null);
    }

    private void handleQrScanWithDeniedPermission() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

      int itemId = item.getItemId();
      if (itemId == android.R.id.home) {
        finish();
        return true;
      } else if (itemId == R.id.troubleshooting) {
        DcHelper.openHelp(this, "#multiclient");
        return true;
      } else if (itemId == R.id.menu_paste) {
        String rawQr = Util.getTextFromClipboard(this);

        Runnable okCallback = () -> {
          Intent intent = new Intent();
          intent.putExtra(QRDATA_EXTRA, rawQr);
          setResult(Activity.RESULT_OK, intent);
          finish();
        };

        showConfirmDialog(rawQr, okCallback, null);

        return true;
      }

        return false;
    }

    private void showConfirmDialog(String rawQr, @NonNull Runnable okCallback, @Nullable Runnable cancelCallback) {
      DcLot qrParsed = dcContext.checkQr(rawQr);

      String dialogMsg = "";
      if (qrParsed.getState() == DcContext.DC_QR_ASK_VERIFYCONTACT) {
        String name = dcContext.getContact(qrParsed.getId()).getDisplayName();
        dialogMsg = getString(R.string.instant_onboarding_confirm_contact, name);
      } else if (qrParsed.getState() == DcContext.DC_QR_ASK_VERIFYGROUP) {
        String groupName = qrParsed.getText1();
        dialogMsg = getString(R.string.instant_onboarding_confirm_group, groupName);
      }

      if (qrParsed.getState() == DcContext.DC_QR_ASK_VERIFYCONTACT
        || qrParsed.getState() == DcContext.DC_QR_ASK_VERIFYGROUP) {
        AlertDialog confirmDialog = new AlertDialog.Builder(this)
          .setMessage(dialogMsg)
          .setPositiveButton("OK", (dialog, which) -> {
            okCallback.run();
          })
          .setNegativeButton("Cancel", (dialog, which) -> {
            if (cancelCallback != null) {
              cancelCallback.run();
            }
          })
          .show();
      } else {
        okCallback.run();
      }
    }

  @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private void init(CompoundBarcodeView barcodeScannerView, Intent intent, Bundle savedInstanceState) {
        capture = new CustomCaptureManager(this, barcodeScannerView);

        capture.setResultInterceptor((result, finishCallback) -> {
          String rawQr = result.getText();

          showConfirmDialog(rawQr, finishCallback, () -> {
            barcodeScannerView.resume();
            capture.decode();
          });
        });

        capture.initializeFromIntent(intent, savedInstanceState);
        capture.decode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (capture != null) {
            capture.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (capture != null) {
            capture.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (capture != null) {
            capture.onDestroy();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.registration_qr_activity, menu);
        boolean addAsAnotherDevice = getIntent().getBooleanExtra(ADD_AS_SECOND_DEVICE_EXTRA, false);
        menu.findItem(R.id.troubleshooting).setVisible(addAsAnotherDevice);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (capture != null) {
            capture.onSaveInstanceState(outState);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }
}
