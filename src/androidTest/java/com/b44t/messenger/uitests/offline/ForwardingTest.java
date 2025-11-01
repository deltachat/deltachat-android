package com.b44t.messenger.uitests.offline;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import androidx.test.espresso.IdlingPolicies;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.TestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;

import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ForwardingTest {
  private static int createdGroupId;

  @BeforeClass
  public static void beforeClass() {
    IdlingPolicies.setMasterPolicyTimeout(10, TimeUnit.SECONDS);
    IdlingPolicies.setIdlingResourceTimeout(10, TimeUnit.SECONDS);
  }

  @Rule
  public final ActivityScenarioRule<ConversationListActivity> activityRule = TestUtils.getOfflineActivityRule(false);

  @Before
  public void createChats() {
    DcContext dcContext = DcHelper.getContext(getInstrumentation().getTargetContext());
    dcContext.createChatByContactId(DcContact.DC_CONTACT_ID_SELF);
    // Disable bcc_self so that DC doesn't try to send messages to the server.
    // If we didn't do this, messages would stay in DC_STATE_OUT_PENDING forever.
    // The thing is, DC_STATE_OUT_PENDING show a rotating circle animation, and Espresso doesn't work
    // with animations, and the tests would hang and never finish.
    dcContext.setConfig("bcc_self", "0");
    activityRule.getScenario().onActivity(a -> createdGroupId = DcHelper.getContext(a).createGroupChat( "group"));
  }

  @After
  public void cleanup() {
    TestUtils.cleanup();
  }

  @Test
  public void testSimpleForwarding() {
    // Open device talk
    // The group is at position 0, self chat is at position 1, device talk is at position 2
    onView(withId(R.id.list)).perform(RecyclerViewActions.actionOnItemAtPosition(2, click()));
    onView(withId(R.id.title)).check(matches(withText(R.string.device_talk)));
    onView(withId(android.R.id.list)).perform(RecyclerViewActions.actionOnItemAtPosition(0, longClick()));
    onView(withId(R.id.menu_context_forward)).perform(click());
    // Send it to self chat (which is sorted to the top because we're forwarding)
    onView(withId(R.id.list)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

    onView(withId(R.id.title)).check(matches(withText(R.string.device_talk)));

    pressBack();

    onView(withId(R.id.toolbar_title)).check(matches(withText(R.string.connectivity_not_connected)));
    // Self chat moved up because we sent a message there
    onView(withId(R.id.list)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
    onView(withId(R.id.title)).check(matches(withText(R.string.saved_messages)));
    onView(withId(android.R.id.list)).perform(RecyclerViewActions.actionOnItemAtPosition(0, longClick()));
    onView(withId(R.id.menu_context_forward)).perform(click());
    // Send it to the group
    onView(withId(R.id.list)).perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));
    onView(withText(android.R.string.ok)).perform(click());
    onView(withId(R.id.title)).check(matches(withText("group")));

    pressBack();
    onView(withId(R.id.toolbar_title)).check(matches(withText(R.string.connectivity_not_connected)));
  }
}
