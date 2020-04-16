package org.thoughtcrime.securesms.connect;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.ApplicationContext;

import java.io.File;
import java.util.ArrayList;

public class AccountManager {

    private static AccountManager self;

    private final static String DEFAULT_DB_NAME = "messenger.db";

    public class Account {
        private String dbName;
        private String displayname;
        private String addr;
        private boolean configured;
        private boolean current;

        public String getDescr(Context context) {
            String ret = "";
            if (!displayname.isEmpty() && !addr.isEmpty()) {
                ret = String.format("%s (%s)", displayname, addr);
            } else if (!addr.isEmpty()) {
                ret = addr;
            } else {
                ret = dbName;
            }
            if (!configured) {
                ret += " (not configured)";
            }
            return ret;
        }

        public boolean isCurrent() {
            return current;
        }

        public String getDbName() {
            return dbName;
        }
    };

    private @Nullable Account maybeGetAccount(File file) {
        try {
            if (!file.isDirectory() && file.getName().endsWith(".db")) {
                DcContext testContext = new DcContext(null);
                if (testContext.open(file.getAbsolutePath()) != 0) {
                    Account ret = new Account();
                    ret.dbName = file.getName();
                    ret.displayname = testContext.getConfig("displayname");
                    ret.addr = testContext.getConfig("addr");
                    ret.configured = testContext.isConfigured() != 0;
                    return ret;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private File getUniqueDbName(Context context) {
        File dir = context.getFilesDir();
        int index = 1;
        while (true) {
            File test = new File(dir, String.format("messenger-%d.db", index));
            File testBlobdir = new File(dir, String.format("messenger-%d.db-blobs", index));
            if (!test.exists() && !testBlobdir.exists()) {
                return test;
            }
            index++;
        }
    }

    private void resetDcContext(Context context) {
        // create an empty DcContext object - this will be set up then, starting with
        // getSelectedAccount()
        ApplicationContext appContext = (ApplicationContext)context.getApplicationContext();
        appContext.dcContext = new ApplicationDcContext(context);
    }


    // public api

    public static AccountManager getInstance() {
        if (self == null) {
            self = new AccountManager();
        }
        return self;
    }

    public ArrayList<Account> getAccounts(Context context) {
        ArrayList<Account> result = new ArrayList<>();

        String dbName = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("curr_account_db_name", DEFAULT_DB_NAME);

        try {
            File dir = context.getFilesDir();
            File[] files = dir.listFiles();
            for (File file : files) {
                Account account = maybeGetAccount(file);
                if (account!=null) {
                    if (account.dbName.equals(dbName)) {
                        account.current = true;
                    }
                    result.add(account);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public void switchAccount(Context context, Account account) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prevDbName = sharedPreferences.getString("curr_account_db_name", DEFAULT_DB_NAME);

        // we remember prev_account_db_name in case the account is not configured
        // and the user want to rollback to the working account
        sharedPreferences.edit().putString("prev_account_db_name", prevDbName).apply();
        sharedPreferences.edit().putString("curr_account_db_name", account.dbName).apply();
        resetDcContext(context);
    }

    public File getSelectedAccount(Context context) {
        String dbName = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("curr_account_db_name", DEFAULT_DB_NAME);
        return new File(context.getFilesDir(), dbName);
    }


    // add accounts

    public void beginAccountCreation(Context context) {
        // pause the current account and let the user create a new one.
        // this function is not needed on the very first account creation.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prevDbName = sharedPreferences.getString("curr_account_db_name", DEFAULT_DB_NAME);
        String inCreationDbName = getUniqueDbName(context).getName();

        sharedPreferences.edit().putString("prev_account_db_name", prevDbName).apply();
        sharedPreferences.edit().putString("curr_account_db_name", inCreationDbName).apply();

        resetDcContext(context);
    }

    public boolean canRollbackAccountCreation(Context context) {
        String prevDbName = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("prev_account_db_name", "");
        return !prevDbName.isEmpty();
    }

    public void rollbackAccountCreation(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prevDbName = sharedPreferences.getString("prev_account_db_name", "");
        String inCreationDbName = sharedPreferences.getString("curr_account_db_name", "");

        sharedPreferences.edit().putString("prev_account_db_name", "").apply();
        sharedPreferences.edit().putString("curr_account_db_name", prevDbName).apply();

        // delete the previous account, however, as a resilience check, make sure,
        // we do not delete already configured accounts (just in case sth. changes the flow of activities)
        DcContext testContext = new DcContext(null);
        if (testContext.open(new File(context.getFilesDir(), inCreationDbName).getAbsolutePath()) != 0) {
            if (testContext.isConfigured() == 0) {
                testContext.close();
                deleteAccount(context, inCreationDbName);
            }
        }

        resetDcContext(context);
    }


    // delete account

    public void deleteAccount(Context context, String dbName) {
        try {
            File file = new File(context.getFilesDir(), dbName);
            file.delete();

            File blobs = new File(context.getFilesDir(), dbName+"-blobs");
            blobs.delete();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
