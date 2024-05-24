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
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;

public class FcmReceiveService extends FirebaseMessagingService {
  private static final String TAG = FcmReceiveService.class.getSimpleName();
  private static final Object INIT_LOCK = new Object();
  private static boolean initialized;
  private static volatile boolean triedRegistering;
  private static volatile String prefixedToken;

  public static void register(Context context) {
    if(!Prefs.isPushEnabled(context)) {
      Log.w(TAG, "FCM disabled in user settings");
      return;
    }

    if (Build.VERSION.SDK_INT < 19) {
      Log.w(TAG, "FCM not available on SDK < 19");
      triedRegistering = true;
      return;
    }

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
      ApplicationContext.dcAccounts.setPushDeviceToken(prefixedToken);
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

  @Override
  public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
    Log.i(TAG, "FCM push notification received");
    // the app is running (again) now and fetching and notifications should be processed as usual.
    // to support accounts that do not send PUSH notifications and for simplicity,
    // we just let the app run as long as possible.
  }

  @Override
  public void onDeletedMessages() {
    Log.i(TAG, "FCM push notifications dropped");
    // nothing special to do as we're running now and notifications should be processed as usual.
  }

  @Override
  public void onNewToken(@NonNull String rawToken) {
    prefixedToken = addPrefix(rawToken);
    Log.i(TAG, "new FCM token: " + prefixedToken);
    ApplicationContext.dcAccounts.setPushDeviceToken(prefixedToken);
  }
}
