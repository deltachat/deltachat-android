/**
 * Copyright (C) 2014 Open Whisper Systems
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
package org.thoughtcrime.securesms.jobmanager;

import androidx.annotation.NonNull;
import android.util.Log;

class JobConsumer extends Thread {

  private static final String TAG = JobConsumer.class.getSimpleName();

  enum JobResult {
    SUCCESS,
    FAILURE,
    DEFERRED
  }

  private final JobQueue          jobQueue;

  public JobConsumer(String name, JobQueue jobQueue) {
    super(name);
    this.jobQueue          = jobQueue;
  }

  @Override
  public void run() {
    while (true) {
      Job       job    = jobQueue.getNext();
      JobResult result = runJob(job);

      if (result == JobResult.DEFERRED) {
        jobQueue.push(job);
      } else {
        if (result == JobResult.FAILURE) {
          job.onCanceled();
        }

        if (job.getWakeLock() != null && job.getWakeLockTimeout() == 0) {
          job.getWakeLock().release();
        }

        if (job.getGroupId() != null) {
          jobQueue.setGroupIdAvailable(job.getGroupId());
        }
      }
    }
  }

  private JobResult runJob(Job job) {
    while (canRetry(job)) {
      try {
        job.onRun();
        return JobResult.SUCCESS;
      } catch (Exception exception) {
        Log.w(TAG, exception);
        if (exception instanceof RuntimeException) {
          throw (RuntimeException)exception;
        } else if (!job.onShouldRetry(exception)) {
          return JobResult.FAILURE;
        }

        job.onRetry();
        if (!job.isRequirementsMet()) {
          return JobResult.DEFERRED;
        }
      }
    }

    return JobResult.FAILURE;
  }

  private boolean canRetry(@NonNull Job job) {
    if (job.getRetryCount() > 0) {
      return job.getRunIteration() < job.getRetryCount();
    }
    return System.currentTimeMillis() < job.getRetryUntil();
  }
}
