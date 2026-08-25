package org.thoughtcrime.securesms.deltax.module;

public class DependencyRef {

  public final String packageName;
  public final String description;

  public DependencyRef(String packageName, String description) {
    this.packageName = packageName;
    this.description = description;
  }
}
