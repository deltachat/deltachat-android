package org.thoughtcrime.securesms.service;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.RegistrationActivity;
import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.DcHelper;

import java.lang.ref.WeakReference;

import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_ADDRESS;

import com.b44t.messenger.DcAccounts;
import com.b44t.messenger.DcContext;

/**
 * This (interprocess communication) service is invoked by companion apps aiming to add a new account to Delta Chat
 */

public class IPCAddAccountsService extends Service {
  public final static int ADD_ACCOUNT = 1;
  public final static String ACCOUNT_DATA = "ACCOUNT_DATA";

  private static final String TAG = IPCAddAccountsService.class.getSimpleName();

  private static class IncomingHandler extends Handler {
    final WeakReference<Context> contextRef;

    public IncomingHandler(Context context) {
      contextRef = new WeakReference<>(context);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
      Bundle data = msg.getData();
      Context context = contextRef.get();
      if (data != null && context != null && msg.what == ADD_ACCOUNT) {
        String newAddress = data.getString(CONFIG_ADDRESS);
        Log.d(TAG, "ADD ACCOUNT called for account: " + newAddress);

        // check if account already exists, if so, switch to that account
        DcAccounts accounts = DcHelper.getAccounts(context);
        int[] accountIds = accounts.getAll();
        for (int accountId : accountIds) {
          DcContext dcContext = accounts.getAccount(accountId);
          String accountAddress = dcContext.getConfig(CONFIG_ADDRESS);
          if (accountAddress.equals(newAddress)) {
            Log.d(TAG, newAddress + " already exists. Switching account.");
            AccountManager.getInstance().switchAccount(context, accountId);
            Intent switchAccountIntent = new Intent(context, ConversationListActivity.class);
            switchAccountIntent.addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(switchAccountIntent);
            return;
          }
        }

        AccountManager.getInstance().beginAccountCreation(context);
        Intent registrationIntent = new Intent(context, RegistrationActivity.class);
        registrationIntent.putExtra(ACCOUNT_DATA, data);
        registrationIntent.addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(registrationIntent);
      }
    }
  }

  private final Messenger messenger = new Messenger(new IncomingHandler(this));

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return messenger.getBinder();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return START_NOT_STICKY;
  }
}
