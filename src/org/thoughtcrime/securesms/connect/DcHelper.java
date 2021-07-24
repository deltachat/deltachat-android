package org.thoughtcrime.securesms.connect;

import android.content.Context;

import androidx.annotation.NonNull;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.R;

public class DcHelper {

    public static final String CONFIG_ADDRESS = "addr";
    public static final String CONFIG_MAIL_SERVER = "mail_server";
    public static final String CONFIG_MAIL_USER = "mail_user";
    public static final String CONFIG_MAIL_PASSWORD = "mail_pw";
    public static final String CONFIG_MAIL_PORT = "mail_port";
    public static final String CONFIG_MAIL_SECURITY = "mail_security";
    public static final String CONFIG_SEND_SERVER = "send_server";
    public static final String CONFIG_SEND_USER = "send_user";
    public static final String CONFIG_SEND_PASSWORD = "send_pw";
    public static final String CONFIG_SEND_PORT = "send_port";
    public static final String CONFIG_SEND_SECURITY = "send_security";
    public static final String CONFIG_SERVER_FLAGS = "server_flags";
    public static final String CONFIG_DISPLAY_NAME = "displayname";
    public static final String CONFIG_SELF_STATUS = "selfstatus";
    public static final String CONFIG_SELF_AVATAR = "selfavatar";
    public static final String CONFIG_E2EE_ENABLED = "e2ee_enabled";
    public static final String CONFIG_QR_OVERLAY_LOGO = "qr_overlay_logo";
    public static final String CONFIG_INBOX_WATCH = "inbox_watch";
    public static final String CONFIG_SENTBOX_WATCH = "sentbox_watch";
    public static final String CONFIG_MVBOX_WATCH = "mvbox_watch";
    public static final String CONFIG_MVBOX_MOVE = "mvbox_move";
    public static final String CONFIG_BCC_SELF = "bcc_self";
    public static final String CONFIG_SHOW_EMAILS = "show_emails";
    public static final String CONFIG_MEDIA_QUALITY = "media_quality";
    public static final String CONFIG_WEBRTC_INSTANCE = "webrtc_instance";

    public static ApplicationDcContext getContext(@NonNull Context context) {
        return ApplicationContext.getInstance(context).dcContext;
    }

    public static boolean isConfigured(Context context) {
        DcContext dcContext = getContext(context);
        return dcContext.isConfigured() == 1;
    }

    public static int getInt(Context context, String key) {
        DcContext dcContext = getContext(context);
        return dcContext.getConfigInt(key);
    }

    public static String get(Context context, String key) {
        DcContext dcContext = getContext(context);
        return dcContext.getConfig(key);
    }

    @Deprecated public static int getInt(Context context, String key, int defaultValue) {
        return getInt(context, key);
    }

    @Deprecated public static String get(Context context, String key, String defaultValue) {
        return get(context, key);
    }

    public static void set(Context context, String key, String value) {
        DcContext dcContext = getContext(context);
        dcContext.setConfig(key, value);
    }

  /**
   * Gets a string you can show to the user with basic information about connectivity.
   * @param context
   * @param connectedString Usually "Connected", but when using this as the title in
   *                        ConversationListActivity, we want to write "Delta Chat" there instead.
   * @return
   */
  public static String getConnectivitySummary(Context context, int connectedString) {
      int connectivity = getContext(context).getConnectivity();
      if (connectivity >= DcContext.DC_CONNECTIVITY_CONNECTED) {
          return context.getString(connectedString);
      } else if (connectivity >= DcContext.DC_CONNECTIVITY_WORKING) {
          return context.getString(R.string.connectivity_updating);
      } else if (connectivity >= DcContext.DC_CONNECTIVITY_CONNECTING) {
          return context.getString(R.string.connectivity_connecting);
      } else {
          return context.getString(R.string.connectivity_not_connected);
      }
  }
}
