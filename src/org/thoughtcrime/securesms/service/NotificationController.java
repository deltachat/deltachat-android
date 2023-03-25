package org.thoughtcrime.securesms.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Class to control notifications triggered by GenericForeGroundService.
 *
 */
public final class NotificationController {

  private final @NonNull Context context;
  private final int id;

  private int     progress;
  private int     progressMax;
  private boolean indeterminate;
  private String  message = "";
  private long    percent = -1;

  private final ServiceConnection serviceConnection;

  private final AtomicReference<GenericForegroundService> service = new AtomicReference<>();

  NotificationController(@NonNull Context context, int id) {
    this.context = context;
    this.id      = id;

    serviceConnection = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName name, IBinder service) {
        GenericForegroundService.LocalBinder binder = (GenericForegroundService.LocalBinder) service;
        GenericForegroundService genericForegroundService = binder.getService();

        NotificationController.this.service.set(genericForegroundService);

        updateProgressOnService();
      }

      @Override
      public void onServiceDisconnected(ComponentName name) {
        service.set(null);
      }
    };

    context.bindService(new Intent(context, GenericForegroundService.class), serviceConnection, Context.BIND_AUTO_CREATE);
  }

  public int getId() {
    return id;
  }

  public void close() {
    try {
      GenericForegroundService.stopForegroundTask(context, id);
      context.unbindService(serviceConnection);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  public void setIndeterminateProgress() {
    setProgress(0, 0, true, message);
  }

  public void setProgress(long newProgressMax, long newProgress, @NonNull String newMessage) {
    setProgress((int) newProgressMax, (int) newProgress, false, newMessage);
  }

  private synchronized void setProgress(int newProgressMax, int newProgress, boolean indeterminant, @NonNull String newMessage) {
    int newPercent = newProgressMax != 0 ? 100 * newProgress / newProgressMax : -1;

    boolean same = newPercent == percent && indeterminate == indeterminant && newMessage.equals(message);

    percent       = newPercent;
    progress      = newProgress;
    progressMax   = newProgressMax;
    indeterminate = indeterminant;
    message       = newMessage;

    if (same) return;

    updateProgressOnService();
  }

  private synchronized void updateProgressOnService() {
    GenericForegroundService genericForegroundService = service.get();

    if (genericForegroundService == null) return;

    genericForegroundService.replaceProgress(id, progressMax, progress, indeterminate,  message);
  }
}
