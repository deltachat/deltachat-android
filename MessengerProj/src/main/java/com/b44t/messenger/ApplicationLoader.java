/*******************************************************************************
 *
 *                              Delta Chat Android
 *                        (C) 2013-2016 Nikolai Kudashov
 *                           (C) 2017 Björn Petersen
 *                    Contact: r10s@b44t.com, http://b44t.com
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see http://www.gnu.org/licenses/ .
 *
 ******************************************************************************/


package com.b44t.messenger;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;

import com.b44t.messenger.Components.ForegroundDetector;

import java.io.File;

public class ApplicationLoader extends Application {

    private static Drawable cachedWallpaper;
    private static final Object sync = new Object();

    public static volatile Context applicationContext;
    public static volatile Handler applicationHandler;

    public static volatile boolean isScreenOn = false;
    public static volatile boolean mainInterfacePaused = true;

    public static PowerManager.WakeLock backendWakeLock = null;
    public static PowerManager.WakeLock wakeupWakeLock = null;
    private static PowerManager.WakeLock stayAwakeWakeLock = null;


    public static int fontSize;

    public static void reloadWallpaper() {
        cachedWallpaper = null;
        loadWallpaper();
    }

    public static int getServiceMessageColor() {
        return 0x44000000; // this color is used as a background for date headlines and empty chat hints
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
                        if (selectedColor == 0) {
                            if (selectedBackground == 1000001) {
                                cachedWallpaper = applicationContext.getResources().getDrawable(R.drawable.background_hd);
                            } else {
                                File toFile = new File(getFilesDirFixed(), "wallpaper.jpg");
                                if (toFile.exists()) {
                                    cachedWallpaper = Drawable.createFromPath(toFile.getAbsolutePath());
                                } else {
                                    cachedWallpaper = applicationContext.getResources().getDrawable(R.drawable.background_hd);
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

        }
        return new File("/data/data/com.b44t.messenger/files");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = getApplicationContext(); // should be set very first as it may be needed eg. for logging

        MrMailbox.log_i("DeltaChat", "*************** ApplicationLoader.onCreate() ***************");
        System.loadLibrary("messenger.1");
        new ForegroundDetector(this);
        applicationHandler = new Handler(applicationContext.getMainLooper());

        // create wake locks
        try {
            PowerManager pm = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);

            backendWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "backendWakeLock" /*any name*/);
            // bakendWakeLock _is_ reference counted by the backend (every acquire() has a release())

            wakeupWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wakeupWakeLock" /*any name*/);
            wakeupWakeLock.setReferenceCounted(false);

            stayAwakeWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "stayAwakeWakeLock" /*any name*/);
            stayAwakeWakeLock.setReferenceCounted(false);

        } catch (Exception e) {
            Log.e("DeltaChat", "Cannot acquire wakeLock");
        }

        // create a MrMailbox object; as android stops the App by just killing it, we do never call MrMailboxUnref()
        // however, we may want to to have a look at onPause() eg. of activities (eg. for flushing data, if needed)
        MrMailbox.MrCallback(0, 0, 0); // do not remove this call; this makes sure, the function is not removed from build or warnings are printed!
        MrMailbox.init();

        // start keep-alive service that restarts the app as soon it is terminated
        // (this is done by just marking the service as START_STICKY which recreates the service as
        // it goes away which also inititialized the app indirectly by calling this function)
        applicationContext.startService(new Intent(applicationContext, KeepAliveService.class));

        // init locale
        try {
            LocaleController.getInstance(); // this call does _not_ do nothing; in fact, it creates eg. the formatters while creating the static object.
        } catch (Exception e) {
            e.printStackTrace();
        }

        // track screen on/off
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
            //Log.i("DeltaChat", "screen state = " + isScreenOn);
        } catch (Exception e) {

        }

        UserConfig.loadConfig();

        // create a timer that wakes up the CPU from time to time
        TimerReceiver.scheduleNextAlarm();

        // make sure, the notifications for the "deaddrop" dialog are muted by default
        SharedPreferences notificationPreferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
        if( notificationPreferences.getInt("deaddrop_initialized", 0)!=1 ) {
            SharedPreferences.Editor editor = notificationPreferences.edit();
            editor.putInt("notify2_"+MrChat.MR_CHAT_ID_DEADDROP, 2);
            editor.putInt("deaddrop_initialized", 1);
            editor.apply();
        }

        // open() sqlite file (you can inspect the file eg. with "Tools / Android Device Monitor / File Explorer")
        // open() should be called before MessagesController.getInstance() as this also initilizes directories based upon getBlobdir().
        File dbfile = new File(getFilesDirFixed(), "messenger.db");
        MrMailbox.open(dbfile.getAbsolutePath());
        if( MrMailbox.isConfigured()!=0 ) {
            MrMailbox.connect();
        }

        // create other default objects
        SharedPreferences mainPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        fontSize = mainPreferences.getInt("msg_font_size", SettingsAdvFragment.defMsgFontSize());

        ImageLoader.getInstance();
        MediaController.getInstance();
        NotificationsController.getInstance(); // force instace creation which also does some init stuff
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // this function is needed to react to changed system configuration, eg. locale or display size
        super.onConfigurationChanged(newConfig);
        try {
            // change some locale stuff that is not updated automatically, see comments in onDeviceConfigurationChange() and rebuildUiParts()
            LocaleController.getInstance().onDeviceConfigurationChange(newConfig);

            // re-calculate some things regarding the display (the size may have changed)
            AndroidUtilities.checkDisplaySize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int lastClassGuid = 1;
    public static int generateClassGuid() {
        return lastClassGuid++;
    }

    public static boolean isNetworkOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) ApplicationLoader.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnected()) {
                return true;
            }

        } catch (Exception e) {
        }
        return false;
    }

    public static void stayAwakeForAMoment()
    {
        stayAwakeWakeLock.acquire(1*60*1000); // 1 Minute to wait for "after chat" messages, after that, we sleep most time, see wakeupWakeLock
    }
}
