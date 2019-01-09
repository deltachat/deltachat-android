/*
 * Copyright (C) 2013 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms;

import android.annotation.SuppressLint;
import android.app.Application;
import android.arch.lifecycle.DefaultLifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.Context;
import android.support.annotation.NonNull;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEventCenter;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.crypto.PRNGFixes;
import org.thoughtcrime.securesms.jobmanager.JobManager;
import org.thoughtcrime.securesms.jobmanager.persistence.JavaJobSerializer;
import org.thoughtcrime.securesms.notifications.MessageNotifier;
import org.thoughtcrime.securesms.util.ScreenLockUtil;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.SignalProtocolLoggerProvider;
import org.thoughtcrime.securesms.util.AndroidSignalProtocolLogger;

/**
 * Will be called once when the TextSecure process is created.
 *
 * We're using this as an insertion point to patch up the Android PRNG disaster,
 * to initialize the job manager, and to check for GCM registration freshness.
 *
 * @author Moxie Marlinspike
 */
public class ApplicationContext extends Application implements DefaultLifecycleObserver {

  private static final String TAG = ApplicationContext.class.getName();

  private JobManager             jobManager;

  private volatile boolean isAppVisible;

  public ApplicationDcContext dcContext;

  public static ApplicationContext getInstance(Context context) {
    return (ApplicationContext)context.getApplicationContext();
  }

  @Override
  public void onCreate() {
    super.onCreate();

    System.loadLibrary("native-utils");
    dcContext = new ApplicationDcContext(this);

    initializeRandomNumberFix();
    initializeLogging();
    initializeJobManager();
    initializePeriodicTasks();
    initializeIncomingMessageNotifier();
    ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
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

  private void initializePeriodicTasks() {
    //DirectoryRefreshListener.schedule(this); -- directory in this sense seems to be the address book
    //LocalBackupListener.schedule(this); -- disabled for now, there is no automatic backup; maybe the implicit IMAP backup is sufficient

    // disabled for now, Delta Chat has no update url; maybe f-droid is sufficient
    //if (BuildConfig.PLAY_STORE_DISABLED) {
    //  UpdateApkRefreshListener.schedule(this);
    //}
  }
}
