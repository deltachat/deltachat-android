package org.thoughtcrime.securesms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationManagerCompat;
import androidx.multidex.MultiDexApplication;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.b44t.messenger.DcAccounts;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcEventEmitter;
import com.b44t.messenger.FFITransport;

import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.connect.FetchWorker;
import org.thoughtcrime.securesms.connect.ForegroundDetector;
import org.thoughtcrime.securesms.connect.KeepAliveService;
import org.thoughtcrime.securesms.connect.NetworkStateReceiver;
import org.thoughtcrime.securesms.crypto.DatabaseSecret;
import org.thoughtcrime.securesms.crypto.DatabaseSecretProvider;
import org.thoughtcrime.securesms.geolocation.DcLocationManager;
import org.thoughtcrime.securesms.jobmanager.JobManager;
import org.thoughtcrime.securesms.notifications.FcmReceiveService;
import org.thoughtcrime.securesms.notifications.InChatSounds;
import org.thoughtcrime.securesms.notifications.NotificationCenter;
import org.thoughtcrime.securesms.util.AndroidSignalProtocolLogger;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.SignalProtocolLoggerProvider;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.webxdc.WebxdcGarbageCollectionWorker;

import java.io.File;
import java.util.concurrent.TimeUnit;

import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;

public class ApplicationContext extends MultiDexApplication {
  private static final String TAG = ApplicationContext.class.getSimpleName();

  public static DcAccounts      dcAccounts;
  public Rpc                    rpc;
  public DcContext              dcContext;
  public DcLocationManager      dcLocationManager;
  public DcEventCenter          eventCenter;
  public NotificationCenter     notificationCenter;
  private JobManager            jobManager;

  private int                   debugOnAvailableCount;
  private int                   debugOnBlockedStatusChangedCount;
  private int                   debugOnCapabilitiesChangedCount;
  private int                   debugOnLinkPropertiesChangedCount;

  public static ApplicationContext getInstance(@NonNull Context context) {
    return (ApplicationContext)context.getApplicationContext();
  }

  @Override
  public void onCreate() {
    super.onCreate();

    // if (LeakCanary.isInAnalyzerProcess(this)) {
    //   // This process is dedicated to LeakCanary for heap analysis.
    //   // You should not init your app in this process.
    //   return;
    // }
    // LeakCanary.install(this);

    Log.i("DeltaChat", "++++++++++++++++++ ApplicationContext.onCreate() ++++++++++++++++++");

    System.loadLibrary("native-utils");

    dcAccounts = new DcAccounts(new File(getFilesDir(), "accounts").getAbsolutePath());
    Log.i(TAG, "DcAccounts created");
    rpc = new Rpc(new FFITransport(dcAccounts.getJsonrpcInstance()));
    Log.i(TAG, "Rpc created");
    AccountManager.getInstance().migrateToDcAccounts(this);

    // October-2025 migration: delete deprecated "permanent channel" id
    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
    notificationManager.deleteNotificationChannel("dc_foreground_notification_ch");
    // end October-2025 migration

    int[] allAccounts = dcAccounts.getAll();
    Log.i(TAG, "Number of profiles: " + allAccounts.length);
    for (int accountId : allAccounts) {
      DcContext ac = dcAccounts.getAccount(accountId);
      if (!ac.isOpen()) {
        try {
          DatabaseSecret secret = DatabaseSecretProvider.getOrCreateDatabaseSecret(this, accountId);
          boolean res = ac.open(secret.asString());
          if (res) Log.i(TAG, "Successfully opened account " + accountId + ", path: " + ac.getBlobdir());
          else Log.e(TAG, "Error opening account " + accountId + ", path: " + ac.getBlobdir());
        } catch (Exception e) {
          Log.e(TAG, "Failed to open account " + accountId + ", path: " + ac.getBlobdir() + ": " + e);
          e.printStackTrace();
        }
      }

      // 2025.11.12: this is needed until core starts ignoring "delete_server_after" for chatmail
      if (ac.isChatmail()) {
        ac.setConfig("delete_server_after", null); // reset
      }
    }
    if (allAccounts.length == 0) {
      try {
        rpc.addAccount();
      } catch (RpcException e) {
        e.printStackTrace();
      }
    }
    dcContext = dcAccounts.getSelectedAccount();
    notificationCenter = new NotificationCenter(this);
    eventCenter = new DcEventCenter(this);
    new Thread(() -> {
      Log.i(TAG, "Starting event loop");
      DcEventEmitter emitter = dcAccounts.getEventEmitter();
      Log.i(TAG, "DcEventEmitter obtained");
      while (true) {
        DcEvent event = emitter.getNextEvent();
        if (event==null) {
          break;
        }
        eventCenter.handleEvent(event);
      }
      Log.i("DeltaChat", "shutting down event handler");
    }, "eventThread").start();

    // set translations before starting I/O to avoid sending untranslated MDNs (issue #2288)
    DcHelper.setStockTranslations(this);

    dcAccounts.startIo();

    new ForegroundDetector(ApplicationContext.getInstance(this));

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      ConnectivityManager connectivityManager =
        (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
      connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull android.net.Network network) {
          Log.i("DeltaChat", "++++++++++++++++++ NetworkCallback.onAvailable() #" + debugOnAvailableCount++);
          dcAccounts.maybeNetwork();
        }

        @Override
        public void onBlockedStatusChanged(@NonNull android.net.Network network, boolean blocked) {
          Log.i("DeltaChat", "++++++++++++++++++ NetworkCallback.onBlockedStatusChanged() #" + debugOnBlockedStatusChangedCount++);
        }

        @Override
        public void onCapabilitiesChanged(@NonNull android.net.Network network, NetworkCapabilities networkCapabilities) {
          // usually called after onAvailable(), so a maybeNetwork seems contraproductive
          Log.i("DeltaChat", "++++++++++++++++++ NetworkCallback.onCapabilitiesChanged() #" + debugOnCapabilitiesChangedCount++);
        }

        @Override
        public void onLinkPropertiesChanged(@NonNull android.net.Network network, LinkProperties linkProperties) {
          Log.i("DeltaChat", "++++++++++++++++++ NetworkCallback.onLinkPropertiesChanged() #" + debugOnLinkPropertiesChangedCount++);
        }
      });
    } // no else: use old method for debugging
    BroadcastReceiver networkStateReceiver = new NetworkStateReceiver();
    registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));

    KeepAliveService.maybeStartSelf(this);

    initializeLogging();
    initializeJobManager();
    InChatSounds.getInstance(this);

    dcLocationManager = new DcLocationManager(this);
    DynamicTheme.setDefaultDayNightMode(this);

    IntentFilter filter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
    registerReceiver(new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Util.localeChanged();
            DcHelper.setStockTranslations(context);
        }
    }, filter);

    AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

    if (Prefs.isPushEnabled(this)) {
      FcmReceiveService.register(this);
    } else {
      Log.i(TAG, "FCM disabled at build time");
      // MAYBE TODO: i think the ApplicationContext is also created
      // when the app is stated by FetchWorker timeouts.
      // in this case, the normal threads shall not be started.
      Constraints constraints = new Constraints.Builder()
              .setRequiredNetworkType(NetworkType.CONNECTED)
              .build();
      PeriodicWorkRequest fetchWorkRequest = new PeriodicWorkRequest.Builder(
              FetchWorker.class,
              PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, // usually 15 minutes
              TimeUnit.MILLISECONDS,
              PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, // the start may be preferred by up to 5 minutes, so we run every 10-15 minutes
              TimeUnit.MILLISECONDS)
              .setConstraints(constraints)
              .build();
      WorkManager.getInstance(this).enqueueUniquePeriodicWork(
              "FetchWorker",
              ExistingPeriodicWorkPolicy.KEEP,
              fetchWorkRequest);
    }

    PeriodicWorkRequest webxdcGarbageCollectionRequest = new PeriodicWorkRequest.Builder(
            WebxdcGarbageCollectionWorker.class,
            PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
            TimeUnit.MILLISECONDS,
            PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS,
            TimeUnit.MILLISECONDS)
            .build();
    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WebxdcGarbageCollectionWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            webxdcGarbageCollectionRequest);
  }

  public JobManager getJobManager() {
    return jobManager;
  }

  private void initializeLogging() {
    SignalProtocolLoggerProvider.setProvider(new AndroidSignalProtocolLogger());
  }

  private void initializeJobManager() {
    this.jobManager = new JobManager(this, 5);
  }
}
