package org.thoughtcrime.securesms.service;

import static org.thoughtcrime.securesms.ApplicationContext.getDcAccounts;
import static org.unifiedpush.android.connector.ConstantsKt.INSTANCE_DEFAULT;

import android.app.ForegroundServiceStartNotAllowedException;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.WorkManager;

import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.FetchWorker;
import org.thoughtcrime.securesms.connect.KeepAliveService;
import org.thoughtcrime.securesms.notifications.NotificationCenter;
import org.thoughtcrime.securesms.util.IntentUtils;
import org.unifiedpush.android.connector.FailedReason;
import org.unifiedpush.android.connector.PushService;
import org.unifiedpush.android.connector.UnifiedPush;
import org.unifiedpush.android.connector.data.PublicKeySet;
import org.unifiedpush.android.connector.data.PushEndpoint;
import org.unifiedpush.android.connector.data.PushMessage;
import org.unifiedpush.android.connector.data.ResolvedDistributor;

import javax.annotation.Nullable;

public class UnifiedPushService extends PushService {
  private static String TAG = "UnifiedPushService";

  @Override
  public void onNewEndpoint(@NonNull PushEndpoint pushEndpoint, @NonNull String _s) {
    // We could have one endpoint/publicKey+auth per account, just like we could have one FCM key
    // per account, but we follow FCM architecture. At least chatmail server can't know, with the push
    // token, that 2 accounts are from the same device. With this unique push token, encrypted, the
    // notifier server could know that a user is on 2 different servers.
    Log.d(TAG, "New endpoint received");
    String token = serializeForNotifiers(pushEndpoint);
    if (token == null) {
      Log.e(TAG, "Couldn't serialize token, aborting.");
    }
    getDcAccounts().setPushDeviceToken(token);
    KeepAliveService.maybeStopSelf(this);
    WorkManager.getInstance(this).cancelAllWorkByTag(FetchWorker.periodicWorkTag);
  }

  @Override
  public void onMessage(@NonNull PushMessage _pushMessage, @NonNull String _s) {
    Log.d(TAG, "New push message received");
    if (Build.VERSION.SDK_INT < 31) {
      onMessageLegacy();
    } else {
      onMessage31();
    }
  }

  private void onMessageLegacy() {
    try {
      FetchForegroundService.start(this);
    } catch (Exception e) {
      Log.e(TAG, "An error occurred:", e);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.S)
  private void onMessage31() {
    try {
      FetchForegroundService.start(this);
    } catch (ForegroundServiceStartNotAllowedException _e) {
      // UnifiedPush distributor are supposed to raise DC to the foreground while pushing
      // We keep the fallback in case something went wrong, or the distributor doesn't
      // implement it correctly
      Log.w(TAG, "Couldn't start foreground service - trying fallback solution");
      FetchForegroundService.fetchSynchronously();
    } catch (Exception e) {
      Log.e(TAG, "An error occurred:", e);
    }
  }

  @Override
  public void onRegistrationFailed(@NonNull FailedReason failedReason, @NonNull String _s) {
    Log.w(TAG, "Registration failed " + failedReason.name());
    // Do nothing, it is either already setup, and we can continue with the current endpoint,
    // or it should resolve by itself during next setup
  }

  @Override
  public void onUnregistered(@NonNull String _s) {
    Log.w(TAG, "Unregistered");
    ResolvedDistributor res = UnifiedPush.resolveDefaultDistributor(this);
    // We are using a single registration for all accounts, we don't need
    // to wait a few seconds to get other instance unregistrations.
    // We can process this unregistration now
    if (res instanceof ResolvedDistributor.Found) {
      String newDistrib = ((ResolvedDistributor.Found) res).getPackageName();
      UnifiedPush.saveDistributor(this, newDistrib);
      register(this);
    } else {
      // We either have:
      // - many distributors available but no default selected
      // - no more distributor on the system (in this case, it may be why we got unregistered)
      // => we ask the user to open the app to reconfigure push notifications (either select a new
      // distrib, or start foreground service)
      showNotificationToReconfigure();
    }
  }

  private void showNotificationToReconfigure() {
    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel channel = new NotificationChannel(NotificationCenter.CH_INFO,
        "General information", NotificationManager.IMPORTANCE_HIGH);
      channel.setDescription("Inform about the application state, e.g. when the app needs to be opened to reconfigure push notifications.");
      notificationManager.createNotificationChannel(channel);
    }

    // Launch the app with a new task flag to get the picker
    Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | IntentUtils.FLAG_MUTABLE());
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationCenter.CH_INFO)
      .setSmallIcon(R.drawable.icon_notification)
      .setColor(getResources().getColor(R.color.delta_primary))
      .setPriority(Notification.PRIORITY_HIGH)
      .setContentText(getString(R.string.notification_reconfigure_notifications))
      .setStyle(
        new NotificationCompat.BigTextStyle()
          .bigText(getString(R.string.notification_reconfigure_notifications))
      )
      .setAutoCancel(true)
      .setContentIntent(pi);
    notificationManager.notify(NotificationCenter.ID_INFO, builder.build());
  }

  private static @Nullable String serializeForNotifiers(PushEndpoint endpoint) {
    PublicKeySet key = endpoint.getPubKeySet();
    if (key == null) {
      // This should never happen with the default keymanager
      Log.e(TAG, "No key found for endpoint");
      return null;
    }
    return "webpush:" + endpoint.getUrl() + '|' + key.getPubKey() + '|' + key.getAuth();
  }

  public static void register(Context context) {
    UnifiedPush.register(context, INSTANCE_DEFAULT, null, BuildConfig.VAPID_KEY);
  }

  public static void unregister(Context context) {
    UnifiedPush.unregister(context, INSTANCE_DEFAULT);
  }
}
