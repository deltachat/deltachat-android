package org.thoughtcrime.securesms.qr;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;

import com.caverock.androidsvg.SVGImageView;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.FileProviderUtil;
import org.thoughtcrime.securesms.util.Util;

import java.io.File;
import java.io.FileOutputStream;

public class QrShowFragment extends Fragment implements DcEventCenter.DcEventDelegate {

    private final static String TAG = QrShowFragment.class.getSimpleName();
    public final static int WHITE = 0xFFFFFFFF;
    private final static int BLACK = 0xFF000000;
    private final static int WIDTH = 400;
    private final static int HEIGHT = 400;
    private final static String CHAT_ID = "chat_id";

    private int chatId = 0;

    private int numJoiners;

    private DcEventCenter dcEventCenter;

    private DcContext dcContext;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // keeping the screen on also avoids falling back from IDLE to POLL
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.qr_show_fragment, container, false);

        dcContext = DcHelper.getContext(getActivity());
        dcEventCenter = DcHelper.getEventCenter(getActivity());

        Bundle extras = getActivity().getIntent().getExtras();
        if (extras != null) {
            chatId = extras.getInt(CHAT_ID);
        }

        dcEventCenter.addObserver(DcContext.DC_EVENT_SECUREJOIN_INVITER_PROGRESS, this);

        numJoiners = 0;

        SVGImageView imageView = view.findViewById(R.id.qrImage);
        try {
            SVG svg = SVG.getFromString(dcContext.getSecurejoinQrSvg(chatId));
            imageView.setSVG(svg);
        } catch (SVGParseException e) {
            e.printStackTrace();
        }

        return view;
    }

    public void shareQr() {
        try {
            File file = new File(getActivity().getExternalCacheDir(), "share-qr.png");
            file.createNewFile();
            file.setReadable(true, false);
            FileOutputStream stream = new FileOutputStream(file);
            Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawRGB(255, 255, 255);  // Clear background to white
            SVG svg = SVG.getFromString(dcContext.getSecurejoinQrSvg(chatId));
            svg.renderToCanvas(canvas);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
            Uri uri = FileProviderUtil.getUriFor(getActivity(), file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, getString(R.string.chat_share_with_title)));
        } catch (Exception e) {
            Log.e(TAG, "failed to share QR", e);
        }
    }

    public void copyQrData() {
        Util.writeTextToClipboard(getActivity(), DcHelper.getContext(getActivity()).getSecurejoinQr(chatId));
        Toast.makeText(getActivity(), getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!DcHelper.isNetworkConnected(getContext())) {
            Toast.makeText(getActivity(), R.string.qrshow_join_contact_no_connection_toast, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dcEventCenter.removeObservers(this);
    }

    @Override
    public void handleEvent(@NonNull DcEvent event) {
        if (event.getId() == DcContext.DC_EVENT_SECUREJOIN_INVITER_PROGRESS) {
            DcContext dcContext = DcHelper.getContext(getActivity());
            int contact_id = event.getData1Int();
            long progress = event.getData2Int();
            String msg = null;
            if (progress == 300) {
                msg = String.format(getString(R.string.qrshow_x_joining), dcContext.getContact(contact_id).getNameNAddr());
                numJoiners++;
            } else if (progress == 600) {
                msg = String.format(getString(R.string.qrshow_x_verified), dcContext.getContact(contact_id).getNameNAddr());
            } else if (progress == 800) {
                msg = String.format(getString(R.string.qrshow_x_has_joined_group), dcContext.getContact(contact_id).getNameNAddr());
            }

            if (msg != null) {
                Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            }

            if (progress == 1000) {
                numJoiners--;
                if (numJoiners <= 0) {
                    if (getActivity() != null) getActivity().finish();
                }
            }
        }

    }


}
