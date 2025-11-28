package org.thoughtcrime.securesms.qr;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.CompoundBarcodeView;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.ViewUtil;

public class QrScanFragment extends Fragment {

    private static final String TAG = QrScanFragment.class.getSimpleName();

    private CompoundBarcodeView barcodeScannerView;
    private MyCaptureManager capture;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.qr_scan_fragment, container, false);

        barcodeScannerView = view.findViewById(R.id.zxing_barcode_scanner);
        barcodeScannerView.setStatusText(getString(R.string.qrscan_hint) + "\n ");

        // add padding to avoid content hidden behind system bars
        ViewUtil.applyWindowInsets(barcodeScannerView.getStatusView());

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
        try {
            capture = new MyCaptureManager(activity, barcodeScannerView);
            capture.initializeFromIntent(activity.getIntent(), savedInstanceState);
            capture.decode();
        }
        catch(Exception e) {
            Log.w(TAG, e);
        }
    }

    public class MyCaptureManager extends CaptureManager {
        private final Activity myActivity;

        public MyCaptureManager(Activity activity, DecoratedBarcodeView barcodeView) {
            super(activity, barcodeView);
            myActivity = activity;
        }

        // the original implementation of displayFrameworkBugMessageAndExit() calls Activity::finish()
        // which makes _showing_ the QR-code impossible if scanning goes wrong.
        // therefore, we only show a non-disturbing error here.
        @Override
        protected void displayFrameworkBugMessageAndExit(String message) {
            if (TextUtils.isEmpty(message)) {
                message = myActivity.getString(R.string.zxing_msg_camera_framework_bug);
            }
            Toast.makeText(myActivity, message, Toast.LENGTH_SHORT).show();
        }
    }

}
