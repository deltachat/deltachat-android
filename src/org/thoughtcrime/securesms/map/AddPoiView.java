package org.thoughtcrime.securesms.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.b44t.messenger.DcContext;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;

import static org.thoughtcrime.securesms.util.BitmapUtil.generateColoredBitmap;

/**
 * Created by cyberta on 24.04.19.
 */

public class AddPoiView extends LinearLayoutCompat {
    private ImageButton sendView;
    private LatLng latLng;

    public AddPoiView(Context context) {
        this(context, null);
    }

    public AddPoiView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AddPoiView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        inflate(context, R.layout.add_poi_view, this);
        sendView = this.findViewById(R.id.sendView);

        sendView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: submit change
                // DcHelper.getContext(AddPoiView.this.getContext()).
            }
        });
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }
}
