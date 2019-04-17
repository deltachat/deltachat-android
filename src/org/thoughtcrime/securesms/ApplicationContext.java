package org.thoughtcrime.securesms;

import android.annotation.SuppressLint;
import android.app.Application;
import android.arch.lifecycle.DefaultLifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEventCenter;
import org.thoughtcrime.securesms.geolocation.DcLocationManager;
import com.mapbox.mapboxsdk.Mapbox;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.crypto.PRNGFixes;
import org.thoughtcrime.securesms.jobmanager.JobManager;
import org.thoughtcrime.securesms.jobmanager.persistence.JavaJobSerializer;
import org.thoughtcrime.securesms.notifications.MessageNotifier;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.ScreenLockUtil;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.SignalProtocolLoggerProvider;
import org.thoughtcrime.securesms.util.AndroidSignalProtocolLogger;
//import com.squareup.leakcanary.LeakCanary;

public class ApplicationContext extends Application implements DefaultLifecycleObserver {

  public ApplicationDcContext   dcContext;
  public DcLocationManager      dcLocationManager;
  private JobManager            jobManager;
  private volatile boolean      isAppVisible;

  public static ApplicationContext getInstance(Context context) {
    return (ApplicationContext)context.getApplicationContext();
  }

  public static long startMillis;

  @Override
  public void onCreate() {

    startMillis = System.currentTimeMillis();
    Log.i("DeltaChat", "////////////////////////////// STARTUPPPPP 1");

    super.onCreate();

    Log.i("DeltaChat", "////////////////////////////// STARTUPPPPP 2");
    // if (LeakCanary.isInAnalyzerProcess(this)) {
    //   // This process is dedicated to LeakCanary for heap analysis.
    //   // You should not init your app in this process.
    //   return;
    // }
    // LeakCanary.install(this);

    System.loadLibrary("native-utils");
    dcContext = new ApplicationDcContext(this);

    Log.i("DeltaChat", "////////////////////////////// STARTUPPPPP 3");

    initializeRandomNumberFix();

    Log.i("DeltaChat", "////////////////////////////// STARTUPPPPP 4");

    initializeLogging();

    Log.i("DeltaChat", "////////////////////////////// STARTUPPPPP 5");

    initializeJobManager();

    Log.i("DeltaChat", "////////////////////////////// STARTUPPPPP 6");

    initializeIncomingMessageNotifier();

    Log.i("DeltaChat", "////////////////////////////// STARTUPPPPP 7");

    ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

    Log.i("DeltaChat", "////////////////////////////// STARTUPPPPP 8");

    Mapbox.getInstance(getApplicationContext(), BuildConfig.MAP_ACCESS_TOKEN);

    Log.i("DeltaChat", "////////////////////////////// STARTUPPPPP 9");

    dcLocationManager = new DcLocationManager(this);
    try {
      DynamicLanguage.setContextLocale(this, DynamicLanguage.getSelectedLocale(this));
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    Log.i("DeltaChat", "////////////////////////////// STARTUPPPPP 10 - " + (System.currentTimeMillis() - startMillis) + "ms");
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
        MessageNotifier.updateNotification(dcContext.context, ((Long) data1).intValue());
      }

      @Override
      public boolean runOnMain() {
        return false;
      }
    });

    // in five seconds, the system should be up and ready so we can start issuing notifications.

    Util.runOnBackgroundDelayed(() -> {
      MessageNotifier.updateNotification(dcContext.context);
      }, 5000);
  }

  private void initializeJobManager() {
    this.jobManager = JobManager.newBuilder(this)
                                .withName("TextSecureJobs")
                                .withJobSerializer(new JavaJobSerializer())
                                .withConsumerThreads(5)
                                .build();
  }
}
