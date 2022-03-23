package org.thoughtcrime.securesms;

import static org.thoughtcrime.securesms.util.RelayUtil.REQUEST_RELAY;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;

import androidx.fragment.app.Fragment;

/**
 * "Relaying" means "Forwarding or Sharing".
 *
 * When forwarding or sharing, we show the ConversationListActivity to the user.
 * However, ConversationListActivity has `launchMode="singleTask"`, which means that this will
 * destroy the existing ConversationListActivity.
 *
 * In API 20-29, `startActivityForResult()` could be used instead of `startActivity()`
 * to override this behavior and get two instances of ConversationListActivity.
 *
 * As this is not possible anymore starting with API 30, we needed another solution, and created
 * this activity here.
 *
 * See https://github.com/deltachat/deltachat-android/issues/1704.
 */

public class ConversationListRelayingActivity extends ConversationListActivity {
  public static void start(Fragment fragment, Intent intent) {
    intent.setComponent(new ComponentName(fragment.getContext(), ConversationListRelayingActivity.class));
    fragment.startActivityForResult(intent, REQUEST_RELAY);
  }

  public static void start(Activity activity, Intent intent) {
    intent.setComponent(new ComponentName(activity, ConversationListRelayingActivity.class));
    activity.startActivityForResult(intent, REQUEST_RELAY);
  }
}
