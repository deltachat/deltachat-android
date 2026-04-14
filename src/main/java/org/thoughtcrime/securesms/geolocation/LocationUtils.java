package org.thoughtcrime.securesms.geolocation;

import android.location.Location;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class LocationUtils {

  private static final long STALE_THRESHOLD_MS = 15_000;
  private static final double SIGNIFICANT_MOVE_M = 30.0;
  private static final double MINOR_MOVE_M = 10.0;
  private static final float SIGNIFICANT_ACCURACY_DELTA = 50f;

  private LocationUtils() {}

  public static boolean isBetter(@NonNull Location candidate, @Nullable Location current) {

    if (current == null) return true;

    long timeDelta = candidate.getTime() - current.getTime();
    if (timeDelta < -STALE_THRESHOLD_MS) return false;

    float accuracyDelta = candidate.getAccuracy() - current.getAccuracy();
    boolean significantlyLessAccurate = accuracyDelta > SIGNIFICANT_ACCURACY_DELTA;
    boolean sameProvider = TextUtils.equals(candidate.getProvider(), current.getProvider());

    if (significantlyLessAccurate && sameProvider) return false;

    boolean moreAccurate = accuracyDelta < 0;
    double distance = candidate.distanceTo(current);

    if (distance > SIGNIFICANT_MOVE_M) return true;
    return distance > MINOR_MOVE_M && moreAccurate;
  }
}
