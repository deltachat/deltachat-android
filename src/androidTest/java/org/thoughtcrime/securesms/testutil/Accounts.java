package org.thoughtcrime.securesms.testutil;

import androidx.test.platform.app.InstrumentationRegistry;
import com.b44t.messenger.DcAccounts;
import com.b44t.messenger.DcContext;
import org.thoughtcrime.securesms.connect.DcHelper;

public final class Accounts {
  private Accounts() {}

  private static DcAccounts accounts() {
    return DcHelper.getAccounts(InstrumentationRegistry.getInstrumentation().getTargetContext());
  }

  public static int count() {
    return accounts().getAll().length;
  }

  public static boolean anyConfigured() {
    DcAccounts accounts = accounts();
    for (int id : accounts.getAll()) {
      if (accounts.getAccount(id).isConfigured() == 1) return true;
    }
    return false;
  }

  public static DcContext selected() {
    return accounts().getSelectedAccount();
  }
}
