package org.thoughtcrime.securesms.geolocation;

import android.content.Context;
import android.location.Location;
import androidx.annotation.NonNull;

/** Abstraction over platform LocationManager and GMS FusedLocationProviderClient. */
public interface LocationSource {

  void startUpdates(@NonNull Context context, @NonNull Callback callback);

  void stopUpdates();

  interface Callback {
    void onLocationUpdate(@NonNull Location location);
  }
}
