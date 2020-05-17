package org.thoughtcrime.securesms.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.preferences.widgets.NotificationPrivacyPreference;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.List;

abstract class AbstractNotificationBuilder extends NotificationCompat.Builder {

  @SuppressWarnings("unused")
  private static final String TAG = AbstractNotificationBuilder.class.getSimpleName();

  protected Context                       context;
  protected NotificationPrivacyPreference privacy;
  private int notificationId;
  private Uri ringtone;
  private boolean vibrate;

  AbstractNotificationBuilder(Context context, NotificationPrivacyPreference privacy) {
    super(context, createMsgNotificationChannel(context));

    this.context = context;
    this.privacy = privacy;

    setLed();
  }

  CharSequence getStyledMessage(@NonNull Recipient recipient, @Nullable CharSequence message) {
    SpannableStringBuilder builder = new SpannableStringBuilder();
    builder.append(Util.getBoldedString(recipient.toShortString()));
    builder.append(": ");
    builder.append(message == null ? "" : message);

    return builder;
  }

  // Alarms are not set in the notification or the notification channel but handled separately
  // by the MessageNotifier. It allows us to dynamically turn on and off the sounds and as well as
  // to change vibration and sounds during runtime
  void setAlarms(int systemRingerMode, @Nullable Uri ringtone, Prefs.VibrateState vibrate) {
    Uri     appDefaultRingtone = Prefs.getNotificationRingtone(context);
    boolean appDefaultVibrate  = Prefs.isNotificationVibrateEnabled(context);
    if (systemRingerMode == AudioManager.RINGER_MODE_NORMAL) {
      if (ringtone == null && !TextUtils.isEmpty(appDefaultRingtone.toString())) {
        this.ringtone = appDefaultRingtone;
      } else if (ringtone != null && !ringtone.toString().isEmpty()) {
        this.ringtone = ringtone;
      }
    }

    this.vibrate = (systemRingerMode != AudioManager.RINGER_MODE_SILENT) &&
            (vibrate == Prefs.VibrateState.ENABLED ||
            (vibrate == Prefs.VibrateState.DEFAULT && appDefaultVibrate));
  }

  private void setLed() {
    /*
    String ledColor              = Prefs.getNotificationLedColor(context);
    String ledBlinkPattern       = Prefs.getNotificationLedPattern(context);
    String ledBlinkPatternCustom = Prefs.getNotificationLedPatternCustom(context);

    if (!ledColor.equals("none")) {
      String[] blinkPatternArray = parseBlinkPattern(ledBlinkPattern, ledBlinkPatternCustom);
      int argb;
      try {
        argb = Color.parseColor(ledColor);
      }
      catch (Exception e) {
        argb = Color.rgb(0xFF, 0xFF, 0xFF);
      }
      setLights(argb,
              Integer.parseInt(blinkPatternArray[0]),
              Integer.parseInt(blinkPatternArray[1]));
    }
    */
  }

  void setTicker(@NonNull Recipient recipient, @Nullable CharSequence message) {
    if (privacy.isDisplayMessage()) {
      setTicker(getStyledMessage(recipient, message));
    } else if (privacy.isDisplayContact()) {
      setTicker(getStyledMessage(recipient, context.getString(R.string.notify_new_message)));
    } else {
      setTicker(context.getString(R.string.notify_new_message));
    }
  }

  private String[] parseBlinkPattern(String blinkPattern, String blinkPatternCustom) {
    if (blinkPattern.equals("custom"))
      blinkPattern = blinkPatternCustom;

    return blinkPattern.split(",");
  }

  // handle NotificationChannels:
  // - since oreo, a NotificationChannel is a MUST
  // - NotificationChannels have default values that have a higher precedence as the Notification.Builder setting
  // - once created, NotificationChannels cannot be modified programmatically
  // - NotificationChannels can be deleted, however, on re-creation it becomes un-deleted with the old settings
  // - the idea is that sound and vibrate are handled outside of the scope of the notification channel

  private static String createMsgNotificationChannel(Context context) {
    String chBase = "ch_msg3_";
    String chId = chBase + "unsupported";

    if(notificationChannelsSupported()) {
      try {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        // get all values we'll use as settings for the NotificationChannel
        String ledColor = Prefs.getNotificationLedColor(context);

        // compute hash from these settings
        String hash = "";
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(ledColor.getBytes());
        hash = String.format("%X", new BigInteger(1, md.digest())).substring(0, 16);

        // get channel name
        chId = chBase + hash;
        String oldChId = Prefs.getStringPreference(context, "ch_curr_" + chBase, "");
        if (!oldChId.equals(chId)) {
          try {
            notificationManager.deleteNotificationChannel(oldChId);
          }
          catch (Exception e) {
            // channel not created before
          }
          Prefs.setStringPreference(context, "ch_curr_" + chBase, chId);
        }

        // check if there is already a channel with the given name
        List<NotificationChannel> channels = notificationManager.getNotificationChannels();
        boolean channelExists = false;
        for (int i = 0; i < channels.size(); i++) {
          if (chId.equals(channels.get(i).getId())) {
            channelExists = true;
          }
        }

        // create a channel with the given settings;
        // we cannot change the settings, however, this is handled by using different values for chId
        if(!channelExists) {
          NotificationChannel channel = new NotificationChannel(chId,
                  "New messages", NotificationManager.IMPORTANCE_HIGH);
          channel.setDescription("Informs about new messages.");

          if (!ledColor.equals("none")) {
            channel.enableLights(true);
            int argb;
            try {
              argb = Color.parseColor(ledColor);
            }
            catch (Exception e) {
              argb = Color.rgb(0xFF, 0xFF, 0xFF);
            }
            channel.setLightColor(argb);
          } else {
            channel.enableLights(false);
          }

          channel.setSound(null, null);
          channel.enableVibration(false);

          notificationManager.createNotificationChannel(channel);
        }
      }
      catch(Exception e) {
        e.printStackTrace();
      }
    }

    return chId;
  }

  private static boolean notificationChannelsSupported() {
    return Build.VERSION.SDK_INT >= 26;
  }


  public void setNotificationId(int notificationId) {
    this.notificationId = notificationId;
  }

  public int getNotificationId() {
    return this.notificationId;
  }

  public Uri getRingtone() {
    return this.ringtone;
  }

  public boolean getVibrate() {
    return this.vibrate;
  }
}
