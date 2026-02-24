package org.thoughtcrime.securesms.notifications;

import static android.content.ContentValues.TAG;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.IntentUtils;
import org.unifiedpush.android.connector.FailedReason;

/** Class to show notifications to inform users about UnifiedPush state */
public class UnifiedPushNotifications {

  public static void showAskToReconfigure(Context context) {
    // Launch the app with a new task flag to get the picker
    Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    PendingIntent pi =
        PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | IntentUtils.FLAG_MUTABLE());

    showNotification(
        context,
        R.string.notification_unifiedpush_reconfigure,
        R.string.notification_title_push_disabled,
        pi);
  }

  public static void showRegistrationFailed(Context context, FailedReason reason) {
    @StringRes int resId = 0;
    switch (reason) {
      case INTERNAL_ERROR:
      case ACTION_REQUIRED:
        resId = R.string.notification_unifiedpush_internal_error;
        break;
      case NETWORK:
        resId = R.string.notification_unifiedpush_network_error;
        break;
      case VAPID_REQUIRED:
        // This should not happen
        Log.e(TAG, "Received a VAPID_REQUIRED!");
        return;
    }
    if (resId != 0) {
      showNotification(context, resId, R.string.notification_title_push_disabled, null);
    }
  }

  public static void showRegistrationTimeout(Context context) {
    showNotification(
        context,
        R.string.notification_unifiedpush_timeout_error,
        R.string.notification_title_push_disabled,
        null);
  }

  /**
   * @param context
   * @param content
   * @param title set to `0` to disable
   * @param pendingIntent
   */
  private static void showNotification(
      Context context,
      @StringRes int content,
      @StringRes int title,
      @Nullable PendingIntent pendingIntent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      makeChannel(context);
    }
    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

    NotificationCompat.Builder builder =
        new NotificationCompat.Builder(context, NotificationCenter.CH_INFO)
            .setSmallIcon(R.drawable.icon_notification)
            .setColor(context.getResources().getColor(R.color.delta_primary))
            .setPriority(Notification.PRIORITY_HIGH)
            .setContentText(context.getString(content))
            .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getString(content)));

    if (title != 0) {
      builder.setContentTitle(context.getString(title));
    }
    if (pendingIntent != null) {
      builder.setAutoCancel(true).setContentIntent(pendingIntent);
    }

    notificationManager.notify(NotificationCenter.ID_INFO, builder.build());
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  private static void makeChannel(Context context) {
    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
    NotificationChannel channel =
        new NotificationChannel(
            NotificationCenter.CH_INFO, "General information", NotificationManager.IMPORTANCE_HIGH);
    channel.setDescription(
        "Inform about the application state, e.g. when the app needs to be opened to reconfigure push notifications.");
    notificationManager.createNotificationChannel(channel);
  }
}
