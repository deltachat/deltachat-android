/*
 * Copyright (C) 2018 Delta Chat contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.util.Util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class ApplicationDcContext extends DcContext {

    public Context context;

    public ApplicationDcContext(Context context) {
        super("Android");
        this.context = context;

        // create wake locks
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            imapWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "imapWakeLock");
            imapWakeLock.setReferenceCounted(false); // if the idle-thread is killed for any reasons, it is better not to rely on reference counting

            smtpWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "smtpWakeLock");
            smtpWakeLock.setReferenceCounted(false); // if the idle-thread is killed for any reasons, it is better not to rely on reference counting

            afterForgroundWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "afterForegroundWakeLock");
            afterForgroundWakeLock.setReferenceCounted(false);

        } catch (Exception e) {
            Log.e("DeltaChat", "Cannot create wakeLocks");
        }

        new ForegroundDetector(ApplicationContext.getInstance(context));
        startThreads();
    }


    /***********************************************************************************************
     * Working Threads
     **********************************************************************************************/


    private final Object threadsCritical = new Object();

    private boolean imapThreadStartedVal;
    private final Object imapThreadStartedCond = new Object();
    public Thread imapThread = null;
    private PowerManager.WakeLock imapWakeLock = null;

    private boolean smtpThreadStartedVal;
    private final Object smtpThreadStartedCond = new Object();
    public Thread smtpThread = null;
    private PowerManager.WakeLock smtpWakeLock = null;

    public PowerManager.WakeLock afterForgroundWakeLock = null;

    public void startThreads()
    {
        synchronized(threadsCritical) {

            if (imapThread == null || !imapThread.isAlive()) {

                synchronized (imapThreadStartedCond) {
                    imapThreadStartedVal = false;
                }

                imapThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // raise the starting condition
                        // after acquiring a wakelock so that the process is not terminated.
                        // as imapWakeLock is not reference counted that would result in a wakelock-gap is not needed here.
                        imapWakeLock.acquire();
                        synchronized (imapThreadStartedCond) {
                            imapThreadStartedVal = true;
                            imapThreadStartedCond.notifyAll();
                        }

                        Log.i("DeltaChat", "###################### IMAP-Thread started. ######################");


                        while (true) {
                            imapWakeLock.acquire();
                            performJobs();
                            fetch();
                            imapWakeLock.release();
                            idle();
                        }
                    }
                }, "imapThread");
                imapThread.start();
            }

            if (smtpThread == null || !smtpThread.isAlive()) {

                synchronized (smtpThreadStartedCond) {
                    smtpThreadStartedVal = false;
                }

                smtpThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        smtpWakeLock.acquire();
                        synchronized (smtpThreadStartedCond) {
                            smtpThreadStartedVal = true;
                            smtpThreadStartedCond.notifyAll();
                        }

                        Log.i("DeltaChat", "###################### SMTP-Thread started. ######################");


                        while (true) {
                            smtpWakeLock.acquire();
                            performSmtpJobs();
                            smtpWakeLock.release();
                            performSmtpIdle();
                        }
                    }
                }, "smtpThread");
                smtpThread.start();
            }
        }
    }

    public void waitForThreadsRunning()
    {
        try {
            synchronized( imapThreadStartedCond ) {
                while( !imapThreadStartedVal ) {
                    imapThreadStartedCond.wait();
                }
            }

            synchronized( smtpThreadStartedCond ) {
                while( !smtpThreadStartedVal ) {
                    smtpThreadStartedCond.wait();
                }
            }
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
    }


    /***********************************************************************************************
     * Event Handling
     **********************************************************************************************/


    public final Object lastErrorLock = new Object();
    public int lastErrorCode = 0;
    public String lastErrorString = "";
    public boolean showNextErrorAsToast = true;

    @Override public long handleEvent(final int event, final long data1, final long data2) {
        switch(event) {
            case DC_EVENT_INFO:
                Log.i("DeltaChat", dataToString(data2));
                break;

            case DC_EVENT_WARNING:
                Log.w("DeltaChat", dataToString(data2));
                break;

            case DC_EVENT_ERROR:
                Log.e("DeltaChat", dataToString(data2));
                synchronized (lastErrorLock) {
                    lastErrorCode = (int)data1;
                    lastErrorString = dataToString(data2);
                }
                Util.runOnMain(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (lastErrorLock) {
                            if (showNextErrorAsToast) {
                                if (ForegroundDetector.getInstance().isForeground()) {
                                    Toast.makeText(context, lastErrorString, Toast.LENGTH_LONG).show();
                                }
                            }
                            showNextErrorAsToast = true;
                        }
                    }
                });
                break;

            case DC_EVENT_HTTP_GET:
                String httpContent = null;
                try {
                    URL url = new URL(dataToString(data1));
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    try {
                        urlConnection.setConnectTimeout(10*1000);
                        InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());

                        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));

                        StringBuilder total = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) {
                            total.append(line).append('\n');
                        }
                        httpContent = total.toString();
                    } finally {
                        urlConnection.disconnect();
                    }
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
                return stringToData(httpContent);
        }
        return 0;
    }
}
