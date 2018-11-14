package org.thoughtcrime.securesms.jobs.requirements;


import org.thoughtcrime.securesms.jobmanager.requirements.RequirementListener;
import org.thoughtcrime.securesms.jobmanager.requirements.RequirementProvider;

public class SqlCipherMigrationRequirementProvider implements RequirementProvider {

  private RequirementListener listener;

  public SqlCipherMigrationRequirementProvider() {
  }

  @Override
  public void setListener(RequirementListener listener) {
    this.listener = listener;
  }

  public static class SqlCipherNeedsMigrationEvent {

  }
}
