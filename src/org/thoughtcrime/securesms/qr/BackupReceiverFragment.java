package org.thoughtcrime.securesms.qr;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;

import java.util.Locale;

public class BackupReceiverFragment extends Fragment implements DcEventCenter.DcEventDelegate {

    private final static String TAG = BackupProviderFragment.class.getSimpleName();

    private DcContext        dcContext;
    private TextView         statusLine;
    private ProgressBar      progressBar;
    private TextView         sameNetworkHint;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.backup_receiver_fragment, container, false);
        statusLine = view.findViewById(R.id.status_line);
        progressBar = view.findViewById(R.id.progress_bar);
        sameNetworkHint = view.findViewById(R.id.same_network_hint);

        statusLine.setText(R.string.connectivity_connecting);
        progressBar.setIndeterminate(true);

        dcContext = DcHelper.getContext(getActivity());
        DcHelper.getEventCenter(getActivity()).addObserver(DcContext.DC_EVENT_IMEX_PROGRESS, this);

        String qrCode = getActivity().getIntent().getStringExtra(BackupTransferActivity.QR_CODE);

        new Thread(() -> {
            Log.i(TAG, "##### receiveBackup() with qr: "+qrCode);
            boolean res = dcContext.receiveBackup(qrCode);
            Log.i(TAG, "##### receiveBackup() done with result: "+res);
        }).start();

        getTransferActivity().appendSSID(sameNetworkHint);

        return view;
    }

    @Override
    public void onDestroyView() {
        dcContext.stopOngoingProcess();
        super.onDestroyView();
        DcHelper.getEventCenter(getActivity()).removeObservers(this);
    }

    @Override
    public void handleEvent(@NonNull DcEvent event) {
        if (event.getId() == DcContext.DC_EVENT_IMEX_PROGRESS) {
            int permille = event.getData1Int();
            int percent = 0;
            int percentMax = 0;
            boolean hideSameNetworkHint = false;
            String statusLineText = "";

            Log.i(TAG,"DC_EVENT_IMEX_PROGRESS, " + permille);
            if (permille == 0) {
                getTransferActivity().setTransferState(BackupTransferActivity.TransferState.TRANSFER_ERROR);
                getTransferActivity().showLastErrorAlert("Receiving Error");
            } else if (permille <= 50) {
                statusLineText = getString(R.string.multidevice_preparing_account); // "Connected"
                hideSameNetworkHint = true;
            } else if (permille <= 100) {
                statusLineText = getString(R.string.multidevice_account_prepared);
                hideSameNetworkHint = true;
            } else if (permille <= 950 ) {
                percent = ((permille-100)*100)/850;
                percentMax = 100;
                statusLineText = getString(R.string.multidevice_transferring) + String.format(Locale.getDefault(), " %d%%", percent);
                hideSameNetworkHint = true;
            } else if (permille < 1000) {
                statusLineText = "Finishing..."; // range not used, should not happen
            } else if (permille == 1000) {
                getTransferActivity().setTransferState(BackupTransferActivity.TransferState.TRANSFER_SUCCESS);
                getTransferActivity().doFinish();
                return;
            }

            statusLine.setText(statusLineText);
            getTransferActivity().notificationController.setProgress(percentMax, percent, statusLineText);
            if (percentMax == 0) {
                progressBar.setIndeterminate(true);
            } else {
                progressBar.setIndeterminate(false);
                progressBar.setMax(percentMax);
                progressBar.setProgress(percent);
            }

            if (hideSameNetworkHint && sameNetworkHint.getVisibility() != View.GONE) {
                sameNetworkHint.setVisibility(View.GONE);
            }
        }
    }

    private BackupTransferActivity getTransferActivity() {
        return (BackupTransferActivity) getActivity();
    }
}
