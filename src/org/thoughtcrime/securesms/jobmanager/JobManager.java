package org.thoughtcrime.securesms.jobmanager;

import android.content.Context;
import android.os.PowerManager;

import org.thoughtcrime.securesms.jobmanager.requirements.RequirementListener;
import org.thoughtcrime.securesms.jobmanager.requirements.RequirementProvider;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A JobManager allows you to enqueue {@link org.thoughtcrime.securesms.jobmanager.Job} tasks
 * that are executed once a Job's {@link org.thoughtcrime.securesms.jobmanager.requirements.Requirement}s
 * are met.
 */
public class JobManager implements RequirementListener {

  private final JobQueue      jobQueue           = new JobQueue();
  private final Executor      eventExecutor      = Executors.newSingleThreadExecutor();

  private final Context                     context;
  private final List<RequirementProvider>   requirementProviders;

  private JobManager(Context context,
                     List<RequirementProvider> requirementProviders,
                     int consumers)
  {
    this.context              = context;
    this.requirementProviders = requirementProviders;

    if (requirementProviders != null && !requirementProviders.isEmpty()) {
      for (RequirementProvider provider : requirementProviders) {
        provider.setListener(this);
      }
    }

    for (int i=0;i<consumers;i++) {
      new JobConsumer("JobConsumer-" + i, jobQueue).start();
    }
  }

  /**
   * @param context An Android {@link android.content.Context}.
   * @return a {@link org.thoughtcrime.securesms.jobmanager.JobManager.Builder} used to construct a JobManager.
   */
  public static Builder newBuilder(Context context) {
    return new Builder(context);
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

  @Override
  public void onRequirementStatusChanged() {
    eventExecutor.execute(new Runnable() {
      @Override
      public void run() {
        jobQueue.onRequirementStatusChanged();
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

  public static class Builder {
    private final Context                   context;
    private       List<RequirementProvider> requirementProviders;
    private       int                       consumerThreads;

    Builder(Context context) {
      this.context         = context;
      this.consumerThreads = 5;
    }

    /**
     * Set the number of threads dedicated to consuming Jobs from the queue and executing them.
     *
     * @param consumerThreads The number of threads.
     * @return The builder.
     */
    public Builder withConsumerThreads(int consumerThreads) {
      this.consumerThreads = consumerThreads;
      return this;
    }

    /**
     * @return A constructed JobManager.
     */
    public JobManager build() {
      if (requirementProviders == null) {
        requirementProviders = new LinkedList<>();
      }

      return new JobManager(context, requirementProviders,
                            consumerThreads);
    }
  }

}
