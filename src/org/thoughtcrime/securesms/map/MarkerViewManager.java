package org.thoughtcrime.securesms.map;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Class responsible for synchronising views at a LatLng on top of a Map.
 */
public class MarkerViewManager implements MapView.OnDidFinishRenderingFrameListener, SendingTask.OnMessageSentListener {

    private final MapView mapView;
    private final MapboxMap mapboxMap;
    private final List<MarkerView> markers = new ArrayList<>();
    private boolean initialised;
    private MarkerView centeredMarker;
    private int keyboardHeight = 0;

    /**
     * Create a MarkerViewManager.
     *
     * @param mapView   the MapView used to synchronise views on
     * @param mapboxMap the MapboxMap to synchronise views with
     */
    public MarkerViewManager(MapView mapView, MapboxMap mapboxMap) {
        this.mapView = mapView;
        this.mapboxMap = mapboxMap;
    }

    /**
     * Destroys the MarkerViewManager.
     * <p>
     * Should be called before MapView#onDestroy
     * </p>
     */
    @UiThread
    public void onDestroy() {
        markers.clear();
        mapView.removeOnDidFinishRenderingFrameListener(this);
        initialised = false;
    }

    /**
     * Add a MarkerView to the map using MarkerView and LatLng.
     *
     * @param markerView the markerView to synchronise on the map
     */
    @UiThread
    public void addMarker(@NonNull MarkerView markerView) {
        if (mapView.isDestroyed() || markers.contains(markerView)) {
            return;
        }

        if (!initialised) {
            initialised = true;
            mapView.addOnDidFinishRenderingFrameListener(this);
        }
        markerView.setProjection(mapboxMap.getProjection());
        mapView.addView(markerView.getView());
        markers.add(markerView);
    }

    public boolean hasMarkers() {
        return markers.size() > 0;
    }

    @UiThread
    public void removeMarkers() {
        if (!mapView.isDestroyed()) {
            for (MarkerView markerView : markers) {
                mapView.removeView(markerView.getView());
            }
        }

        centeredMarker = null;
        markers.clear();
    }

    @Override
    public void onDidFinishRenderingFrame(boolean fully) {
        if (fully) {
            update();
        }
    }

    private void update() {
        for (MarkerView marker : markers) {
            marker.update();
        }
    }

    @Override
    public void onMessageSent() {
        removeMarkers();
    }

    @UiThread
    public void center(MarkerView view) {
        centeredMarker = view;
        view.getView().post(() -> {
            int markerWidth = view.getView().getWidth();
            CameraPosition currentPosition = mapboxMap.getCameraPosition();
            CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(
                    new CameraPosition.Builder().
                            target(view.getLatLng()).
                            bearing(currentPosition.bearing).
                            tilt(currentPosition.tilt).
                            zoom(currentPosition.zoom).
                            padding(0d , 0d, markerWidth / 2d, keyboardHeight).
                            build());
            mapboxMap.easeCamera(cameraUpdate);
        });
    }

    MarkerView getCenteredMarker() {
        return centeredMarker;
    }

    @UiThread
    void onKeyboardShown(int keyboardHeight) {
        this.keyboardHeight = keyboardHeight;
        if (centeredMarker != null) {
            center(centeredMarker);
        }
    }
}
