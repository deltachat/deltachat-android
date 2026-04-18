package org.thoughtcrime.securesms.geolocation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;

public class LocationStreamingService extends Service {

  private static final String TAG = LocationStreamingService.class.getSimpleName();
  private static final String ACTION_STOP = "org.thoughtcrime.securesms.geolocation.STOP_STREAMING";
  private static final int NOTIFICATION_ID = 8801;
  private static final String CHANNEL_ID = "location_streaming";

  private static volatile boolean running = false;

  private LocationSource source;
  private Location lastPublished;

  // static API

  /** Register a chat for location updates, then ensure the service is running. */
  public static void startSharing(Context context, int chatId, int durationSeconds) {
    ActiveLocationChats.add(context, chatId);
    DcHelper.getContext(context).sendLocationsToChat(chatId, durationSeconds);
    ContextCompat.startForegroundService(
        context, new Intent(context, LocationStreamingService.class));
  }

  /** Unregister a chat. If no chats remain, stop the service. */
  public static void stopSharing(Context context, int chatId) {
    ActiveLocationChats.remove(context, chatId);
    DcHelper.getContext(context).sendLocationsToChat(chatId, 0);
    if (!DcHelper.getContext(context).isSendingLocationsToChat(0)) {
      context.stopService(new Intent(context, LocationStreamingService.class));
    }
  }

  public static void ensureRunning(Context context) {
    if (!hasLocationPermission(context)) {
      for (int chatId : ActiveLocationChats.getAllIds(context)) {
        DcHelper.getContext(context).sendLocationsToChat(chatId, 0);
      }
      ActiveLocationChats.clear(context);
      return;
    }
    ContextCompat.startForegroundService(
        context, new Intent(context, LocationStreamingService.class));
  }

  public static boolean isRunning() {
    return running;
  }

  // lifecycle

  @Override
  public void onCreate() {
    super.onCreate();
    if (!hasLocationPermission(this)) {
      Log.w(TAG, "Location permission not granted, stopping");
      stopSelf();
      return;
    }
    running = true;
    promoteToForeground();
    beginLocationUpdates();
  }

  @Override
  public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
    if (intent != null && ACTION_STOP.equals(intent.getAction())) {
      stopAllSharing();
      stopSelf();
      return START_NOT_STICKY;
    }

    // If the service is already running, and we already have a fix,
    // push it immediately.
    if (lastPublished != null) {
      publishAndWrite(lastPublished);
    }

    return START_STICKY;
  }

  private void stopAllSharing() {
    for (int chatId : ActiveLocationChats.getAllIds(this)) {
      DcHelper.getContext(this).sendLocationsToChat(chatId, 0);
    }
    ActiveLocationChats.clear(this);
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onDestroy() {
    running = false;
    if (source != null) {
      source.stopUpdates();
      source = null;
    }
    LocationData.getInstance().clear();
    super.onDestroy();
  }

  @Override
  public void onTimeout(int startId) {
    stopSelf();
  }

  @Override
  public void onTimeout(int startId, int fgsType) {
    stopSelf();
  }

  // location

  private void beginLocationUpdates() {
    source = LocationSourceFactory.create(this);
    source.startUpdates(this, this::onNewLocation);
  }

  private void onNewLocation(Location location) {
    Log.d(TAG, "onNewLocation raw: " + location);
    publishAndWrite(location);
    lastPublished = location;
  }

  private void publishAndWrite(Location location) {
    LocationData.getInstance().post(location);

    boolean keepGoing =
        DcHelper.getContext(this)
            .setLocation(
                (float) location.getLatitude(),
                (float) location.getLongitude(),
                location.getAccuracy());
    Log.d(TAG, "keepGoing: " + keepGoing);

    if (!keepGoing) {
      stopAllSharing();
      stopSelf();
    }
  }

  // foreground / notification

  private void promoteToForeground() {
    ensureNotificationChannel();
    Notification notification = buildNotification();
    try {
      int type =
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
              ? ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
              : 0;
      ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type);
    } catch (Exception e) {
      // SecurityException on API 34+ if permission missing,
      // ForegroundServiceStartNotAllowedException on API 31+ if in background.
      Log.e(TAG, "Cannot promote to foreground", e);
      stopSelf();
    }
  }

  private void ensureNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel channel =
          new NotificationChannel(
              CHANNEL_ID,
              getString(R.string.location_streaming_notification_title),
              NotificationManager.IMPORTANCE_LOW);
      channel.setDescription(getString(R.string.location_streaming_channel_desc));
      channel.setShowBadge(false);
      NotificationManager nm = getSystemService(NotificationManager.class);
      if (nm != null) nm.createNotificationChannel(channel);
    }
  }

  private Notification buildNotification() {
    Intent tapIntent = new Intent(this, ConversationListActivity.class);
    PendingIntent contentPendingIntent =
        PendingIntent.getActivity(
            this, 0, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    Intent stopIntent = new Intent(this, LocationStreamingService.class);
    stopIntent.setAction(ACTION_STOP);
    PendingIntent stopPendingIntent =
        PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE);

    return new NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.location_streaming_notification_title))
        .setContentText(getString(R.string.location_streaming_notification_text))
        .setSmallIcon(R.drawable.ic_location_on_white_24dp)
        .setOngoing(true)
        .setContentIntent(contentPendingIntent)
        .addAction(
            R.drawable.ic_stop_circle, getString(R.string.stop_sharing_location), stopPendingIntent)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build();
  }

  private static boolean hasLocationPermission(Context context) {
    return ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
  }
}
