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
    private boolean          transferStarted;
    private SVGImageView     qrImageView;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // keeping the screen on also avoids falling back from IDLE to POLL
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.backup_provider_fragment, container, false);
        statusLine = view.findViewById(R.id.status_line);
        qrImageView = view.findViewById(R.id.qrImage);

        statusLine.setText(R.string.one_moment);

        dcContext = DcHelper.getContext(getActivity());
        dcContext.stopIo();
        DcHelper.getEventCenter(getActivity()).addObserver(DcContext.DC_EVENT_IMEX_PROGRESS, this);

        new Thread(() -> {
            Log.i(TAG, "##### newBackupProvider()");
            dcBackupProvider = dcContext.newBackupProvider();
            Log.i(TAG, "##### newBackupProvider() returned");
            if (dcBackupProvider != null) {
                Util.runOnMain(() -> {
                    statusLine.setVisibility(View.GONE);
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
            }
        }).start();

        return view;
    }

    @Override
    public void onDestroyView() {
        dcContext.stopOngoingProcess();
        dcContext.startIo();
        dcBackupProvider.unref();
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
                new AlertDialog.Builder(getActivity())
                  .setMessage(dcContext.getLastError())
                  .setPositiveButton(android.R.string.ok, null)
                  .setCancelable(false)
                  .show();
            } else if(permille < 500) {
                percent = permille/5;
                percentMax = 100;
                statusLineText = String.format(Locale.getDefault(), "Prepare... %d%%", percent);
            } else if(permille == 500) {
                statusLineText = String.format(Locale.getDefault(), "Waiting for connection...");
            } else if (permille < 1000) {
                percent = (permille-500)/5;
                percentMax = 100;
                statusLineText = String.format(Locale.getDefault(), "Transfer... %d%%", percent);
                if (!transferStarted) {
                    qrImageView.setVisibility(View.GONE);
                    statusLine.setVisibility(View.VISIBLE);
                    transferStarted = true;
                }
            } else if (permille == 1000) {
                statusLineText = "Done.";
            }

            statusLine.setText(statusLineText);
            ((BackupProviderActivity)getActivity()).notificationController.setProgress(percentMax, percent, statusLineText);
        }
    }
}
