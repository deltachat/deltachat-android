package org.thoughtcrime.securesms.testing;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.os.Build;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.WelcomeActivity;
import org.thoughtcrime.securesms.testutil.Ime;
import org.thoughtcrime.securesms.testutil.Screenshots;
import org.thoughtcrime.securesms.testutil.Shell;
import org.thoughtcrime.securesms.testutil.TestEnv;
import org.thoughtcrime.securesms.testutil.Wait;

/**
 * Creates a *real* account on the default server (currently nine). Must start from a fresh
 * installation.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OnboardingTest {
  @ClassRule public static final TestRule env = TestEnv.forScreenshots();

  @Rule public final Screenshots shots = new Screenshots();

  private static final String NAME = "UI Test";
  private static final long ACCOUNT_CREATION_TIMEOUT_MS = 90_000;
  private static final long KEYBOARD_WAIT_LIMIT_MS = 10_000;

  @BeforeClass
  public static void grantNotificationPermission() {
    if (Build.VERSION.SDK_INT >= 33) {
      String pkg = InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName();
      Shell.run("pm grant " + pkg + " android.permission.POST_NOTIFICATIONS");
    }
  }

  @Test
  public void signUp_defaultServer_reachesConversationList() {
    try (ActivityScenario<WelcomeActivity> ignored =
        ActivityScenario.launch(WelcomeActivity.class)) {

      shots.capture("Welcome_firstStart");

      onView(withId(R.id.signup_button)).perform(click());
      onView(withId(R.id.name_text)).check(matches(isDisplayed()));
      shots.capture("InstantOnboarding_default");

      onView(withId(R.id.name_text)).perform(click(), replaceText(NAME));
      Ime.requestFor(R.id.name_text);
      Ime.waitUntilVisibleBestEffort(KEYBOARD_WAIT_LIMIT_MS);
      shots.capture("InstantOnboarding_keyboard");

      onView(withId(R.id.name_text)).perform(closeSoftKeyboard());
      shots.capture("InstantOnboarding_nameEntered");

      onView(withId(R.id.signup_button)).perform(click());
      Wait.forActivity(ConversationListActivity.class, ACCOUNT_CREATION_TIMEOUT_MS, shots);
      shots.capture("ConversationList_afterSignUp");
    }
  }
}
