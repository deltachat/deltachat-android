package org.thoughtcrime.securesms.geolocation;

import android.location.Location;

import java.util.Observable;


/**
 * Created by cyberta on 06.03.19.
 */

public class DcLocation extends Observable {
    private Location lastLocation;
    private static DcLocation instance;

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

    void updateLocation(Location location) {
        lastLocation = location;

        instance.setChanged();
        instance.notifyObservers();
    }

    void reset() {
        updateLocation(getDefault());

    }

    private Location getDefault() {
        Location location = new Location("?");
        location.setLatitude(52.52);
        location.setLongitude(13.404);
        return location;
    }

}
