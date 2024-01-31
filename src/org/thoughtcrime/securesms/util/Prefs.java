package org.thoughtcrime.securesms.util;

import static com.mapbox.mapboxsdk.constants.MapboxConstants.MINIMUM_ZOOM;

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

import com.b44t.messenger.DcContext;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.preferences.widgets.NotificationPrivacyPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Prefs {

  private static final String TAG = Prefs.class.getSimpleName();

  public  static final String DISABLE_PASSPHRASE_PREF          = "pref_disable_passphrase";
  public  static final String THEME_PREF                       = "pref_theme";
  public  static final String LANGUAGE_PREF                    = "pref_language";
  public  static final String BACKGROUND_PREF                  = "pref_chat_background";

  private static final String DATABASE_ENCRYPTED_SECRET        = "pref_database_encrypted_secret_"; // followed by account-id
  private static final String DATABASE_UNENCRYPTED_SECRET      = "pref_database_unencrypted_secret_"; // followed by account-id

  public  static final String RINGTONE_PREF                    = "pref_key_ringtone";
  private static final String VIBRATE_PREF                     = "pref_key_vibrate";
  private static final String CHAT_VIBRATE                     = "pref_chat_vibrate_"; // followed by chat-id
  private static final String NOTIFICATION_PREF                = "pref_key_enable_notifications";
  public  static final String LED_COLOR_PREF                   = "pref_led_color";
  private static final String CHAT_RINGTONE                    = "pref_chat_ringtone_"; // followed by chat-id
  public  static final String SCREEN_SECURITY_PREF             = "pref_screen_security";
  private static final String ENTER_SENDS_PREF                 = "pref_enter_sends";
  private static final String PROMPTED_DOZE_MSG_ID_PREF        = "pref_prompted_doze_msg_id";
  public  static final String DOZE_ASKED_DIRECTLY              = "pref_doze_asked_directly";
  public  static final String ASKED_FOR_NOTIFICATION_PERMISSION= "pref_asked_for_notification_permission";
  private static final String IN_THREAD_NOTIFICATION_PREF      = "pref_key_inthread_notifications";
  public  static final String MESSAGE_BODY_TEXT_SIZE_PREF      = "pref_message_body_text_size";

  public  static final String NOTIFICATION_PRIVACY_PREF        = "pref_notification_privacy";
  public  static final String NOTIFICATION_PRIORITY_PREF       = "pref_notification_priority";

  public  static final String SYSTEM_EMOJI_PREF                = "pref_system_emoji";
  public  static final String BUILTIN_CAMERA_PREF              = "pref_builtin_camera";
  public  static final String SHOW_SYSTEM_CONTACTS             = "pref_show_system_contacts";
  public  static final String DIRECT_CAPTURE_CAMERA_ID         = "pref_direct_capture_camera_id";
  private static final String PROFILE_AVATAR_ID_PREF           = "pref_profile_avatar_id";
  public  static final String INCOGNITO_KEYBORAD_PREF          = "pref_incognito_keyboard";

  private static final String PREF_CONTACT_PHOTO_IDENTIFIERS = "pref_contact_photo_identifiers";

  private static final String MAP_CENTER_LATITUDE = "pref_map_center_latitude";
  private static final String MAP_CENTER_LONGITUDE = "pref_map_center_longitude";
  private static final String MAP_ZOOM = "pref_map_zoom";

  public  static final String  ALWAYS_LOAD_REMOTE_CONTENT = "pref_always_load_remote_content";
  public  static final boolean ALWAYS_LOAD_REMOTE_CONTENT_DEFAULT = false;

  public enum VibrateState {
    DEFAULT(0), ENABLED(1), DISABLED(2);
    private final int id;
    VibrateState(int id) { this.id = id; }
    public int getId() { return id; }
    public static VibrateState fromId(int id) { return values()[id]; }
  }

  public static void setDatabaseEncryptedSecret(@NonNull Context context, @NonNull String secret, int accountId) {
    setStringPreference(context, DATABASE_ENCRYPTED_SECRET + accountId, secret);
  }

  public static void setDatabaseUnencryptedSecret(@NonNull Context context, @Nullable String secret, int accountId) {
    setStringPreference(context, DATABASE_UNENCRYPTED_SECRET + accountId, secret);
  }

  public static @Nullable String getDatabaseUnencryptedSecret(@NonNull Context context, int accountId) {
    return getStringPreference(context, DATABASE_UNENCRYPTED_SECRET + accountId, null);
  }

  public static @Nullable String getDatabaseEncryptedSecret(@NonNull Context context, int accountId) {
    return getStringPreference(context, DATABASE_ENCRYPTED_SECRET + accountId, null);
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

  public static void setEnterSendsEnabled(Context context, boolean value) {
    setBooleanPreference(context, ENTER_SENDS_PREF, value);
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
    return getStringPreference(context, THEME_PREF, DynamicTheme.systemThemeAvailable() ? DynamicTheme.SYSTEM : DynamicTheme.LIGHT);
  }

  public static String getLanguage(Context context) {
    return getStringPreference(context, LANGUAGE_PREF, "zz");
  }

  public static void setLanguage(Context context, String language) {
    setStringPreference(context, LANGUAGE_PREF, language);
  }

  public static void setPromptedDozeMsgId(Context context, int msg_id) {
    setIntegerPrefrence(context, PROMPTED_DOZE_MSG_ID_PREF, msg_id);
  }

  public static int getPrompteDozeMsgId(Context context) {
    return getIntegerPreference(context, PROMPTED_DOZE_MSG_ID_PREF, 0);
  }

  public static boolean isNotificationsEnabled(Context context) {
    return getBooleanPreference(context, NOTIFICATION_PREF, true);
  }

  public static boolean isHardCompressionEnabled(Context context) {
    return DcHelper.getContext(context).getConfigInt(DcHelper.CONFIG_MEDIA_QUALITY) == DcContext.DC_MEDIA_QUALITY_WORSE;
  }

  public static boolean isLocationStreamingEnabled(Context context) {
    try {
      return getBooleanPreference(context, "pref_location_streaming_enabled", false);
    }
    catch(Exception e) {
      return false;
    }
  }

  public static boolean isDeveloperModeEnabled(Context context) {
    return getBooleanPreference(context, "pref_developer_mode_enabled", false);
  }

  public static boolean isNewBroadcastListAvailable(Context context) {
    return getBooleanPreference(context, "pref_new_broadcast_list", false);
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

  public static void setChatRingtone(Context context, int accountId, int chatId, Uri ringtone) {
    final String KEY = (accountId != 0 && chatId != 0)? CHAT_RINGTONE+accountId+"."+chatId : CHAT_RINGTONE;
    if(ringtone!=null) {
      setStringPreference(context, KEY, ringtone.toString());
    }
    else {
      removePreference(context, KEY);
    }
  }

  public static @Nullable Uri getChatRingtone(Context context, int accountId, int chatId) {
    final String KEY = (accountId != 0 && chatId != 0)? CHAT_RINGTONE+accountId+"."+chatId : CHAT_RINGTONE;
    String result = getStringPreference(context, KEY, null);
    return result==null? null : Uri.parse(result);
  }

  public static boolean reliableService(Context context) {
    try {
      return getBooleanPreference(context, "pref_reliable_service", false);
    }
    catch(Exception e) {
      return false;
    }
  }

  // vibrate

  public static boolean isNotificationVibrateEnabled(Context context) {
    return getBooleanPreference(context, VIBRATE_PREF, true);
  }

  public static void setChatVibrate(Context context, int accountId, int chatId, VibrateState vibrateState) {
    final String KEY = (accountId != 0 && chatId != 0)? CHAT_VIBRATE+accountId+"."+chatId : CHAT_VIBRATE;
    if(vibrateState!=VibrateState.DEFAULT) {
      setIntegerPrefrence(context, KEY, vibrateState.getId());
    }
    else {
      removePreference(context, KEY);
    }
  }

  public static VibrateState getChatVibrate(Context context, int accountId, int chatId) {
    final String KEY = (accountId != 0 && chatId != 0)? CHAT_VIBRATE+accountId+"."+chatId : CHAT_VIBRATE;
    return VibrateState.fromId(getIntegerPreference(context, KEY, VibrateState.DEFAULT.getId()));
  }

  // led

  public static String getNotificationLedColor(Context context) {
    return getStringPreference(context, LED_COLOR_PREF, "blue");
  }

  // map

  public static void setMapCenter(Context context, int accountId, int chatId, LatLng latLng) {
    setLongPreference(context, MAP_CENTER_LATITUDE+accountId+"."+chatId, Double.doubleToRawLongBits(latLng.getLatitude()));
    setLongPreference(context, MAP_CENTER_LONGITUDE+accountId+"."+chatId, Double.doubleToRawLongBits(latLng.getLongitude()));
  }

  public static void setMapZoom(Context context, int accountId, int chatId, double zoom) {
    setLongPreference(context, MAP_ZOOM+accountId+"."+chatId, Double.doubleToRawLongBits(zoom));
  }

  public static LatLng getMapCenter(Context context, int accountId, int chatId) {
    long latitude = getLongPreference(context, MAP_CENTER_LATITUDE+accountId+"."+chatId, Long.MAX_VALUE);
    long longitude = getLongPreference(context, MAP_CENTER_LONGITUDE+accountId+"."+chatId, Long.MAX_VALUE);
    if (latitude == Long.MAX_VALUE || longitude == Long.MAX_VALUE) {
      return null;
    }
    return new LatLng(Double.longBitsToDouble(latitude), Double.longBitsToDouble(longitude));
  }

  public static double getMapZoom(Context context, int accountId, int chatId) {
    long zoom = getLongPreference(context, MAP_ZOOM+accountId+"."+chatId, Double.doubleToLongBits(MINIMUM_ZOOM));
    return Double.longBitsToDouble(zoom);
  }

  // misc.

  public static String getBackgroundImagePath(Context context, int accountId) {
    return getStringPreference(context, BACKGROUND_PREF+accountId, "");
  }

  public static void setBackgroundImagePath(Context context, int accountId, String path) {
    setStringPreference(context, BACKGROUND_PREF+accountId, path);
  }

  public static boolean isSystemEmojiPreferred(Context context) {
    return getBooleanPreference(context, SYSTEM_EMOJI_PREF, false);
  }

  public static boolean isBuiltInCameraPreferred(Context context) {
    return getBooleanPreference(context, BUILTIN_CAMERA_PREF, false);
  }

  public static boolean showSystemContacts(Context context) {
    return getBooleanPreference(context, SHOW_SYSTEM_CONTACTS, false);
  }

  public static boolean getAlwaysLoadRemoteContent(Context context) {
    return getBooleanPreference(context, Prefs.ALWAYS_LOAD_REMOTE_CONTENT,
      Prefs.ALWAYS_LOAD_REMOTE_CONTENT_DEFAULT);
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

  public static long getLongPreference(Context context, String key, long defaultValue) {
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
