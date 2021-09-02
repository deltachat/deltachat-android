package org.thoughtcrime.securesms.scribbles;

import android.os.Bundle;

import org.thoughtcrime.securesms.PassphraseRequiredActionBarActivity;
import org.thoughtcrime.securesms.R;

public class ScribbleActivity extends PassphraseRequiredActionBarActivity {
  public static final int SCRIBBLE_REQUEST_CODE       = 31424;
  ImageEditorFragment imageEditorFragment;

  @Override
  protected void onCreate(Bundle savedInstanceState, boolean ready) {
    setContentView(R.layout.scribble_activity);
    imageEditorFragment = initFragment(R.id.scribble_container, ImageEditorFragment.newInstance(getIntent().getData()));
  }

/*  @Override
  public void onBackPressed() {
    if (!imageEditorFragment.onBackPressed()) {
      super.onBackPressed();
    }
  } */
}
