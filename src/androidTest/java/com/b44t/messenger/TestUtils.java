package com.b44t.messenger;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.util.TreeIterables;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.hamcrest.Matcher;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.AccessibilityUtil;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;

public class TestUtils {
  private static int createdAccountId = 0;
  private static boolean resetEnterSends = false;

  public static void cleanupCreatedAccount(Context context) {
    DcAccounts accounts = DcHelper.getAccounts(context);
    if (createdAccountId != 0) {
      accounts.removeAccount(createdAccountId);
      createdAccountId = 0;
    }
  }

  public static void cleanup() {
    Context context = getInstrumentation().getTargetContext();
    cleanupCreatedAccount(context);
    if (resetEnterSends) {
      Prefs.setEnterSendsEnabled(getInstrumentation().getTargetContext(), false);
    }
  }

  public static void createOfflineAccount() {
    Context context = getInstrumentation().getTargetContext();
    cleanupCreatedAccount(context);
    createdAccountId = AccountManager.getInstance().beginAccountCreation(context);
    DcContext c = DcHelper.getContext(context);
    c.setConfig("configured_addr", "alice@example.org");
    c.setConfig("configured_mail_pw", "abcd");
    c.setConfig("configured", "1");
  }

  @NonNull
  public static ActivityScenarioRule<ConversationListActivity> getOfflineActivityRule(boolean useExistingChats) {
    Intent intent =
            Intent.makeMainActivity(
                    new ComponentName(getInstrumentation().getTargetContext(), ConversationListActivity.class));
    if (!useExistingChats) {
      createOfflineAccount();
    }
    prepare();
    return new ActivityScenarioRule<>(intent);
  }

  @NonNull
  public static <T extends Activity> ActivityScenarioRule<T> getOnlineActivityRule(Class<T> activityClass) {
    Context context = getInstrumentation().getTargetContext();
    AccountManager.getInstance().beginAccountCreation(context);
    prepare();
    return new ActivityScenarioRule<>(new Intent(getInstrumentation().getTargetContext(), activityClass));
  }

  private static void prepare() {
    Prefs.setBooleanPreference(getInstrumentation().getTargetContext(), Prefs.DOZE_ASKED_DIRECTLY, true);
    if (!AccessibilityUtil.areAnimationsDisabled(getInstrumentation().getTargetContext())) {
      throw new RuntimeException("To run the tests, disable animations at Developer options' " +
              "-> 'Window/Transition/Animator animation scale' -> Set all 3 to 'off'");
    }
  }

  /**
   * Perform action of waiting for a certain view within a single root view
   *
   * @param matcher Generic Matcher used to find our view
   */
  private static ViewAction searchFor(Matcher<View> matcher) {
    return new ViewAction() {

      public Matcher<View> getConstraints() {
        return isRoot();
      }

      public String getDescription() {
        return "searching for view $matcher in the root view";
      }

      public void perform(UiController uiController, View view) {

        Iterable<View> childViews = TreeIterables.breadthFirstViewTraversal(view);

        // Look for the match in the tree of childviews
        for (View it : childViews) {
          if (matcher.matches(it)) {
            // found the view
            return;
          }
        }

        throw new NoMatchingViewException.Builder()
                .withRootView(view)
                .withViewMatcher(matcher)
                .build();
      }
    };
  }

  /**
   * Perform action of implicitly waiting for a certain view.
   * This differs from EspressoExtensions.searchFor in that,
   * upon failure to locate an element, it will fetch a new root view
   * in which to traverse searching for our @param match
   *
   * @param viewMatcher ViewMatcher used to find our view
   */
  public static ViewInteraction waitForView(
          Matcher<View> viewMatcher,
          int waitMillis,
          int waitMillisPerTry
  ) {

    // Derive the max tries
    int maxTries = (int) (waitMillis / waitMillisPerTry);

    int tries = 0;

    for (int i = 0; i < maxTries; i++)
      try {
        // Track the amount of times we've tried
        tries++;

        // Search the root for the view
        onView(isRoot()).perform(searchFor(viewMatcher));

        // If we're here, we found our view. Now return it
        return onView(viewMatcher);

      } catch (Exception e) {
        if (tries == maxTries) {
          throw e;
        }
        Util.sleep(waitMillisPerTry);
      }

    throw new RuntimeException("Error finding a view matching $viewMatcher");
  }

  /**
   * Normally, you would do
   * onView(withId(R.id.send_button)).perform(click());
   * to send the draft message. However, in order to change the send button to the attach button
   * while there is no draft, the send button is made invisible and the attach button is made
   * visible instead. This confuses the test framework.<br/><br/>
   *
   * So, this is a workaround for pressing the send button.
   */
  public static void pressSend() {
    if (!Prefs.isEnterSendsEnabled(getInstrumentation().getTargetContext())) {
      resetEnterSends = true;
      Prefs.setEnterSendsEnabled(getInstrumentation().getTargetContext(), true);
    }
    waitForView(withHint(R.string.chat_input_placeholder), 10000, 100).perform(typeText("\n"));
  }
}
