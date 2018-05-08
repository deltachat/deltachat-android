package com.b44t.messenger;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.Hashtable;

public class QRshowActivity extends AppCompatActivity implements NotificationCenter.NotificationCenterDelegate {

    public final static int WHITE = 0xFFFFFFFF;
    public final static int BLACK = 0xFF000000;
    public final static int WIDTH = 400;
    public final static int HEIGHT = 400;

    public int num_joiners;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrshow);

        Bundle b = getIntent().getExtras();
        int chat_id = 0;
        if(b != null) {
            chat_id = b.getInt("chat_id");
        }

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView hintBelowQr = (TextView) findViewById(R.id.qrShowHint);
        if( chat_id != 0 ) {
            getSupportActionBar().setTitle(R.string.QrShowInviteCode);
            String groupName = MrMailbox.getChat(chat_id).getName();
            hintBelowQr.setText(AndroidUtilities.replaceTags(String.format(ApplicationLoader.applicationContext.getString(R.string.QrJoinVerifiedGroupHint), groupName)));
        }
        else {
            getSupportActionBar().setTitle(R.string.QrShowVerifyCode);
            String self = MrMailbox.getContact(MrContact.MR_CONTACT_ID_SELF).getAddr();
            hintBelowQr.setText(AndroidUtilities.replaceTags(String.format(ApplicationLoader.applicationContext.getString(R.string.QrVerifyContactHint), self)));
        }
        num_joiners = 0;

        ImageView imageView = (ImageView) findViewById(R.id.qrImage);
        try {
            Bitmap bitmap = encodeAsBitmap(MrMailbox.getSecurejoinQr(chat_id));
            imageView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.secureJoinInviterProgress);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.secureJoinInviterProgress);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case android.R.id.home: finish(); return true;
        }

        return false;
    }

    Bitmap encodeAsBitmap(String str) throws WriterException {
        BitMatrix result;
        try {
            Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
            hints.put(EncodeHintType.ERROR_CORRECTION, "8");
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

        Bitmap overlay = BitmapFactory.decodeResource(ApplicationLoader.applicationContext.getResources(), R.drawable.qr_overlay);
        putOverlay(bitmap, overlay);

        return bitmap;
    }

    public void putOverlay(Bitmap bitmap, Bitmap overlay) {
        int bw = bitmap.getWidth();
        int bh = bitmap.getHeight();
        int ow = bw/5;
        int oh = bh/5;

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(overlay, null, new Rect(bw/2-ow/2, bh/2-oh/2, bw/2+ow/2, bh/2+oh/2), paint);
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if( id==NotificationCenter.secureJoinInviterProgress) {
            int contact_id = (Integer)args[0];
            int step = (Integer)args[1];
            String msg = null;
            if( step == 3) {
                msg = String.format(ApplicationLoader.applicationContext.getString(R.string.OobSecureJoinRequested), MrMailbox.getContact(contact_id).getNameNAddr());
                num_joiners++;
            }
            else if( step == 6 ){
                msg = String.format(ApplicationLoader.applicationContext.getString(R.string.OobAddrVerified), MrMailbox.getContact(contact_id).getNameNAddr());
                num_joiners--;
            }

            if( msg != null ) {
                AndroidUtilities.showHint(ApplicationLoader.applicationContext, msg);
            }

            if( step == 6 && num_joiners <= 0 ) {
                finish();
            }
        }
    }

}
