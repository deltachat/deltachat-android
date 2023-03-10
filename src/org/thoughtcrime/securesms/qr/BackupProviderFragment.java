package org.thoughtcrime.securesms.qr;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

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

public class BackupProviderFragment extends Fragment implements DcEventCenter.DcEventDelegate {

    private final static String TAG = BackupProviderFragment.class.getSimpleName();

    private DcContext        dcContext;
    private DcBackupProvider dcBackupProvider;

    private TextView         statusLine;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // keeping the screen on also avoids falling back from IDLE to POLL
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.backup_provider_fragment, container, false);
        statusLine = view.findViewById(R.id.status_line);
        statusLine.setText(R.string.one_moment);

        dcContext = DcHelper.getContext(getActivity());
        dcContext.stopIo();
        DcHelper.getEventCenter(getActivity()).addObserver(DcContext.DC_EVENT_IMEX_PROGRESS, this);

        new Thread(() -> {
            dcBackupProvider = dcContext.newBackupProvider();
            if (dcBackupProvider != null) {
                Util.runOnMain(() -> {
                    statusLine.setVisibility(View.GONE);
                    SVGImageView imageView = view.findViewById(R.id.qrImage);
                    try {
                        SVG svg = SVG.getFromString(QrShowFragment.fixSVG(dcBackupProvider.getQrSvg()));
                        imageView.setSVG(svg);
                    } catch (SVGParseException e) {
                        e.printStackTrace();
                    }
                    new Thread(() -> {
                        dcBackupProvider.waitForReceiver();
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
            Log.i(TAG,"DC_EVENT_IMEX_PROGRESS, " + event.getData1Int());
            // TODO: update status line once core sends events during prepare and wait
        }
    }
}
