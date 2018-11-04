/* *****************************************************************************
 *
 *                           Delta Chat Java Adapter
 *                           (C) 2017 Björn Petersen
 *                    Contact: r10s@b44t.com, http://b44t.com
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see http://www.gnu.org/licenses/ .
 *
 ******************************************************************************/


package com.b44t.messenger;


import android.os.AsyncTask;

import org.thoughtcrime.securesms.util.Util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Hashtable;

public class DcEventCenter {
    private Hashtable<Integer, ArrayList<DcEventDelegate>> allObservers = new Hashtable<>();
    private final Object LOCK = new Object();

    public interface DcEventDelegate {
        void handleEvent(int eventId, Object data1, Object data2);
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

    public void sendToObservers(int eventId, Object data1, Object data2) {
        synchronized (LOCK) {
            ArrayList<DcEventDelegate> idObservers = allObservers.get(eventId);
            if (idObservers != null) {
                for (DcEventDelegate observer : idObservers) {
                    if(observer.runOnMain()) {
                        Util.runOnMain(() -> observer.handleEvent(eventId, data1, data2));
                    } else {
                        new BackgroundEventHandler(observer, eventId)
                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, data1, data2);
                    }
                }
            }
        }
    }

    private static class BackgroundEventHandler extends AsyncTask<Object, Void, Void> {
        private final WeakReference<DcEventDelegate> asyncDelegate;
        private final int eventId;
        BackgroundEventHandler(DcEventDelegate delegate, int eventId) {
            asyncDelegate = new WeakReference<>(delegate);
            this.eventId = eventId;
        }
        @Override
        protected Void doInBackground(Object... data) {
            DcEventDelegate delegate = asyncDelegate.get();
            if(delegate != null) {
                delegate.handleEvent(eventId, data[0], data[1]);
            }
            return null;
        }
    }
}
