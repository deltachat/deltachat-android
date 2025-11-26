package org.thoughtcrime.securesms;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;

import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.lang.reflect.Field;


public abstract class BaseActionBarActivity extends AppCompatActivity {

  private static final String TAG = BaseActionBarActivity.class.getSimpleName();
  protected DynamicTheme dynamicTheme = new DynamicTheme();

  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    onPreCreate();
    EdgeToEdge.enable(this);  // TODO: docs says to use: WindowCompat.enableEdgeToEdge(getWindow()); but it requires dep androidx.core:core:1.17.0 which in turns requires to target SDK 36
    super.onCreate(savedInstanceState);
    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false); // force white text in status bar
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    // Apply window insets for edge-to-edge display
    // The toolbar/app bar should extend behind the status bar with padding applied
    View toolbar = findViewById(R.id.toolbar);
    if (toolbar != null) {
      // Check if toolbar is inside an AppBarLayout
      View parent = (View) toolbar.getParent();
      if (parent instanceof com.google.android.material.appbar.AppBarLayout) {
        ViewUtil.applyWindowInsets(parent, true, true, true, false);
      } else {
        ViewUtil.applyWindowInsets(toolbar, true, true, true, false);
      }
    }

    // For activities without a custom toolbar, apply insets to status_bar_background view
    View statusBarBackground = findViewById(R.id.status_bar_background);
    if (statusBarBackground != null) {
      ViewUtil.applyWindowInsetsAsHeight(statusBarBackground);
      ActionBar actionBar = getSupportActionBar();
      if (actionBar != null) {
        // elevation is set via status_bar_background view
        // otherwise there is a drop-shadow at the top
        actionBar.setElevation(0);
      }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    initializeScreenshotSecurity();
    dynamicTheme.onResume(this);
  }

  private void initializeScreenshotSecurity() {
    if (Prefs.isScreenSecurityEnabled(this)) {
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
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

  public void makeSearchMenuVisible(final Menu menu, final MenuItem searchItem, boolean visible) {
    for (int i = 0; i < menu.size(); ++i) {
      MenuItem item = menu.getItem(i);
      int id = item.getItemId();
      if (id == R.id.menu_search_up || id == R.id.menu_search_down) {
        item.setVisible(visible);
      } else if (id == R.id.menu_search_counter) {
        item.setVisible(false); // always hide menu_search_counter initially
      } else if (item == searchItem) {
        ; // searchItem is just always visible
      } else {
        item.setVisible(!visible); // if search is shown, other items are hidden - and the other way round
      }
    }
  }

  protected <T extends Fragment> T initFragment(@IdRes int target,
                                                @NonNull T fragment)
  {
    return initFragment(target, fragment, null);
  }

  protected <T extends Fragment> T initFragment(@IdRes int target,
                                                @NonNull T fragment,
                                                @Nullable Bundle extras)
  {
    Bundle args = new Bundle();

    if (extras != null) {
      args.putAll(extras);
    }

    fragment.setArguments(args);
    getSupportFragmentManager().beginTransaction()
      .replace(target, fragment)
      .commitAllowingStateLoss();
    return fragment;
  }
}
