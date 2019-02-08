package org.thoughtcrime.securesms.components.reminder;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.view.View;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.Prefs;

@SuppressLint("BatteryLife")
public class DozeReminder extends Reminder {

  @RequiresApi(api = Build.VERSION_CODES.M)
  public DozeReminder(@NonNull final Context context) {
    super("Tap here to receive messages while Delta Chat is in background.",
          "Delta Chat uses minimal resources and takes care not to drain your battery.");

    setOkListener(v -> {
      if(ContextCompat.checkSelfPermission(context, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)==PackageManager.PERMISSION_GRANTED) {
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
      }
      else {
        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        context.startActivity(intent);
      }
    });

    setDismissListener(v -> {
      Prefs.  setPromptedOptimizeDoze(context, true);
    });
  }

  public static boolean isEligible(Context context) {
    if(context==null) {
      return false;
    }

    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return false;
    }

    if(Prefs.hasPromptedOptimizeDoze(context)) {
      return false;
    }

    PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
    if(pm.isIgnoringBatteryOptimizations(context.getPackageName())) {
      return false;
    }

    return true; // yip, asking for disabling battery optimisations makes sense
  }
}
