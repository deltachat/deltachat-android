package com.b44t.messenger;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.util.HumanReadables;
import androidx.test.espresso.util.TreeIterables;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.PassphraseRequiredActionBarActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Prefs;
import androidx.test.espresso.contrib.RecyclerViewActions;

import java.util.concurrent.TimeoutException;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;



@RunWith(AndroidJUnit4.class)
@LargeTest
public class HelloWorldEspressoTest {
  private final static String TAG = HelloWorldEspressoTest.class.getSimpleName();

  @Rule
  public ActivityScenarioRule<ConversationListActivity> activityRule = new ActivityScenarioRule<>(getConversationsListIntent());

  private Intent getConversationsListIntent() {
    Intent intent =
            Intent.makeMainActivity(
                    new ComponentName(getInstrumentation().getTargetContext(), ConversationListActivity.class));
    intent.putExtra(PassphraseRequiredActionBarActivity.PRETEND_TO_BE_CONFIGURED, true);
    return intent;
  }

  @Test
  public void createAndEnter10Chats() {
    Prefs.setEnterSendsEnabled(getInstrumentation().getTargetContext(), true);
    createChatAndGoBack("Group #1", "Hello!", "Some links: https://testrun.org", "And a command: /help");
    createChatAndGoBack("Group #2", "example.org, alice@example.org", "aaaaaaa", "bbbbbb");
    createChatAndGoBack("Group #3", repeat("Some string ", 600), repeat("Another string", 200), "Hi!!!");
    createChatAndGoBack("Group #4", "xyzabc", "Hi!!!!", "Let's meet!");
    createChatAndGoBack("Group #5", repeat("aaaa", 40), "bbbbbbbbbbbbbbbbbb", "ccccccccccccccc");
    createChatAndGoBack("Group #6", "aaaaaaaaaaa", repeat("Hi! ", 100), "bbbbbbbbbb");
    createChatAndGoBack("Group #7", repeat("abcdefg ", 500), repeat("xxxxx", 100), "yrrrrrrrrrrrrr");
    createChatAndGoBack("Group #8", "and a number: 037362/384756", "ccccc", "Nice!");
    createChatAndGoBack("Group #9", "ddddddddddddddddd", "zuuuuuuuuuuuuuuuu", "ccccc");
    createChatAndGoBack("Group #10", repeat("xxxxxxyyyyy", 100), repeat("String!!", 10), "abcd");

    String[] times = new String[10];
    for (int i = 0; i<10; i++) {
      times[i] = "" + timeGoToAllChats();
    }
    Log.i(TAG, "MEASURED RESULTS (Benchmark): " + String.join(",", times));
  }

  private long timeGoToAllChats() {
    long start = System.currentTimeMillis();
    for (int i=0; i<10; i++) {
      onView(withId(R.id.list)).perform(RecyclerViewActions.actionOnItemAtPosition(i, click()));

      pressBack();
    }
    long end = System.currentTimeMillis();
    long diff = end - start;
    Log.i(TAG, "Measured (Benchmark): Going through all chats took " + diff + "ms");
    return diff;
  }

  private String repeat(String string, int n) {
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < n; i++) {
      s.append(string);
    }
    return s.toString();
  }

  private void createChatAndGoBack(String groupName, String text1, String text2, String text3) {
    onView(withId(R.id.fab)).perform(click());
    onView(withText(R.string.menu_new_group)).perform(click());
    onView(withHint(R.string.group_name)).perform(replaceText(groupName));
    onView(withContentDescription(R.string.group_create_button)).perform(click());
    sendText(text1);
    sendText(text2);
    sendText(text3);
    sendText(text1);
    sendText(text2);
    sendText(text3);

    pressBack();
    pressBack();
  }

  private void sendText(String text1) {
    onView(withHint(R.string.chat_input_placeholder)).perform(replaceText(text1));
    onView(withHint(R.string.chat_input_placeholder)).perform(typeText("\n"));
  }

  @After
  public void removeMockAccount() {
    Context context = getInstrumentation().getTargetContext();
    DcAccounts accounts = DcHelper.getAccounts(context);

    DcContext selectedAccount = accounts.getSelectedAccount();
    accounts.removeAccount(selectedAccount.getAccountId());

    AccountManager.getInstance().switchAccount(context, selectedAccount.getAccountId());
  }
}
