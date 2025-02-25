package org.thoughtcrime.securesms.geolocation;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.thoughtcrime.securesms.R;

import java.util.concurrent.atomic.AtomicBoolean;

public class LocationStreamingService extends Service {

  private static final String TAG = LocationStreamingService.class.getSimpleName();
  private static final int INITIAL_TIMEOUT = 1000 * 60 * 2;
  private LocationManager locationManager = null;
  private static final int LOCATION_INTERVAL = 1000;
  private static final float LOCATION_DISTANCE = 25F;
  ServiceLocationListener locationListener;

  private static final String CHANNEL_ID = "LOCATION";
  private static final int NOTIFICATION_ID = 280125;
  private static final AtomicBoolean CHANNEL_CREATED = new AtomicBoolean(false);

  @RequiresPermission(value = ACCESS_FINE_LOCATION)
  public static void startForegroundService(@NonNull Context context) {
    createNotificationChannel(context);
    Intent intent = new Intent(context, LocationStreamingService.class);
    ContextCompat.startForegroundService(context, intent);
  }

  public static void stopService(@NonNull Context context) {
    context.stopService(new Intent(context, LocationStreamingService.class));
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
    if (locationManager == null) {
      Log.e(TAG, "Unable to initialize location service");
      return;
    }

    locationListener = new ServiceLocationListener();
    Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    if (lastLocation != null) {
      long locationAge = System.currentTimeMillis() - lastLocation.getTime();
      if (locationAge <= 600 * 1000) { // not older than 10 minutes
        DcLocation.getInstance().updateLocation(lastLocation);
      }
    }
    //requestLocationUpdate(LocationManager.NETWORK_PROVIDER);
    requestLocationUpdate(LocationManager.GPS_PROVIDER);
    initialLocationUpdate();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle(getString(R.string.pref_on_demand_location_streaming))
      .setSmallIcon(R.drawable.notification_permanent)
      .build();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_LOCATION);
    } else {
      startForeground(NOTIFICATION_ID, notification);
    }

    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    if (locationManager == null) {
      return;
    }

    try {
      locationManager.removeUpdates(locationListener);
    } catch (Exception ex) {
      Log.i(TAG, "fail to remove location listeners, ignore", ex);
    }
  }

  private void requestLocationUpdate(String provider) {
    try {
      locationManager.requestLocationUpdates(
                                             provider, LOCATION_INTERVAL, LOCATION_DISTANCE,
                                             locationListener);
    } catch (SecurityException | IllegalArgumentException  ex) {
      Log.e(TAG, String.format("Unable to request %s provider based location updates.", provider), ex);
    }
  }

  private void initialLocationUpdate() {
    try {
      Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
      if (gpsLocation != null && System.currentTimeMillis() - gpsLocation.getTime() < INITIAL_TIMEOUT) {
        locationListener.onLocationChanged(gpsLocation);
      }

    } catch (NullPointerException | SecurityException e) {
      e.printStackTrace();
    }
  }

  static public void createNotificationChannel(Context context) {
    if(!CHANNEL_CREATED.get() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      CHANNEL_CREATED.set(true);
      NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
              "Location Streaming Service", NotificationManager.IMPORTANCE_MIN);
      channel.setDescription("Ensure app will not be killed while location is being streamed in background.");
      NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
      notificationManager.createNotificationChannel(channel);
    }
  }

  private class ServiceLocationListener implements LocationListener {

    @Override
    public void onLocationChanged(Location location) {
      Log.d(TAG, "onLocationChanged: " + location);
      if (location == null) {
        return;
      }
      DcLocation.getInstance().updateLocation(location);
    }

    @Override
    public void onProviderDisabled(String provider) {
      Log.e(TAG, "onProviderDisabled: " + provider);
    }

    @Override
    public void onProviderEnabled(String provider) {
      Log.e(TAG, "onProviderEnabled: " + provider);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
      Log.e(TAG, "onStatusChanged: " + provider + " status: " + status);
    }
  }

}
