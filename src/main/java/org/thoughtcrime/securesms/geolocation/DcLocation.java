package org.thoughtcrime.securesms.geolocation;

import android.location.Location;
import android.util.Log;

import java.util.Observable;

public class DcLocation extends Observable {
    private static final String TAG = DcLocation.class.getSimpleName();
    private Location lastLocation;
    private static DcLocation instance;
    private static final int TIMEOUT = 1000 * 15;
    private static final int EARTH_RADIUS = 6371;

    private DcLocation() {
        lastLocation = getDefault();
    }

    public static DcLocation getInstance() {
        if (instance == null) {
            instance = new DcLocation();
        }
        return instance;
    }

    public Location getLastLocation() {
        return lastLocation;
    }


    public boolean isValid() {
        return !"?".equals(lastLocation.getProvider());
    }

    void updateLocation(Location location) {
        if (isBetterLocation(location, lastLocation)) {
            lastLocation = location;

            instance.setChanged();
            instance.notifyObservers();
        }
    }

    void reset() {
        updateLocation(getDefault());

    }

    private Location getDefault() {
        return new Location("?");
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
