package org.thoughtcrime.securesms.map;

import android.graphics.PointF;
import androidx.annotation.NonNull;
import android.view.View;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.Projection;

/**
 * MarkerView class wraps a latitude-longitude pair with a Android SDK View.
 * <p>
 * It can be used in conjunction with {@link com.mapbox.mapboxsdk.plugins.markerview.MarkerViewManager} to synchronise the Android SDK View on top
 * of map at the latitude-longitude pair.
 * </p>
 */
public class MarkerView {

    private final View view;
    private LatLng latLng;
    private Projection projection;
    private OnPositionUpdateListener onPositionUpdateListener;

    /**
     * Create a MarkerView
     *
     * @param latLng latitude-longitude pair
     * @param view   an Android SDK View
     */
    public MarkerView(@NonNull LatLng latLng, @NonNull View view) {
        this.latLng = latLng;
        this.view = view;
    }

    /**
     * Update the location of the MarkerView on the map.
     * <p>
     * Provided as a latitude-longitude pair.
     * </p>
     *
     * @param latLng latitude-longitude pair
     */
    public void setLatLng(@NonNull LatLng latLng) {
        this.latLng = latLng;
        update();
    }

    /**
     * Set a callback to be invoked when position placement is calculated.
     * <p>
     * Can be used to offset a MarkerView on screen.
     * </p>
     *
     * @param onPositionUpdateListener callback to be invoked when position placement is calculated
     */
    public void setOnPositionUpdateListener(OnPositionUpdateListener onPositionUpdateListener) {
        this.onPositionUpdateListener = onPositionUpdateListener;
    }

    /**
     * Callback definition that is invoked when position placement of a MarkerView is calculated.
     */
    public interface OnPositionUpdateListener {
        PointF onUpdate(PointF pointF);
    }

    void setProjection(Projection projection) {
        this.projection = projection;
    }

    View getView() {
        return view;
    }

    void update() {
        PointF point = projection.toScreenLocation(latLng);
        if (onPositionUpdateListener != null) {
            point = onPositionUpdateListener.onUpdate(point);
        }
        view.setX(point.x);
        view.setY(point.y);
    }

}