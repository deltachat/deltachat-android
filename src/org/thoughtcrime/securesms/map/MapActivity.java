package org.thoughtcrime.securesms.map;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import org.thoughtcrime.securesms.geolocation.DcLocation;
import org.thoughtcrime.securesms.geolocation.DcLocationManager;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.Style;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.BaseActivity;
import org.thoughtcrime.securesms.R;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class MapActivity extends BaseActivity implements PermissionsListener, Observer {

    private static final String TAG = MapActivity.class.getSimpleName();
    private MapView mapView;
    private PermissionsManager permissionsManager;
    private DcLocation dcLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(mapboxMap -> mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {

            mapboxMap.setCameraPosition(new CameraPosition.Builder()
                    .target(new LatLng(53.5505D, 10.001D))
                    .zoom(12)
                    .build());
            mapboxMap.getUiSettings().setLogoEnabled(false);
            mapboxMap.getUiSettings().setAttributionEnabled(false);

            permissionsManager = new PermissionsManager(this);

            if (PermissionsManager.areLocationPermissionsGranted(this)) {
                showDeviceLocation();
            } else {
                permissionsManager = new PermissionsManager(this);
                permissionsManager.requestLocationPermissions(this);
            }

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
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        DcLocation.getInstance().deleteObserver(this);
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
    public void onBackPressed() {
        super.onBackPressed();

        //FIXME: only for testing --v
        DcLocationManager locationManager = ApplicationContext.getInstance(this).dcLocationManager;
        locationManager.stopLocationEngine();

    }

    //Android SDK callback for the result from requesting permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //map sdk callbacks for the result from requesting permissions
    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            showDeviceLocation();
        } else {
            Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        // no explanation text before the permission pop up is shown
    }

    private void showDeviceLocation() {
        DcLocationManager locationManager = ApplicationContext.getInstance(this).dcLocationManager;
        locationManager.startLocationEngine();

    }


    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof DcLocation) {
            this.dcLocation = (DcLocation) o;
            Log.d(TAG, "show marker on map: " + dcLocation.getLastLocation().getLatitude() + ", " + dcLocation.getLastLocation().getLongitude());

        }
    }
}
