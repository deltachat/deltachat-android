package org.thoughtcrime.securesms;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MenuItem;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.CompoundBarcodeView;

import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;

public class QrScanActivity extends AppCompatActivity {

    private final DynamicTheme dynamicTheme    = new DynamicTheme();
    private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

    private CaptureManager capture;

    private CompoundBarcodeView barcodeScannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dynamicTheme.onCreate(this);
        dynamicLanguage.onCreate(this);

        setContentView(R.layout.activity_qr_scan);

        assert getSupportActionBar() != null;
        getSupportActionBar().setTitle(R.string.qrscan_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner);
        barcodeScannerView.setStatusText(getString(R.string.qrscan_hint) + "\n ");

        if (savedInstanceState != null) {
            init(barcodeScannerView, getIntent(), savedInstanceState);
        }

        Permissions.with(this)
                .request(Manifest.permission.CAMERA)
                .ifNecessary()
                .onAnyResult(this::handleQrScanWithPermissions)
                .onAnyDenied(this::handleQrScanWithDeniedPermission)
                .execute();
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

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private void init(CompoundBarcodeView barcodeScannerView, Intent intent, Bundle savedInstanceState) {
        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(intent, savedInstanceState);
        capture.decode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        dynamicTheme.onResume(this);
        dynamicLanguage.onResume(this);
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
