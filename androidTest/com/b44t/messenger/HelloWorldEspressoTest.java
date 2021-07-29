package com.b44t.messenger;

import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.thoughtcrime.securesms.ConversationListActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class HelloWorldEspressoTest {

  @Rule
  public ActivityScenarioRule<ConversationListActivity> activityRule =
          new ActivityScenarioRule<>(ConversationListActivity.class);

  @Rule
  public BenchmarkRule benchmarkRule = new BenchmarkRule();

  @Test
  public void listGoesOverTheFold() {
    openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
    onView(withText("Switch Account")).perform(click());
    onView(withText("Add Account")).perform(click());
  }

  @Test
  public void benchmarkSomeWork() {
    final BenchmarkState state = benchmarkRule.getState();
    while (state.keepRunning()) {
      //doSomeWork();
    }
  }
}
