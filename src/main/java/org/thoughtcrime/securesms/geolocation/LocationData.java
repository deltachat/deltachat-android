package org.thoughtcrime.securesms.geolocation;

import android.location.Location;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Process-wide holder for the current streamed location. Foreground service writes, UI observes.
 */
public final class LocationData {

  private static final LocationData INSTANCE = new LocationData();

  private final MutableLiveData<Location> liveLocation = new MutableLiveData<>();

  private LocationData() {}

  public static LocationData getInstance() {
    return INSTANCE;
  }

  public LiveData<Location> observable() {
    return liveLocation;
  }

  @Nullable
  public Location current() {
    return liveLocation.getValue();
  }

  void post(@NonNull Location location) {
    liveLocation.postValue(location);
  }

  void clear() {
    liveLocation.postValue(null);
  }
}
