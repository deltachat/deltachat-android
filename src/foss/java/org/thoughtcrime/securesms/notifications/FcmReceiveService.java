package org.thoughtcrime.securesms.notifications;

import android.content.Context;

import androidx.annotation.Nullable;

/*
  Fake do-nothing implementation of FcmReceiveService.
  The real implementation is in the gplay flavor only.
*/
public class FcmReceiveService {
  public static void register(Context context) {}
  public static void waitForRegisterFinished() {}
  @Nullable public static String getToken() { return null; }
}
