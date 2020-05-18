package org.thoughtcrime.securesms;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
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

    private boolean isHiddenByScreenLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (BaseActivity.isMenuWorkaroundRequired()) {
            forceOverflowMenu();
        }
        super.onCreate(savedInstanceState);

        if (shouldLock()) {
            // Already hide everything here to prevent sensible data from popping up shortly:
            findViewById(android.R.id.content).setVisibility(View.INVISIBLE);
            isHiddenByScreenLock = true;
        }
    }

    private boolean shouldLock() {
        return ScreenLockUtil.isScreenLockEnabled(this) && ScreenLockUtil.getShouldLockApp();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ScreenLockUtil.REQUEST_CODE_CONFIRM_CREDENTIALS) {
            if (resultCode == RESULT_OK) {
                ScreenLockUtil.setShouldLockApp(false);
            } else {
                Toast.makeText(this, R.string.screenlock_authentication_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        if (shouldLock()) {
            ScreenLockUtil.applyScreenLock(this);
        } else if (isHiddenByScreenLock) {
            findViewById(android.R.id.content).setVisibility(View.VISIBLE);
            isHiddenByScreenLock = false;
        }
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
        if (ScreenLockUtil.isScreenLockEnabled(this)) {
            findViewById(android.R.id.content).setVisibility(View.INVISIBLE);
            isHiddenByScreenLock = true;
        }
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
                Prefs.isScreenSecurityEnabled(this)) {
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
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
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

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
        if (requestCode != -1) {
        }
    }

    public void makeSearchMenuVisible(final Menu menu, final MenuItem exception, boolean visible) {
        for (int i = 0; i < menu.size(); ++i) {
            MenuItem item = menu.getItem(i);
            int id = item.getItemId();
            if (id == R.id.menu_search_up || id == R.id.menu_search_down) {
                item.setVisible(!visible);
            } else if (id == R.id.menu_search_counter) {
                item.setVisible(false);
            } else if (item != exception) {
                item.setVisible(visible);
            }
        }
    }
}
