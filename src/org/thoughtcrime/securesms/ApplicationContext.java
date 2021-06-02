package org.thoughtcrime.securesms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.FetchWorker;
import org.thoughtcrime.securesms.connect.ForegroundDetector;
import org.thoughtcrime.securesms.connect.KeepAliveService;
import org.thoughtcrime.securesms.connect.NetworkStateReceiver;
import org.thoughtcrime.securesms.crypto.PRNGFixes;
import org.thoughtcrime.securesms.geolocation.DcLocationManager;
import org.thoughtcrime.securesms.jobmanager.JobManager;
import org.thoughtcrime.securesms.notifications.InChatSounds;
import org.thoughtcrime.securesms.util.AndroidSignalProtocolLogger;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.SignalProtocolLoggerProvider;

import java.util.concurrent.TimeUnit;
//import com.squareup.leakcanary.LeakCanary;

public class ApplicationContext extends MultiDexApplication {

  public ApplicationDcContext   dcContext;
  public DcLocationManager      dcLocationManager;
  private JobManager            jobManager;

  public static ApplicationContext getInstance(Context context) {
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
    dcContext = new ApplicationDcContext(this);

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

    dcContext.setStockTranslations();

    IntentFilter filter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
    registerReceiver(new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            dcContext.setStockTranslations();
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
