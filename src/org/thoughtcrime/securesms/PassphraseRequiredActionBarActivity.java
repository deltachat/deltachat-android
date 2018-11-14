package org.thoughtcrime.securesms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.push.SignalServiceNetworkAccess;
import org.thoughtcrime.securesms.service.KeyCachingService;

import java.util.Locale;

public abstract class PassphraseRequiredActionBarActivity extends BaseActionBarActivity implements MasterSecretListener {
  private static final String TAG = PassphraseRequiredActionBarActivity.class.getSimpleName();

  public static final String LOCALE_EXTRA = "locale_extra";

  private static final int STATE_NORMAL          = 0;
  private static final int STATE_NEEDS_CONFIGURE = 1;

  private SignalServiceNetworkAccess networkAccess;
  private BroadcastReceiver          clearKeyReceiver;
  private boolean                    isVisible;

  @Override
  protected final void onCreate(Bundle savedInstanceState) {
    Log.w(TAG, "onCreate(" + savedInstanceState + ")");
    this.networkAccess = new SignalServiceNetworkAccess(this);
    onPreCreate();

    final boolean locked = KeyCachingService.isLocked(this);
    routeApplicationState(locked);

    super.onCreate(savedInstanceState);

    if (!isFinishing()) {
      initializeClearKeyReceiver();
      onCreate(savedInstanceState, true);
    }
  }

  protected void onPreCreate() {}
  protected void onCreate(Bundle savedInstanceState, boolean ready) {}

  @Override
  protected void onResume() {
    Log.w(TAG, "onResume()");
    super.onResume();
    KeyCachingService.registerPassphraseActivityStarted(this);

    isVisible = true;
  }

  @Override
  protected void onPause() {
    Log.w(TAG, "onPause()");
    super.onPause();
    KeyCachingService.registerPassphraseActivityStopped(this);

    isVisible = false;
  }

  @Override
  protected void onDestroy() {
    Log.w(TAG, "onDestroy()");
    super.onDestroy();
    removeClearKeyReceiver(this);
  }

  @Override
  public void onMasterSecretCleared() {
    Log.w(TAG, "onMasterSecretCleared()");
    if (isVisible) routeApplicationState(true);
    else           finish();
  }

  protected <T extends Fragment> T initFragment(@IdRes int target,
                                                @NonNull T fragment)
  {
    return initFragment(target, fragment, null);
  }

  protected <T extends Fragment> T initFragment(@IdRes int target,
                                                @NonNull T fragment,
                                                @Nullable Locale locale)
  {
    return initFragment(target, fragment, locale, null);
  }

  protected <T extends Fragment> T initFragment(@IdRes int target,
                                                @NonNull T fragment,
                                                @Nullable Locale locale,
                                                @Nullable Bundle extras)
  {
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

  private void routeApplicationState(boolean locked) {
    Intent intent = getIntentForState(getApplicationState(locked));
    if (intent != null) {
      startActivity(intent);
      finish();
    }
  }

  private Intent getIntentForState(int state) {
    switch (state) {
      case STATE_NEEDS_CONFIGURE: return getWelcomeIntent();
      default:                    return null;
    }
  }

  private int getApplicationState(boolean locked) {
    boolean isConfigured = DcHelper.isConfigured(getApplicationContext());
    if (!isConfigured) {
      return STATE_NEEDS_CONFIGURE;
    } else {
      return STATE_NORMAL;
    }
  }

  private Intent getCreatePassphraseIntent() {
    return getRoutedIntent(PassphraseCreateActivity.class, getIntent());
  }

  private Intent getPromptPassphraseIntent() {
    return getRoutedIntent(PassphrasePromptActivity.class, getIntent());
  }

  private Intent getWelcomeIntent() {
    return getRoutedIntent(WelcomeActivity.class, null);
  }

  private Intent getLoginIntent() {
    return getRoutedIntent(RegistrationActivity.class, null);
  }

  private Intent getCreateProfileIntent() {
    return getRoutedIntent(CreateProfileActivity.class, getConversationListIntent());
  }

  private Intent getRoutedIntent(Class<?> destination, @Nullable Intent nextIntent) {
    final Intent intent = new Intent(this, destination);
    if (nextIntent != null)   intent.putExtra("next_intent", nextIntent);
    return intent;
  }

  private Intent getConversationListIntent() {
    return new Intent(this, ConversationListActivity.class);
  }

  private void initializeClearKeyReceiver() {
    Log.w(TAG, "initializeClearKeyReceiver()");
    this.clearKeyReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        Log.w(TAG, "onReceive() for clear key event");
        onMasterSecretCleared();
      }
    };

    IntentFilter filter = new IntentFilter(KeyCachingService.CLEAR_KEY_EVENT);
    registerReceiver(clearKeyReceiver, filter, KeyCachingService.KEY_PERMISSION, null);
  }

  private void removeClearKeyReceiver(Context context) {
    if (clearKeyReceiver != null) {
      context.unregisterReceiver(clearKeyReceiver);
      clearKeyReceiver = null;
    }
  }
}
