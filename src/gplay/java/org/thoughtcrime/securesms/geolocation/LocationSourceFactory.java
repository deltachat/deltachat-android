package org.thoughtcrime.securesms.geolocation;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

/**
 * Prefers FusedLocationProviderClient, falls back to platform LocationManager if Play Services are
 * somehow unavailable.
 */
public final class LocationSourceFactory {

  private static final String TAG = LocationSourceFactory.class.getSimpleName();

  private LocationSourceFactory() {}

  public static LocationSource create(Context context) {
    if (isGmsAvailable(context)) {
      Log.i(TAG, "Using FusedLocationProviderClient");
      return new GmsLocationSource();
    }
    Log.i(TAG, "GMS unavailable, falling back to LocationManager");
    return new PlatformLocationSource();
  }

  private static boolean isGmsAvailable(Context context) {
    try {
      return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
          == ConnectionResult.SUCCESS;
    } catch (Exception e) {
      return false;
    }
  }
}
