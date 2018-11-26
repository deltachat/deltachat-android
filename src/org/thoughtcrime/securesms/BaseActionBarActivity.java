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

import org.thoughtcrime.securesms.util.ScreenLockUtil;
import org.thoughtcrime.securesms.util.TextSecurePreferences;

import java.lang.reflect.Field;
import java.util.Timer;

import static org.thoughtcrime.securesms.util.ScreenLockUtil.shouldLockApp;


public abstract class BaseActionBarActivity extends AppCompatActivity {

  private static final String TAG = BaseActionBarActivity.class.getSimpleName();

  private Timer timer;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if (BaseActivity.isMenuWorkaroundRequired()) {
      forceOverflowMenu();
    }
    super.onCreate(savedInstanceState);
  }

    @Override
    protected void onStart() {
        super.onStart();
        if (ScreenLockUtil.isScreenLockEnabled(this) && shouldLockApp) {
            ScreenLockUtil.applyScreenLock(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      super.onActivityResult(requestCode, resultCode, data);
      if (requestCode == ScreenLockUtil.REQUEST_CODE_CONFIRM_CREDENTIALS) {
            if (resultCode == RESULT_OK) {
                shouldLockApp = false;
            } else {
                Toast.makeText(this, R.string.security_authentication_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

  @Override
  protected void onResume() {
    super.onResume();
    initializeScreenshotSecurity();
    initializeScreenLockTimeout();
  }

  private void initializeScreenLockTimeout() {
    if (ScreenLockUtil.isScreenLockTimeoutEnabled(this)) {
        timer = ScreenLockUtil.scheduleScreenLockTimer(timer, this);
    }
  }

  @Override
    protected void onPause() {
      super.onPause();
      tearDownScreenLockTimeout();
  }

  private void tearDownScreenLockTimeout() {
    ScreenLockUtil.cancelScreenLockTimer(timer);
  }

  @Override
  public void onUserInteraction() {
    super.onUserInteraction();
    initializeScreenLockTimeout();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    return (keyCode == KeyEvent.KEYCODE_MENU && BaseActivity.isMenuWorkaroundRequired()) || super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_MENU && BaseActivity.isMenuWorkaroundRequired()) {
      openOptionsMenu();
      return true;
    }
    return super.onKeyUp(keyCode, event);
  }

  private void initializeScreenshotSecurity() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH &&
            TextSecurePreferences.isScreenSecurityEnabled(this))
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
    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(this, sharedView, transitionName)
                                         .toBundle();
    ActivityCompat.startActivity(this, intent, bundle);
  }

}
