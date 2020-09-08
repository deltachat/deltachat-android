package org.thoughtcrime.securesms;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.thoughtcrime.securesms.service.GenericForegroundService;

// if there is a ongoing task running,
// we might not be able to create new activities as eg. the database is blocked.
// therefore, we do not want to create new activities
// but just show the existing one with the progress dialog.
// (this is a little hack, in theory, the ongoing task should be decoupled completely from activities,
// and activities should be recreated as needed and reflect the ongoing task state -
// however, handling all that is quite some effort)
//
// - for normal activities, aborting creation is easy - just call finish() in onCreate()
//
// - for singleTask activities (as ConversationListActivity),
//   however, the system just calls onNewIntent() - with the activity stack already altered,
//   so there is no way to abort getting to a weird state if the new activity cannot be shown
//   because of the ongoing task (eg. database blocked by backup)
//
// therefore, this little RoutingActivity exist - it is a non-singleTask activity
// that creates the singleTask ConversationListActivity if possible.
public class RoutingActivity extends Activity {
  @Override
  protected final void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!GenericForegroundService.isForegroundTaskStarted()) {
      Intent intent = new Intent(this, ConversationListActivity.class);
      startActivity(intent);
    }

    finish();
  }
}
