package org.thoughtcrime.securesms;

import org.webrtc.EglBase;

public class EglUtils {
  private static EglBase eglBase;

  public static synchronized EglBase getEglBase() {
    if (eglBase == null) {
      eglBase = EglBase.create();
    }
    return eglBase;
  }

  public static synchronized void release() {
    if (eglBase != null) {
      eglBase.release();
      eglBase = null;
    }
  }
}
