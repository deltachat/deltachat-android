package com.b44t.messenger;

import android.content.Intent;
import android.net.Uri;

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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;


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
    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("Hallo!"));
    activityRule.getScenario().onActivity(a -> a.startActivity(i));
    onView(withId(R.id.list)).perform(RecyclerViewActions.actionOnItem(withText(R.string.saved_messages), click()));

  }

  @After
  public void cleanup() {
      TestUtils.cleanup();
  }
}
