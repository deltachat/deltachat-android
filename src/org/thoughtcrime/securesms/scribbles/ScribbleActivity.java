package org.thoughtcrime.securesms.scribbles;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import org.thoughtcrime.securesms.PassphraseRequiredActionBarActivity;
import org.thoughtcrime.securesms.R;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class ScribbleActivity extends PassphraseRequiredActionBarActivity {
  public static final int SCRIBBLE_REQUEST_CODE       = 31424;

  @Override
  protected void onCreate(Bundle savedInstanceState, boolean ready) {
    setContentView(R.layout.scribble_activity);
    initFragment(R.id.scribble_container, ImageEditorFragment.newInstance(getIntent().getData()));

    if (Build.VERSION.SDK_INT >= 19) {
      getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN       |
              View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
              View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }
  }

}
