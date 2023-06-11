package org.thoughtcrime.securesms.scribbles;

import android.os.Bundle;

import org.thoughtcrime.securesms.PassphraseRequiredActionBarActivity;
import org.thoughtcrime.securesms.R;

public class ScribbleActivity extends PassphraseRequiredActionBarActivity {
  public static final int SCRIBBLE_REQUEST_CODE       = 31424;
  public static final String CROP_AVATAR              = "crop_avatar";
  ImageEditorFragment imageEditorFragment;

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
