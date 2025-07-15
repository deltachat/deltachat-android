package org.thoughtcrime.securesms;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;

import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.Prefs;

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
    EdgeToEdge.enable(this);  // docs says to use: WindowCompat.enableEdgeToEdge(getWindow()); but it is not available
    super.onCreate(savedInstanceState);
    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false); // force white text in status bar
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
