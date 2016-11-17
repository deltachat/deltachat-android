/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package org.telegram.messenger;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.Components.ForegroundDetector;

import java.io.File;

public class ApplicationLoader extends Application {
    private static PendingIntent pendingIntent;

    private static Drawable cachedWallpaper;
    private static int selectedColor;
    private static boolean isCustomTheme;
    private static final Object sync = new Object();

    private static int serviceMessageColor;
    private static int serviceSelectedMessageColor;

    public static volatile Context applicationContext;
    public static volatile Handler applicationHandler;
    private static volatile boolean applicationInited = false;

    public static volatile boolean isScreenOn = false;
    public static volatile boolean mainInterfacePaused = true;

    public static boolean isCustomTheme() {
        return isCustomTheme;
    }

    public static int getSelectedColor() {
        return selectedColor;
    }

    public static void reloadWallpaper() {
        cachedWallpaper = null;
        serviceMessageColor = 0;
        ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE).edit().remove("serviceMessageColor").commit();
        loadWallpaper();
    }

    private static void calcBackgroundColor() {
        int result[] = AndroidUtilities.calcDrawableColor(cachedWallpaper);
        serviceMessageColor = result[0];
        serviceSelectedMessageColor = result[1];
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        preferences.edit().putInt("serviceMessageColor", serviceMessageColor).putInt("serviceSelectedMessageColor", serviceSelectedMessageColor).commit();
    }

    public static int getServiceMessageColor() {
        return serviceMessageColor;
    }

    public static int getServiceSelectedMessageColor() {
        return serviceSelectedMessageColor;
    }

    public static void loadWallpaper() {
        if (cachedWallpaper != null) {
            return;
        }
        Utilities.searchQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                synchronized (sync) {
                    int selectedColor = 0;
                    try {
                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                        int selectedBackground = preferences.getInt("selectedBackground", 1000001);
                        selectedColor = preferences.getInt("selectedColor", 0);
                        serviceMessageColor = preferences.getInt("serviceMessageColor", 0);
                        serviceSelectedMessageColor = preferences.getInt("serviceSelectedMessageColor", 0);
                        if (selectedColor == 0) {
                            if (selectedBackground == 1000001) {
                                cachedWallpaper = applicationContext.getResources().getDrawable(R.drawable.background_hd);
                                isCustomTheme = false;
                            } else {
                                File toFile = new File(getFilesDirFixed(), "wallpaper.jpg");
                                if (toFile.exists()) {
                                    cachedWallpaper = Drawable.createFromPath(toFile.getAbsolutePath());
                                    isCustomTheme = true;
                                } else {
                                    cachedWallpaper = applicationContext.getResources().getDrawable(R.drawable.background_hd);
                                    isCustomTheme = false;
                                }
                            }
                        }
                    } catch (Throwable throwable) {
                        //ignore
                    }
                    if (cachedWallpaper == null) {
                        if (selectedColor == 0) {
                            selectedColor = -2693905;
                        }
                        cachedWallpaper = new ColorDrawable(selectedColor);
                    }
                    if (serviceMessageColor == 0) {
                        calcBackgroundColor();
                    }
                }
            }
        });
    }

    public static Drawable getCachedWallpaper() {
        synchronized (sync) {
            return cachedWallpaper;
        }
    }

    public static File getFilesDirFixed() {
        for (int a = 0; a < 10; a++) { // sometimes getFilesDir() returns NULL, see https://code.google.com/p/android/issues/detail?id=8886
            File path = ApplicationLoader.applicationContext.getFilesDir();
            if (path != null) {
                return path;
            }
        }
        try {
            ApplicationInfo info = applicationContext.getApplicationInfo();
            File path = new File(info.dataDir, "files");
            path.mkdirs();
            return path;
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
        return new File("/data/data/com.b44t.messenger/files"); // EDIT BY MR
    }

    public static void postInitApplication() {
        if (applicationInited) {
            return;
        }

        applicationInited = true;
        //convertConfig();

        try {
            LocaleController.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            final BroadcastReceiver mReceiver = new ScreenReceiver();
            applicationContext.registerReceiver(mReceiver, filter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            PowerManager pm = (PowerManager)ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
            isScreenOn = pm.isScreenOn();
            FileLog.e("tmessages", "screen state = " + isScreenOn);
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }

        UserConfig.loadConfig();
        String deviceModel;
        String langCode;
        String appVersion;
        String systemVersion;
        String configPath = getFilesDirFixed().toString();

        try {
            langCode = LocaleController.getLocaleStringIso639();
            deviceModel = Build.MANUFACTURER + Build.MODEL;
            PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            appVersion = pInfo.versionName + " (" + pInfo.versionCode + ")";
            systemVersion = "SDK " + Build.VERSION.SDK_INT;
        } catch (Exception e) {
            langCode = "en";
            deviceModel = "Android unknown";
            appVersion = "App version unknown";
            systemVersion = "SDK " + Build.VERSION.SDK_INT;
        }
        if (langCode.trim().length() == 0) {
            langCode = "en";
        }
        if (deviceModel.trim().length() == 0) {
            deviceModel = "Android unknown";
        }
        if (appVersion.trim().length() == 0) {
            appVersion = "App version unknown";
        }
        if (systemVersion.trim().length() == 0) {
            systemVersion = "SDK Unknown";
        }

        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
        boolean enablePushConnection = preferences.getBoolean("pushConnection", true);

        if( preferences.getInt("notify2_"+MrMailbox.MR_CHAT_ID_STRANGERS, 666)==666 ) {
            // make sure, the notifications for the "strangers" dialog are muted by default
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("notify2_"+MrMailbox.MR_CHAT_ID_STRANGERS, 2);
            editor.commit();
        }

        MessagesController.getInstance();
        ConnectionsManager.getInstance().init(deviceModel, systemVersion, appVersion, langCode, configPath, FileLog.getNetworkLogPath(), UserConfig.getClientUserId(), enablePushConnection);
        if (UserConfig.getCurrentUser() != null) {
            MessagesController.getInstance().putUser(UserConfig.getCurrentUser(), true);
            SendMessagesHelper.getInstance().checkUnsentMessages();
        }

        ApplicationLoader app = (ApplicationLoader)ApplicationLoader.applicationContext;
        FileLog.e("tmessages", "app initied");

        ContactsController.getInstance().checkAppAccount();
        MediaController.getInstance();

        // EDIT BY MR - open my sqlite file (you can inspect the file eg. with "Tools / Android Device Monitor / File Explorer")
        File dbfile = new File(getFilesDirFixed(), "messenger.db");
        MrMailbox.MrMailboxOpen(MrMailbox.hMailbox, dbfile.getAbsolutePath(), "");
        MrMailbox.MrMailboxConnect(MrMailbox.hMailbox);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        applicationContext = getApplicationContext();
        NativeLoader.initNativeLibs(ApplicationLoader.applicationContext);
        //ConnectionsManager.native_setJava(Build.VERSION.SDK_INT == 14 || Build.VERSION.SDK_INT == 15);
        new ForegroundDetector(this);

        // EDIT BY MR - create a MrMailbox object; as android stops the App by just killing it, we do never call MrMailboxUnref()
        // however, we may want to to have a look at onPause() eg. of activities (eg. for flushing data, if needed)
        MrMailbox.MrCallback(0, 0, 0); // do not remove this call; this makes sure, the function is not removed from build or warnings are printed!
        MrMailbox.hMailbox = MrMailbox.MrMailboxNew();
        MrMailbox.MrStockAddStr(1, LocaleController.getString("NoMessages", R.string.NoMessages));
        MrMailbox.MrStockAddStr(2, LocaleController.getString("FromSelf", R.string.FromSelf));
        MrMailbox.MrStockAddStr(3, LocaleController.getString("Draft", R.string.Draft));
        MrMailbox.MrStockAddStr(4, LocaleController.getString("MemberSg", R.string.MemberSg));
        MrMailbox.MrStockAddStr(5, LocaleController.getString("MemberPl", R.string.MemberPl));
        MrMailbox.MrStockAddStr(6, LocaleController.getString("ContactSg", R.string.ContactSg));
        MrMailbox.MrStockAddStr(7, LocaleController.getString("ContactPl", R.string.ContactPl));
        MrMailbox.MrStockAddStr(8, LocaleController.getString("Strangers", R.string.Strangers));
        MrMailbox.MrStockAddStr(9, LocaleController.getString("AttachPhoto", R.string.AttachPhoto));
        MrMailbox.MrStockAddStr(10, LocaleController.getString("AttachVideo", R.string.AttachVideo));
        MrMailbox.MrStockAddStr(11, LocaleController.getString("AttachAudio", R.string.AttachAudio));
        MrMailbox.MrStockAddStr(12, LocaleController.getString("AttachDocument", R.string.AttachDocument));
        MrMailbox.MrStockAddStr(13, LocaleController.getString("DefaultStatusText", R.string.DefaultStatusText));
        MrMailbox.MrStockAddStr(14, LocaleController.getString("SubjectPrefix", R.string.SubjectPrefix));

        applicationHandler = new Handler(applicationContext.getMainLooper());

        startPushService();
    }

    /*public static void sendRegIdToBackend(final String token) {
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                UserConfig.pushString = token;
                UserConfig.registeredForPush = false;
                UserConfig.saveConfig(false);
                if (UserConfig.getClientUserId() != 0) {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesController.getInstance().registerForPush(token);
                        }
                    });
                }
            }
        });
    }*/

    public static void startPushService() {
        SharedPreferences preferences = applicationContext.getSharedPreferences("Notifications", MODE_PRIVATE);

        if (preferences.getBoolean("pushService", true)) {
            AlarmManager am = (AlarmManager) applicationContext.getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(applicationContext, ApplicationLoader.class);
            pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, i, 0);

            am.cancel(pendingIntent);
            am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 60000, pendingIntent);

            applicationContext.startService(new Intent(applicationContext, NotificationsService.class));
        } else {
            stopPushService();
        }
    }

    public static void stopPushService() {
        applicationContext.stopService(new Intent(applicationContext, NotificationsService.class));

        PendingIntent pintent = PendingIntent.getService(applicationContext, 0, new Intent(applicationContext, NotificationsService.class), 0);
        AlarmManager alarm = (AlarmManager)applicationContext.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pintent);
        alarm.cancel(pendingIntent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            LocaleController.getInstance().onDeviceConfigurationChange(newConfig);
            AndroidUtilities.checkDisplaySize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
