/*******************************************************************************
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


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

public class DcEventCenter {
    private Hashtable<Integer, ArrayList<DcEventDelegate>> allObservers = new Hashtable<>();
    private final Object LOCK = new Object();

    public interface DcEventDelegate {
        void handleEvent(int eventId, Object data1, Object data2);
    }

    public void addObserver(DcEventDelegate observer, int eventId) {
        synchronized (LOCK) {
            ArrayList<DcEventDelegate> idObservers = allObservers.get(eventId);
            if (idObservers == null) {
                allObservers.put(eventId, (idObservers = new ArrayList<>()));
            }
            idObservers.add(observer);
        }
    }

    public void removeObserver(DcEventDelegate observer, int eventId) {
        synchronized (LOCK) {
            ArrayList<DcEventDelegate> idObservers = allObservers.get(eventId);
            if (idObservers != null) {
                idObservers.remove(observer);
            }
        }
    }

    public void removeObservers(Object observer) {
        synchronized (LOCK) {
            Enumeration<Integer> enumKey = allObservers.keys();
            while(enumKey.hasMoreElements()) {
                Integer eventId = enumKey.nextElement();
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
                for (int i = 0; i < idObservers.size(); i++) {
                    DcEventDelegate observer = idObservers.get(i);
                    observer.handleEvent(eventId, data1, data2);
                }
            }
        }
    }
}
