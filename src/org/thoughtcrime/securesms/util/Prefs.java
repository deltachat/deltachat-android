package org.thoughtcrime.securesms.util;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera.CameraInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.mapbox.mapboxsdk.geometry.LatLng;

import org.thoughtcrime.securesms.preferences.widgets.NotificationPrivacyPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.mapbox.mapboxsdk.constants.MapboxConstants.MINIMUM_ZOOM;

public class Prefs {

  private static final String TAG = Prefs.class.getSimpleName();

  public  static final String CHANGE_PASSPHRASE_PREF           = "pref_change_passphrase";
  public  static final String DISABLE_PASSPHRASE_PREF          = "pref_disable_passphrase";
  public  static final String THEME_PREF                       = "pref_theme";
  public  static final String LANGUAGE_PREF                    = "pref_language";
  public  static final String BACKGROUND_PREF                  = "pref_chat_background";

  public  static final String RINGTONE_PREF                    = "pref_key_ringtone";
  private static final String VIBRATE_PREF                     = "pref_key_vibrate";
  private static final String CHAT_VIBRATE                     = "pref_chat_vibrate_"; // followed by chat-id
  private static final String NOTIFICATION_PREF                = "pref_key_enable_notifications";
  public  static final String LED_COLOR_PREF                   = "pref_led_color";
  public  static final String LED_BLINK_PREF                   = "pref_led_blink";
  private static final String LED_BLINK_PREF_CUSTOM            = "pref_led_blink_custom";
  private static final String CHAT_MUTED_UNTIL                 = "pref_chat_muted_until_"; // followed by chat-id
  private static final String CHAT_RINGTONE                    = "pref_chat_ringtone_"; // followed by chat-id
  public  static final String SCREEN_LOCK_TIMEOUT_INTERVAL_PREF = "pref_timeout_interval";
  public  static final String SCREEN_LOCK_TIMEOUT_PREF         = "pref_timeout_passphrase";
  public  static final String SCREEN_SECURITY_PREF             = "pref_screen_security";
  private static final String ENTER_SENDS_PREF                 = "pref_enter_sends";
  private static final String PROMPTED_OPTIMIZE_DOZE_PREF      = "pref_prompted_optimize_doze";
  private static final String IN_THREAD_NOTIFICATION_PREF      = "pref_key_inthread_notifications";
  public  static final String MESSAGE_BODY_TEXT_SIZE_PREF      = "pref_message_body_text_size";

  public  static final String NOTIFICATION_PRIVACY_PREF        = "pref_notification_privacy";
  public  static final String NOTIFICATION_PRIORITY_PREF       = "pref_notification_priority";

  public  static final String SYSTEM_EMOJI_PREF                = "pref_system_emoji";
  public  static final String DIRECT_CAPTURE_CAMERA_ID         = "pref_direct_capture_camera_id";
  private static final String PROFILE_AVATAR_ID_PREF           = "pref_profile_avatar_id";
  public  static final String INCOGNITO_KEYBORAD_PREF          = "pref_incognito_keyboard";

  public static final String SCREEN_LOCK         = "pref_android_screen_lock";

  private static final String PREF_CONTACT_PHOTO_IDENTIFIERS = "pref_contact_photo_identifiers";

  private static final String MAP_CENTER_LATITUDE = "pref_map_center_latitude";
  private static final String MAP_CENTER_LONGITUDE = "pref_map_center_longitude";
  private static final String MAP_ZOOM = "pref_map_zoom";

  public enum VibrateState {
    DEFAULT(0), ENABLED(1), DISABLED(2);
    private final int id;
    VibrateState(int id) { this.id = id; }
    public int getId() { return id; }
    public static VibrateState fromId(int id) { return values()[id]; }
  }

  public static boolean isScreenLockEnabled(@NonNull Context context) {
    return getBooleanPreference(context, SCREEN_LOCK, false);
  }

  public static void setScreenLockEnabled(@NonNull Context context, boolean value) {
    setBooleanPreference(context, SCREEN_LOCK, value);
  }

  public static boolean isIncognitoKeyboardEnabled(Context context) {
    return getBooleanPreference(context, INCOGNITO_KEYBORAD_PREF, false);
  }

  public static void setProfileAvatarId(Context context, int id) {
    setIntegerPrefrence(context, PROFILE_AVATAR_ID_PREF, id);
  }

  public static int getProfileAvatarId(Context context) {
    return getIntegerPreference(context, PROFILE_AVATAR_ID_PREF, 0);
  }

  public static int getNotificationPriority(Context context) {
    return Integer.valueOf(getStringPreference(context, NOTIFICATION_PRIORITY_PREF, String.valueOf(NotificationCompat.PRIORITY_HIGH)));
  }

  public static int getMessageBodyTextSize(Context context) {
    return Integer.valueOf(getStringPreference(context, MESSAGE_BODY_TEXT_SIZE_PREF, "16"));
  }

  public static void setDirectCaptureCameraId(Context context, int value) {
    setIntegerPrefrence(context, DIRECT_CAPTURE_CAMERA_ID, value);
  }

  @SuppressWarnings("deprecation")
  public static int getDirectCaptureCameraId(Context context) {
    return getIntegerPreference(context, DIRECT_CAPTURE_CAMERA_ID, CameraInfo.CAMERA_FACING_FRONT);
  }

  public static NotificationPrivacyPreference getNotificationPrivacy(Context context) {
    return new NotificationPrivacyPreference(getStringPreference(context, NOTIFICATION_PRIVACY_PREF, "all"));
  }

  public static boolean isInChatNotifications(Context context) {
    return getBooleanPreference(context, IN_THREAD_NOTIFICATION_PREF, true);
  }

  public static boolean isEnterSendsEnabled(Context context) {
    return getBooleanPreference(context, ENTER_SENDS_PREF, false);
  }

  public static boolean isPasswordDisabled(Context context) {
    return getBooleanPreference(context, DISABLE_PASSPHRASE_PREF, false);
  }

  public static void setScreenSecurityEnabled(Context context, boolean value) {
    setBooleanPreference(context, SCREEN_SECURITY_PREF, value);
  }

  public static boolean isScreenSecurityEnabled(Context context) {
    return getBooleanPreference(context, SCREEN_SECURITY_PREF, false);
  }

  public static String getTheme(Context context) {
    return getStringPreference(context, THEME_PREF, "light");
  }

  public static boolean isScreenLockTimeoutEnabled(Context context) {
    return getBooleanPreference(context, SCREEN_LOCK_TIMEOUT_PREF, false);
  }

  public static int getScreenLockTimeoutInterval(Context context) {
    return getIntegerPreference(context, SCREEN_LOCK_TIMEOUT_INTERVAL_PREF, 5 * 60);
  }

  public static void setScreenLockTimeoutInterval(Context context, int interval) {
    setIntegerPrefrence(context, SCREEN_LOCK_TIMEOUT_INTERVAL_PREF, interval);
  }

  public static String getLanguage(Context context) {
    return getStringPreference(context, LANGUAGE_PREF, "zz");
  }

  public static void setLanguage(Context context, String language) {
    setStringPreference(context, LANGUAGE_PREF, language);
  }

  public static void setPromptedOptimizeDoze(Context context, boolean value) {
    setBooleanPreference(context, PROMPTED_OPTIMIZE_DOZE_PREF, value);
  }

  public static boolean hasPromptedOptimizeDoze(Context context) {
    return getBooleanPreference(context, PROMPTED_OPTIMIZE_DOZE_PREF, false);
  }

  public static boolean isNotificationsEnabled(Context context) {
    return getBooleanPreference(context, NOTIFICATION_PREF, true);
  }

  public static boolean isHardCompressionEnabled(Context context) {
    try {
      return getStringPreference(context, "pref_compression", "0").equals("1");
    }
    catch(Exception e) {
      return false;
    }
  }

  public static boolean isLocationStreamingEnabled(Context context) {
    try {
      return getBooleanPreference(context, "pref_location_streaming_enabled", false);
    }
    catch(Exception e) {
      return false;
    }
  }

  // ringtone

  public static @NonNull Uri getNotificationRingtone(Context context) {
    String result = getStringPreference(context, RINGTONE_PREF, Settings.System.DEFAULT_NOTIFICATION_URI.toString());

    if (result != null && result.startsWith("file:")) {
      result = Settings.System.DEFAULT_NOTIFICATION_URI.toString();
    }

    return Uri.parse(result);
  }

  public static void removeNotificationRingtone(Context context) {
    removePreference(context, RINGTONE_PREF);
  }

  public static void setNotificationRingtone(Context context, Uri ringtone) {
    setStringPreference(context, RINGTONE_PREF, ringtone.toString());
  }

  public static void setChatRingtone(Context context, int chatId, Uri ringtone) {
    if(ringtone!=null) {
      setStringPreference(context, CHAT_RINGTONE+chatId, ringtone.toString());
    }
    else {
      removePreference(context, CHAT_RINGTONE+chatId);
    }
  }

  public static @Nullable Uri getChatRingtone(Context context, int chatId) {
    String result = getStringPreference(context, CHAT_RINGTONE+chatId, null);
    return result==null? null : Uri.parse(result);
  }

  // vibrate

  public static boolean isNotificationVibrateEnabled(Context context) {
    return getBooleanPreference(context, VIBRATE_PREF, true);
  }

  public static void setChatVibrate(Context context, int chatId, VibrateState vibrateState) {
    if(vibrateState!=VibrateState.DEFAULT) {
      setIntegerPrefrence(context, CHAT_VIBRATE+chatId, vibrateState.getId());
    }
    else {
      removePreference(context, CHAT_VIBRATE+chatId);
    }
  }

  public static VibrateState getChatVibrate(Context context, int chatId) {
    return VibrateState.fromId(getIntegerPreference(context, CHAT_VIBRATE+chatId, VibrateState.DEFAULT.getId()));
  }

  // led

  public static String getNotificationLedColor(Context context) {
    return getStringPreference(context, LED_COLOR_PREF, "blue");
  }

  public static String getNotificationLedPattern(Context context) {
    return getStringPreference(context, LED_BLINK_PREF, "500,2000");
  }

  public static String getNotificationLedPatternCustom(Context context) {
    return getStringPreference(context, LED_BLINK_PREF_CUSTOM, "500,2000");
  }

  // mute

  public static void setChatMutedUntil(Context context, int chatId, long until) {
    setLongPreference(context, CHAT_MUTED_UNTIL+chatId, until);
  }

  public static long getChatMutedUntil(Context context, int chatId) {
    return getLongPreference(context, CHAT_MUTED_UNTIL+chatId, 0);
  }

  public static boolean isChatMuted(Context context, int chatId) {
    return System.currentTimeMillis() <= getChatMutedUntil(context, chatId);
  }

  // map

  public static void setMapCenter(Context context, int chatId, LatLng latLng) {
    setLongPreference(context, MAP_CENTER_LATITUDE+chatId, Double.doubleToRawLongBits(latLng.getLatitude()));
    setLongPreference(context, MAP_CENTER_LONGITUDE+chatId, Double.doubleToRawLongBits(latLng.getLongitude()));
  }

  public static void setMapZoom(Context context, int chatId, double zoom) {
    setLongPreference(context, MAP_ZOOM+chatId, Double.doubleToRawLongBits(zoom));
  }

  public static LatLng getMapCenter(Context context, int chatId) {
    long latitude = getLongPreference(context, MAP_CENTER_LATITUDE+chatId, Long.MAX_VALUE);
    long longitude = getLongPreference(context, MAP_CENTER_LONGITUDE+chatId, Long.MAX_VALUE);
    if (latitude == Long.MAX_VALUE || longitude == Long.MAX_VALUE) {
      return null;
    }
    return new LatLng(Double.longBitsToDouble(latitude), Double.longBitsToDouble(longitude));
  }

  public static double getMapZoom(Context context, int chatId) {
    long zoom = getLongPreference(context, MAP_ZOOM+chatId, Double.doubleToLongBits(MINIMUM_ZOOM));
    return Double.longBitsToDouble(zoom);
  }

  // misc.

  public static String getBackgroundImagePath(Context context) {
    return getStringPreference(context, BACKGROUND_PREF, "");
  }

  public static void setBackgroundImagePath(Context context, String path) {
    setStringPreference(context, BACKGROUND_PREF, path);
  }

  public static boolean isSystemEmojiPreferred(Context context) {
    return getBooleanPreference(context, SYSTEM_EMOJI_PREF, false);
  }

  // generic preference functions

  public static void setBooleanPreference(Context context, String key, boolean value) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, value).apply();
  }

  public static boolean getBooleanPreference(Context context, String key, boolean defaultValue) {
    return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, defaultValue);
  }

  public static void setStringPreference(Context context, String key, String value) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).apply();
  }

  public static String getStringPreference(Context context, String key, String defaultValue) {
    return PreferenceManager.getDefaultSharedPreferences(context).getString(key, defaultValue);
  }

  private static int getIntegerPreference(Context context, String key, int defaultValue) {
    return PreferenceManager.getDefaultSharedPreferences(context).getInt(key, defaultValue);
  }

  private static void setIntegerPrefrence(Context context, String key, int value) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(key, value).apply();
  }

  private static long getLongPreference(Context context, String key, long defaultValue) {
    return PreferenceManager.getDefaultSharedPreferences(context).getLong(key, defaultValue);
  }

  private static void setLongPreference(Context context, String key, long value) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(key, value).apply();
  }

  private static void removePreference(Context context, String key) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().remove(key).apply();
  }

  private static Set<String> getStringSetPreference(Context context, String key, Set<String> defaultValues) {
    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    if (prefs.contains(key)) {
      return prefs.getStringSet(key, Collections.<String>emptySet());
    } else {
      return defaultValues;
    }
  }

  public static void setSystemContactPhotos(Context context, Set<String> contactPhotoIdentifiers) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet(PREF_CONTACT_PHOTO_IDENTIFIERS, contactPhotoIdentifiers).apply();
  }

  public static Uri getSystemContactPhoto(Context context, String identifier) {
    List<String> contactPhotoIdentifiers = new ArrayList<>(getStringSetPreference(context, PREF_CONTACT_PHOTO_IDENTIFIERS, new HashSet<>()));
    for(String contactPhotoIdentifier : contactPhotoIdentifiers) {
      if (contactPhotoIdentifier.contains(identifier)) {
        String[] parts = contactPhotoIdentifier.split("\\|");
        long contactId = Long.valueOf(parts[1]);
        return ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
      }
    }
    return null;
  }

}