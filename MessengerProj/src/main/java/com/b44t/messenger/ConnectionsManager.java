package com.b44t.messenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;

public class ConnectionsManager {

    public final static int RequestFlagFailOnServerErrors = 2;

    public final static int ConnectionStateConnecting = 1;
    public final static int ConnectionStateWaitingForNetwork = 2;
    public final static int ConnectionStateConnected = 3;
    public final static int ConnectionStateUpdating = 4;

    private long lastPauseTime = System.currentTimeMillis();
    private boolean appPaused = true;
    private int lastClassGuid = 1;
    private PowerManager.WakeLock wakeLock = null;

    private static volatile ConnectionsManager Instance = null;

    public static ConnectionsManager getInstance() {
        ConnectionsManager localInstance = Instance;
        if (localInstance == null) {
            synchronized (ConnectionsManager.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new ConnectionsManager();
                }
            }
        }
        return localInstance;
    }

    public ConnectionsManager() {
        try {
            PowerManager pm = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "lock");
            wakeLock.setReferenceCounted(false);
        } catch (Exception e) {
            FileLog.e("messenger", e);
        }
    }

    public int getCurrentTime() {
        return MrMailbox.getCurrentTime();
    }

    public void cancelRequest(int token, boolean notifyServer) {
        //native_cancelRequest(token, notifyServer);
    }

    public void cleanup() {
        //native_cleanUp();
    }

    public int getConnectionState() {
        return ConnectionStateWaitingForNetwork; // EDIT BY MR - we're always disconnected from the view of the caller
        /* EDIT BY MR
        if (connectionState == ConnectionStateConnected && isUpdating) {
            return ConnectionStateUpdating;
        }
        return connectionState;
        */
    }

    public void setPushConnectionEnabled(boolean value) {
        //native_setPushConnectionEnabled(value);
    }

    public void init(String deviceModel, String systemVersion, String appVersion, String langCode, String configPath, String logPath, int userId, boolean enablePushConnection) {
        //native_init(version, layer, apiId, deviceModel, systemVersion, appVersion, langCode, configPath, logPath, userId, enablePushConnection);
        //checkConnection();
        BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //checkConnection();
            }
        };
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        ApplicationLoader.applicationContext.registerReceiver(networkStateReceiver, filter);
    }

    public void resumeNetworkMaybe() {
        //native_resumeNetwork(true);
    }

    public void setAppPaused(final boolean value, final boolean byScreenState) {
        if (!byScreenState) {
            appPaused = value;
            FileLog.d("messenger", "app paused = " + value);
        }
        if (value) {
            if (lastPauseTime == 0) {
                lastPauseTime = System.currentTimeMillis();
            }
            //native_pauseNetwork();
        } else {
            if (appPaused) {
                return;
            }
            FileLog.e("messenger", "reset app pause time");
            /*if (lastPauseTime != 0 && System.currentTimeMillis() - lastPauseTime > 5000) {
                ContactsController.getInstance().checkContacts();
            }*/
            lastPauseTime = 0;
            //native_resumeNetwork(false);
        }
    }

    public int generateClassGuid() {
        return lastClassGuid++;
    }

    /* -- leave this for future use
    public static boolean isRoaming() {
        try {
            ConnectivityManager cm = (ConnectivityManager) ApplicationLoader.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null) {
                return netInfo.isRoaming();
            }
        } catch (Exception e) {
            FileLog.e("messenger", e);
        }
        return false;
    } */

    /* -- leave this for future use
    public static boolean isConnectedToWiFi() {
        try {
            ConnectivityManager cm = (ConnectivityManager) ApplicationLoader.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED) {
                return true;
            }
        } catch (Exception e) {
            FileLog.e("messenger", e);
        }
        return false;
    } */

    public static boolean isNetworkOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) ApplicationLoader.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && (netInfo.isConnectedOrConnecting() || netInfo.isAvailable())) {
                return true;
            }

            netInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                return true;
            } else {
                netInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                    return true;
                }
            }
        } catch (Exception e) {
            FileLog.e("messenger", e);
            return true;
        }
        return false;
    }
}
