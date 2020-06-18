package org.thoughtcrime.securesms.util;

import android.app.Activity;

import org.thoughtcrime.securesms.R;

public class DynamicNoActionBarTheme extends DynamicTheme {
  @Override
  protected int getSelectedTheme(Activity activity) {
    String theme = Prefs.getTheme(activity);

    if (theme.equals(DynamicTheme.DARK)) return R.style.TextSecure_DarkNoActionBar;
    if (theme.equals(DynamicTheme.PURPLE)) return R.style.TextSecure_PurpleNoActionBar;
    if (theme.equals(DynamicTheme.GREEN)) return R.style.TextSecure_GreenNoActionBar;
    if (theme.equals(DynamicTheme.BLUE)) return R.style.TextSecure_BlueNoActionBar;
    if (theme.equals(DynamicTheme.RED)) return R.style.TextSecure_RedNoActionBar;
    if (theme.equals(DynamicTheme.PINK)) return R.style.TextSecure_PinkNoActionBar;

    return R.style.TextSecure_LightNoActionBar;
  }
}
