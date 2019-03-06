package com.b44t.messenger.geolocation;

import android.location.Location;

import java.util.Observable;


/**
 * Created by cyberta on 06.03.19.
 */

public class DcLocation extends Observable {
    private Location lastLocation;
    private static DcLocation instance;

    private DcLocation() {
        lastLocation = new Location("?");
    }

    public static DcLocation getInstance() {
        if (instance == null) {
            instance = new DcLocation();
        }
        return instance;
    }

    public Location getLastLocation() {
        return instance.lastLocation;
    }

    void updateLocation(Location location) {
        lastLocation = location;

        instance.setChanged();
        instance.notifyObservers();
    }

    void reset() {
        updateLocation(new Location("?"));
    }


}
