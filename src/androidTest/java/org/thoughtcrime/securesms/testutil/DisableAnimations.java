package org.thoughtcrime.securesms.testutil;

import java.util.HashMap;
import java.util.Map;
import org.junit.rules.ExternalResource;

public final class DisableAnimations extends ExternalResource {
  private static final String[] KEYS = {
    "window_animation_scale", "transition_animation_scale", "animator_duration_scale"
  };

  private final Map<String, String> original = new HashMap<>();

  @Override
  protected void before() {
    for (String key : KEYS) {
      original.put(key, Shell.run("settings get global " + key));
      Shell.run("settings put global " + key + " 0");
    }
  }

  @Override
  protected void after() {
    for (String key : KEYS) {
      String value = original.get(key);
      if (value == null || value.isEmpty() || value.equals("null")) {
        Shell.run("settings delete global " + key);
      } else {
        Shell.run("settings put global " + key + " " + value);
      }
    }
  }
}
