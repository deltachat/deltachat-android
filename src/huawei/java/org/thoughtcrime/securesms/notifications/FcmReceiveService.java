package org.thoughtcrime.securesms.notifications;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.service.FetchForegroundService;
import org.thoughtcrime.securesms.util.Util;

public class FcmReceiveService extends HmsMessageService {
  private static final String TAG = "Huawei:PushService";
  private static volatile boolean triedRegistering;
  private static volatile String prefixedToken;

  public static void register(Context context) {

    if (FcmReceiveService.prefixedToken != null) {
      Log.i(TAG, "Huawei: PushNotifications already registered");
      triedRegistering = true;
      return;
    }

    Util.runOnAnyBackgroundThread(() -> {
      final String rawToken;
      final String tokenScope = "HCM";
      final String appId = "108252361"; // taken from agconnect-services.json

      try {
        rawToken = HmsInstanceId.getInstance(context).getToken(appId, tokenScope);
      } catch (Exception e) {
        // we're here usually when PushNotifications is not available and getToken() failed.
        Log.w(TAG, "cannot get huawei token for " + BuildConfig.APPLICATION_ID + ": " + e);
        triedRegistering = true;
        return;
      }
      if (TextUtils.isEmpty(rawToken)) {
        Log.w(TAG, "got empty huawei token for " + BuildConfig.APPLICATION_ID);
        triedRegistering = true;
        return;
      }

      prefixedToken = addPrefix(rawToken);
      Log.i(TAG, "FCM token: " + prefixedToken);
      ApplicationContext.dcAccounts.setPushDeviceToken(prefixedToken);
      triedRegistering = true;
    });
  }

  // wait a until registration got a token or not.
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
    return "huawei:" + rawToken;
  }

  @Nullable
  public static String getToken() {
    return prefixedToken;
  }

  @WorkerThread
  @Override
  public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
    Log.i(TAG, "push notification received");
    FetchForegroundService.start(this);
  }

  @Override
  public void onDeletedMessages() {
    Log.i(TAG, "push notifications dropped");
    FetchForegroundService.start(this);
  }

  @Override
  public void onNewToken(@NonNull String rawToken) {
    prefixedToken = addPrefix(rawToken);
    Log.i(TAG, "new token: " + prefixedToken);
    ApplicationContext.dcAccounts.setPushDeviceToken(prefixedToken);
  }
}
