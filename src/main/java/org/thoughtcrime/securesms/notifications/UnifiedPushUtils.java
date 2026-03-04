package org.thoughtcrime.securesms.notifications;

import android.app.Activity;
import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.Prefs;
import org.unifiedpush.android.connector.UnifiedPush;
import org.unifiedpush.android.connector.data.ResolvedDistributor;

public class UnifiedPushUtils {

  private interface DialogCallback{
    void onCancel();
    void onConfirm();
  }

  public static void mayInitUnifiedPush(Activity activity) {
    if (Prefs.isFcmPushEnabled(activity)) {
      // Do nothing, the application supports FCM
      return;
    }
    if (!Prefs.unifiedPush(activity)) {
      // return if UnifiedPush is explicitly disabled
      return;
    }
    if (UnifiedPush.getAckDistributor(activity) != null) {
      // Do nothing, UnifiedPush is initialized with ApplicationContext
      return;
    }
    ResolvedDistributor resolvedDistributor = UnifiedPush.INSTANCE.resolveDefaultDistributor(activity);
    if (resolvedDistributor instanceof ResolvedDistributor.Found) {
      // We now have a default distributor -> we use it
      UnifiedPush.saveDistributor(activity, ((ResolvedDistributor.Found) resolvedDistributor).getPackageName());
      ApplicationContext.getInstance(activity).initializePush();
    } else if (resolvedDistributor instanceof ResolvedDistributor.ToSelect) {
      selectUnifiedPushDistributor(activity);
    }
    // Else do nothing: the periodic sync is already setup during ApplicationContext init
  }


/**
 * The user has many distributors installed on the system, and none of them is defined as the
 * default one: it needs to be selected with the OS dialog
 *
 * <p>To avoid the OS dialog to pop from nowhere, we introduce it with a simple dialog to show
 * "You're about to select your UnifiedPush service." The OS dialog has a background showing
 * "UnifiedPush" but some users may not see it. It is better to introduce with plain text.</p>
 *
 * <p>Note: The user necessarily knows about UnifiedPush: they have installed and enabled UnifiedPush
 * on at least 2 distributors.</p>
 */
  private static void selectUnifiedPushDistributor(Activity activity) {
    DialogCallback callback = new DialogCallback() {
      private final Activity context = activity;
      @Override
      public void onCancel() {
        Prefs.setUnifiedPush(context, false);
      }

      @Override
      public void onConfirm() {
        UnifiedPush.tryUseDefaultDistributor(context, success -> {
          if (success) {
            Prefs.resetReliableService(context);
            ApplicationContext.getInstance(context).initializePush();
          } else {
            // The user has closed the OS dialog, we consider they don't want UnifiedPush
            Prefs.setUnifiedPush(context, false);
          }
          return null;
        });
      }
    };

    introduceUnifiedPushDialog(activity, callback);
  }

  private static void introduceUnifiedPushDialog(Context context, DialogCallback callback) {
    new AlertDialog.Builder(context)
      .setMessage(R.string.dialog_introduce_unifiedpush_selection)
      .setCancelable(true)
      .setNegativeButton(android.R.string.cancel, (_d, _i) -> callback.onCancel())
      .setPositiveButton(android.R.string.ok, (_d, _i) -> callback.onConfirm())
      .show();
  }
}
