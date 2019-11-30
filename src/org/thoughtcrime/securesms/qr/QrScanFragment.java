package org.thoughtcrime.securesms.qr;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.CompoundBarcodeView;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.permissions.Permissions;

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

		barcodeScannerView = getActivity().findViewById(R.id.zxing_barcode_scanner);
		barcodeScannerView.setStatusText(getString(R.string.qrscan_hint) + "\n ");


		if (savedInstanceState != null) {
			init(barcodeScannerView, getActivity().getIntent(), savedInstanceState);
		}

		Permissions.with(this)
				.request(Manifest.permission.CAMERA)
				.ifNecessary()
				.onAnyResult(this::handleQrScanWithPermissions)
				.onAnyDenied(this::handleQrScanWithDeniedPermission)
				.execute();

		return view;
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



	private void handleQrScanWithPermissions() {
		init(barcodeScannerView, getActivity().getIntent(), null);
	}


	private void init(CompoundBarcodeView barcodeScannerView, Intent intent, Bundle savedInstanceState) {
		capture = new CaptureManager(getActivity(), barcodeScannerView);
		capture.initializeFromIntent(intent, savedInstanceState);
		capture.decode();
	}


	private void handleQrScanWithDeniedPermission() {
		getActivity().setResult(Activity.RESULT_CANCELED);
		getActivity().finish();
	}

}
