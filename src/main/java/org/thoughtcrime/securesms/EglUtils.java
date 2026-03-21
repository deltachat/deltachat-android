package org.thoughtcrime.securesms;

import android.os.Build;
import androidx.annotation.RequiresApi;
import org.webrtc.EglBase;

@RequiresApi(Build.VERSION_CODES.M)
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
