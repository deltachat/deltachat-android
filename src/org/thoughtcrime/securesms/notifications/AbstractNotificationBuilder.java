package org.thoughtcrime.securesms.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
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

public abstract class AbstractNotificationBuilder extends NotificationCompat.Builder {

  @SuppressWarnings("unused")
  private static final String TAG = AbstractNotificationBuilder.class.getSimpleName();

  protected Context                       context;
  protected NotificationPrivacyPreference privacy;

  public AbstractNotificationBuilder(Context context, NotificationPrivacyPreference privacy) {
    super(context);

    this.context = context;
    this.privacy = privacy;

    setLed();
  }

  protected CharSequence getStyledMessage(@NonNull Recipient recipient, @Nullable CharSequence message) {
    SpannableStringBuilder builder = new SpannableStringBuilder();
    builder.append(Util.getBoldedString(recipient.toShortString()));
    builder.append(": ");
    builder.append(message == null ? "" : message);

    return builder;
  }

  public void setAlarms(@Nullable Uri ringtone, Prefs.VibrateState vibrate) {
    Uri     defaultRingtone = Prefs.getNotificationRingtone(context);
    boolean defaultVibrate  = Prefs.isNotificationVibrateEnabled(context);

    if      (ringtone == null && !TextUtils.isEmpty(defaultRingtone.toString())) setSound(defaultRingtone);
    else if (ringtone != null && !ringtone.toString().isEmpty())                 setSound(ringtone);

    if (vibrate == Prefs.VibrateState.ENABLED ||
        (vibrate == Prefs.VibrateState.DEFAULT && defaultVibrate))
    {
      setDefaults(Notification.DEFAULT_VIBRATE);
    }
  }

  private void setLed() {
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
  }

  public void setTicker(@NonNull Recipient recipient, @Nullable CharSequence message) {
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
  // - the idea is that sound, led, vibrate is edited by the user
  //   via the ACTION_CHANNEL_NOTIFICATION_SETTINGS intent that takes the channelId

  protected String createMsgNotificationChannel(Context context) {
    String chBase = "ch_msg2_";
    String chId = chBase + "unsupported";

    if(notificationChannelsSupported()) {
      try {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        // get all values we'll use as settings for the NotificationChannel
        String ledColor = Prefs.getNotificationLedColor(context);
        boolean defaultVibrate = Prefs.isNotificationVibrateEnabled(context);
        Uri ringtone = Prefs.getNotificationRingtone(context);

        // compute hash from these settings
        String hash = "";
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(ledColor.getBytes());
        md.update(defaultVibrate ? (byte) 1 : (byte) 0);
        md.update(ringtone.toString().getBytes());
        hash = String.format("%X", new BigInteger(1, md.digest())).substring(0, 16);

        // get channel name
        chId = chBase + hash;
        String oldChId = Prefs.getStringPreference(context, "ch_curr_" + chBase, "");
        if (!oldChId.equals(chId)) {
          try {
            notificationManager.deleteNotificationChannel(oldChId);
          }
          catch (Exception e) {
            ; // channel not created before
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
              "New messages", NotificationManager.IMPORTANCE_DEFAULT);
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

          channel.enableVibration(defaultVibrate);

          if (!TextUtils.isEmpty(ringtone.toString())) {
            channel.setSound(ringtone,
                new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                    .build());
          }

          notificationManager.createNotificationChannel(channel);
        }
      }
      catch(Exception e) {
        e.printStackTrace();
      }
    }

    return chId;
  }

  protected static boolean notificationChannelsSupported() {
    return Build.VERSION.SDK_INT >= 26;
  }
}
