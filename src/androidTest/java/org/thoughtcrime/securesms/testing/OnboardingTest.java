package org.thoughtcrime.securesms.testing;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.rule.IntentsRule;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import com.b44t.messenger.DcContext;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.InstantOnboardingActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.WelcomeActivity;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.testutil.Accounts;
import org.thoughtcrime.securesms.testutil.Ime;
import org.thoughtcrime.securesms.testutil.Screenshots;
import org.thoughtcrime.securesms.testutil.Shell;
import org.thoughtcrime.securesms.testutil.TestEnv;
import org.thoughtcrime.securesms.testutil.TestRelaySet;
import org.thoughtcrime.securesms.testutil.Wait;

/** Creates a *real* account on the default relays. Must start from a fresh installation. */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OnboardingTest {
  @ClassRule public static final TestRule env = TestEnv.forScreenshots();

  @Rule public final IntentsRule intents = new IntentsRule();
  @Rule public final Screenshots shots = new Screenshots();
  @Rule public final TestRelaySet testRelaySet = new TestRelaySet("ci-chatmail.testrun.org");

  private static final String NAME = "UI Test";
  private static final long ACCOUNT_CREATION_TIMEOUT_MS = 90_000;
  private static final long KEYBOARD_WAIT_LIMIT_MS = 10_000;
  private static final long TOAST_CAPTURE_DELAY_MS = 1_500;
  private static final long TOAST_CLEAR_DELAY_MS = 4_000;

  @BeforeClass
  public static void grantNotificationPermission() {
    if (Build.VERSION.SDK_INT >= 33) {
      String pkg = InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName();
      Shell.run("pm grant " + pkg + " android.permission.POST_NOTIFICATIONS");
    }
  }

  @Test
  public void signUp_withDefaultServer_reachesConversationList() {
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    // 0. Precondition, fresh installation.
    assertFalse(
        "Selected account is already configured " + context.getPackageName(),
        Accounts.anyConfigured());

    try (ActivityScenario<WelcomeActivity> ignored =
        ActivityScenario.launch(WelcomeActivity.class)) {

      // 1. Welcome
      shots.capture("Welcome_firstStart");
      onView(withId(R.id.signup_button)).check(matches(isDisplayed()));
      onView(withId(R.id.signin_button)).check(matches(isDisplayed()));

      // 2. Sign-up opens onboarding
      onView(withId(R.id.signup_button)).perform(click());
      shots.capture("InstantOnboarding_default");
      intended(hasComponent(InstantOnboardingActivity.class.getName()));
      onView(withId(R.id.name_text)).check(matches(allOf(isDisplayed(), withText(""))));
      onView(withId(R.id.provider_label))
          .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
      onView(withId(R.id.signup_button))
          .check(
              matches(
                  allOf(isDisplayed(), isEnabled(), withText(R.string.instant_onboarding_create))));

      // 2b. Empty name is rejected synchronously
      int accountsBefore = Accounts.count();
      onView(withId(R.id.signup_button)).perform(click());
      SystemClock.sleep(TOAST_CAPTURE_DELAY_MS);
      shots.capture("InstantOnboarding_emptyNameRejected");
      onView(withId(R.id.name_text)).check(matches(isDisplayed()));
      assertEquals(
          "Sign-up with empty name must not create account", accountsBefore, Accounts.count());
      assertFalse("Sign-up with empty name must not configure account", Accounts.anyConfigured());
      SystemClock.sleep(TOAST_CLEAR_DELAY_MS);

      // 3. Name filled in
      onView(withId(R.id.name_text)).perform(click(), replaceText(NAME));
      Ime.requestFor(R.id.name_text);
      Ime.waitUntilVisibleBestEffort(KEYBOARD_WAIT_LIMIT_MS);
      shots.capture("InstantOnboarding_keyboard");
      onView(withId(R.id.name_text)).check(matches(withText(NAME)));

      // 4. Keyboard closed
      onView(withId(R.id.name_text)).perform(closeSoftKeyboard());
      shots.capture("InstantOnboarding_nameEntered");

      // 5. Real account creation
      onView(withId(R.id.signup_button)).perform(click());
      Wait.forActivity(ConversationListActivity.class, ACCOUNT_CREATION_TIMEOUT_MS, shots);
      shots.capture("ConversationList_afterSignUp");
      DcContext selected = Accounts.selected();
      assertEquals("Selected account not configured after sign-up", 1, selected.isConfigured());
      assertEquals(
          "Display name after sign-up", NAME, selected.getConfig(DcHelper.CONFIG_DISPLAY_NAME));
      assertEquals(
          "Java cached account differs from core's selected account",
          selected.getAccountId(),
          DcHelper.getContext(context).getAccountId());
    }
  }
}
