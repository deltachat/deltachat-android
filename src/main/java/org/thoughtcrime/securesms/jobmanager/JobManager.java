package org.thoughtcrime.securesms.jobmanager;

import android.content.Context;
import android.os.PowerManager;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A JobManager allows you to enqueue {@link org.thoughtcrime.securesms.jobmanager.Job} tasks
 * that are executed once a Job's {@link org.thoughtcrime.securesms.jobmanager.requirements.Requirement}s
 * are met.
 */
public class JobManager {

  private final JobQueue      jobQueue           = new JobQueue();
  private final Executor      eventExecutor      = Executors.newSingleThreadExecutor();

  private final Context                     context;

  public JobManager(Context context, int consumers)
  {
    this.context              = context;

    for (int i=0;i<consumers;i++) {
      new JobConsumer("JobConsumer-" + i, jobQueue).start();
    }
  }

  /**
   * Queue a {@link org.thoughtcrime.securesms.jobmanager.Job} to be executed.
   *
   * @param job The Job to be executed.
   */
  public void add(final Job job) {
    if (job.needsWakeLock()) {
      job.setWakeLock(acquireWakeLock(context, job.toString(), job.getWakeLockTimeout()));
    }

    eventExecutor.execute(new Runnable() {
      @Override
      public void run() {
        job.onAdded();
        jobQueue.add(job);
      }
    });
  }

  private PowerManager.WakeLock acquireWakeLock(Context context, String name, long timeout) {
    PowerManager          powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    PowerManager.WakeLock wakeLock     = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, name);

    if (timeout == 0) wakeLock.acquire();
    else              wakeLock.acquire(timeout);

    return wakeLock;
  }
}
