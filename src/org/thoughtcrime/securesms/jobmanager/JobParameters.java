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

import org.thoughtcrime.securesms.jobmanager.requirements.Requirement;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The set of parameters that describe a {@link org.thoughtcrime.securesms.jobmanager.Job}.
 */
public class JobParameters implements Serializable {

  private static final long serialVersionUID = 4880456378402584584L;

  private final List<Requirement> requirements;
  private final int               retryCount;
  private final long              retryUntil;
  private final String            groupId;

  private JobParameters(List<Requirement> requirements,
                        String groupId,
                        int retryCount, long retryUntil)
  {
    this.requirements    = requirements;
    this.groupId         = groupId;
    this.retryCount      = retryCount;
    this.retryUntil      = retryUntil;
  }

  public List<Requirement> getRequirements() {
    return requirements;
  }

  public boolean isPersistent() {
    return false;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public long getRetryUntil() {
    return retryUntil;
  }

  /**
   * @return a builder used to construct JobParameters.
   */
  public static Builder newBuilder() {
    return new Builder();
  }

  public String getGroupId() {
    return groupId;
  }

  public static class Builder {
    private List<Requirement> requirements    = new LinkedList<>();
    private int               retryCount      = 100;
    private long              retryDuration   = 0;
    private String            groupId         = null;

    /**
     * Specify a {@link org.thoughtcrime.securesms.jobmanager.requirements.Requirement }that must be met
     * before the Job is executed.  May be called multiple times to register multiple requirements.
     * @param requirement The Requirement that must be met.
     * @return the builder.
     */
    public Builder withRequirement(Requirement requirement) {
      this.requirements.add(requirement);
      return this;
    }

    /**
     * Specify how many times the job should be retried if execution fails but onShouldRetry() returns
     * true.
     *
     * @param retryCount The number of times the job should be retried.
     * @return the builder.
     */
    public Builder withRetryCount(int retryCount) {
      this.retryCount    = retryCount;
      this.retryDuration = 0;
      return this;
    }

    public Builder withRetryDuration(long duration) {
      this.retryDuration = duration;
      this.retryCount    = 0;
      return this;
    }

    /**
     * Specify a groupId the job should belong to.  Jobs with the same groupId are guaranteed to be
     * executed serially.
     *
     * @param groupId The job's groupId.
     * @return the builder.
     */
    public Builder withGroupId(String groupId) {
      this.groupId = groupId;
      return this;
    }

    /**
     * @return the JobParameters instance that describes a Job.
     */
    public JobParameters create() {
      return new JobParameters(requirements, groupId, retryCount, System.currentTimeMillis() + retryDuration);
    }
  }
}
