package org.thoughtcrime.securesms.connect;

import android.content.Context;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.ApplicationContext;

public class DcHelper {

    public static final String CONFIG_ADDRESS = "addr";
    public static final String CONFIG_MAIL_SERVER = "mail_server";
    public static final String CONFIG_MAIL_USER = "mail_user";
    public static final String CONFIG_MAIL_PASSWORD = "mail_pw";
    public static final String CONFIG_MAIL_PORT = "mail_port";
    public static final String CONFIG_SEND_SERVER = "send_server";
    public static final String CONFIG_SEND_USER = "send_user";
    public static final String CONFIG_SEND_PASSWORD = "send_pw";
    public static final String CONFIG_SEND_PORT = "send_port";
    public static final String CONFIG_SERVER_FLAGS = "server_flags";
    public static final String CONFIG_DISPLAY_NAME = "displayname";
    public static final String CONFIG_SELF_STATUS = "selfstatus";
    public static final String CONFIG_SELF_AVATAR = "selfavatar";
    public static final String CONFIG_E2EE_ENABLED = "e2ee_enabled";

    public static ApplicationDcContext getContext(Context context) {
        return ApplicationContext.getInstance(context).dcContext;
    }

    public static boolean isConfigured(Context context) {
        DcContext dcContext = getContext(context);
        return dcContext.isConfigured() == 1;
    }

    public static String get(Context context, String key) {
        DcContext dcContext = getContext(context);
        return dcContext.getConfig(key, "");
    }

    public static void set(Context context, String key, String value) {
        DcContext dcContext = getContext(context);
        dcContext.setConfig(key, value);
    }

}
