package org.thoughtcrime.securesms;

import android.app.Activity;
import android.os.Bundle;

// dummy activity that just pushes the app to foreground when fired.
// can also be used to work around android bug https://code.google.com/p/android/issues/detail?id=53313
public class DummyActivity extends Activity {
  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    finish();
  }
}
