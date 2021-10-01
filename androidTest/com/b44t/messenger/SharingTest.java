package com.b44t.messenger;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.ShareActivity;
import org.thoughtcrime.securesms.connect.DcHelper;

import java.io.File;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
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

  @SuppressWarnings("unused")
  private final static String TAG = SharingTest.class.getSimpleName();
  private static int createdGroupId;

  @Rule
  public ActivityScenarioRule<ConversationListActivity> activityRule = TestUtils.getOfflineActivityRule();

  @Before
  public void createGroup() {
    activityRule.getScenario().onActivity(a -> {
      createdGroupId = DcHelper.getContext(a).createGroupChat(false, "group");
    });
  }

  @Test
  public void testNormalSharing() {
    Intent i = new Intent(Intent.ACTION_SEND);
    i.putExtra(Intent.EXTRA_TEXT, "Hello!");
    i.setComponent(new ComponentName(getInstrumentation().getTargetContext().getApplicationContext(), ShareActivity.class));
    activityRule.getScenario().onActivity(a -> a.startActivity(i));

    onView(withId(R.id.list)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

    onView(withHint(R.string.chat_input_placeholder)).check(matches(withText("Hello!")));
    TestUtils.pressSend();
  }

  /**
   * Test direct sharing from a screenshot.
   * Also, this is the regression test for https://github.com/deltachat/deltachat-android/issues/2040
   * where network changes during sharing lead to
   */
  @Test
  public void testShareFromScreenshot() {
    DcContext dcContext = DcHelper.getContext(getInstrumentation().getTargetContext());
    String[] files = new File(dcContext.getBlobdir()).list();
    String pngImage = null;
    assert files != null;
    for (String file : files) {
      if (file.endsWith(".png")) {
        pngImage = file;
      }
    }
    Uri uri = Uri.parse("content://" + BuildConfig.APPLICATION_ID + ".attachments/" + Uri.encode(pngImage));
    DcHelper.sharedFiles.put("/" + pngImage, 1);

    Intent i = new Intent(Intent.ACTION_SEND);
    i.setType("image/png");
    i.putExtra(Intent.EXTRA_SUBJECT, "Screenshot (Sep 27, 2021 00:00:00");
    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
            | Intent.FLAG_ACTIVITY_FORWARD_RESULT
            | Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP
            | Intent.FLAG_RECEIVER_FOREGROUND
            | Intent.FLAG_GRANT_READ_URI_PERMISSION);
    i.putExtra(Intent.EXTRA_STREAM, uri);
    i.putExtra(ShareActivity.EXTRA_CHAT_ID, createdGroupId);
    i.setComponent(new ComponentName(getInstrumentation().getTargetContext().getApplicationContext(), ShareActivity.class));
    activityRule.getScenario().onActivity(a -> a.startActivity(i));

    TestUtils.waitForView(withId(R.id.send_button), 10000, 50);

    dcContext.maybeNetwork();
    dcContext.maybeNetwork();
    dcContext.maybeNetwork();

    onView(withId(R.id.send_button)).perform(click());
    pressBack();

    onView(withId(R.id.fab)).check(matches(isClickable()));
  }

  // TODO test other things from https://github.com/deltachat/interface/blob/master/user-testing/mailto-links.md

  @After
  public void cleanup() {
    TestUtils.cleanup();
  }
}
