package org.thoughtcrime.securesms.qr;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.CompoundBarcodeView;

import org.thoughtcrime.securesms.R;

public class QrScanFragment extends Fragment {

    private CompoundBarcodeView barcodeScannerView;
    private CaptureManager capture;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.qr_scan_fragment, container, false);

        barcodeScannerView = view.findViewById(R.id.zxing_barcode_scanner);
        barcodeScannerView.setStatusText(getString(R.string.qrscan_hint) + "\n ");

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        init(barcodeScannerView, requireActivity(), savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (capture != null) {
            capture.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (capture != null) {
            capture.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (capture != null) {
            capture.onDestroy();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (capture != null) {
            capture.onSaveInstanceState(outState);
        }
    }

    void handleQrScanWithPermissions(Activity activity) {
        if (barcodeScannerView != null)
            init(barcodeScannerView, activity, null);
    }

    private void init(CompoundBarcodeView barcodeScannerView, Activity activity, Bundle savedInstanceState) {
        capture = new CaptureManager(activity, barcodeScannerView);
        capture.initializeFromIntent(activity.getIntent(), savedInstanceState);
        capture.decode();
    }
}
