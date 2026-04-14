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
  private static final long UPDATE_INTERVAL_MS = 3000;
  private static final long FASTEST_INTERVAL_MS = 1000;
  private static final float MIN_DISTANCE_M = 5f;

  private FusedLocationProviderClient client;
  private LocationCallback locationCallback;

  @Override
  public void startUpdates(@NonNull Context context, @NonNull Callback callback) {
    client = LocationServices.getFusedLocationProviderClient(context);

    LocationRequest request =
        new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_M)
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

  @Override
  public void getCurrentLocation(@NonNull Context context, @NonNull Callback callback) {
    FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(context);
    try {
      client
          .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
          .addOnSuccessListener(
              location -> {
                if (location != null) {
                  callback.onLocationUpdate(location);
                }
              });
    } catch (SecurityException e) {
      Log.w(TAG, "No permission for getCurrentLocation", e);
    }
  }
}
