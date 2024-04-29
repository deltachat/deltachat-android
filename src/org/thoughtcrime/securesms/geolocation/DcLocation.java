package org.thoughtcrime.securesms.geolocation;

import android.location.Location;

import java.util.Observable;

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


    public boolean isValid() {
        return !lastLocation.getProvider().equals("?");
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
        return location;
    }

}
