package com.b44t.messenger;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.thoughtcrime.securesms.ConversationListActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class HelloWorldEspressoTest {

  @Rule
  public ActivityScenarioRule<ConversationListActivity> activityRule =
          new ActivityScenarioRule<>(ConversationListActivity.class);

  @Test
  public void listGoesOverTheFold() {
    onView(withText("Hello world!")).check(matches(isDisplayed()));
  }
}
