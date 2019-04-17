package org.thoughtcrime.securesms;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;

import org.thoughtcrime.securesms.connect.DcHelper;

import java.util.Locale;

public abstract class PassphraseRequiredActionBarActivity extends BaseActionBarActivity {
  private static final String TAG = PassphraseRequiredActionBarActivity.class.getSimpleName();

  public static final String LOCALE_EXTRA = "locale_extra";

  private static final int STATE_NORMAL          = 0;
  private static final int STATE_NEEDS_CONFIGURE = 1;

  @Override
  protected final void onCreate(Bundle savedInstanceState) {
    Log.i("DeltaChat", "PassphraseRequiredActionBarActivity/onCreate");
    onPreCreate();

    routeApplicationState();

    super.onCreate(savedInstanceState);

    if (!isFinishing()) {
      onCreate(savedInstanceState, true);
    }
  }

  protected void onPreCreate() {
    Log.i("DeltaChat", "PassphraseRequiredActionBarActivity/onPreCreate");

  }
  protected void onCreate(Bundle savedInstanceState, boolean ready) {
    Log.i("DeltaChat", "PassphraseRequiredActionBarActivity/onCreate");

  }

  @Override
  protected void onResume() {
    Log.i("DeltaChat", "PassphraseRequiredActionBarActivity/onResume");
    super.onResume();
  }

  @Override
  protected void onPause() {
    Log.i("DeltaChat", "PassphraseRequiredActionBarActivity/onPause");
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    Log.i("DeltaChat", "PassphraseRequiredActionBarActivity/onDestroy");
    super.onDestroy();
  }

  protected <T extends Fragment> T initFragment(@IdRes int target,
                                                @NonNull T fragment)
  {
    Log.i("DeltaChat", "PassphraseRequiredActionBarActivity/initFragment");
    return initFragment(target, fragment, null);
  }

  protected <T extends Fragment> T initFragment(@IdRes int target,
                                                @NonNull T fragment,
                                                @Nullable Locale locale)
  {
    Log.i("DeltaChat", "PassphraseRequiredActionBarActivity/initFragment");
    return initFragment(target, fragment, locale, null);
  }

  protected <T extends Fragment> T initFragment(@IdRes int target,
                                                @NonNull T fragment,
                                                @Nullable Locale locale,
                                                @Nullable Bundle extras)
  {
    Log.i("DeltaChat", "PassphraseRequiredActionBarActivity/initFragment");
    Bundle args = new Bundle();
    args.putSerializable(LOCALE_EXTRA, locale);

    if (extras != null) {
      args.putAll(extras);
    }

    fragment.setArguments(args);
    getSupportFragmentManager().beginTransaction()
                               .replace(target, fragment)
                               .commitAllowingStateLoss();
    return fragment;
  }

  private void routeApplicationState() {
    Log.i("DeltaChat", "PassphraseRequiredActionBarActivity/routeApplicationState");
    Intent intent = getIntentForState(getApplicationState());
    if (intent != null) {
      startActivity(intent);
      finish();
    }
  }

  private Intent getIntentForState(int state) {
    Log.i("DeltaChat", "PassphraseRequiredActionBarActivity/getIntentForState");
    switch (state) {
      case STATE_NEEDS_CONFIGURE: return getWelcomeIntent();
      default:                    return null;
    }
  }

  private int getApplicationState() {
    Log.i("DeltaChat", "PassphraseRequiredActionBarActivity/getApplicationState");
    boolean isConfigured = DcHelper.isConfigured(getApplicationContext());
    if (!isConfigured) {
      return STATE_NEEDS_CONFIGURE;
    } else {
      return STATE_NORMAL;
    }
  }

  private Intent getWelcomeIntent() {
    Log.i("DeltaChat", "PassphraseRequiredActionBarActivity/getWelcomeIntent");
    return getRoutedIntent(WelcomeActivity.class, null);
  }

  private Intent getRoutedIntent(Class<?> destination, @Nullable Intent nextIntent) {
    Log.i("DeltaChat", "PassphraseRequiredActionBarActivity/getRoutedIntent");
    final Intent intent = new Intent(this, destination);
    if (nextIntent != null)   intent.putExtra("next_intent", nextIntent);
    return intent;
  }
}
