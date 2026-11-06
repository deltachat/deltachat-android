package org.thoughtcrime.securesms.testutil;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

public final class TestEnv {
  private TestEnv() {}

  public static TestRule forScreenshots() {
    return RuleChain.outerRule(new DisableAnimations()).around(SystemUiDemoMode.clean());
  }
}
