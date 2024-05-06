package org.thoughtcrime.securesms.scribbles;

import android.os.Bundle;

import org.thoughtcrime.securesms.PassphraseRequiredActionBarActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;

public class ScribbleActivity extends PassphraseRequiredActionBarActivity {
  public static final int SCRIBBLE_REQUEST_CODE       = 31424;
  public static final String CROP_AVATAR              = "crop_avatar";
  ImageEditorFragment imageEditorFragment;

  protected boolean allowInLockedMode() {
    return getIntent().getBooleanExtra(CROP_AVATAR, false);
  }

  @Override
  protected void onPreCreate() {
    dynamicTheme = new DynamicNoActionBarTheme();
    super.onPreCreate();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState, boolean ready) {
    setContentView(R.layout.scribble_activity);
    boolean cropAvatar = getIntent().getBooleanExtra(CROP_AVATAR, false);
    imageEditorFragment = initFragment(R.id.scribble_container, ImageEditorFragment.newInstance(getIntent().getData(), cropAvatar));
  }

/*  @Override
  public void onBackPressed() {
    if (!imageEditorFragment.onBackPressed()) {
      super.onBackPressed();
    }
  } */
}
