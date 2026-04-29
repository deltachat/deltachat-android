package org.thoughtcrime.securesms.geolocation;

import android.content.Context;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationListenerCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.location.LocationRequestCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class PlatformLocationSource implements LocationSource {

  private static final String TAG = "PlatformLocationSource";
  private static final long UPDATE_INTERVAL_MS = 0;

  private LocationManager locationManager;
  private final List<LocationListenerCompat> activeListeners = new ArrayList<>();

  @Override
  public void startUpdates(@NonNull Context context, @NonNull Callback callback) {
    locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    if (locationManager == null) {
      Log.e(TAG, "LocationManager unavailable");
      return;
    }

    boolean registered = false;

    // API 31+: try the platform fused provider first
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      registered = requestProvider(context, LocationManager.FUSED_PROVIDER, callback);
    }

    // Fall back with individual providers
    if (!registered) {
      requestProvider(context, LocationManager.GPS_PROVIDER, callback);
      requestProvider(context, LocationManager.NETWORK_PROVIDER, callback);
    }
  }

  private boolean requestProvider(Context context, String provider, Callback callback) {
    if (locationManager == null) return false;

    if (!locationManager.isProviderEnabled(provider)) {
      Log.d(TAG, "Provider " + provider + " not enabled, skipping");
      return false;
    }

    try {
      LocationRequestCompat request =
          new LocationRequestCompat.Builder(UPDATE_INTERVAL_MS)
              .setMinUpdateDistanceMeters(0)
              .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
              .build();

      LocationListenerCompat listener = callback::onLocationUpdate;
      Executor mainExecutor = ContextCompat.getMainExecutor(context);

      LocationManagerCompat.requestLocationUpdates(
          locationManager, provider, request, mainExecutor, listener);
      activeListeners.add(listener);
      Log.d(TAG, "Registered on provider: " + provider);
      return true;
    } catch (SecurityException | IllegalArgumentException e) {
      Log.e(TAG, "Cannot request " + provider + " updates", e);
      return false;
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
  }
}
