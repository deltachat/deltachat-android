package com.b44t.messenger.uitests.offline;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.TestUtils;

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


@RunWith(AndroidJUnit4.class)
@LargeTest
public class SharingTest {
  // ==============================================================================================
  // PLEASE BACKUP YOUR ACCOUNT BEFORE RUNNING THIS!
  // ==============================================================================================

  private static int createdGroupId;
  private static int createdSingleChatId;

  @Rule
  public final ActivityScenarioRule<ConversationListActivity> activityRule = TestUtils.getOfflineActivityRule(false);

  @Before
  public void createGroup() {
    activityRule.getScenario().onActivity(a -> createdGroupId = DcHelper.getContext(a).createGroupChat( "group"));
  }

  @Before
  public void createSingleChat() {
    activityRule.getScenario().onActivity(a -> {
      int contactId = DcHelper.getContext(a).createContact("", "abc@example.org");
      createdSingleChatId = DcHelper.getContext(a).createChatByContactId(contactId);
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
   * where network changes during sharing lead to a bug
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
    DcHelper.sharedFiles.put(pngImage, "image/png");

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

  /**
   * Tests https://github.com/deltachat/interface/blob/master/user-testing/mailto-links.md#mailto-links:
   *
   * <ul dir="auto">
   * <li><a href="mailto:abc@example.org">Just an email address</a> - should open a chat with <code>abc@example.org</code> (and maybe ask whether a chat should be created if it does not exist already)</li>
   * <li><a href="mailto:abc@example.org?subject=testing%20mailto%20uris">email address with subject</a> - should open a chat with <code>abc@example.org</code> and fill <code>testing mailto uris</code>; as we created the chat in the previous step, it should not ask <code>Chat with …</code> but directly open the chat</li>
   * <li><a href="mailto:abc@example.org?body=this%20is%20a%20test">email address with body</a> - should open a chat with <code>abc@example.org</code>, draft <code>this is a test</code></li>
   * <li><a href="mailto:abc@example.org?subject=testing%20mailto%20uris&amp;body=this%20is%20a%20test">email address with subject and body</a> - should open a chat with <code>abc@example.org</code>, draft <code>testing mailto uris</code> &lt;newline&gt; <code>this is a test</code></li>
   * <li><a href="mailto:%20info@example.org">HTML encoding</a> - should open a chat with <code>info@example.org</code></li>
   * <li><a href="mailto:simplebot@example.org?body=!web%20https%3A%2F%2Fduckduckgo.com%2Flite%3Fq%3Dduck%2520it">more HTML encoding</a> - should open a chat with <code>simplebot@example.org</code>, draft <code>!web https://duckduckgo.com/lite?q=duck%20it</code></li>
   * <li><a href="mailto:?subject=bla&amp;body=blub">no email, just subject&amp;body</a> - this should let you choose a chat and create a draft <code>bla</code> &lt;newline&gt; <code>blub</code> there</li>
   * </ul>
   */
  @Test
  public void testShareFromLink() {
    openLink("mailto:abc@example.org");
    onView(withId(R.id.subtitle)).check(matches(withText("abc@example.org")));

    openLink("mailto:abc@example.org?subject=testing%20mailto%20uris");
    onView(withId(R.id.subtitle)).check(matches(withText("abc@example.org")));
    onView(withHint(R.string.chat_input_placeholder)).check(matches(withText("testing mailto uris")));

    openLink("mailto:abc@example.org?body=this%20is%20a%20test");
    onView(withId(R.id.subtitle)).check(matches(withText("abc@example.org")));
    onView(withHint(R.string.chat_input_placeholder)).check(matches(withText("this is a test")));

    openLink("mailto:abc@example.org?subject=testing%20mailto%20uris&body=this%20is%20a%20test");
    onView(withId(R.id.subtitle)).check(matches(withText("abc@example.org")));
    onView(withHint(R.string.chat_input_placeholder)).check(matches(withText("testing mailto uris\nthis is a test")));

    openLink("mailto:%20abc@example.org");
    onView(withId(R.id.subtitle)).check(matches(withText("abc@example.org")));

    openLink("mailto:abc@example.org?body=!web%20https%3A%2F%2Fduckduckgo.com%2Flite%3Fq%3Dduck%2520it");
    onView(withId(R.id.subtitle)).check(matches(withText("abc@example.org")));
    onView(withHint(R.string.chat_input_placeholder)).check(matches(withText("!web https://duckduckgo.com/lite?q=duck%20it")));

    openLink("mailto:?subject=bla&body=blub");
    onView(withId(R.id.list)).perform(RecyclerViewActions.actionOnItem(hasDescendant(withText("abc@example.org")), click()));
    onView(withId(R.id.subtitle)).check(matches(withText("abc@example.org")));
    onView(withHint(R.string.chat_input_placeholder)).check(matches(withText("bla\nblub")));
  }

  private void openLink(String link) {
    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
    i.setPackage(getInstrumentation().getTargetContext().getPackageName());
    activityRule.getScenario().onActivity(a -> a.startActivity(i));
  }

  /**
   * <ul dir="auto">
   * <li>Open Saved Messages chat (could be any other chat too)</li>
   * <li>Go to another app and share some text to DC</li>
   * <li>In DC select Saved Messages. Edit the shared text if you like. <em>Don't</em> hit the Send button.</li>
   * <li>Leave DC</li>
   * <li>Open DC again from the "Recent apps"</li>
   * <li>Check that your draft is still there</li>
   * </ul>
   */
  @Test
  public void testOpenAgainFromRecents() {
    // Open a chat
    onView(withId(R.id.list)).perform(RecyclerViewActions.actionOnItem(hasDescendant(withText("abc@example.org")), click()));

    // Share some text to DC
    Intent i = new Intent(Intent.ACTION_SEND);
    i.putExtra(Intent.EXTRA_TEXT, "Veeery important draft");
    i.setComponent(new ComponentName(getInstrumentation().getTargetContext().getApplicationContext(), ShareActivity.class));
    activityRule.getScenario().onActivity(a -> a.startActivity(i));

    // In DC, select the same chat you opened before
    onView(withId(R.id.list)).perform(RecyclerViewActions.actionOnItem(hasDescendant(withText("abc@example.org")), click()));

    // Leave DC and go back to the previous activity
    pressBack();

    // Here, we can't exactly replicate the "steps to reproduce". Previously, the other activity
    // stayed open in the background, but since it doesn't anymore, we need to open it again:
    onView(withId(R.id.list)).perform(RecyclerViewActions.actionOnItem(hasDescendant(withText("abc@example.org")), click()));

    // Check that the draft is still there
    // Util.sleep(2000);  // Uncomment for debugging
    onView(withHint(R.string.chat_input_placeholder)).check(matches(withText("Veeery important draft")));
  }

  /**
   * Regression test:
   *
   * If you save your contacts's emails in the contacts app of the phone, there are buttons to call
   * them and also to write an email to them.
   *
   * If you click the email button, Delta Chat opened but instead of opening a chat with that contact,
   * the chat list was show and "share with" was displayed at the top
   */
  @Test
  public void testOpenChatFromContacts() {
    Intent i = new Intent(Intent.ACTION_SENDTO);
    i.setData(Uri.parse("mailto:bob%40example.org"));
    i.setPackage(getInstrumentation().getTargetContext().getPackageName());
    activityRule.getScenario().onActivity(a -> a.startActivity(i));

    onView(withId(R.id.subtitle)).check(matches(withText("bob@example.org")));
  }

  @After
  public void cleanup() {
    TestUtils.cleanup();
  }
}
