package org.thoughtcrime.securesms.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioAttributes;
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

  void setAlarms(@Nullable Uri ringtone, Prefs.VibrateState vibrate) {
    Uri     appDefaultRingtone = Prefs.getNotificationRingtone(context);
    boolean appDefaultVibrate  = Prefs.isNotificationVibrateEnabled(context);

    // TODO: chat-specific sounds/vibrate do not work in Android O or newer,
    // we should move these settings to the notification channel, if we want to continue support this
    // (maybe "mute" is sufficient)
    if (ringtone == null && !TextUtils.isEmpty(appDefaultRingtone.toString())) {
      setSound(appDefaultRingtone);
    } else if (ringtone != null && !ringtone.toString().isEmpty()) {
      setSound(ringtone);
    }

    if (vibrate == Prefs.VibrateState.ENABLED ||
            (vibrate == Prefs.VibrateState.DEFAULT && appDefaultVibrate))
    {
      setDefaults(Notification.DEFAULT_VIBRATE);
    }
  }

  private void setLed() {
    // for Android O or newer, this is handled by the notification channel
    String ledColor = Prefs.getNotificationLedColor(context);
    if (!ledColor.equals("none")) {
      int argb;
      try {
        argb = Color.parseColor(ledColor);
      }
      catch (Exception e) {
        argb = Color.rgb(0xFF, 0xFF, 0xFF);
      }
      setLights(argb,500, 2000);
    }
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

          if (!TextUtils.isEmpty(ringtone.toString())) {
            channel.setSound(ringtone,
                    new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                            .build());
          }

          channel.enableVibration(defaultVibrate);

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
}
