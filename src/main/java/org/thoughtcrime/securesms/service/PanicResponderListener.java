package org.thoughtcrime.securesms.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.thoughtcrime.securesms.util.Prefs;

/**
 * Respond to a PanicKit trigger Intent by locking the app.  PanicKit provides a
 * common framework for creating "panic button" apps that can trigger actions
 * in "panic responder" apps.  In this case, the response is to lock the app,
 * if it has been configured to do so via the Signal lock preference. If the
 * user has not set a passphrase, then the panic trigger intent does nothing.
 */
public class PanicResponderListener extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent != null  && !Prefs.isPasswordDisabled(context) &&
        "info.guardianproject.panic.action.TRIGGER".equals(intent.getAction()))
    {
      // as delta is protected with the system credentials,
      // the current suggestion on "panic" would probably just be to lock the device.
      // this would also lock delta chat.
      // however, we leave this class to allow easy changes on this.
    }
  }
}