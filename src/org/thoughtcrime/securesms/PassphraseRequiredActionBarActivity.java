package org.thoughtcrime.securesms;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.service.GenericForegroundService;

public abstract class PassphraseRequiredActionBarActivity extends BaseActionBarActivity {
  private static final String TAG = PassphraseRequiredActionBarActivity.class.getSimpleName();

  @Override
  protected final void onCreate(Bundle savedInstanceState) {
    Log.w(TAG, "onCreate(" + savedInstanceState + ")");

    if (allowInLockedMode()) {
      super.onCreate(savedInstanceState);
      onCreate(savedInstanceState, true);
      return;
    }

    if (GenericForegroundService.isForegroundTaskStarted()) {
      // this does not prevent intent set by onNewIntent(),
      // however, at least during onboarding,
      // this catches a lot of situations with otherwise weird app states.
      super.onCreate(savedInstanceState);
      finish();
      return;
    }

    if (!DcHelper.isConfigured(getApplicationContext())) {
      Intent intent = new Intent(this, WelcomeActivity.class);
      startActivity(intent);
      super.onCreate(savedInstanceState);
      finish();
    } else {
      super.onCreate(savedInstanceState);
    }

    if (!isFinishing()) {
      onCreate(savedInstanceState, true);
    }
  }

  protected void onCreate(Bundle savedInstanceState, boolean ready) {}

  // "Locked Mode" is when the account is not configured (Welcome screen) or when sharing a backup
  // (Add second device) and the user should not leave that screen otherwise the state would be lost.
  // Sometimes it is fine to allow to change in these situations, like to see the logs or offline help.
  protected boolean allowInLockedMode() { return false; }
}
