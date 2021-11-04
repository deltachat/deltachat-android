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

import org.thoughtcrime.securesms.RegistrationActivity;
import org.thoughtcrime.securesms.connect.AccountManager;

import java.lang.ref.WeakReference;

import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_ADDRESS;

/**
 * This service is invoked by companion apps aiming to add a new account to Delta Chat
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
        Log.d(TAG, "ADD ACCOUNT called for account: " + data.getString(CONFIG_ADDRESS));
        AccountManager.getInstance().beginAccountCreation(context);
        Intent registrationIntent = new Intent(context, RegistrationActivity.class);
        registrationIntent.putExtra(ACCOUNT_DATA, data);
        registrationIntent.addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(registrationIntent);
      } else {
        super.handleMessage(msg);
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
