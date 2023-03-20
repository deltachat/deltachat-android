package org.thoughtcrime.securesms.qr;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.backup_provider_fragment, container, false);
        statusLine = view.findViewById(R.id.status_line);

        statusLine.setText("Connecting...");

        dcContext = DcHelper.getContext(getActivity());
        DcHelper.getEventCenter(getActivity()).addObserver(DcContext.DC_EVENT_IMEX_PROGRESS, this);

        String qrCode = getActivity().getIntent().getStringExtra(BackupTransferActivity.QR_CODE);

        new Thread(() -> {
            Log.i(TAG, "##### receiveBackup() with qr: "+qrCode);
            boolean res = dcContext.receiveBackup(qrCode);
            Log.i(TAG, "##### receiveBackup() done with result: "+res);
        }).start();

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
            String statusLineText = "";

            Log.i(TAG,"DC_EVENT_IMEX_PROGRESS, " + permille);
            if (permille == 0) {
                ((BackupTransferActivity)getActivity()).setTransferState(BackupTransferActivity.TransferState.TRANSFER_ERROR);
                new AlertDialog.Builder(getActivity())
                    .setMessage(dcContext.getLastError())
                    .setPositiveButton(android.R.string.ok, null)
                    .setCancelable(false)
                    .show();
            } else if (permille < 1000) {
                percent = permille/10;
                percentMax = 100;
                statusLineText = String.format(Locale.getDefault(), "Transfer... %d%%", percent);
            } else if (permille == 1000) {
                ((BackupTransferActivity)getActivity()).setTransferState(BackupTransferActivity.TransferState.TRANSFER_SUCCESS);
                ((BackupTransferActivity)getActivity()).doFinish();
                return;
            }

            statusLine.setText(statusLineText);
            ((BackupTransferActivity)getActivity()).notificationController.setProgress(percentMax, percent, statusLineText);
        }
    }
}
