package org.thoughtcrime.securesms;

import android.annotation.SuppressLint;
import android.arch.lifecycle.DefaultLifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDexApplication;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEventCenter;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.crypto.PRNGFixes;
import org.thoughtcrime.securesms.geolocation.DcLocationManager;
import org.thoughtcrime.securesms.jobmanager.JobManager;
import org.thoughtcrime.securesms.notifications.MessageNotifier;
import org.thoughtcrime.securesms.util.AndroidSignalProtocolLogger;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.ScreenLockUtil;
import org.thoughtcrime.securesms.util.SignalProtocolLoggerProvider;
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
    MessageNotifier.init(this);

    dcLocationManager = new DcLocationManager(this);
    try {
      DynamicLanguage.setContextLocale(this, DynamicLanguage.getSelectedLocale(this));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
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
  }

  private void initializeJobManager() {
    this.jobManager = JobManager.newBuilder(this)
                                .withName("TextSecureJobs")
                                .withConsumerThreads(5)
                                .build();
  }
}
