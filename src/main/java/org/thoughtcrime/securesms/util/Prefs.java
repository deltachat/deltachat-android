package org.thoughtcrime.securesms.util;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.preferences.widgets.NotificationPrivacyPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Prefs {

  private static final String TAG = Prefs.class.getSimpleName();

  public  static final String RELIABLE_SERVICE_PREF         = "pref_reliable_service";
  public  static final String DISABLE_PASSPHRASE_PREF          = "pref_disable_passphrase";
  public  static final String THEME_PREF                       = "pref_theme";
  public  static final String BACKGROUND_PREF                  = "pref_chat_background";

  private static final String DATABASE_ENCRYPTED_SECRET        = "pref_database_encrypted_secret_"; // followed by account-id
  private static final String DATABASE_UNENCRYPTED_SECRET      = "pref_database_unencrypted_secret_"; // followed by account-id

  public  static final String RINGTONE_PREF                    = "pref_key_ringtone";
  private static final String VIBRATE_PREF                     = "pref_key_vibrate";
  private static final String CHAT_VIBRATE                     = "pref_chat_vibrate_"; // followed by chat-id
  public  static final String LED_COLOR_PREF                   = "pref_led_color";
  private static final String CHAT_RINGTONE                    = "pref_chat_ringtone_"; // followed by chat-id
  public  static final String SCREEN_SECURITY_PREF             = "pref_screen_security";
  private static final String ENTER_SENDS_PREF                 = "pref_enter_sends";
  private static final String PROMPTED_DOZE_MSG_ID_PREF        = "pref_prompted_doze_msg_id";
  private static final String STATS_DEVICE_MSG_ID_PREF         = "pref_stats_device_msg_id";
  public  static final String DOZE_ASKED_DIRECTLY              = "pref_doze_asked_directly";
  public  static final String ASKED_FOR_NOTIFICATION_PERMISSION= "pref_asked_for_notification_permission";
  private static final String IN_THREAD_NOTIFICATION_PREF      = "pref_key_inthread_notifications";

  public  static final String NOTIFICATION_PRIVACY_PREF        = "pref_notification_privacy";
  public  static final String NOTIFICATION_PRIORITY_PREF       = "pref_notification_priority";

  private static final String PROFILE_AVATAR_ID_PREF           = "pref_profile_avatar_id";
  public  static final String INCOGNITO_KEYBORAD_PREF          = "pref_incognito_keyboard";

  private static final String PREF_CONTACT_PHOTO_IDENTIFIERS = "pref_contact_photo_identifiers";

  public  static final String  ALWAYS_LOAD_REMOTE_CONTENT = "pref_always_load_remote_content";
  public  static final boolean ALWAYS_LOAD_REMOTE_CONTENT_DEFAULT = false;

  public  static final String LAST_DEVICE_MSG_LABEL            = "pref_last_device_msg_id";
  public  static final String WEBXDC_STORE_URL_PREF            = "pref_webxdc_store_url";
  public  static final String DEFAULT_WEBXDC_STORE_URL         = "https://webxdc.org/apps/";

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
    setIntegerPreference(context, PROFILE_AVATAR_ID_PREF, id);
  }

  public static int getProfileAvatarId(Context context) {
    return getIntegerPreference(context, PROFILE_AVATAR_ID_PREF, 0);
  }

  public static int getNotificationPriority(Context context) {
    return Integer.valueOf(getStringPreference(context, NOTIFICATION_PRIORITY_PREF, String.valueOf(NotificationCompat.PRIORITY_HIGH)));
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

  public static String getWebxdcStoreUrl(Context context) {
    return getStringPreference(context, WEBXDC_STORE_URL_PREF, DEFAULT_WEBXDC_STORE_URL);
  }

  public static void setWebxdcStoreUrl(Context context, String url) {
    if (url == null || url.trim().isEmpty() || DEFAULT_WEBXDC_STORE_URL.equals(url)) url = null;
    setStringPreference(context, WEBXDC_STORE_URL_PREF, url);
  }

  public static void setPromptedDozeMsgId(Context context, int msg_id) {
    setIntegerPreference(context, PROMPTED_DOZE_MSG_ID_PREF, msg_id);
  }

  public static int getPrompteDozeMsgId(Context context) {
    return getIntegerPreference(context, PROMPTED_DOZE_MSG_ID_PREF, 0);
  }

  public static void setStatsDeviceMsgId(Context context, int msg_id) {
    setIntegerPreference(context, STATS_DEVICE_MSG_ID_PREF, msg_id);
  }

  public static int getStatsDeviceMsgId(Context context) {
    return getIntegerPreference(context, STATS_DEVICE_MSG_ID_PREF, 0);
  }

  public static boolean isPushEnabled(Context context) {
      return BuildConfig.USE_PLAY_SERVICES;
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

  public static boolean isNewBroadcastAvailable(Context context) {
    return getBooleanPreference(context, "pref_new_broadcast_list", false);
  }


  public static boolean isCallsEnabled(Context context) {
    return getBooleanPreference(context, "pref_calls_enabled", false);
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

  public static void setReliableService(Context context, boolean value) {
    setBooleanPreference(context, RELIABLE_SERVICE_PREF, value);
  }

  public static boolean reliableService(Context context) {
    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    if (prefs.contains(RELIABLE_SERVICE_PREF)) {
      try {
        return prefs.getBoolean(RELIABLE_SERVICE_PREF, true);
      } catch(Exception e) {}
    }

    // if the key was unset, then calculate default value
    return !isPushEnabled(context) || !DcHelper.getAccounts(context).isAllChatmail();
  }

  // vibrate

  public static boolean isNotificationVibrateEnabled(Context context) {
    return getBooleanPreference(context, VIBRATE_PREF, true);
  }

  public static void setChatVibrate(Context context, int accountId, int chatId, VibrateState vibrateState) {
    final String KEY = (accountId != 0 && chatId != 0)? CHAT_VIBRATE+accountId+"."+chatId : CHAT_VIBRATE;
    if(vibrateState!=VibrateState.DEFAULT) {
      setIntegerPreference(context, KEY, vibrateState.getId());
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

  // misc.

  public static String getBackgroundImagePath(Context context, int accountId) {
    return getStringPreference(context, BACKGROUND_PREF+accountId, "");
  }

  public static void setBackgroundImagePath(Context context, int accountId, String path) {
    setStringPreference(context, BACKGROUND_PREF+accountId, path);
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

  private static void setIntegerPreference(Context context, String key, int value) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(key, value).apply();
  }

  public static long getLongPreference(Context context, String key, long defaultValue) {
    return PreferenceManager.getDefaultSharedPreferences(context).getLong(key, defaultValue);
  }

  private static void setLongPreference(Context context, String key, long value) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(key, value).apply();
  }

  public static void removePreference(Context context, String key) {
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
