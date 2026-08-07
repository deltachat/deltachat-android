package org.thoughtcrime.securesms.notifications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import javax.annotation.Nullable;
import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.KeepAliveService;
import org.thoughtcrime.securesms.service.UnifiedPushService;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;
import org.unifiedpush.android.connector.UnifiedPush;
import org.unifiedpush.android.connector.data.ResolvedDistributor;

public class UnifiedPushUtils {
  private static String TAG = "UnifiedPushUtils";

  /** Used to update the UI with broadcasts if something fails */
  public static final String PUSH_ERROR_ACTION = "push_event";

  private interface DialogCallback {
    void onCancel();

    void onConfirm();
  }

  public interface InitCallback {
    void onInit(InitStatus status);
  }

  public interface TryPickCallback {
    void run(boolean success);
  }

  public enum InitStatus {
    /** Push is configured */
    HasPush,
    /** There is no push: we need to ask for doze reminder */
    NoPush,
    /** Push is being setting up, we <i>should</i> have Push in a few seconds */
    PushInit
  }

  /**
   * Init UnifiedPush if FCM isn't enabled and UnifiedPush isn't disabled
   *
   * @param activity Activity
   * @param initCallback Callback with [InitStatus]
   */
  public static void mayInitUnifiedPush(Activity activity, InitCallback initCallback) {
    Log.d(TAG, "mayInitUnifiedPush");
    if (Prefs.isFcmPushEnabled(activity)) {
      initCallback.onInit(InitStatus.HasPush);
      return;
    }
    if (Prefs.unifiedPushDisabled(activity)) {
      initCallback.onInit(InitStatus.NoPush);
      return;
    }
    if (UnifiedPush.getAckDistributor(activity) != null) {
      initCallback.onInit(InitStatus.HasPush);
      return;
    }
    ResolvedDistributor resolvedDistributor = UnifiedPush.resolveDefaultDistributor(activity);
    if (resolvedDistributor instanceof ResolvedDistributor.Found) {
      // We now have a default distributor -> we use it
      UnifiedPush.saveDistributor(
          activity, ((ResolvedDistributor.Found) resolvedDistributor).getPackageName());
      ApplicationContext.getInstance(activity).initializePush();
      initCallback.onInit(InitStatus.PushInit);
    } else if (resolvedDistributor instanceof ResolvedDistributor.ToSelect) {
      selectUnifiedPushDistributor(activity, initCallback);
    } else {
      initCallback.onInit(InitStatus.NoPush);
    }
  }

  /**
   * The user has many distributors installed on the system, and none of them is defined as the
   * default one: it needs to be selected with the OS dialog
   *
   * <p>To avoid the OS dialog to pop from nowhere, we introduce it with a simple dialog to show
   * "You're about to select your UnifiedPush service." The OS dialog has a background showing
   * "UnifiedPush" but some users may not see it. It is better to introduce with plain text.
   *
   * <p>Note: The user necessarily knows about UnifiedPush: they have installed and enabled
   * UnifiedPush on at least 2 distributors.
   */
  private static void selectUnifiedPushDistributor(Activity activity, InitCallback initCallback) {
    Log.d(TAG, "selectUnifiedPushDistributor");
    DialogCallback callback =
        new DialogCallback() {
          private final Activity context = activity;

          @Override
          public void onCancel() {
            Prefs.disableUnifiedPush(context);
            initCallback.onInit(InitStatus.NoPush);
          }

          @Override
          public void onConfirm() {
            UnifiedPush.tryUseDefaultDistributor(
                context,
                success -> {
                  if (success) {
                    ApplicationContext.getInstance(context).initializePush();
                    initCallback.onInit(InitStatus.PushInit);
                  } else {
                    // The user has closed the OS dialog, we consider they don't want UnifiedPush
                    Prefs.disableUnifiedPush(context);
                    initCallback.onInit(InitStatus.NoPush);
                  }
                  return null;
                });
          }
        };

    introduceUnifiedPushDialog(activity, callback);
  }

  private static void introduceUnifiedPushDialog(Context context, DialogCallback callback) {
    Log.d(TAG, "introduceUnifiedPushDialog");
    new AlertDialog.Builder(context)
        .setMessage(R.string.dialog_introduce_unifiedpush_selection)
        .setCancelable(true)
        .setNegativeButton(android.R.string.cancel, (_d, _i) -> callback.onCancel())
        .setPositiveButton(android.R.string.ok, (_d, _i) -> callback.onConfirm())
        .show();
  }

  /**
   * Try to use a non-default distributor.
   *
   * <p>Runs `callback.run(true)` if a new distributor has been picked, else `callback.run(false)`
   *
   * <p>Does not check if Push notifications, or UnifiedPush are enabled
   *
   * @param activity Activity
   * @param callback Callback
   */
  public static void tryPickUnifiedPushDistributor(Activity activity, TryPickCallback callback) {
    Log.d(TAG, "tryPickUnifiedPushDistributor");
    UnifiedPush.tryPickDistributor(
        activity,
        res -> {
          if (res) {
            ApplicationContext.getInstance(activity).initializePush();
          }
          callback.run(res);
          return null;
        });
  }

  /**
   * @param context
   * @param confirmed if we have already received an endpoint
   * @return `true` if we have a distributor registered, with the confirmed condition
   */
  public static boolean hasPushDistributor(Context context, boolean confirmed) {
    Log.d(TAG, "hasPushDistributor");
    if (confirmed) {
      return UnifiedPush.getAckDistributor(context) != null;
    } else {
      return UnifiedPush.getSavedDistributor(context) != null;
    }
  }

  public static int countAvailableDistributors(Context context) {
    Log.d(TAG, "countAvailableDistributor");
    return UnifiedPush.getDistributors(context).size();
  }

  public static @Nullable String getDistributorName(Context context) {
    String distributor = UnifiedPush.getSavedDistributor(context);
    if (distributor == null) return null;
    try {
      ApplicationInfo ai;
      if (Build.VERSION.SDK_INT >= 33) {
        ai =
            context
                .getPackageManager()
                .getApplicationInfo(
                    distributor,
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA));
      } else {
        ai = context.getPackageManager().getApplicationInfo(distributor, 0);
      }
      return context.getPackageManager().getApplicationLabel(ai).toString();
    } catch (PackageManager.NameNotFoundException e) {
      Log.e(TAG, "Could not resolve app name", e);
      return distributor;
    }
  }

  /**
   * If an error occur during registration, we disable UnifiedPush, reset reliable service pref and
   * try to start the KeepAliveService
   *
   * @param context
   */
  public static void disableOnError(Context context) {
    Prefs.disableUnifiedPush(context);
    UnifiedPushService.unregister(context);
    Prefs.resetReliableService(context);
    context.sendBroadcast(
        new Intent().setPackage(context.getPackageName()).setAction(PUSH_ERROR_ACTION));
    try {
      KeepAliveService.maybeStartSelf(context);
    } catch (Exception e) {
      Log.e(TAG, "An error occurred while trying to start KeepAliveService", e);
    }
  }

  /**
   * Returns directly if we don't have registered for UnifiedPush, or if we are already registered,
   * and we have received an endpoint. Else, wait for the endpoint, or a registration failed.
   *
   * <p>Disable UnifiedPush if we don't receive one or the other within the timeout, and show a
   * notification to the user.
   *
   * @param context
   */
  public static void waitForRegisterFinished(Context context) {
    Log.d(TAG, "waitForRegisterFinished");
    // Wait 8 secs at most
    for (int i = 0; i < 16; ++i) {
      // This is the distributor we registered to
      String saved = UnifiedPush.getSavedDistributor(context);
      // This is the distributor we registered to, which has sent an endpoint
      String ack = UnifiedPush.getAckDistributor(context);
      // If we don't have a saved distributor (saved == null: 1. we never registered,
      // or 2. it received registrationFailed during the first registration
      // or 3. we were unregistered)
      // Or if we received an endpoint (saved.equals(ack))
      // => return
      if (saved == null || saved.equals(ack)) return;
      Util.sleep(500);
    }
    disableOnError(context);
    UnifiedPushNotifications.showRegistrationTimeout(context);
  }
}
