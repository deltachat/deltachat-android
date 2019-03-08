package org.thoughtcrime.securesms.map;

import android.graphics.BitmapFactory;
import android.graphics.Color;
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

import java.util.Observable;
import java.util.Observer;

public class MapActivity extends BaseActivity implements Observer {

    public static final String TAG = MapActivity.class.getSimpleName();
    public static final String CHAT_ID = "chat_id";

    public static final String LINE_LAYER = "line_layer";
    public static final String LINE_SOURCE = "line_source";
    public static final String MARKER_LAYER = "symbol_layer";
    public static final String MARKER_ICON = "marker_icon_id";
    public static final String MARKER_POSITION_SOURCE = "marker_position";

    private MapView mapView;
    private MapboxMap mapboxMap;
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
            this.mapboxMap = mapboxMap;

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

    private void initMapDrawings() {
        if (mapboxMap == null || mapboxMap.getStyle() == null) {
            return;
        }
        Style style = mapboxMap.getStyle();
        initGeoJsonSources(style);
        initLayers(style);
    }

    private void initGeoJsonSources(Style style) {
        GeoJsonSource markerPositionSource = new GeoJsonSource(MARKER_POSITION_SOURCE);
        GeoJsonSource linePositionSource = new GeoJsonSource(LINE_SOURCE);
        style.addSource(markerPositionSource);
        style.addSource(linePositionSource);
    }

    private void initLayers(Style style) {
        style.addImage(MARKER_ICON,
                BitmapFactory.decodeResource(
                        MapActivity.this.getResources(), R.drawable.mapbox_marker_icon_default));
        style.addLayer(new SymbolLayer(MARKER_LAYER, MARKER_POSITION_SOURCE)
                .withProperties(PropertyFactory.iconImage(MARKER_ICON)));

        style.addLayer(new LineLayer(LINE_LAYER, LINE_SOURCE)
                .withProperties(PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_MITER),
                        PropertyFactory.lineOpacity(.7f),
                        PropertyFactory.lineWidth(7f),
                        PropertyFactory.lineColor(Color.parseColor("#3bb2d0"))));
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
