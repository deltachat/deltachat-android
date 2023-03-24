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
    onPreCreate();

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
      finish();
    }

    super.onCreate(savedInstanceState);

    if (!isFinishing()) {
      onCreate(savedInstanceState, true);
    }
  }

  protected void onPreCreate() {}
  protected void onCreate(Bundle savedInstanceState, boolean ready) {}
}
