package com.b44t.messenger;

import android.content.ComponentName;
import android.content.Intent;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.ShareActivity;
import org.thoughtcrime.securesms.connect.DcHelper;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class SharingTest {
  // ==============================================================================================
  // PLEASE BACKUP YOUR ACCOUNT BEFORE RUNNING THIS!
  // ==============================================================================================

  private final static String TAG = SharingTest.class.getSimpleName();

  @Rule
  public ActivityScenarioRule<ConversationListActivity> activityRule = TestUtils.getOfflineActivityRule();

  @Test
  public void testNormalSharing() {
    activityRule.getScenario().onActivity(a -> {
      DcHelper.getContext(a).createGroupChat(false, "group");
    });

    Intent i = new Intent(Intent.ACTION_SEND);
    i.putExtra(Intent.EXTRA_TEXT, "Hello!");
    i.setComponent(new ComponentName(getInstrumentation().getTargetContext().getApplicationContext(), ShareActivity.class));
    activityRule.getScenario().onActivity(a -> a.startActivity(i));

    onView(withId(R.id.list)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

    onView(withHint(R.string.chat_input_placeholder)).check(matches(withText("Hello!")));
    TestUtils.pressSend();
  }

  // TODO test other things from https://github.com/deltachat/interface/blob/master/user-testing/mailto-links.md

  @After
  public void cleanup() {
    TestUtils.cleanup();
  }
}
