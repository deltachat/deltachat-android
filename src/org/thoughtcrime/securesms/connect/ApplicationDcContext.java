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
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEventCenter;
import com.b44t.messenger.DcLot;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.contacts.avatars.SystemContactPhoto;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.database.ThreadDatabase;
import org.thoughtcrime.securesms.database.model.ThreadRecord;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientProvider;
import org.thoughtcrime.securesms.util.Hash;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.Util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ApplicationDcContext extends DcContext {

    @IntDef({RECIPIENT_TYPE_CHAT, RECIPIENT_TYPE_CONTACT})
    public @interface RecipientType {}

    public static final int RECIPIENT_TYPE_CHAT = 0;
    public static final int RECIPIENT_TYPE_CONTACT = 1;

    public Context context;

    public ApplicationDcContext(Context context) {
        super("android-dev");
        this.context = context;

        File dbfile = new File(context.getFilesDir(), "messenger.db");
        open(dbfile.getAbsolutePath());
        setConfig("e2ee_enabled", "1");

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

        TimerReceiver.scheduleNextAlarm(context);
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

    public static HashMap<String, Integer> sharedFiles = new HashMap<>();

    public void openForViewOrShare(int msg_id, String cmd)
    {
        DcMsg msg = getMsg(msg_id);
        String path = msg.getFile();
        String mimeType = msg.getFilemime();
        try {
            File file = new File(path);
            if( !file.exists() ) {
                Toast.makeText(context, context.getString(R.string.ShareActivity_file_not_found, path), Toast.LENGTH_LONG).show();
                return;
            }

            Uri uri;
            if (path.startsWith(getBlobdir())) {
                uri = Uri.parse("content://" + BuildConfig.APPLICATION_ID + ".attachments/" + file.getName());
                sharedFiles.put("/"+file.getName(), 1); // as different Android version handle uris in putExtra differently, we also check them on our own
            } else {
                if (Build.VERSION.SDK_INT >= 24) {
                    uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", file);
                }
                else {
                    uri = Uri.fromFile(file);
                }
            }

            if( cmd.equals(Intent.ACTION_VIEW) ) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, mimeType);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);
            }
            else {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType(mimeType);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.ShareActivity_share_with)));
            }

        }
        catch (Exception e) {
            Toast.makeText(context, R.string.ShareActivity_unable_to_open_media, Toast.LENGTH_LONG).show();
        }
    }

    /***********************************************************************************************
     * create objects compatible to the database model of Signal
     **********************************************************************************************/

    @NonNull
    public Recipient getRecipient(@RecipientType int recipientType, int id) {
        switch (recipientType) {
            case RECIPIENT_TYPE_CHAT:
                return getRecipient(getChat(id));
            case RECIPIENT_TYPE_CONTACT:
                return getRecipient(getContact(id));
            default:
                throw new IllegalArgumentException("Wrong RecipientType");
        }
    }

    @NonNull
    public Recipient getRecipient(DcChat chat) {
        int[] contactIds = getChatContacts(chat.getId());
        List<Recipient> participants = new ArrayList<>();
        for(int contactId : contactIds) {
            participants.add(getRecipient(RECIPIENT_TYPE_CONTACT, contactId));
        }
        RecipientProvider.RecipientDetails recipientDetails = new RecipientProvider.RecipientDetails(chat.getName(), null, false, null, participants);
        Recipient recipient = new Recipient(Address.fromChat(chat.getId()), recipientDetails);
        if (!chat.isGroup()) {
            String identifier = Hash.sha256(chat.getName() + chat.getSubtitle());
            Uri systemContactPhoto = TextSecurePreferences.getSystemContactPhoto(context, identifier);
            if (systemContactPhoto != null) {
                recipient.setSystemContactPhoto(systemContactPhoto);
            }
        }
        return recipient;
    }
    @NonNull

    public Recipient getRecipient(DcContact contact) {
        RecipientProvider.RecipientDetails recipientDetails = new RecipientProvider.RecipientDetails(contact.getDisplayName(), null, false, null, null);
        Recipient recipient = new Recipient(Address.fromContact(contact.getId()), recipientDetails);
        String identifier = Hash.sha256(contact.getName() + contact.getAddr());
        Uri systemContactPhoto = TextSecurePreferences.getSystemContactPhoto(context, identifier);
        if (systemContactPhoto != null) {
            recipient.setSystemContactPhoto(systemContactPhoto);
        }
        if (contact.getId() == DcContact.DC_CONTACT_ID_SELF) {
            recipient.setProfileAvatar("SELF");
        }
        return recipient;
    }

    @NonNull
    public ThreadRecord getThreadRecord(DcLot summary, DcChat chat) { // adapted from ThreadDatabase.getCurrent()
        int     chatId = chat.getId();
        int     distributionType = chatId==DcChat.DC_CHAT_ID_ARCHIVED_LINK? ThreadDatabase.DistributionTypes.ARCHIVE : ThreadDatabase.DistributionTypes.CONVERSATION;

        String body = summary.getText1();
        if(!body.isEmpty()) { body += ": "; }
        body += summary.getText2();

        Recipient          recipient            = getRecipient(chat);
        long               date                 = summary.getTimestamp();
        long               count                = 1;
        int                unreadCount          = getFreshMsgCount(chatId);
        long               type                 = 0;//cursor.getLong(cursor.getColumnIndexOrThrow(ThreadDatabase.SNIPPET_TYPE));
        boolean            archived             = chat.getArchived()!=0;
        int                status               = 0;
        long               expiresIn            = 0;
        long               lastSeen             = 0;
        boolean            verified             = chat.isVerified();

        return new ThreadRecord(context, body, null, recipient, date, count,
                unreadCount, chatId, 0, status, type,
                distributionType, archived, expiresIn, lastSeen, 0, verified);
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

                imapThread = new Thread(() -> {
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
                        performImapJobs();
                        performImapFetch();
                        imapWakeLock.release();
                        performImapIdle();
                    }
                }, "imapThread");
                imapThread.setPriority(Thread.NORM_PRIORITY);
                imapThread.start();
            }

            if (smtpThread == null || !smtpThread.isAlive()) {

                synchronized (smtpThreadStartedCond) {
                    smtpThreadStartedVal = false;
                }

                smtpThread = new Thread(() -> {
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
                }, "smtpThread");
                smtpThread.setPriority(Thread.MAX_PRIORITY);
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

    @Override
    public long handleEvent(final int event, long data1, long data2) {
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

            case DC_EVENT_GET_STRING:
                String s;
                switch( (int)data1 ) { // the integers are defined in the core and used only here, an enum or sth. like that won't have a big benefit
                    case  8: s = context.getString(R.string.menu_deaddrop); break;
                    case 13: s = context.getString(R.string.default_status_text); break;
                    case 42: s = context.getString(R.string.autocrypt__asm_subject); break;
                    case 43: s = context.getString(R.string.autocrypt__asm_general_body); break;
                    default: s = null; break;
                }
                return stringToData(s);

            default:
                {
                    final Object data1obj = data1IsString(event)? dataToString(data1) : data1;
                    final Object data2obj = data2IsString(event)? dataToString(data2) : data2;
                    if(eventCenter!=null) {
                        eventCenter.sendToObservers(event, data1obj, data2obj);
                    }
                }
                break;
        }
        return 0;
    }
}
