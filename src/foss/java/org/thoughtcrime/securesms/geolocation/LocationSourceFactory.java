package org.thoughtcrime.securesms.geolocation;

import android.content.Context;
import android.util.Log;

/** Non-GMS, always uses the platform LocationManager. */
public final class LocationSourceFactory {

  private static final String TAG = LocationSourceFactory.class.getSimpleName();

  private LocationSourceFactory() {}

  public static LocationSource create(Context context) {
    Log.i(TAG, "Non-GMS build, Using platform LocationManager");
    return new PlatformLocationSource();
  }
}
