package org.thoughtcrime.securesms;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.Toast;

import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ScreenLockUtil;

import java.lang.reflect.Field;
import java.util.Timer;


public abstract class BaseActionBarActivity extends AppCompatActivity {

  private static final String TAG = BaseActionBarActivity.class.getSimpleName();

  private Timer timer;

  private boolean isWaitingForResult;
  private boolean isHiddenByScreenLock;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.i("DeltaChat", "BaseActionBarActivity/onCreate");
    if (BaseActivity.isMenuWorkaroundRequired()) {
      forceOverflowMenu();
    }
    super.onCreate(savedInstanceState);
  }

    @Override
    protected void onStart() {
      Log.i("DeltaChat", "BaseActionBarActivity/onStart");
      if (ScreenLockUtil.isScreenLockEnabled(this) && ScreenLockUtil.getShouldLockApp() && !isWaitingForResult) {
          ScreenLockUtil.applyScreenLock(this);
        } else if (isHiddenByScreenLock) {
          findViewById(android.R.id.content).setVisibility(View.VISIBLE);
          isHiddenByScreenLock = false;
        }
      Log.i("DeltaChat", "BaseActionBarActivity/onStart/2");
        super.onStart();
      Log.i("DeltaChat", "BaseActionBarActivity/onStart/3");
    }

  @Override
  protected void onStop() {
    Log.i("DeltaChat", "BaseActionBarActivity/onStop");

    if (ScreenLockUtil.isScreenLockEnabled(this)) {
      findViewById(android.R.id.content).setVisibility(View.GONE);
      isHiddenByScreenLock = true;
    }
    super.onStop();
  }

  @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.i("DeltaChat", "BaseActionBarActivity/onActivityResult");
    super.onActivityResult(requestCode, resultCode, data);
      isWaitingForResult = false;
      if (requestCode == ScreenLockUtil.REQUEST_CODE_CONFIRM_CREDENTIALS) {
            if (resultCode == RESULT_OK) {
                ScreenLockUtil.setShouldLockApp(false);
            } else {
                Toast.makeText(this, R.string.screenlock_authentication_failed, Toast.LENGTH_SHORT).show();
                ScreenLockUtil.applyScreenLock(this);
            }
        }
    }

  @Override
  protected void onResume() {
    Log.i("DeltaChat", "BaseActionBarActivity/onResume");
    super.onResume();
    initializeScreenshotSecurity();
    initializeScreenLockTimeout();
  }

  private void initializeScreenLockTimeout() {
    Log.i("DeltaChat", "BaseActionBarActivity/initializeScreenLockTimeout");

    if (ScreenLockUtil.isScreenLockTimeoutEnabled(this)) {
        timer = ScreenLockUtil.scheduleScreenLockTimer(timer, this);
    }
  }

  @Override
    protected void onPause() {
    Log.i("DeltaChat", "BaseActionBarActivity/onPause");

    super.onPause();
      tearDownScreenLockTimeout();
  }

  private void tearDownScreenLockTimeout() {
    Log.i("DeltaChat", "BaseActionBarActivity/tearDown...");

    ScreenLockUtil.cancelScreenLockTimer(timer);
  }

  @Override
  public void onUserInteraction() {
    Log.i("DeltaChat", "BaseActionBarActivity/onUserInteraction");

    super.onUserInteraction();
    initializeScreenLockTimeout();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    Log.i("DeltaChat", "BaseActionBarActivity/onKeyDown");

    return (keyCode == KeyEvent.KEYCODE_MENU && BaseActivity.isMenuWorkaroundRequired()) || super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
    Log.i("DeltaChat", "BaseActionBarActivity/onKeyUp");

    if (keyCode == KeyEvent.KEYCODE_MENU && BaseActivity.isMenuWorkaroundRequired()) {
      openOptionsMenu();
      return true;
    }
    return super.onKeyUp(keyCode, event);
  }

  private void initializeScreenshotSecurity() {
    Log.i("DeltaChat", "BaseActionBarActivity/initializeScreenshotSecurity");

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH &&
            Prefs.isScreenSecurityEnabled(this))
    {
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    } else {
      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
  }

  /**
   * Modified from: http://stackoverflow.com/a/13098824
   */
  private void forceOverflowMenu() {
    Log.i("DeltaChat", "BaseActionBarActivity/forceOverflowMenu");

    try {
      ViewConfiguration config       = ViewConfiguration.get(this);
      Field             menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
      if(menuKeyField != null) {
        menuKeyField.setAccessible(true);
        menuKeyField.setBoolean(config, false);
      }
    } catch (IllegalAccessException e) {
      Log.w(TAG, "Failed to force overflow menu.");
    } catch (NoSuchFieldException e) {
      Log.w(TAG, "Failed to force overflow menu.");
    }
  }

  protected void startActivitySceneTransition(Intent intent, View sharedView, String transitionName) {
    Log.i("DeltaChat", "BaseActionBarActivity/startActivitySceneTransition");

    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(this, sharedView, transitionName)
                                         .toBundle();
    ActivityCompat.startActivity(this, intent, bundle);
  }

  @Override
  public void startActivityForResult(Intent intent, int requestCode) {
    Log.i("DeltaChat", "BaseActionBarActivity/startActivityForResult");

    super.startActivityForResult(intent, requestCode);
    if (requestCode != -1) {
      isWaitingForResult = true;
    }
  }
}
