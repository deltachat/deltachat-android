package org.thoughtcrime.securesms.connect;

import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_VERIFIED_ONE_ON_ONE_CHATS;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.b44t.messenger.DcAccounts;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.InstantOnboardingActivity;
import org.thoughtcrime.securesms.WelcomeActivity;
import org.thoughtcrime.securesms.accounts.AccountSelectionListFragment;
import org.thoughtcrime.securesms.crypto.DatabaseSecret;
import org.thoughtcrime.securesms.crypto.DatabaseSecretProvider;

import java.io.File;

public class AccountManager {

    private static final String TAG = AccountManager.class.getSimpleName();
    private static final String LAST_ACCOUNT_ID = "last_account_id";
    private static AccountManager self;

    private void resetDcContext(Context context) {
        ApplicationContext appContext = (ApplicationContext)context.getApplicationContext();
        appContext.dcContext = appContext.dcAccounts.getSelectedAccount();
        DcHelper.setStockTranslations(context);
        DcHelper.getContext(context).setConfig(CONFIG_VERIFIED_ONE_ON_ONE_CHATS, "1");
        DirectShareUtil.resetAllShortcuts(appContext);
    }


    // public api

    public static AccountManager getInstance() {
        if (self == null) {
            self = new AccountManager();
        }
        return self;
    }

    public void migrateToDcAccounts(ApplicationContext context) {
        try {
            int selectAccountId = 0;

            File[] files = context.getFilesDir().listFiles();
            for (File file : files) {
                // old accounts have the pattern "messenger*.db"
                if (!file.isDirectory() && file.getName().startsWith("messenger") && file.getName().endsWith(".db")) {
                    int accountId = context.dcAccounts.migrateAccount(file.getAbsolutePath());
                    if (accountId != 0) {
                        String selName = PreferenceManager.getDefaultSharedPreferences(context)
                                .getString("curr_account_db_name", "messenger.db");
                        if (file.getName().equals(selName)) {
                            // postpone selection as it will otherwise be overwritten by the next migrateAccount() call
                            // (if more than one account needs to be migrated)
                            selectAccountId = accountId;
                        }
                    }
                }
            }

            if (selectAccountId != 0) {
                context.dcAccounts.selectAccount(selectAccountId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void switchAccount(Context context, int accountId) {
        DcHelper.getAccounts(context).selectAccount(accountId);
        resetDcContext(context);
    }


    // add accounts

    public int beginAccountCreation(Context context) {
        DcAccounts accounts = DcHelper.getAccounts(context);
        DcContext selectedAccount = accounts.getSelectedAccount();
        if (selectedAccount.isOk()) {
          PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(LAST_ACCOUNT_ID, selectedAccount.getAccountId()).apply();
        }

        int id = accounts.addAccount();
        resetDcContext(context);
        return id;
    }

    public boolean canRollbackAccountCreation(Context context) {
        return DcHelper.getAccounts(context).getAll().length > 1;
    }

    public void rollbackAccountCreation(Activity activity) {
        DcAccounts accounts = DcHelper.getAccounts(activity);

        DcContext selectedAccount = accounts.getSelectedAccount();
        if (selectedAccount.isConfigured() == 0) {
          accounts.removeAccount(selectedAccount.getAccountId());
        }

        int lastAccountId = PreferenceManager.getDefaultSharedPreferences(activity).getInt(LAST_ACCOUNT_ID, 0);
        if (lastAccountId == 0 || !accounts.getAccount(lastAccountId).isOk()) {
            lastAccountId = accounts.getSelectedAccount().getAccountId();
        }
        switchAccountAndStartActivity(activity, lastAccountId);
    }

    public void switchAccountAndStartActivity(Activity activity, int destAccountId) {
        if (destAccountId==0) {
            beginAccountCreation(activity);
        } else {
            switchAccount(activity, destAccountId);
        }

        activity.finishAffinity();
        if (destAccountId==0) {
            Intent intent = new Intent(activity, WelcomeActivity.class);
            activity.startActivity(intent);
        } else {
            activity.startActivity(new Intent(activity.getApplicationContext(), ConversationListActivity.class));
        }
    }

    /**
     * Deletes the current account (must be unconfigured) and creates an encrypted account instead.
     * @throws IllegalStateException if the currently selected account is already configured.
     */
    public void switchToEncrypted(Activity activity) {
        Log.i(TAG, "Switching to encrypted account...");
        DcAccounts accounts = DcHelper.getAccounts(activity);
        DcContext selectedAccount = accounts.getSelectedAccount();
        if (selectedAccount.isConfigured() == 1) {
            throw new IllegalStateException("Can't switch to encrypted account if already configured");
        }

        int selectedAccountId = selectedAccount.getAccountId();
        accounts.addClosedAccount();
        accounts.removeAccount(selectedAccountId);

        DcContext newAccount = accounts.getSelectedAccount();
        DatabaseSecret secret = DatabaseSecretProvider.getOrCreateDatabaseSecret(activity, newAccount.getAccountId());
        newAccount.open(secret.asString());
        resetDcContext(activity);
    }

    // ui

    public void showSwitchAccountMenu(Activity activity) {
        AccountSelectionListFragment dialog = new AccountSelectionListFragment();
        dialog.show(((FragmentActivity) activity).getSupportFragmentManager(), null);
    }

    public void addAccountFromQr(Activity activity, String qr) {
        beginAccountCreation(activity);
        activity.finishAffinity();
        Intent intent = new Intent(activity, InstantOnboardingActivity.class);
        intent.putExtra(InstantOnboardingActivity.QR_ACCOUNT_EXTRA, qr);
        activity.startActivity(intent);
    }
}
