package org.thoughtcrime.securesms.lock;


import android.content.Context;
import android.support.annotation.NonNull;

import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class RegistrationLockReminders {

  private static final NavigableSet<Long> INTERVALS = new TreeSet<Long>() {{
    add(TimeUnit.HOURS.toMillis(6));
    add(TimeUnit.HOURS.toMillis(12));
    add(TimeUnit.DAYS.toMillis(1));
    add(TimeUnit.DAYS.toMillis(3));
    add(TimeUnit.DAYS.toMillis(7));
  }};

  public static final long INITIAL_INTERVAL = INTERVALS.first();

  public static boolean needsReminder(@NonNull Context context) {
    return false;
  }

  public static void scheduleReminder(@NonNull Context context, boolean success) {
    //TODO Remove class
  }

}
