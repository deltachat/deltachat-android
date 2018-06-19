/*******************************************************************************
 *
 *                              Delta Chat Android
 *                        (C) 2013-2016 Nikolai Kudashov
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


package com.b44t.messenger.Components;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.b44t.messenger.ApplicationLoader;

@SuppressLint("NewApi")
public class ForegroundDetector implements Application.ActivityLifecycleCallbacks {

    private int refs = 0;
    private boolean wasInBackground = true;
    private long enterBackgroundTime = 0;
    private static ForegroundDetector Instance = null;

    public static ForegroundDetector getInstance() {
        return Instance;
    }

    public ForegroundDetector(Application application) {
        Instance = this;
        application.registerActivityLifecycleCallbacks(this);
    }

    public boolean isForeground() {
        return refs > 0;
    }

    public boolean isBackground() {
        return refs == 0;
    }


    @Override
    public void onActivityStarted(Activity activity) {
        Log.i("DeltaChat", "-------------------- Activity started --------------------");

        refs++;
        if (refs == 1) {
            if (System.currentTimeMillis() - enterBackgroundTime < 200) {
                wasInBackground = false;
            }
        }

        //ApplicationLoader.imapForeground = true;
        ApplicationLoader.startThreads(); // we call this without checking getPermanentPush() to have a simple guarantee that push is always active when the app is in foregroud (startIdleThread makes sure the thread is not started twice)
    }

    public boolean isWasInBackground(boolean reset) {
        if (reset && Build.VERSION.SDK_INT >= 21 && (System.currentTimeMillis() - enterBackgroundTime < 200)) {
            wasInBackground = false;
        }
        return wasInBackground;
    }

    public void resetBackgroundVar() {
        wasInBackground = false;
    }

    @Override
    public void onActivityStopped(Activity activity) {

        Log.i("DeltaChat", "-------------------- Activity stopped --------------------");

        if( refs <= 0 ) {
            Log.i("DeltaChat", String.format("Bad activity count: activityCount=%d", refs));
            return;
        }

        refs--;
        if (refs == 0) {
            enterBackgroundTime = System.currentTimeMillis();
            wasInBackground = true;

            /*if( !ApplicationLoader.getPermanentPush() ) {
                ApplicationLoader.scheduleStopImapThread();;
            }*/
        }
        else {
            Log.i("DeltaChat", String.format("Not yet switched to background, %d activities left", refs));
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
        // pause/resume will also be called when the app is partially covered by a dialog
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}
