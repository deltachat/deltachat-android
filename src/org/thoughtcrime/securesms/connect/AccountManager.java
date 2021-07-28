package org.thoughtcrime.securesms.connect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.b44t.messenger.DcAccounts;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.WelcomeActivity;
import org.thoughtcrime.securesms.notifications.NotificationCenter;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.task.ProgressDialogAsyncTask;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

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
            File[] files = context.getFilesDir().listFiles();
            for (File file : files) {
                // old accounts have the pattern "messenger*.db"
                if (!file.isDirectory() && file.getName().startsWith("messenger") && file.getName().endsWith(".db")) {
                    int accountId = context.dcAccounts.migrateAccount(file.getAbsolutePath());
                    if (accountId != 0) {
                        String selName = PreferenceManager.getDefaultSharedPreferences(context)
                                .getString("curr_account_db_name", "messenger.db");
                        if (file.getName().equals(selName)) {
                            context.dcAccounts.selectAccount(accountId);
                        }
                    }
                }
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

    private void beginAccountCreation(Context context) {
        DcHelper.getAccounts(context).addAccount();
        resetDcContext(context);
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

        new SwitchAccountAsyncTask(activity, R.string.switching_account, accounts.getSelectedAccount().getAccountId(), null).execute();
    }


    // helper class for switching accounts gracefully

    private static class SwitchAccountAsyncTask extends ProgressDialogAsyncTask<Void, Void, Void> {
        private final WeakReference<Activity> activityWeakReference;
        private final int destAccountId; // 0 creates a new account
        private final @Nullable String qrAccount;

        public SwitchAccountAsyncTask(Activity activity, int title, int destAccountId, @Nullable String qrAccount) {
            super(activity, null, activity.getString(title));
            this.activityWeakReference = new WeakReference<>(activity);
            this.destAccountId = destAccountId;
            this.qrAccount = qrAccount;
        }
        @Override
        protected Void doInBackground(Void... voids) {
            Activity activity = activityWeakReference.get();
            if (activity!=null) {
                if (destAccountId==0) {
                    AccountManager.getInstance().beginAccountCreation(activity);
                } else {
                    AccountManager.getInstance().switchAccount(activity, destAccountId);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Activity activity = activityWeakReference.get();
            if (activity!=null) {
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
        }
    }

    // ui

    public void showSwitchAccountMenu(Activity activity) {
        DcAccounts accounts = DcHelper.getAccounts(activity);
        int[] accountIds = accounts.getAll();
        int selectedAccountId = accounts.getSelectedAccount().getAccountId();

        // build menu
        int presel = 0;
        ArrayList<String> menu = new ArrayList<>();
        for (int i = 0; i < accountIds.length; i++) {
            DcContext context = accounts.getAccount(accountIds[i]);
            if (accountIds[i] == selectedAccountId) {
                presel = i;
            }
            menu.add(context.getNameNAddr());
        }

        int addAccount = menu.size();
        menu.add(activity.getString(R.string.add_account));

        // show dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(R.string.switch_account)
                .setNegativeButton(R.string.cancel, null)
                .setSingleChoiceItems(menu.toArray(new String[0]), presel, (dialog, which) -> {
                    dialog.dismiss();
                    if (which==addAccount) {
                        new SwitchAccountAsyncTask(activity, R.string.one_moment, 0, null).execute();
                    } else { // switch account
                        if (accountIds[which] != selectedAccountId) {
                            new SwitchAccountAsyncTask(activity, R.string.switching_account, accountIds[which], null).execute();
                        }
                    }
                });
        if (accountIds.length > 1) {
            builder.setNeutralButton(R.string.delete_account, (dialog, which) -> {
                showDeleteAccountMenu(activity);
            });
        }
        builder.show();
    }

    private void showDeleteAccountMenu(Activity activity) {
        DcAccounts accounts = DcHelper.getAccounts(activity);
        int[] accountIds = accounts.getAll();
        int selectedAccountId = accounts.getSelectedAccount().getAccountId();

        ArrayList<String> menu = new ArrayList<>();
        for (int accountId : accountIds) {
            menu.add(accounts.getAccount(accountId).getNameNAddr());
        }
        int[] selection = {-1};
        new AlertDialog.Builder(activity)
                .setTitle(R.string.delete_account)
                .setSingleChoiceItems(menu.toArray(new String[menu.size()]), -1, (dialog, which) -> selection[0] = which)
                .setNegativeButton(R.string.cancel, (dialog, which) -> showSwitchAccountMenu(activity))
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    if (selection[0] >= 0 && selection[0] < accountIds.length) {
                        int accountId = accountIds[selection[0]];
                        if (accountId == selectedAccountId) {
                            new AlertDialog.Builder(activity)
                                    .setMessage("To delete the currently active account, switch to another account first.")
                                    .setPositiveButton(R.string.ok, null)
                                    .show();
                        } else {
                            new AlertDialog.Builder(activity)
                                    .setTitle(accounts.getAccount(accountId).getNameNAddr())
                                    .setMessage(R.string.forget_login_confirmation_desktop)
                                    .setNegativeButton(R.string.cancel, (dialog2, which2) -> showSwitchAccountMenu(activity))
                                    .setPositiveButton(R.string.ok, (dialog2, which2) -> {
                                        accounts.removeAccount(accountId);
                                        showSwitchAccountMenu(activity);
                                    })
                                    .show();
                        }
                    } else {
                        showDeleteAccountMenu(activity);
                    }
                })
                .show();
    }

    public void addAccountFromQr(Activity activity, String qr) {
        new SwitchAccountAsyncTask(activity, R.string.one_moment, 0, qr).execute();
    }
}
