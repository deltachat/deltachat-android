package org.thoughtcrime.securesms.connect;

import android.content.Context;

import androidx.annotation.Nullable;

import com.b44t.messenger.DcContext;

import java.io.File;
import java.util.ArrayList;

public class AccountManager {

    private static AccountManager self;

    public static AccountManager getInstance() {
        if (self == null) {
            self = new AccountManager();
        }
        return self;
    }

    public class Account {
        private String file;
        private String displayname;
        private String addr;
        private boolean configured;
        public String getDescr() {
            String ret = "";
            if (!displayname.isEmpty() && !addr.isEmpty()) {
                ret = String.format("%s (%s)", displayname, addr);
            } else if (!addr.isEmpty()) {
                ret = addr;
            } else {
                ret = file;
            }
            if (!configured) {
                ret += " (not configured)";
            }
            return ret;
        }
    };

    public @Nullable Account maybeGetAccount(File file) {
        try {
            if (!file.isDirectory() && file.getName().endsWith(".db")) {
                DcContext testContext = new DcContext(null);
                if (testContext.open(file.getAbsolutePath()) != 0) {
                    Account ret = new Account();
                    ret.file = file.getAbsolutePath();
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

    public ArrayList<Account> getAccounts(Context context) {
        ArrayList<Account> result = new ArrayList<>();

        try {
            File dir = context.getFilesDir();
            File[] files = dir.listFiles();
            for (File file : files) {
                Account account = maybeGetAccount(file);
                if (account!=null) {
                    result.add(account);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}
