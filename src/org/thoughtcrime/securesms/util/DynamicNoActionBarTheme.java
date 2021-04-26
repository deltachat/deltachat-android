package org.thoughtcrime.securesms.util;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import org.thoughtcrime.securesms.R;

public class DynamicNoActionBarTheme extends DynamicTheme {
  protected @StyleRes int getLightThemeStyle(@NonNull String theme) {
    if (theme.equals(PURPLE)) return R.style.TextSecure_PurpleNoActionBar;
    if (theme.equals(GREEN)) return R.style.TextSecure_GreenNoActionBar;
    if (theme.equals(BLUE)) return R.style.TextSecure_BlueNoActionBar;
    if (theme.equals(RED)) return R.style.TextSecure_RedNoActionBar;
    if (theme.equals(PINK)) return R.style.TextSecure_PinkNoActionBar;
    if (theme.equals(INDIGO)) return R.style.TextSecure_IndigoNoActionBar;
    return R.style.TextSecure_LightNoActionBar;
  }

  protected @StyleRes int getDarkThemeStyle(@NonNull String theme) {
    if (theme.equals(PURPLE)) return R.style.TextSecure_PurpleDarkNoActionBar;
    if (theme.equals(GREEN)) return R.style.TextSecure_GreenDarkNoActionBar;
    if (theme.equals(BLUE)) return R.style.TextSecure_BlueDarkNoActionBar;
    if (theme.equals(RED)) return R.style.TextSecure_RedDarkNoActionBar;
    if (theme.equals(PINK)) return R.style.TextSecure_PinkDarkNoActionBar;
    if (theme.equals(INDIGO)) return R.style.TextSecure_IndigoDarkNoActionBar;
    return R.style.TextSecure_DarkNoActionBar;
  }
}
