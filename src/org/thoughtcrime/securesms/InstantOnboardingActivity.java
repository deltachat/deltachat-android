package org.thoughtcrime.securesms;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class InstantOnboardingActivity extends BaseActionBarActivity {

  private static final String TAG = InstantOnboardingActivity.class.getSimpleName();

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);

    setContentView(R.layout.instant_onboarding_activity);

    getSupportActionBar().setTitle(R.string.instant_onboarding_title);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;
      default:
        return false;
    }
  }

}
