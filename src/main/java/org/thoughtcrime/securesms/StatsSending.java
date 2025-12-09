package org.thoughtcrime.securesms;

import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_STATS_SENDING;
import static org.thoughtcrime.securesms.connect.DcHelper.openHelp;

import android.app.Activity;
import android.content.Context;
import android.text.util.Linkify;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.IntentUtils;
import org.thoughtcrime.securesms.util.Prefs;

import java.util.Locale;

public class StatsSending {
  /** @noinspection unused: We will start adding a device message once stats-sending is tested a bit */
  public static void maybeAddStatsSendingDeviceMsg(Context context) {
    if (Prefs.getStatsDeviceMsgId(context) != 0) {
      return;
    }
    if (DcHelper.getInt(context, CONFIG_STATS_SENDING) != 0) {
      return; // Stats-sending is already enabled
    }
    DcContext dcContext = DcHelper.getContext(context);
    DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
    msg.setText(context.getString(R.string.stats_device_message));
    int msgId = dcContext.addDeviceMsg("stats_device_message", msg);
    if (msgId!=0) {
      Prefs.setStatsDeviceMsgId(context, msgId);
    }
  }

  public static boolean isStatsSendingDeviceMsg(Context context, DcMsg msg) {
    return msg != null
      && msg.getFromId() == DcContact.DC_CONTACT_ID_DEVICE
      && msg.getId() == Prefs.getStatsDeviceMsgId(context);
  }

  public static void statsDeviceMsgTapped(Activity activity) {
    if (DcHelper.getInt(activity, CONFIG_STATS_SENDING) != 0) {
      showStatsDisableDialog(activity);
    } else {
      showStatsConfirmationDialog(activity, () -> {});
    }
  }

  public static void showStatsConfirmationDialog(Activity activity, Runnable onConfigChangedListener) {
    AlertDialog d = new AlertDialog.Builder(activity)
      .setMessage(R.string.stats_confirmation_dialog)
      .setNegativeButton(R.string.cancel, (_d, i) -> {})
      .setPositiveButton(R.string.yes, (_d, i) -> {
        DcHelper.set(activity, DcHelper.CONFIG_STATS_SENDING, "1");
        onConfigChangedListener.run();
        showStatsThanksDialog(activity);
      })
      .setNeutralButton(R.string.more_info_desktop, (_d, i) -> openHelp(activity, "#statssending"))
      .create();
    d.show();
    try {
      //noinspection DataFlowIssue
      Linkify.addLinks((TextView) d.findViewById(android.R.id.message), Linkify.WEB_URLS);
    } catch (NullPointerException e) {
      //noinspection CallToPrintStackTrace
      e.printStackTrace();
    }
  }

  private static void showStatsThanksDialog(Activity activity) {
    String stats_id = DcHelper.get(activity, DcHelper.CONFIG_STATS_ID);
    new AlertDialog.Builder(activity)
      .setMessage(R.string.stats_thanks)
      .setNeutralButton(R.string.no, (d, i) -> {})
      .setPositiveButton(R.string.yes, (d, i) -> {
        String ln = Locale.getDefault().getLanguage();
        IntentUtils.showInBrowser(activity, "https://cispa.qualtrics.com/jfe/form/SV_9YmhkpGa48KxfLg?id=" + stats_id + "&ln=" + ln);
      })
      .show();
  }

  public static void showStatsDisableDialog(Activity activity) {
    AlertDialog d = new AlertDialog.Builder(activity)
      .setMessage(R.string.stats_disable_dialog)
      .setNegativeButton(R.string.disable, (_d, i) -> {
        DcHelper.set(activity, DcHelper.CONFIG_STATS_SENDING, "0");
      })
      .setPositiveButton(R.string.stats_keep_sending, (_d, i) -> {
      })
      .setNeutralButton(R.string.more_info_desktop, (_d, i) -> openHelp(activity, "#statssending"))
      .create();
    d.show();
  }
}
