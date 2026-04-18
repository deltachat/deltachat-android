package org.thoughtcrime.securesms.geolocation;

import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class GmsLocationSource implements LocationSource {

  private static final String TAG = GmsLocationSource.class.getSimpleName();
  private static final long UPDATE_INTERVAL_MS = 3_000;
  private static final long FASTEST_INTERVAL_MS = 1_000;

  private FusedLocationProviderClient client;
  private LocationCallback locationCallback;

  @Override
  public void startUpdates(@NonNull Context context, @NonNull Callback callback) {
    client = LocationServices.getFusedLocationProviderClient(context);

    LocationRequest request =
        new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            .setMinUpdateDistanceMeters(0)
            .setWaitForAccurateLocation(false)
            .build();

    locationCallback =
        new LocationCallback() {
          @Override
          public void onLocationResult(@NonNull LocationResult result) {
            Location loc = result.getLastLocation();
            if (loc != null) {
              callback.onLocationUpdate(loc);
            }
          }
        };

    try {
      client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    } catch (SecurityException e) {
      Log.e(TAG, "Missing location permission", e);
    }
  }

  @Override
  public void stopUpdates() {
    if (client != null && locationCallback != null) {
      client.removeLocationUpdates(locationCallback);
      client = null;
      locationCallback = null;
    }
  }
}
