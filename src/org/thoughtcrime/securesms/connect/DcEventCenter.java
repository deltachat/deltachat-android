package org.thoughtcrime.securesms.connect;

import com.b44t.messenger.DcEvent;

import org.thoughtcrime.securesms.util.Util;

import java.util.ArrayList;
import java.util.Hashtable;

public class DcEventCenter {
    private Hashtable<Integer, ArrayList<DcEventDelegate>> allObservers = new Hashtable<>();
    private final Object LOCK = new Object();

    public interface DcEventDelegate {
        void handleEvent(DcEvent event);
        default boolean runOnMain() {
            return true;
        }
    }

    /**
     * @deprecated use addObserver(int, DcEventDelegate) instead.
     */
    @Deprecated
    public void addObserver(DcEventDelegate observer, int eventId) {
        addObserver(eventId, observer);
    }

    public void addObserver(int eventId, DcEventDelegate observer) {
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

    public void sendToObservers(DcEvent event) {
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
}
