package org.thoughtcrime.securesms.geolocation;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

public class LocationBackgroundService extends Service {

    private static final int INITIAL_TIMEOUT = 1000 * 60 * 2;
    private static final String TAG = LocationBackgroundService.class.getSimpleName();
    private LocationManager locationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 25F;
    ServiceLocationListener locationListener;

    private final IBinder mBinder = new LocationBackgroundServiceBinder();

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        return super.bindService(service, conn, flags);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            Log.e(TAG, "Unable to initialize location service");
            return;
        }

        locationListener = new ServiceLocationListener();
        Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastLocation != null) {
          long locationAge = System.currentTimeMillis() - lastLocation.getTime();
          if (locationAge <= 600 * 1000) { // not older than 10 minutes
            DcLocation.getInstance().updateLocation(lastLocation);
          }
        }
        //requestLocationUpdate(LocationManager.NETWORK_PROVIDER);
        requestLocationUpdate(LocationManager.GPS_PROVIDER);
        initialLocationUpdate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (locationManager == null) {
            return;
        }

        try {
            locationManager.removeUpdates(locationListener);
        } catch (Exception ex) {
            Log.i(TAG, "fail to remove location listeners, ignore", ex);
        }
    }

    private void requestLocationUpdate(String provider) {
        try {
            locationManager.requestLocationUpdates(
                    provider, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    locationListener);
        } catch (SecurityException | IllegalArgumentException  ex) {
            Log.e(TAG, String.format("Unable to request %s provider based location updates.", provider), ex);
        }
    }

    private void initialLocationUpdate() {
        try {
            Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (gpsLocation != null && System.currentTimeMillis() - gpsLocation.getTime() < INITIAL_TIMEOUT) {
              locationListener.onLocationChanged(gpsLocation);
            }

        } catch (NullPointerException | SecurityException e) {
            e.printStackTrace();
        }
    }

    class LocationBackgroundServiceBinder extends Binder {
        LocationBackgroundServiceBinder getService() {
            return LocationBackgroundServiceBinder.this;
        }

        void stop() {
            DcLocation.getInstance().reset();
            stopSelf();
        }
    }

    private class ServiceLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(@NonNull Location location) {
            Log.d(TAG, "onLocationChanged: " + location);
            if (location == null) {
                return;
            }
            DcLocation.getInstance().updateLocation(location);
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + provider + " status: " + status);
        }
    }

}
