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

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Prefs;

@SuppressLint("BatteryLife")
public class DozeReminder {
  public static boolean isEligible(Context context) {
    if(context==null) {
      return false;
    }

    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return false;
    }

    if(Prefs.getPrompteDozeMsgId(context)!=0) {
      return false;
    }

    // If we did never ask directly, we do not want to add a device message yet. First we want to try to ask directly.
    if (!Prefs.getBooleanPreference(context, Prefs.DOZE_ASKED_DIRECTLY, false)) {
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

  public static void addDozeReminderDeviceMsg(Context context) {
    DcContext dcContext = DcHelper.getContext(context);
    DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
    msg.setText(context.getString(R.string.perm_enable_bg_reminder_title)+"\n\n"
               +context.getString(R.string.perm_enable_bg_reminder_text));
    int msgId = dcContext.addDeviceMsg("android.doze-reminder", msg);
    if(msgId!=0) {
      Prefs.setPromptedDozeMsgId(context, msgId);
    }
  }

  public static boolean isDozeReminderMsg(Context context, DcMsg msg) {
    return msg != null
        && msg.getFromId() == DcContact.DC_CONTACT_ID_DEVICE
        && msg.getId() == Prefs.getPrompteDozeMsgId(context);
  }

  public static void dozeReminderTapped(Context context) {
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return;
    }

    PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
    if(pm.isIgnoringBatteryOptimizations(context.getPackageName())) {
      new AlertDialog.Builder(context)
              .setMessage(R.string.perm_enable_bg_already_done)
              .setPositiveButton(android.R.string.ok, null)
              .show();
      return;
    }

    if(ContextCompat.checkSelfPermission(context, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)==PackageManager.PERMISSION_GRANTED) {
      Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
              Uri.parse("package:" + context.getPackageName()));
      context.startActivity(intent);
    }
    else {
      Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
      context.startActivity(intent);
    }
  }

  public static void maybeAskDirectly(Context context) {
    try {
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
          && !Prefs.getBooleanPreference(context, Prefs.DOZE_ASKED_DIRECTLY, false)
          && ContextCompat.checkSelfPermission(context, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) == PackageManager.PERMISSION_GRANTED
          && !((PowerManager) context.getSystemService(Context.POWER_SERVICE)).isIgnoringBatteryOptimizations(context.getPackageName())) {

        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
      }
      // Prefs.DOZE_ASKED_DIRECTLY is also used above in isEligible().
      // As long as Prefs.DOZE_ASKED_DIRECTLY is false, isEligible() will return false
      // and no device message will be added.
      Prefs.setBooleanPreference(context, Prefs.DOZE_ASKED_DIRECTLY, true);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}
