package org.thoughtcrime.securesms.notifications;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.util.Util;

public class FcmReceiveService extends FirebaseMessagingService {
  private static final String TAG = FcmReceiveService.class.getSimpleName();
  private static final String TOKEN_PREFIX = "fcm:";
  private static String prefixedToken;

  public static void register(Context context) {
    Util.runOnAnyBackgroundThread(() -> {
      final String rawToken;

      try {
        // init needed if running outside of main process
        if (FirebaseApp.initializeApp(context) == null) {
          Log.w(TAG, "cannot init FCM for " + BuildConfig.APPLICATION_ID);
          return;
        } else {
          rawToken = Tasks.await(FirebaseMessaging.getInstance().getToken());
        }
      } catch (Exception e) {
        Log.w(TAG, "cannot get FCM token for " + BuildConfig.APPLICATION_ID + ": " + e);
        return;
      }
      if (TextUtils.isEmpty(rawToken)) {
        Log.w(TAG, "got empty FCM token for " + BuildConfig.APPLICATION_ID);
        return;
      }

      String prefixedToken = TOKEN_PREFIX + rawToken;
      synchronized (FcmReceiveService.TOKEN_PREFIX) {
        FcmReceiveService.prefixedToken = prefixedToken;
      }

      Log.w(TAG, "FCM token for " + BuildConfig.APPLICATION_ID + ": " + prefixedToken);
      ApplicationContext.dcAccounts.setPushDeviceToken(prefixedToken);
    });
  }

  @Nullable
  public static String getToken() {
    synchronized (FcmReceiveService.TOKEN_PREFIX) {
      return FcmReceiveService.prefixedToken;
    }
  }

  @Override
  public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
    Log.i(TAG, "FCM push notification received");
  }

  @Override
  public void onNewToken(@NonNull String rawToken) {
    String prefixedToken = TOKEN_PREFIX + rawToken;
    synchronized (FcmReceiveService.TOKEN_PREFIX) {
      FcmReceiveService.prefixedToken = prefixedToken;
    }

    Log.i(TAG, "new FCM token for" + BuildConfig.APPLICATION_ID + ": " + prefixedToken);
    ApplicationContext.dcAccounts.setPushDeviceToken(prefixedToken);
  }
}
