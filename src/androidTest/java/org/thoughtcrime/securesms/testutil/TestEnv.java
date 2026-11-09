package org.thoughtcrime.securesms.testutil;

import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

public final class TestEnv {
  private TestEnv() {}

  public static TestRule forScreenshots() {
    return RuleChain.outerRule(new DisableAnimations())
        .around(
            new ExternalResource() {
              @Override
              protected void before() {
                Ime.disableStylusHandwriting();
              }
            })
        .around(SystemUiDemoMode.clean());
  }
}
