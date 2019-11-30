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
import android.os.Bundle;
import android.text.Html;
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
import com.b44t.messenger.DcEventCenter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;

public class QrShowFragment extends Fragment implements DcEventCenter.DcEventDelegate {

	public final static int WHITE = 0xFFFFFFFF;
	public final static int BLACK = 0xFF000000;
	public final static int WIDTH = 400;
	public final static int HEIGHT = 400;
	public final static String CHAT_ID = "chat_id";

	public int numJoiners;

	DcEventCenter dcEventCenter;

	ApplicationDcContext dcContext;

	private String hint;

	private String errorHint;

	private TextView hintBelowQr;

	private BroadcastReceiver broadcastReceiver;


	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // keeping the screen on also avoids falling back from IDLE to POLL

		dcContext = DcHelper.getContext(getActivity());
		dcEventCenter = dcContext.eventCenter;

		Bundle extras = getActivity().getIntent().getExtras();
		int chatId = 0;
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
		hintBelowQr = getActivity().findViewById(R.id.qrShowHint);
		setHintText();

		numJoiners = 0;

		ImageView imageView = getActivity().findViewById(R.id.qrImage);
		try {
			Bitmap bitmap = encodeAsBitmap(dcContext.getSecurejoinQr(chatId));
			imageView.setImageBitmap(bitmap);
		} catch (WriterException e) {
			e.printStackTrace();
		}

		dcEventCenter.addObserver(DcContext.DC_EVENT_SECUREJOIN_INVITER_PROGRESS, this);
		broadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				setHintText();
			}
		};
		getActivity().registerReceiver(broadcastReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
	}

	private void setHintText() {
		if (!dcContext.isNetworkConnected()) {
			hintBelowQr.setText(Html.fromHtml(errorHint));
		} else {
			hintBelowQr.setText(Html.fromHtml(hint));
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!dcContext.isNetworkConnected()) {
			Toast.makeText(getActivity(), R.string.qrshow_join_contact_no_connection_toast, Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		dcEventCenter.removeObservers(this);
		getActivity().unregisterReceiver(broadcastReceiver);
	}

	Bitmap encodeAsBitmap(String str) throws WriterException {
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

	public void putOverlay(Bitmap bitmap, Bitmap overlay) {
		int bw = bitmap.getWidth();
		int bh = bitmap.getHeight();
		int ow = bw / 6;
		int oh = bh / 6;

		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
		canvas.drawBitmap(overlay, null, new Rect(bw / 2 - ow / 2, bh / 2 - oh / 2, bw / 2 + ow / 2, bh / 2 + oh / 2), paint);
	}

	@Override
	public void handleEvent(int eventId, Object data1, Object data2) {
		if (eventId == DcContext.DC_EVENT_SECUREJOIN_INVITER_PROGRESS) {
			DcContext dcContext = DcHelper.getContext(getActivity());
			int contact_id = ((Long) data1).intValue();
			long progress = (Long) data2;
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
					getActivity().finish();
				}
			}
		}

	}
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.qr_show_fragment, container, false);

		return view;
	}

}
