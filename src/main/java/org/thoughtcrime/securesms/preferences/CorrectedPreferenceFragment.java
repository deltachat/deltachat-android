package org.thoughtcrime.securesms.preferences;

import android.os.Bundle;
import android.view.View;
import androidx.preference.PreferenceFragmentCompat;

public abstract class CorrectedPreferenceFragment extends PreferenceFragmentCompat {
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    View lv = getView().findViewById(android.R.id.list);
    if (lv != null) lv.setPadding(0, 0, 0, 0);
  }
}
