package org.thoughtcrime.securesms.testutil;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.rules.ExternalResource;
import org.thoughtcrime.securesms.connect.DcHelper;

/** Points onboarding at test relays. Must be applied before the test launches any activity. */
public final class TestRelaySet extends ExternalResource {

  private static final String KEY = "test_relay_set";

  private final String hosts;

  /**
   * @param hosts space-separated relay hosts
   */
  public TestRelaySet(@NonNull String hosts) {
    if (hosts.trim().isEmpty()) {
      throw new IllegalArgumentException("TestRelaySet needs at least one host");
    }
    this.hosts = hosts;
  }

  @Override
  protected void before() {
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    DcHelper.set(context, KEY, hosts);
    String readBack = DcHelper.get(context, KEY);
    if (!hosts.equals(readBack)) {
      throw new IllegalStateException(
          "Core did not store '" + KEY + "' (actual: " + readBack + ").");
    }
  }
}
