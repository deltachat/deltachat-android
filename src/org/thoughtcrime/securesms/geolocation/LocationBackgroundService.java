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

    private static final int TIMEOUT = 1000 * 30;
    private static final String TAG = LocationBackgroundService.class.getSimpleName();
    private LocationManager locationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 20f;
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
        requestLocationUpdate(LocationManager.NETWORK_PROVIDER);
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
        } catch (SecurityException | IllegalArgumentException ex) {
            Log.e(TAG, String.format("Unable to request %s provider based location updates.", provider), ex);
        }
    }

    private void initialLocationUpdate() {
        try {
            Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            locationListener.onLocationChanged(networkLocation);
            locationListener.onLocationChanged(gpsLocation);

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
        public void onLocationChanged(Location location) {
            Log.e(TAG, "onLocationChanged: " + location);
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
            Log.e(TAG, "onStatusChanged: " + provider);
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
            boolean isSignificantlyNewer = timeDelta > TIMEOUT;
            boolean isSignificantlyOlder = timeDelta < -TIMEOUT;
            boolean isNewer = timeDelta > 0;

            // If it's been more than two minutes since the current location, use the new location
            // because the user has likely moved
            if (isSignificantlyNewer) {
                return true;
                // If the new location is more than two minutes older, it must be worse
            } else if (isSignificantlyOlder) {
                return false;
            }

            // Check whether the new location fix is more or less accurate
            int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
            boolean isLessAccurate = accuracyDelta > 0;
            boolean isMoreAccurate = accuracyDelta < 0;
            boolean isSignificantlyLessAccurate = accuracyDelta > 200;

            // Check if the old and new location are from the same provider
            boolean isFromSameProvider = isSameProvider(location.getProvider(),
                    currentBestLocation.getProvider());

            // Determine location quality using a combination of timeliness and accuracy
            if (isMoreAccurate) {
                return true;
            } else if (isNewer && !isLessAccurate) {
                return true;
            } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
                return true;
            }
            return false;
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
