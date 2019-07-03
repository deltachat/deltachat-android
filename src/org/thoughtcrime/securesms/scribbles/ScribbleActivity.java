package org.thoughtcrime.securesms.scribbles;

import android.os.Bundle;

import org.thoughtcrime.securesms.PassphraseRequiredActionBarActivity;
import org.thoughtcrime.securesms.R;

public class ScribbleActivity extends PassphraseRequiredActionBarActivity {
  public static final int SCRIBBLE_REQUEST_CODE       = 31424;

  @Override
  protected void onCreate(Bundle savedInstanceState, boolean ready) {
    setContentView(R.layout.scribble_activity);
    initFragment(R.id.scribble_container, ImageEditorFragment.newInstance(getIntent().getData()));
  }

}
