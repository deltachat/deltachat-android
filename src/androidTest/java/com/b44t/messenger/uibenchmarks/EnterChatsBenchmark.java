package com.b44t.messenger.uibenchmarks;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.util.Log;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.b44t.messenger.TestUtils;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;

@Ignore("This is not a test, but a benchmark. Remove the @Ignore to run it.")
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EnterChatsBenchmark {

  // ==============================================================================================
  // Set this to true if you already have at least 10 chats on your existing DeltaChat installation
  // and want to traverse through them instead of 10 newly created chats
  private final static boolean USE_EXISTING_CHATS = false;
  // ==============================================================================================
  private final static int GO_THROUGH_ALL_CHATS_N_TIMES = 8;

  // ==============================================================================================
  // PLEASE BACKUP YOUR ACCOUNT BEFORE RUNNING THIS!
  // ==============================================================================================

  private final static String TAG = EnterChatsBenchmark.class.getSimpleName();

  @Rule
  public ActivityScenarioRule<ConversationListActivity> activityRule = TestUtils.getOfflineActivityRule(USE_EXISTING_CHATS);

  @Test
  public void createAndEnter10FilledChats() {
    create10Chats(true);

    String[] times = new String[GO_THROUGH_ALL_CHATS_N_TIMES];
    for (int i = 0; i < GO_THROUGH_ALL_CHATS_N_TIMES; i++) {
      times[i] = "" + timeGoToNChats(10); // 10 group chats were created
    }
    Log.i(TAG, "MEASURED RESULTS (Benchmark) - Going thorough all 10 chats: " + String.join(",", times));
  }

  @Test
  public void createAndEnterEmptyChats() {
    create10Chats(false);

    String[] times = new String[GO_THROUGH_ALL_CHATS_N_TIMES];
    for (int i = 0; i < GO_THROUGH_ALL_CHATS_N_TIMES; i++) {
      times[i] = "" + timeGoToNChats(1);
    }
    Log.i(TAG, "MEASURED RESULTS (Benchmark) - Entering and leaving 1 empty chat: " + String.join(",", times));
  }

  @Test
  public void enterFilledChat() {
    if (!USE_EXISTING_CHATS) {
      createChatAndGoBack("Group #1", true, "Hello!", "Some links: https://testrun.org", "And a command: /help");
    }

    String[] times = new String[50];
    for (int i = 0; i < times.length; i++) {
      long start = System.currentTimeMillis();
      onView(withId(R.id.list)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
      long end = System.currentTimeMillis();
      long diff = end - start;
      pressBack();
      Log.i(TAG, "Measured (Benchmark) " + (i+1) + "/" + times.length + ": Entering 1 filled chat took " + diff + "ms " + "(going back took " + (System.currentTimeMillis() - end) + "ms)");

      times[i] = "" + diff;
    }
    Log.i(TAG, "MEASURED RESULTS (Benchmark) - Entering 1 filled chat: " + String.join(",", times));
  }

  private void create10Chats(boolean fillWithMsgs) {
    if (!USE_EXISTING_CHATS) {
      createChatAndGoBack("Group #1", fillWithMsgs, "Hello!", "Some links: https://testrun.org", "And a command: /help");
      createChatAndGoBack("Group #2", fillWithMsgs, "example.org, alice@example.org", "aaaaaaa", "bbbbbb");
      createChatAndGoBack("Group #3", fillWithMsgs, repeat("Some string ", 600), repeat("Another string", 200), "Hi!!!");
      createChatAndGoBack("Group #4", fillWithMsgs, "xyzabc", "Hi!!!!", "Let's meet!");
      createChatAndGoBack("Group #5", fillWithMsgs, repeat("aaaa", 40), "bbbbbbbbbbbbbbbbbb", "ccccccccccccccc");
      createChatAndGoBack("Group #6", fillWithMsgs, "aaaaaaaaaaa", repeat("Hi! ", 1000), "bbbbbbbbbb");
      createChatAndGoBack("Group #7", fillWithMsgs, repeat("abcdefg ", 500), repeat("xxxxx", 100), "yrrrrrrrrrrrrr");
      createChatAndGoBack("Group #8", fillWithMsgs, "and a number: 037362/384756", "ccccc", "Nice!");
      createChatAndGoBack("Group #9", fillWithMsgs, "ddddddddddddddddd", "zuuuuuuuuuuuuuuuu", "ccccc");
      createChatAndGoBack("Group #10", fillWithMsgs, repeat("xxxxxxyyyyy", 100), repeat("String!!", 10), "abcd");
    }
  }

  private long timeGoToNChats(int numChats) {
    long start = System.currentTimeMillis();
    for (int i = 0; i < numChats; i++) {
      onView(withId(R.id.list)).perform(RecyclerViewActions.actionOnItemAtPosition(i, click()));
      pressBack();
    }
    long diff = System.currentTimeMillis() - start;
    Log.i(TAG, "Measured (Benchmark): Going through " + numChats + " chats took " + diff + "ms");
    return diff;
  }

  private String repeat(String string, int n) {
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < n; i++) {
      s.append(string);
    }
    return s.toString();
  }

  private void createChatAndGoBack(String groupName, boolean fillWithMsgs, String... texts) {
    onView(withId(R.id.fab)).perform(click());
    onView(withText(R.string.menu_new_group)).perform(click());
    onView(withHint(R.string.name_desktop)).perform(replaceText(groupName));
    onView(withContentDescription(R.string.group_create_button)).perform(click());

    if (fillWithMsgs) {
      for (String t: texts) {
        sendText(t);
      }
      for (String t: texts) {
        sendText(t);
      }
    }

    pressBack();
    pressBack();
  }

  private void sendText(String text1) {
    onView(withHint(R.string.chat_input_placeholder)).perform(replaceText(text1));
    TestUtils.pressSend();
  }

  @After
  public void cleanup() {
    TestUtils.cleanup();
  }
}
