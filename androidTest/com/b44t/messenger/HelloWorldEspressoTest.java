package com.b44t.messenger;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.PassphraseRequiredActionBarActivity;
import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.DcHelper;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class HelloWorldEspressoTest {

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
  public void simpleTest() {
    openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
    onView(withText("Settings")).perform(click());
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
