package org.thoughtcrime.securesms;


import android.app.Activity;

public class ClearProfileAvatarActivity extends Activity {

  @Override
  public void onResume() {
    super.onResume();

//    new AlertDialog.Builder(this)
//        .setTitle(R.string.pref_profile_photo_remove_ask)
//        .setNegativeButton(android.R.string.cancel, (dialog, which) -> finish())
//        .setPositiveButton(R.string.ok, (dialog, which) -> {
//          Intent result = new Intent();
//          result.putExtra("delete", true);
//          setResult(Activity.RESULT_OK, result);
//          finish();
//        })
//        .show(); // TODO
  }

}
