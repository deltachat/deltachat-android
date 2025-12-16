package org.thoughtcrime.securesms.notifications;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.service.FetchForegroundService;
import org.thoughtcrime.securesms.util.Util;

public class FcmReceiveService extends FirebaseMessagingService {
  private static final String TAG = FcmReceiveService.class.getSimpleName();
  private static final Object INIT_LOCK = new Object();
  private static boolean initialized;
  private static volatile boolean triedRegistering;
  private static volatile String prefixedToken;

  public static void register(Context context) {

    if (FcmReceiveService.prefixedToken != null) {
      Log.i(TAG, "FCM already registered");
      triedRegistering = true;
      return;
    }

    Util.runOnAnyBackgroundThread(() -> {
      final String rawToken;

      try {
        synchronized (INIT_LOCK) {
          if (!initialized) {
            // manual init: read tokens from `./google-services.json`;
            // automatic init disabled in AndroidManifest.xml to skip FCM code completely.
            FirebaseApp.initializeApp(context);
          }
          initialized = true;
        }
        rawToken = Tasks.await(FirebaseMessaging.getInstance().getToken());
      } catch (Exception e) {
        // we're here usually when FCM is not available and initializeApp() or getToken() failed.
        Log.w(TAG, "cannot get FCM token for " + BuildConfig.APPLICATION_ID + ": " + e);
        triedRegistering = true;
        return;
      }
      if (TextUtils.isEmpty(rawToken)) {
        Log.w(TAG, "got empty FCM token for " + BuildConfig.APPLICATION_ID);
        triedRegistering = true;
        return;
      }

      prefixedToken = addPrefix(rawToken);
      Log.i(TAG, "FCM token: " + prefixedToken);
      ApplicationContext.getDcAccounts().setPushDeviceToken(prefixedToken);
      triedRegistering = true;
    });
  }

  // wait a until FCM registration got a token or not.
  // we're calling register() pretty soon and getToken() pretty late on init,
  // so usually, this should not block anything.
  // still, waitForRegisterFinished() needs to be called from a background thread.
  @WorkerThread
  public static void waitForRegisterFinished() {
    while (!triedRegistering) {
      Util.sleep(100);
    }
  }

  private static String addPrefix(String rawToken) {
    return "fcm-" + BuildConfig.APPLICATION_ID + ":" + rawToken;
  }

  @Nullable
  public static String getToken() {
    return prefixedToken;
  }

  @WorkerThread
  @Override
  public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
    Log.i(TAG, "FCM push notification received");
    FetchForegroundService.start(this);
  }

  @Override
  public void onDeletedMessages() {
    Log.i(TAG, "FCM push notifications dropped");
    FetchForegroundService.start(this);
  }

  @Override
  public void onNewToken(@NonNull String rawToken) {
    prefixedToken = addPrefix(rawToken);
    Log.i(TAG, "new FCM token: " + prefixedToken);
    ApplicationContext.getDcAccounts().setPushDeviceToken(prefixedToken);
  }
}
