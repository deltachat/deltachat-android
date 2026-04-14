package org.thoughtcrime.securesms.geolocation;

import android.content.Context;
import android.location.LocationManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.location.LocationListenerCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.location.LocationRequestCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlatformLocationSource implements LocationSource {

  private static final String TAG = PlatformLocationSource.class.getSimpleName();
  private static final long UPDATE_INTERVAL_MS = 3_000;
  private static final float MIN_DISTANCE_M = 5f;

  private LocationManager locationManager;
  private final List<LocationListenerCompat> activeListeners = new ArrayList<>();
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  @Override
  public void startUpdates(@NonNull Context context, @NonNull Callback callback) {
    locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    if (locationManager == null) {
      Log.e(TAG, "LocationManager unavailable");
      return;
    }

    requestProvider(LocationManager.GPS_PROVIDER, callback);
    requestProvider(LocationManager.NETWORK_PROVIDER, callback);
  }

  private void requestProvider(String provider, Callback callback) {
    if (locationManager == null) return;

    try {
      if (!locationManager.isProviderEnabled(provider)) return;

      LocationRequestCompat request =
          new LocationRequestCompat.Builder(UPDATE_INTERVAL_MS)
              .setMinUpdateDistanceMeters(MIN_DISTANCE_M)
              .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
              .build();

      LocationListenerCompat listener = callback::onLocationUpdate;

      LocationManagerCompat.requestLocationUpdates(
          locationManager, provider, request, executor, listener);
      activeListeners.add(listener);
    } catch (SecurityException | IllegalArgumentException e) {
      Log.e(TAG, "Cannot request " + provider + " updates", e);
    }
  }

  @Override
  public void stopUpdates() {
    if (locationManager != null) {
      for (LocationListenerCompat listener : activeListeners) {
        try {
          locationManager.removeUpdates(listener);
        } catch (Exception e) {
          Log.w(TAG, "Error removing listener", e);
        }
      }
      activeListeners.clear();
    }
    executor.shutdown();
  }

  @Override
  public void getCurrentLocation(@NonNull Context context, @NonNull Callback callback) {
    LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    if (lm == null) return;

    // Prefer GPS, but fall back to network if GPS isn't enabled
    String provider =
        lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
            ? LocationManager.GPS_PROVIDER
            : LocationManager.NETWORK_PROVIDER;

    try {
      LocationManagerCompat.getCurrentLocation(
          lm,
          provider,
          (android.os.CancellationSignal) null,
          executor,
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
