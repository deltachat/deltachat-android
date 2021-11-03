package org.thoughtcrime.securesms.connect;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.Util;

import java.util.ArrayList;
import java.util.Hashtable;

public class DcEventCenter {
    private @NonNull final Hashtable<Integer, ArrayList<DcEventDelegate>> allObservers = new Hashtable<>();
    private final Object LOCK = new Object();
    private final @NonNull ApplicationContext context;

    public interface DcEventDelegate {
        void handleEvent(@NonNull DcEvent event);
        default boolean runOnMain() {
            return true;
        }
    }

    public DcEventCenter(@NonNull Context context) {
        this.context = ApplicationContext.getInstance(context);
    }

    public void addObserver(int eventId, @NonNull DcEventDelegate observer) {
        synchronized (LOCK) {
            ArrayList<DcEventDelegate> idObservers = allObservers.get(eventId);
            if (idObservers == null) {
                allObservers.put(eventId, (idObservers = new ArrayList<>()));
            }
            idObservers.add(observer);
        }
    }

    public void removeObserver(int eventId, DcEventDelegate observer) {
        synchronized (LOCK) {
            ArrayList<DcEventDelegate> idObservers = allObservers.get(eventId);
            if (idObservers != null) {
                idObservers.remove(observer);
            }
        }
    }

    public void removeObservers(DcEventDelegate observer) {
        synchronized (LOCK) {
            for(Integer eventId : allObservers.keySet()) {
                ArrayList<DcEventDelegate> idObservers = allObservers.get(eventId);
                if (idObservers != null) {
                    idObservers.remove(observer);
                }
            }
        }
    }

    public void sendToObservers(@NonNull DcEvent event) {
        synchronized (LOCK) {
            ArrayList<DcEventDelegate> idObservers = allObservers.get(event.getId());
            if (idObservers != null) {
                for (DcEventDelegate observer : idObservers) {
                    // using try/catch blocks as under some circumstances eg. getContext() may return NULL -
                    // and as this function is used virtually everywhere, also in libs,
                    // it's not feasible to check all single occurrences.
                    if(observer.runOnMain()) {
                        Util.runOnMain(() -> {
                            try {
                                observer.handleEvent(event);
                            }
                            catch(Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } else {
                        Util.runOnBackground(() -> {
                            try {
                                observer.handleEvent(event);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            }
        }
    }

  private final Object lastErrorLock = new Object();
  private boolean showNextErrorAsToast = true;

  public void captureNextError() {
    synchronized (lastErrorLock) {
      showNextErrorAsToast = false;
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
    Log.e("DeltaChat", string);
    synchronized (lastErrorLock) {
      showAsToast = showNextErrorAsToast;
      showNextErrorAsToast = true;
    }

    // show error to user
    Util.runOnMain(() -> {
      if (showAsToast) {
        String toastString = null;

        if (event == DcContext.DC_EVENT_ERROR_SELF_NOT_IN_GROUP) {
          toastString = context.getString(R.string.group_self_not_in_group);
        }

        ForegroundDetector foregroundDetector = ForegroundDetector.getInstance();
        if (toastString != null && (foregroundDetector == null || foregroundDetector.isForeground())) {
          Toast.makeText(context, toastString, Toast.LENGTH_LONG).show();
        }
      }
    });
  }

  public long handleEvent(@NonNull DcEvent event) {
    int accountId = event.getAccountId();
    if (accountId != context.dcContext.getAccountId()) {
      return 0;
    }

    int id = event.getId();
    switch (id) {
      case DcContext.DC_EVENT_INFO:
        Log.i("DeltaChat", event.getData2Str());
        break;

      case DcContext.DC_EVENT_WARNING:
        Log.w("DeltaChat", event.getData2Str());
        break;

      case DcContext.DC_EVENT_ERROR:
        handleError(id, event.getData2Str());
        break;

      case DcContext.DC_EVENT_ERROR_SELF_NOT_IN_GROUP:
        handleError(id, event.getData2Str());
        break;

      case DcContext.DC_EVENT_INCOMING_MSG:
        DcHelper.getNotificationCenter(context).addNotification(event.getData1Int(), event.getData2Int());
        sendToObservers(event);
        break;

      case DcContext.DC_EVENT_MSGS_NOTICED:
        DcHelper.getNotificationCenter(context).removeNotifications(event.getData1Int());
        sendToObservers(event);
        break;

      default: {
        sendToObservers(event);
      }
      break;
    }

    if (id == DcContext.DC_EVENT_CHAT_MODIFIED) {
      // Possibly a chat was deleted or the avatar was changed, directly refresh DirectShare so that
      // a new chat can move up / the chat avatar change is populated
      DirectShareUtil.triggerRefreshDirectShare(context);
    }

    return 0;
  }
}
