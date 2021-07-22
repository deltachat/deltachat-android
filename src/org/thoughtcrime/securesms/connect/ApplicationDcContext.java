package org.thoughtcrime.securesms.connect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcEventEmitter;
import com.b44t.messenger.DcLot;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.database.model.ThreadRecord;
import org.thoughtcrime.securesms.notifications.NotificationCenter;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;

import java.io.File;
import java.util.Date;
import java.util.HashMap;

public class ApplicationDcContext extends DcContext {

  public static final String TAG = "DeltaChat";

  public Context context;
  public NotificationCenter notificationCenter;

  public ApplicationDcContext(Context context) {
    super("Android "+BuildConfig.VERSION_NAME, AccountManager.getInstance().getSelectedAccount(context).getAbsolutePath());
    this.context = context;

    new Thread(() -> {
      DcEventEmitter emitter = getEventEmitter();
      while (true) {
        DcEvent event = emitter.getNextEvent();
        if (event==null) {
          break;
        }
        handleEvent(event);
      }
      Log.i(TAG, "shutting down event handler");
    }, "eventThread").start();

    notificationCenter = new NotificationCenter(this);
    maybeStartIo();
  }

  public void maybeStartIo() {
    Log.i("DeltaChat", "++++++++++++++++++ ApplicationDcContext.maybeStartIo() ++++++++++++++++++");
    if (isConfigured()!=0) {
      startIo();
    }
  }

  /***********************************************************************************************
   * Event Handling
   **********************************************************************************************/

  public DcEventCenter eventCenter = new DcEventCenter();

  private final Object lastErrorLock = new Object();
  private String lastErrorString = "";
  private boolean showNextErrorAsToast = true;
  public boolean showNetworkErrors = true; // set to false if one network error was reported while having no internet

  public void captureNextError() {
    synchronized (lastErrorLock) {
      showNextErrorAsToast = false;
      lastErrorString = "";
    }
  }

  public boolean hasCapturedError() {
    synchronized (lastErrorLock) {
      return !lastErrorString.isEmpty();
    }
  }

  public String getCapturedError() {
    synchronized (lastErrorLock) {
      return lastErrorString;
    }
  }

  public void endCaptureNextError() {
    synchronized (lastErrorLock) {
      showNextErrorAsToast = true;
    }
  }

  private void handleError(int event, String string) {
    // log error
    boolean showAsToast;
    Log.e(TAG, string);
    synchronized (lastErrorLock) {
      lastErrorString = string;
      showAsToast = showNextErrorAsToast;
      showNextErrorAsToast = true;
    }

    // show error to user
    Util.runOnMain(() -> {
      if (showAsToast) {
        String toastString = null;

        if (event == DC_EVENT_ERROR_SELF_NOT_IN_GROUP) {
          toastString = context.getString(R.string.group_self_not_in_group);
        }

        ForegroundDetector foregroundDetector = ForegroundDetector.getInstance();
        if (toastString != null && (foregroundDetector == null || foregroundDetector.isForeground())) {
          Toast.makeText(context, toastString, Toast.LENGTH_LONG).show();
        }
      }
    });
  }

  public long handleEvent(DcEvent event) {
    int id = event.getId();
    switch (id) {
      case DC_EVENT_INFO:
        Log.i(TAG, event.getData2Str());
        break;

      case DC_EVENT_WARNING:
        Log.w(TAG, event.getData2Str());
        break;

      case DC_EVENT_ERROR:
        handleError(id, event.getData2Str());
        break;

      case DC_EVENT_ERROR_SELF_NOT_IN_GROUP:
        handleError(id, event.getData2Str());
        break;

      case DC_EVENT_INCOMING_MSG:
        notificationCenter.addNotification(event.getData1Int(), event.getData2Int());
        if (eventCenter != null) {
          eventCenter.sendToObservers(event);
        }
        break;

      case DC_EVENT_MSGS_NOTICED:
        notificationCenter.removeNotifications(event.getData1Int());
        if (eventCenter != null) {
          eventCenter.sendToObservers(event);
        }
        break;

      default: {
        if (eventCenter != null) {
          eventCenter.sendToObservers(event);
        }
      }
      break;
    }
    return 0;
  }

}
