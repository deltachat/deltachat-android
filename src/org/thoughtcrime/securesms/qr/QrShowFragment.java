package org.thoughtcrime.securesms.qr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

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

    private String hint;

    private String errorHint;

    private TextView hintBelowQr;

    private BroadcastReceiver broadcastReceiver;

    private Bitmap bitmap;

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

        errorHint = getString(R.string.qrshow_join_contact_no_connection_hint);

        if (chatId != 0) {
            // verified-group
            String groupName = dcContext.getChat(chatId).getName();
            hint = String.format(this.getString(R.string.qrshow_join_group_hint), groupName);
        } else {
            // verify-contact
            String selfName = DcHelper.get(getActivity(), DcHelper.CONFIG_DISPLAY_NAME); // we cannot use MrContact.getDisplayName() as this would result in "Me" instead of
            String nameAndAddress;
            if (selfName.isEmpty()) {
                selfName = DcHelper.get(getActivity(), DcHelper.CONFIG_ADDRESS, "unknown");
                nameAndAddress = selfName;
            } else {
                nameAndAddress = String.format("%s (%s)", selfName, DcHelper.get(getActivity(), DcHelper.CONFIG_ADDRESS));
            }
            hint = String.format(this.getString(R.string.qrshow_join_contact_hint), nameAndAddress);
        }
        hintBelowQr = view.findViewById(R.id.qrShowHint);
        setHintText();

        dcEventCenter.addObserver(DcContext.DC_EVENT_SECUREJOIN_INVITER_PROGRESS, this);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setHintText();
            }
        };
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));

        numJoiners = 0;

        ImageView imageView = view.findViewById(R.id.qrImage);
        try {
            bitmap = encodeAsBitmap(dcContext.getSecurejoinQr(chatId));
            imageView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return view;
    }

    public void shareQr() {
        try {
            File file = new File(getActivity().getCacheDir(), "share-qr.png");
            file.createNewFile();
            file.setReadable(true, false);
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
            Uri uri = FileProviderUtil.getUriFor(getActivity(), file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_TEXT, hint);
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

    private void setHintText() {
        if (!DcHelper.isNetworkConnected(getContext())) {
            hintBelowQr.setText(errorHint);
        } else {
            hintBelowQr.setText(hint);
        }
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
        getActivity().unregisterReceiver(broadcastReceiver);
    }

    private Bitmap encodeAsBitmap(String str) throws WriterException {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(str,
                    BarcodeFormat.QR_CODE, WIDTH, HEIGHT, null);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }

        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);

        Bitmap overlay = BitmapFactory.decodeResource(this.getResources(), R.drawable.qr_overlay);
        putOverlay(bitmap, overlay);

        return bitmap;
    }

    private void putOverlay(Bitmap bitmap, Bitmap overlay) {
        int bw = bitmap.getWidth();
        int bh = bitmap.getHeight();
        int ow = bw / 6;
        int oh = bh / 6;

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(overlay, null, new Rect(bw / 2 - ow / 2, bh / 2 - oh / 2, bw / 2 + ow / 2, bh / 2 + oh / 2), paint);
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
