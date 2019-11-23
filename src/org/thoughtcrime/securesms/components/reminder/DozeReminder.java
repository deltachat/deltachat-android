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
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Prefs;

@SuppressLint("BatteryLife")
public class DozeReminder extends Reminder {

  @RequiresApi(api = Build.VERSION_CODES.M)
  public DozeReminder(@NonNull final Context context) {
    super(context.getString(R.string.perm_enable_bg_reminder_title),
          context.getString(R.string.perm_enable_bg_reminder_text));

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

    // if the chatlist only contains device-talk and self-talk, just after installation,
    // do not bother with battery, let the user check out other things first.
    try {
      int numberOfChats = DcHelper.getContext(context).getChatlist(0, null, 0).getCnt();
      if (numberOfChats <= 2) {
        return false;
      }
    }
    catch(Exception e) {
      e.printStackTrace();
    }

    return true; // yip, asking for disabling battery optimisations makes sense
  }
}
