package org.thoughtcrime.securesms.connect;

import android.content.Context;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.ApplicationContext;

public class DcHelper {

    private static final String CONFIG_ADDRESS = "addr";
    private static final String CONFIG_MAIL_SERVER = "mail_server";
    private static final String CONFIG_MAIL_USER = "mail_user";
    private static final String CONFIG_MAIL_PASSWORD = "mail_pw";
    private static final String CONFIG_MAIL_PORT = "mail_port";
    private static final String CONFIG_SEND_SERVER = "send_server";
    private static final String CONFIG_SEND_USER = "send_user";
    private static final String CONFIG_SEND_PASSWORD = "send_pw";
    private static final String CONFIG_SEND_PORT = "send_port";
    private static final String CONFIG_SERVER_FLAGS = "server_flags";
    private static final String CONFIG_DISPLAY_NAME = "displayname";
    private static final String CONFIG_SELF_STATUS = "selfstatus";
    private static final String CONFIG_E2EE_ENABLED = "e2ee_enabled";

    public static ApplicationDcContext getContext(Context context) {
        return ApplicationContext.getInstance(context).dcContext;
    }

    public static boolean isConfigured(Context context) {
        DcContext dcContext = getContext(context);
        return dcContext.isConfigured() == 1;
    }

    public static String getAccountAddress(Context context) {
        DcContext dcContext = getContext(context);
        return dcContext.getConfig(CONFIG_ADDRESS, "");
    }

    public static String getAccountName(Context context) {
        DcContext dcContext = getContext(context);
        return dcContext.getConfig(CONFIG_DISPLAY_NAME, "");
    }

    public static String getAccountStatus(Context context) {
        DcContext dcContext = getContext(context);
        return dcContext.getConfig(CONFIG_SELF_STATUS, "");
    }

}
