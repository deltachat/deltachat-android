package org.thoughtcrime.securesms;

import android.annotation.SuppressLint;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.annotation.NonNull;
import androidx.multidex.MultiDexApplication;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEventCenter;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.FetchWorker;
import org.thoughtcrime.securesms.crypto.PRNGFixes;
import org.thoughtcrime.securesms.geolocation.DcLocationManager;
import org.thoughtcrime.securesms.jobmanager.JobManager;
import org.thoughtcrime.securesms.notifications.MessageNotifierCompat;
import org.thoughtcrime.securesms.util.AndroidSignalProtocolLogger;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.ScreenLockUtil;
import org.thoughtcrime.securesms.util.SignalProtocolLoggerProvider;

import java.util.concurrent.TimeUnit;
//import com.squareup.leakcanary.LeakCanary;

public class ApplicationContext extends MultiDexApplication implements DefaultLifecycleObserver {

  public ApplicationDcContext   dcContext;
  public DcLocationManager      dcLocationManager;
  private JobManager            jobManager;
  private volatile boolean      isAppVisible;

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

    System.loadLibrary("native-utils");
    dcContext = new ApplicationDcContext(this);

    initializeRandomNumberFix();
    initializeLogging();
    initializeJobManager();
    initializeIncomingMessageNotifier();
    ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    MessageNotifierCompat.init(this);

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
  }

  @Override
  public void onStart(@NonNull LifecycleOwner owner) {
    isAppVisible = true;
  }

  @Override
  public void onStop(@NonNull LifecycleOwner owner) {
    isAppVisible = false;
    ScreenLockUtil.setShouldLockApp(true);
  }

  public JobManager getJobManager() {
    return jobManager;
  }

  public boolean isAppVisible() {
    return isAppVisible;
  }

  private void initializeRandomNumberFix() {
    PRNGFixes.apply();
  }

  private void initializeLogging() {
    SignalProtocolLoggerProvider.setProvider(new AndroidSignalProtocolLogger());
  }

  @SuppressLint("StaticFieldLeak")
  private void initializeIncomingMessageNotifier() {

    DcEventCenter dcEventCenter = dcContext.eventCenter;
    dcEventCenter.addObserver(DcContext.DC_EVENT_INCOMING_MSG, new DcEventCenter.DcEventDelegate() {
      @Override
      public void handleEvent(int eventId, Object data1, Object data2) {
        MessageNotifierCompat.updateNotification(((Long) data1).intValue(), ((Long) data2).intValue());
      }

      @Override
      public boolean runOnMain() {
        return false;
      }
    });
  }

  private void initializeJobManager() {
    this.jobManager = JobManager.newBuilder(this)
                                .withName("TextSecureJobs")
                                .withConsumerThreads(5)
                                .build();
  }
}
