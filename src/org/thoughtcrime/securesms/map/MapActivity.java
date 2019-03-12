package org.thoughtcrime.securesms.map;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import org.thoughtcrime.securesms.geolocation.DcLocation;

import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.thoughtcrime.securesms.BaseActivity;
import org.thoughtcrime.securesms.R;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import static org.thoughtcrime.securesms.map.MapDataManager.MARKER_SELECTED;

public class MapActivity extends BaseActivity implements Observer {

    public static final String TAG = MapActivity.class.getSimpleName();
    public static final String CHAT_ID = "chat_id";

    private MapView mapView;
    private DcLocation dcLocation;
    private MapDataManager mapDataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map);
        int chatId = getIntent().getIntExtra(CHAT_ID, -1);
        if (chatId == -1) {
            finish();
            return;
        }

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(mapboxMap -> mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {

            mapboxMap.setCameraPosition(new CameraPosition.Builder()
                    .target(new LatLng(dcLocation.getLastLocation().getLatitude(), dcLocation.getLastLocation().getLongitude()))
                    .zoom(12)
                    .build());
            mapboxMap.getUiSettings().setLogoEnabled(false);
            mapboxMap.getUiSettings().setAttributionEnabled(false);

            Style mapBoxStyle = mapboxMap.getStyle();
            if (mapBoxStyle == null) {
                return;
            }

            mapDataManager = new MapDataManager(this, mapBoxStyle, chatId);
            mapboxMap.addOnMapClickListener(new MapboxMap.OnMapClickListener() {
                @Override
                public boolean onMapClick(@NonNull LatLng point) {
                    final PointF pixel = mapboxMap.getProjection().toScreenLocation(point);
                    Log.d(TAG, "on item clicked.");

                    List<Feature> features = mapboxMap.queryRenderedFeatures(pixel, mapDataManager.getMarkerLayers());
                    for (Feature feature : features) {
                        Log.d(TAG, "found feature: " + feature.toJson());
                        if (feature.hasProperty(MapDataManager.TIMESTAMP)){
                            //show first feature that has meta data infos
                            if (feature.hasProperty(MARKER_SELECTED))  {
                                mapDataManager.setMarkerSelected(feature.id());
                            }
                            Log.d(TAG, "on item clicked. timestamp : " + feature.getNumberProperty(MapDataManager.TIMESTAMP));

                            return true;
                        }
                    }
                    mapDataManager.unselectMarker();
                    return false;
                }
            });
        }));

        dcLocation = DcLocation.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        DcLocation.getInstance().addObserver(this);
        if (mapDataManager != null) {
            mapDataManager.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        DcLocation.getInstance().deleteObserver(this);
        if (mapDataManager != null) {
            mapDataManager.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof DcLocation) {
            this.dcLocation = (DcLocation) o;
            Log.d(TAG, "show marker on map: " +
                    dcLocation.getLastLocation().getLatitude() + ", " +
                    dcLocation.getLastLocation().getLongitude());
            //TODO: consider implementing a button -> center map to current location
        }
    }
}
