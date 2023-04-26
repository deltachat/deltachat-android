package org.thoughtcrime.securesms;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import java.lang.ref.WeakReference;

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
  static WeakReference<ConversationListRelayingActivity> INSTANCE = null;

  @Override
  protected void onCreate(Bundle icicle, boolean ready) {
    super.onCreate(icicle, ready);
    INSTANCE = new WeakReference<>(this);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    INSTANCE = null;
  }

  // =================== Static Methods ===================
  public static void start(Fragment fragment, Intent intent) {
    intent.setComponent(new ComponentName(fragment.getContext(), ConversationListRelayingActivity.class));
    fragment.startActivity(intent);
  }

  public static void start(Activity activity, Intent intent) {
    intent.setComponent(new ComponentName(activity, ConversationListRelayingActivity.class));
    activity.startActivity(intent);
  }

  public static void finishActivity() {
    if (INSTANCE != null && INSTANCE.get() != null) {
      INSTANCE.get().finish();
    }
  }
}
