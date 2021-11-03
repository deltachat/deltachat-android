package org.thoughtcrime.securesms.service;

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

import java.lang.ref.WeakReference;

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
      Log.d(TAG, "handle Message");
      if (msg.what == ADD_ACCOUNT) {
        Log.d(TAG, "ADD ACCOUNT called");
        Bundle data = msg.getData();
        Context context = contextRef.get();
        if (data == null || context == null) {
          // ignore
          super.handleMessage(msg);
          return;
        }
        Intent registrationIntent = new Intent(context, RegistrationActivity.class);
        registrationIntent.putExtra(ACCOUNT_DATA, data);
        registrationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(registrationIntent);
      } else {
        super.handleMessage(msg);
      }
    }
  }

  private final Messenger messenger = new Messenger(new IncomingHandler(this));

  /**
   * Return the communication channel to the service.  May return null if
   * clients can not bind to the service.  The returned
   * {@link IBinder} is usually for a complex interface
   * that has been <a href="{@docRoot}guide/components/aidl.html">described using
   * aidl</a>.
   *
   * <p><em>Note that unlike other application components, calls on to the
   * IBinder interface returned here may not happen on the main thread
   * of the process</em>.  More information about the main thread can be found in
   * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html">Processes and
   * Threads</a>.</p>
   *
   * @param intent The Intent that was used to bind to this service,
   *               as given to {@link Context#bindService
   *               Context.bindService}.  Note that any extras that were included with
   *               the Intent at that point will <em>not</em> be seen here.
   * @return Return an IBinder through which clients can call on to the
   * service.
   */
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
