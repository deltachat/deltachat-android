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
package org.thoughtcrime.securesms.connect;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcChatlist;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEventCenter;
import com.b44t.messenger.DcLot;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.GroupDatabase;
import org.thoughtcrime.securesms.database.RecipientDatabase;
import org.thoughtcrime.securesms.database.ThreadDatabase;
import org.thoughtcrime.securesms.database.model.ThreadRecord;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.Util;
import org.whispersystems.libsignal.util.guava.Optional;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class ApplicationDcContext extends DcContext {

    public Context context;

    public ApplicationDcContext(Context context) {
        super("Android");
        this.context = context;

        File dbfile = new File(context.getFilesDir(), "messenger.db");
        open(dbfile.getAbsolutePath());

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

    public File getImexDir()
    {
        // DIRECTORY_DOCUMENTS is only available since KitKat;
        // as we also support Ice Cream Sandwich and Jellybean (2017: 11% in total), this is no option.
        // Moreover, DIRECTORY_DOWNLOADS seems to be easier accessible by the user,
        // eg. "Download Managers" are nearly always installed.
        // CAVE: do not use DownloadManager to add the file as it is deleted on uninstall then ...
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }


    /***********************************************************************************************
     * create objects compatible to the database model of Signal
     **********************************************************************************************/

    @NonNull
    public ThreadRecord getThreadRecord(DcChatlist chatlist, int i) { // adapted from ThreadDatabase.getCurrent()
        int     chatId           = chatlist.getChatId(i);
        DcChat  chat             = chatlist.getChat(i);
        DcLot   summary          = chatlist.getSummary(i, chat);
        int     distributionType = chatId==DcChat.DC_CHAT_ID_ARCHIVED_LINK? ThreadDatabase.DistributionTypes.ARCHIVE : ThreadDatabase.DistributionTypes.CONVERSATION;
        Address address          = Address.UNKNOWN;//Address.fromSerialized(cursor.getString(cursor.getColumnIndexOrThrow(ThreadDatabase.ADDRESS)));

        Optional<RecipientDatabase.RecipientSettings> settings;
        Optional<GroupDatabase.GroupRecord>       groupRecord;

        if (distributionType != ThreadDatabase.DistributionTypes.ARCHIVE && distributionType != ThreadDatabase.DistributionTypes.INBOX_ZERO) {
            settings    = Optional.absent();//DatabaseFactory.getRecipientDatabase(context).getRecipientSettings(cursor);
            groupRecord = Optional.absent();//DatabaseFactory.getGroupDatabase(context).getGroup(cursor);
        } else {
            settings    = Optional.absent();
            groupRecord = Optional.absent();
        }

        String body = summary.getText1();
        if(!body.isEmpty()) { body += ": "; }
        body += summary.getText2();

        Recipient          recipient            = Recipient.from(context, address, settings, groupRecord, true);
        long               date                 = summary.getTimestamp()*1000;
        long               count                = 1;
        int                unreadCount          = getFreshMsgCount(chatId);
        long               type                 = 0;//cursor.getLong(cursor.getColumnIndexOrThrow(ThreadDatabase.SNIPPET_TYPE));
        boolean            archived             = chat.getArchived()!=0;
        int                status               = 0;
        long               expiresIn            = 0;
        long               lastSeen             = 0;

        return new ThreadRecord(context, body, null, recipient, date, count,
                unreadCount, chatId, 0, status, type,
                distributionType, archived, expiresIn, lastSeen, 0);
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

    public DcEventCenter eventCenter = new DcEventCenter();

    private final Object lastErrorLock = new Object();
    private String lastErrorString = "";
    private boolean showNextErrorAsToast = true;

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

    @Override public long handleEvent(final int event, long data1, long data2) {
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

            default:
                {
                    final Object data1obj = data1IsString(event)? dataToString(data1) : data1;
                    final Object data2obj = data2IsString(event)? dataToString(data2) : data2;
                    Util.runOnMain(new Runnable() {
                        @Override
                        public void run() {
                            if(eventCenter!=null) {
                                eventCenter.sendToObservers(event, data1obj, data2obj);
                            }
                        }
                    });

                }
                break;
        }
        return 0;
    }
}
