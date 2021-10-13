package org.thoughtcrime.securesms.connect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.b44t.messenger.DcAccounts;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.WelcomeActivity;
import org.thoughtcrime.securesms.accounts.AccountSelectionListFragment;
import org.thoughtcrime.securesms.notifications.NotificationCenter;
import org.thoughtcrime.securesms.util.task.ProgressDialogAsyncTask;

import java.io.File;
import java.lang.ref.WeakReference;

public class AccountManager {

    private static final String TAG = AccountManager.class.getSimpleName();
    private static AccountManager self;

    private void resetDcContext(Context context) {
        ApplicationContext appContext = (ApplicationContext)context.getApplicationContext();
        DcHelper.getNotificationCenter(context).removeAllNotifiations();
        appContext.dcContext = appContext.dcAccounts.getSelectedAccount();
        appContext.notificationCenter = new NotificationCenter(context);
        appContext.eventCenter = new DcEventCenter(context);
        DcHelper.setStockTranslations(context);
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
        int id = DcHelper.getAccounts(context).addAccount();
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

        switchAccountAndStartActivity(activity, accounts.getSelectedAccount().getAccountId(), null);
    }

    public void switchAccountAndStartActivity(Activity activity, int destAccountId, @Nullable String qrAccount) {
        if (destAccountId==0) {
            beginAccountCreation(activity);
        } else {
            switchAccount(activity, destAccountId);
        }

        activity.finishAffinity();
        if (destAccountId==0) {
            Intent intent = new Intent(activity, WelcomeActivity.class);
            if (qrAccount!=null) {
                intent.putExtra(WelcomeActivity.QR_ACCOUNT_EXTRA, qrAccount);
            }
            activity.startActivity(intent);
        } else {
            activity.startActivity(new Intent(activity.getApplicationContext(), ConversationListActivity.class));
        }
    }

    // ui

    public void showSwitchAccountMenu(Activity activity) {
        AccountSelectionListFragment dialog = new AccountSelectionListFragment();
        dialog.show(((FragmentActivity) activity).getSupportFragmentManager(), null);
    }

    public void addAccountFromQr(Activity activity, String qr) {
        switchAccountAndStartActivity(activity, 0, qr);
    }
}
