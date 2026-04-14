package org.thoughtcrime.securesms.geolocation;

import android.content.Context;
import android.location.Location;
import androidx.annotation.NonNull;

/** Abstraction over platform LocationManager and GMS FusedLocationProviderClient. */
public interface LocationSource {

  void startUpdates(@NonNull Context context, @NonNull Callback callback);

  void stopUpdates();

  /**
   * Request a single current location to seed the stream. May return null if no fresh fix is
   * available.
   */
  void getCurrentLocation(@NonNull Context context, @NonNull Callback callback);

  interface Callback {
    void onLocationUpdate(@NonNull Location location);
  }
}
