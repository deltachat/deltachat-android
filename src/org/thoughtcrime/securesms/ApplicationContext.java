package org.thoughtcrime.securesms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.b44t.messenger.DcAccounts;
import com.b44t.messenger.DcAccountsEventEmitter;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;

import org.thoughtcrime.securesms.components.emoji.EmojiProvider;
import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.connect.FetchWorker;
import org.thoughtcrime.securesms.connect.ForegroundDetector;
import org.thoughtcrime.securesms.connect.KeepAliveService;
import org.thoughtcrime.securesms.connect.NetworkStateReceiver;
import org.thoughtcrime.securesms.crypto.DatabaseSecret;
import org.thoughtcrime.securesms.crypto.DatabaseSecretProvider;
import org.thoughtcrime.securesms.crypto.PRNGFixes;
import org.thoughtcrime.securesms.geolocation.DcLocationManager;
import org.thoughtcrime.securesms.jobmanager.JobManager;
import org.thoughtcrime.securesms.notifications.InChatSounds;
import org.thoughtcrime.securesms.notifications.NotificationCenter;
import org.thoughtcrime.securesms.util.AndroidSignalProtocolLogger;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.SignalProtocolLoggerProvider;

import java.io.File;
import java.util.concurrent.TimeUnit;
//import com.squareup.leakcanary.LeakCanary;

public class ApplicationContext extends MultiDexApplication {
  private static final String TAG = ApplicationContext.class.getSimpleName();

  public DcAccounts             dcAccounts;
  public DcContext              dcContext;
  public DcLocationManager      dcLocationManager;
  public DcEventCenter          eventCenter;
  public NotificationCenter     notificationCenter;
  private JobManager            jobManager;

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

    // The first call to `getInstance` takes about 100ms-300ms, so, do it on a background thread
    Thread t = new Thread(() -> EmojiProvider.getInstance(this), "InitEmojiProviderThread");
    t.setPriority(Thread.MIN_PRIORITY);
    t.start();

    System.loadLibrary("native-utils");

    dcAccounts = new DcAccounts("Android "+BuildConfig.VERSION_NAME, new File(getFilesDir(), "accounts").getAbsolutePath());
    AccountManager.getInstance().migrateToDcAccounts(this);
    int[] allAccounts = dcAccounts.getAll();
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
    }
    if (allAccounts.length == 0) {
      dcAccounts.addAccount();
    }
    dcContext = dcAccounts.getSelectedAccount();
    notificationCenter = new NotificationCenter(this);
    eventCenter = new DcEventCenter(this);
    new Thread(() -> {
      DcAccountsEventEmitter emitter = dcAccounts.getEventEmitter();
      while (true) {
        DcEvent event = emitter.getNextEvent();
        if (event==null) {
          break;
        }
        eventCenter.handleEvent(event);
      }
      Log.i("DeltaChat", "shutting down event handler");
    }, "eventThread").start();
    dcAccounts.startIo();

    new ForegroundDetector(ApplicationContext.getInstance(this));

    BroadcastReceiver networkStateReceiver = new NetworkStateReceiver();
    registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));

    KeepAliveService.maybeStartSelf(this);

    initializeRandomNumberFix();
    initializeLogging();
    initializeJobManager();
    InChatSounds.getInstance(this);

    dcLocationManager = new DcLocationManager(this);
    try {
      DynamicLanguage.setContextLocale(this, DynamicLanguage.getSelectedLocale(this));
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    DynamicTheme.setDefaultDayNightMode(this);

    DcHelper.setStockTranslations(this);

    IntentFilter filter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
    registerReceiver(new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            DcHelper.setStockTranslations(context);
        }
    }, filter);

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
    AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
  }

  public JobManager getJobManager() {
    return jobManager;
  }

  private void initializeRandomNumberFix() {
    PRNGFixes.apply();
  }

  private void initializeLogging() {
    SignalProtocolLoggerProvider.setProvider(new AndroidSignalProtocolLogger());
  }

  private void initializeJobManager() {
    this.jobManager = new JobManager(this, 5);
  }
}
