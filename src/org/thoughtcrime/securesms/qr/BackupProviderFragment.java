package org.thoughtcrime.securesms.qr;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.b44t.messenger.DcBackupProvider;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;

import com.caverock.androidsvg.SVGImageView;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Util;

import java.util.Locale;

public class BackupProviderFragment extends Fragment implements DcEventCenter.DcEventDelegate {

    private final static String TAG = BackupProviderFragment.class.getSimpleName();

    private DcContext        dcContext;
    private DcBackupProvider dcBackupProvider;

    private TextView         statusLine;
    private ProgressBar      progressBar;
    private View             topText;
    private SVGImageView     qrImageView;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.backup_provider_fragment, container, false);
        statusLine = view.findViewById(R.id.status_line);
        progressBar = view.findViewById(R.id.progress_bar);
        topText = view.findViewById(R.id.top_text);
        qrImageView = view.findViewById(R.id.qrImage);
        setHasOptionsMenu(true);

        statusLine.setText(R.string.preparing_account);
        progressBar.setIndeterminate(true);

        dcContext = DcHelper.getContext(getActivity());
        DcHelper.getEventCenter(getActivity()).addObserver(DcContext.DC_EVENT_IMEX_PROGRESS, this);

        new Thread(() -> {
            Log.i(TAG, "##### newBackupProvider()");
            dcBackupProvider = dcContext.newBackupProvider();
            Log.i(TAG, "##### newBackupProvider() returned");
            Util.runOnMain(() -> {
                progressBar.setVisibility(View.GONE);
                if (!dcBackupProvider.isOk()) {
                    getTransferActivity().setTransferState(BackupTransferActivity.TransferState.TRANSFER_ERROR);
                    getTransferActivity().showLastErrorAlert("Cannot create backup provider");
                    return;
                }
                statusLine.setVisibility(View.GONE);
                topText.setVisibility(View.VISIBLE);
                try {
                    SVG svg = SVG.getFromString(QrShowFragment.fixSVG(dcBackupProvider.getQrSvg()));
                    qrImageView.setSVG(svg);
                } catch (SVGParseException e) {
                    e.printStackTrace();
                }
                new Thread(() -> {
                    Log.i(TAG, "##### waitForReceiver() with qr: "+dcBackupProvider.getQr());
                    dcBackupProvider.waitForReceiver();
                    Log.i(TAG, "##### done waiting");
                }).start();
            });
        }).start();

        BackupTransferActivity.appendSSID(getActivity(), view.findViewById(R.id.same_network_hint));

        return view;
    }

    @Override
    public void onDestroyView() {
        dcContext.stopOngoingProcess();
        dcBackupProvider.unref();
        super.onDestroyView();
        DcHelper.getEventCenter(getActivity()).removeObservers(this);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.copy).setVisible(qrImageView.getVisibility() == View.VISIBLE);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
      super.onOptionsItemSelected(item);

      switch (item.getItemId()) {
        case R.id.copy:
          if (dcBackupProvider != null) {
              Util.writeTextToClipboard(getActivity(), dcBackupProvider.getQr());
              Toast.makeText(getActivity(), getString(R.string.done), Toast.LENGTH_SHORT).show();
              getTransferActivity().warnAboutCopiedQrCodeOnAbort = true;
          }
          return true;
      }

      return false;
    }

    @Override
    public void handleEvent(@NonNull DcEvent event) {
        if (event.getId() == DcContext.DC_EVENT_IMEX_PROGRESS) {
            int permille = event.getData1Int();
            int percent = 0;
            int percentMax = 0;
            boolean hideQrCode = false;
            String statusLineText = "";

            Log.i(TAG,"DC_EVENT_IMEX_PROGRESS, " + permille);
            if (permille == 0) {
                getTransferActivity().setTransferState(BackupTransferActivity.TransferState.TRANSFER_ERROR);
                getTransferActivity().showLastErrorAlert("Sending Error");
                hideQrCode = true;
            } else if(permille <= 350) {
                statusLineText = getString(R.string.preparing_account);
            } else if(permille <= 400) {
                statusLine.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                statusLineText = getString(R.string.waiting_for_receiver);
            } else if(permille <= 450) {
                statusLineText = getString(R.string.receiver_connected);
                hideQrCode = true;
            } else if (permille < 1000) {
                percent = (permille-450)/5;
                percentMax = 100;
                statusLineText = getString(R.string.transferring) + String.format(Locale.getDefault(), " %d%%", percent);
                hideQrCode = true;
            } else if (permille == 1000) {
                statusLineText = getString(R.string.done) + " \uD83D\uDE00";
                getTransferActivity().setTransferState(BackupTransferActivity.TransferState.TRANSFER_SUCCESS);
                progressBar.setVisibility(View.GONE);
                hideQrCode = true;
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

            if (hideQrCode && qrImageView.getVisibility() != View.GONE) {
                qrImageView.setVisibility(View.GONE);
                topText.setVisibility(View.GONE);
                statusLine.setVisibility(View.VISIBLE);
                progressBar.setVisibility(permille == 1000 ? View.GONE : View.VISIBLE);
            }
        }
    }

    private BackupTransferActivity getTransferActivity() {
        return (BackupTransferActivity) getActivity();
    }
}
