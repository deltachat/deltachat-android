package org.thoughtcrime.securesms.util;

import android.content.Context;

import java.io.IOException;

public class VersionTracker {


  public static int getLastSeenVersion(Context context) {
    return Prefs.getLastVersionCode(context);
  }

  public static void updateLastSeenVersion(Context context) {
    try {
      int currentVersionCode = Util.getCurrentApkReleaseVersion(context);
      Prefs.setLastVersionCode(context, currentVersionCode);
    } catch (IOException ioe) {
      throw new AssertionError(ioe);
    }
  }
}
