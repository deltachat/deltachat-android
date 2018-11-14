package org.thoughtcrime.securesms.push;

import android.content.Context;

import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;

public class AccountManagerFactory {

  public static SignalServiceAccountManager createManager(Context context) {
    return new SignalServiceAccountManager(new SignalServiceNetworkAccess(context).getConfiguration(context),
                                           TextSecurePreferences.getLocalNumber(context),
                                           TextSecurePreferences.getPushServerPassword(context),
                                           BuildConfig.USER_AGENT);
  }
}
