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

/**
 * Created by cyberta on 06.03.19.
 */

public class LocationBackgroundService extends Service {

    private static final int TIMEOUT = 1000 * 15;
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
            Log.i(TAG, "fail to remove location listners, ignore", ex);
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
        private static final int EARTH_RADIUS = 6371;

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged: " + location);
            if (location == null) {
                return;
            }
            if (isBetterLocation(location, DcLocation.getInstance().getLastLocation())) {
                DcLocation.getInstance().updateLocation(location);
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + provider + " status: " + status);
        }


        /** https://developer.android.com/guide/topics/location/strategies
         * Determines whether one Location reading is better than the current Location fix
         * @param location  The new Location that you want to evaluate
         * @param currentBestLocation  The current Location fix, to which you want to compare the new one
         */
        private boolean isBetterLocation(Location location, Location currentBestLocation) {
            if (currentBestLocation == null) {
                // A new location is always better than no location
                return true;
            }

            // Check whether the new location fix is newer or older
            long timeDelta = location.getTime() - currentBestLocation.getTime();
            boolean isSignificantlyOlder = timeDelta < -TIMEOUT;

            if (isSignificantlyOlder) {
                return false;
            }

            // Check whether the new location fix is more or less accurate
            int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
            Log.d(TAG, "accuracyDelta: " + accuracyDelta);
            boolean isSignificantlyMoreAccurate = accuracyDelta > 50;
            boolean isSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

            if (isSignificantlyMoreAccurate && isSameProvider) {
                return true;
            }

            boolean isMoreAccurate = accuracyDelta > 0;
            double distance = distance(location, currentBestLocation);
            return hasLocationChanged(distance) && isMoreAccurate ||
                    hasLocationSignificantlyChanged(distance);

        }

        private boolean hasLocationSignificantlyChanged(double distance) {
            return distance > 30D;
        }

        private boolean hasLocationChanged(double distance) {
            return distance > 10D;
        }

        private double distance(Location location, Location currentBestLocation) {

            double startLat = location.getLatitude();
            double startLong = location.getLongitude();
            double endLat = currentBestLocation.getLatitude();
            double endLong = currentBestLocation.getLongitude();

            double dLat  = Math.toRadians(endLat - startLat);
            double dLong = Math.toRadians(endLong - startLong);

            startLat = Math.toRadians(startLat);
            endLat   = Math.toRadians(endLat);

            double a = haversin(dLat) + Math.cos(startLat) * Math.cos(endLat) * haversin(dLong);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

            double distance = EARTH_RADIUS * c * 1000;
            Log.d(TAG, "Distance between location updates: " + distance);
            return distance;
        }

        private double haversin(double val) {
            return Math.pow(Math.sin(val / 2), 2);
        }

        /** Checks whether two providers are the same */
        private boolean isSameProvider(String provider1, String provider2) {
            if (provider1 == null) {
                return provider2 == null;
            }
            return provider1.equals(provider2);
        }
    }

}
