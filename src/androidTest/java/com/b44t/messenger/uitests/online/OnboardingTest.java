package com.b44t.messenger.uitests.online;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.text.TextUtils;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.b44t.messenger.TestUtils;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.WelcomeActivity;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class OnboardingTest {
  @Rule
  public ActivityScenarioRule<WelcomeActivity> activityRule = TestUtils.getOnlineActivityRule(WelcomeActivity.class);

  @Test
  public void testAccountCreation() {
    if (TextUtils.isEmpty(BuildConfig.TEST_ADDR) || TextUtils.isEmpty(BuildConfig.TEST_MAIL_PW)) {
      throw new RuntimeException("You need to set TEST_ADDR and TEST_MAIL_PW; " +
              "either in gradle.properties or via an environment variable. " +
              "See README.md for more details.");
    }
    onView(withText(R.string.scan_invitation_code)).check(matches(isClickable()));
    onView(withText(R.string.import_backup_title)).check(matches(isClickable()));
    onView(withText(R.string.manual_account_setup_option)).perform(click());
    onView(withHint(R.string.email_address)).perform(replaceText(BuildConfig.TEST_ADDR));
    onView(withHint(R.string.existing_password)).perform(replaceText(BuildConfig.TEST_MAIL_PW));
    onView(withContentDescription(R.string.ok)).perform(click());
    TestUtils.waitForView(withText(R.string.app_name), 10000, 100);

    // TODO: Try to also perform other steps of the release checklist at
    // https://github.com/deltachat/deltachat-android/blob/master/docs/release-checklist.md#testing-checklist
  }

  @After
  public void cleanup() {
    TestUtils.cleanup();
  }
}
