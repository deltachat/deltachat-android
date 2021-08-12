package com.b44t.messenger;

import android.content.Context;
import android.view.View;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.util.TreeIterables;

import org.hamcrest.Matcher;
import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Util;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

class TestUtils {
  public static void removeAccount() {
    Context context = getInstrumentation().getTargetContext();
    DcAccounts accounts = DcHelper.getAccounts(context);

    DcContext selectedAccount = accounts.getSelectedAccount();
    accounts.removeAccount(selectedAccount.getAccountId());
  }


  public static void createOfflineAccount() {
    Context context = getInstrumentation().getTargetContext();
    AccountManager.getInstance().beginAccountCreation(context);
    DcContext c = DcHelper.getContext(context);
    c.setConfig("configured_addr", "alice@example.org");
    c.setConfig("configured_mail_pw", "abcd");
    c.setConfig("configured", "1");
  }

  /**
   * Perform action of waiting for a certain view within a single root view
   *
   * @param matcher Generic Matcher used to find our view
   */
  static ViewAction searchFor(Matcher<View> matcher) {
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

}
