package org.thoughtcrime.securesms.connect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.webkit.MimeTypeMap;
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
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.database.model.ThreadRecord;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.Hash;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ApplicationDcContext extends DcContext {

  public static final String TAG = "DeltaChat";

  @IntDef({RECIPIENT_TYPE_CHAT, RECIPIENT_TYPE_CONTACT})
  public @interface RecipientType {
  }

  public static final int RECIPIENT_TYPE_CHAT = 0;
  public static final int RECIPIENT_TYPE_CONTACT = 1;

  public Context context;
  public volatile boolean isScreenOn = false;

  public ApplicationDcContext(Context context) {
    super("android-dev");
    this.context = context;

    File dbfile = new File(context.getFilesDir(), "messenger.db");
    open(dbfile.getAbsolutePath());

    try {
      PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

      imapWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "imapWakeLock");
      imapWakeLock.setReferenceCounted(false); // if the idle-thread is killed for any reasons, it is better not to rely on reference counting

      mvboxWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mvboxWakeLock");
      mvboxWakeLock.setReferenceCounted(false); // if the idle-thread is killed for any reasons, it is better not to rely on reference counting

      sentboxWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sentboxWakeLock");
      sentboxWakeLock.setReferenceCounted(false); // if the idle-thread is killed for any reasons, it is better not to rely on reference counting

      smtpWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "smtpWakeLock");
      smtpWakeLock.setReferenceCounted(false); // if the idle-thread is killed for any reasons, it is better not to rely on reference counting

      afterForgroundWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "afterForegroundWakeLock");
      afterForgroundWakeLock.setReferenceCounted(false);

    } catch (Exception e) {
      Log.e(TAG, "Cannot create wakeLocks");
    }

    new ForegroundDetector(ApplicationContext.getInstance(context));
    startThreads(0);

    TimerReceiver.scheduleNextAlarm(context);

    BroadcastReceiver networkStateReceiver = new NetworkStateReceiver();
    context.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));

    IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
    filter.addAction(Intent.ACTION_SCREEN_OFF);
    BroadcastReceiver screenReceiver = new ScreenReceiver();
    context.registerReceiver(screenReceiver, filter);

    try {
      PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
      isScreenOn = pm.isScreenOn();
    } catch (Exception e) {

    }

    if (!isScreenOn) {
      context.startService(new Intent(context, KeepAliveService.class));
    }
  }

  public File getImexDir() {
    // DIRECTORY_DOCUMENTS is only available since KitKat;
    // as we also support Ice Cream Sandwich and Jellybean (2017: 11% in total), this is no option.
    // Moreover, DIRECTORY_DOWNLOADS seems to be easier accessible by the user,
    // eg. "Download Managers" are nearly always installed.
    // CAVE: do not use DownloadManager to add the file as it is deleted on uninstall then ...
    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
  }

  public static HashMap<String, Integer> sharedFiles = new HashMap<>();

  public void openForViewOrShare(int msg_id, String cmd) {
    DcMsg msg = getMsg(msg_id);
    String path = msg.getFile();
    String mimeType = msg.getFilemime();
    try {
      File file = new File(path);
      if (!file.exists()) {
        Toast.makeText(context, context.getString(R.string.file_not_found, path), Toast.LENGTH_LONG).show();
        return;
      }

      Uri uri;
      if (path.startsWith(getBlobdir())) {
        uri = Uri.parse("content://" + BuildConfig.APPLICATION_ID + ".attachments/" + file.getName());
        sharedFiles.put("/" + file.getName(), 1); // as different Android version handle uris in putExtra differently, we also check them on our own
      } else {
        if (Build.VERSION.SDK_INT >= 24) {
          uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", file);
        } else {
          uri = Uri.fromFile(file);
        }
      }

      if (cmd.equals(Intent.ACTION_VIEW)) {
        mimeType = checkMime(path, mimeType);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
      } else {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.chat_share_with_title)));
      }
    } catch (RuntimeException e) {
      Toast.makeText(context, R.string.no_app_to_handle_data, Toast.LENGTH_LONG).show();
      Toast.makeText(context, "Media-Type: " + mimeType, Toast.LENGTH_LONG).show();
      Log.i(TAG, "opening of external activity failed.", e);
    }
  }

  private String checkMime(String path, String mimeType) {
    if(mimeType == null || mimeType.equals("application/octet-stream")) {
      String extension = MimeTypeMap.getFileExtensionFromUrl(path);
      String newType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
      if(newType != null) return newType;
    }
    return mimeType;
  }

  public String getBlobdirFile(String filename, String ext) {
    String outPath = null;
    for (int i = 0; i < 1000; i++) {
      String test = getBlobdir() + "/" + filename + (i == 0 ? "" : i < 100 ? "-" + i : "-" + (new Date().getTime() + i)) + ext;
      if (!new File(test).exists()) {
        outPath = test;
        break;
      }
    }
    if(outPath==null) {
      // should not happen
      outPath = getBlobdir() + "/" + Math.random();
    }
    return outPath;
  }

  public String getBlobdirFile(String path) {
    String filename = path.substring(path.lastIndexOf('/')+1); // is the whole path if '/' is not found (lastIndexOf() returns -1 then)
    String ext = "";
    int point = filename.indexOf('.');
    if(point!=-1) {
      ext = filename.substring(point);
      filename = filename.substring(0, point);
    }
    return getBlobdirFile(filename, ext);

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
    for (int contactId : contactIds) {
      participants.add(getRecipient(RECIPIENT_TYPE_CONTACT, contactId));
    }
    Recipient recipient = new Recipient(Address.fromChat(chat.getId()), chat.getName(), participants);
    if (!chat.isGroup()) {
      String identifier = Hash.sha256(chat.getName() + chat.getSubtitle());
      Uri systemContactPhoto = Prefs.getSystemContactPhoto(context, identifier);
      if (systemContactPhoto != null) {
        recipient.setSystemContactPhoto(systemContactPhoto);
      }
    }
    return recipient;
  }

  @NonNull

  public Recipient getRecipient(DcContact contact) {
    Recipient recipient = new Recipient(Address.fromContact(contact.getId()), contact.getDisplayName(), null);
    String identifier = Hash.sha256(contact.getName() + contact.getAddr());
    Uri systemContactPhoto = Prefs.getSystemContactPhoto(context, identifier);
    if (systemContactPhoto != null) {
      recipient.setSystemContactPhoto(systemContactPhoto);
    }
    if (contact.getId() == DcContact.DC_CONTACT_ID_SELF) {
      recipient.setProfileAvatar("SELF");
    }
    int[] blockedContacts = getBlockedContacts();
    boolean blocked = false;
    for (int blockedContact : blockedContacts) {
      if (contact.getId() == blockedContact) {
        blocked = true;
        break;
      }
    }
    recipient.setBlocked(blocked);
    return recipient;
  }

  @NonNull
  public ThreadRecord getThreadRecord(DcLot summary, DcChat chat) { // adapted from ThreadDatabase.getCurrent()
    int chatId = chat.getId();

    String body = summary.getText1();
    if (!body.isEmpty()) {
      body += ": ";
    }
    body += summary.getText2();

    Recipient recipient = getRecipient(chat);
    long date = summary.getTimestamp();
    int unreadCount = getFreshMsgCount(chatId);
    boolean archived = chat.getArchived() != 0;
    boolean verified = chat.isVerified();

    return new ThreadRecord(context, body, recipient, date,
        unreadCount, chatId,
        archived, verified, summary);
  }


  /***********************************************************************************************
   * Working Threads
   **********************************************************************************************/

  private final Object threadsCritical = new Object();

  private boolean imapThreadStartedVal;
  private final Object imapThreadStartedCond = new Object();
  public Thread imapThread = null;
  private PowerManager.WakeLock imapWakeLock = null;

  private boolean mvboxThreadStartedVal;
  private final Object mvboxThreadStartedCond = new Object();
  public Thread mvboxThread = null;
  private PowerManager.WakeLock mvboxWakeLock = null;

  private boolean sentboxThreadStartedVal;
  private final Object sentboxThreadStartedCond = new Object();
  public Thread sentboxThread = null;
  private PowerManager.WakeLock sentboxWakeLock = null;

  private boolean smtpThreadStartedVal;
  private final Object smtpThreadStartedCond = new Object();
  public Thread smtpThread = null;
  private PowerManager.WakeLock smtpWakeLock = null;

  public PowerManager.WakeLock afterForgroundWakeLock = null;

  public final static int INTERRUPT_IDLE = 0x01; // interrupt idle if the thread is already running

  public void startThreads(int flags) {
    synchronized (threadsCritical) {

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

          Log.i(TAG, "###################### IMAP-Thread started. ######################");


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
      } else {
        if ((flags & INTERRUPT_IDLE) != 0) {
          interruptImapIdle();
        }
      }


      if (mvboxThread == null || !mvboxThread.isAlive()) {

        synchronized (mvboxThreadStartedCond) {
          mvboxThreadStartedVal = false;
        }

        mvboxThread = new Thread(() -> {
          mvboxWakeLock.acquire();
          synchronized (mvboxThreadStartedCond) {
            mvboxThreadStartedVal = true;
            mvboxThreadStartedCond.notifyAll();
          }

          Log.i(TAG, "###################### MVBOX-Thread started. ######################");


          while (true) {
            mvboxWakeLock.acquire();
            performMvboxFetch();
            mvboxWakeLock.release();
            performMvboxIdle();
          }
        }, "mvboxThread");
        mvboxThread.setPriority(Thread.NORM_PRIORITY);
        mvboxThread.start();
      } else {
        if ((flags & INTERRUPT_IDLE) != 0) {
          interruptMvboxIdle();
        }
      }


      if (sentboxThread == null || !sentboxThread.isAlive()) {

        synchronized (sentboxThreadStartedCond) {
          sentboxThreadStartedVal = false;
        }

        sentboxThread = new Thread(() -> {
          sentboxWakeLock.acquire();
          synchronized (sentboxThreadStartedCond) {
            sentboxThreadStartedVal = true;
            sentboxThreadStartedCond.notifyAll();
          }

          Log.i(TAG, "###################### SENTBOX-Thread started. ######################");


          while (true) {
            sentboxWakeLock.acquire();
            performSentboxFetch();
            sentboxWakeLock.release();
            performSentboxIdle();
          }
        }, "sentboxThread");
        sentboxThread.setPriority(Thread.NORM_PRIORITY-1);
        sentboxThread.start();
      } else {
        if ((flags & INTERRUPT_IDLE) != 0) {
          interruptSentboxIdle();
        }
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

          Log.i(TAG, "###################### SMTP-Thread started. ######################");


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

  public void waitForThreadsRunning() {
    try {
      synchronized (imapThreadStartedCond) {
        while (!imapThreadStartedVal) {
          imapThreadStartedCond.wait();
        }
      }

      synchronized (mvboxThreadStartedCond) {
        while (!mvboxThreadStartedVal) {
          mvboxThreadStartedCond.wait();
        }
      }

      synchronized (sentboxThreadStartedCond) {
        while (!sentboxThreadStartedVal) {
          sentboxThreadStartedCond.wait();
        }
      }

      synchronized (smtpThreadStartedCond) {
        while (!smtpThreadStartedVal) {
          smtpThreadStartedCond.wait();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  /***********************************************************************************************
   * Tools
   **********************************************************************************************/

  public boolean isNetworkConnected() {
    try {
      ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo netInfo = cm.getActiveNetworkInfo();
      if (netInfo != null && netInfo.isConnected()) {
        return true;
      }

    } catch (Exception e) {
    }
    return false;
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

  private void handleError(int event, boolean popUp, String string) {
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
      if (popUp && showAsToast) {
        String toastString = string;

        if (event == DC_EVENT_ERROR_NETWORK) {
          if (!isNetworkConnected()) {
            toastString = context.getString(R.string.error_no_network);
          }
        }

        if (ForegroundDetector.getInstance().isForeground()) {
          Toast.makeText(context, toastString, Toast.LENGTH_LONG).show();
        }
      }
    });
  }

  @Override
  public long handleEvent(final int event, long data1, long data2) {
    switch (event) {
      case DC_EVENT_INFO:
        Log.i(TAG, dataToString(data2));
        break;

      case DC_EVENT_WARNING:
        Log.w(TAG, dataToString(data2));
        break;

      case DC_EVENT_ERROR:
        handleError(event, true, dataToString(data2));
        break;

      case DC_EVENT_ERROR_NETWORK:
        handleError(event, data1 != 0, dataToString(data2));
        break;

      case DC_EVENT_HTTP_GET:
        String httpContent = null;
        try {
          URL url = new URL(dataToString(data1));
          HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
          try {
            urlConnection.setConnectTimeout(10 * 1000);
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
        } catch (Exception e) {
          e.printStackTrace();
        }
        return stringToData(httpContent);

      case DC_EVENT_GET_STRING:
        String s;
        switch ((int) data1) {
          // the integers are defined in the core and used only here, an enum or sth. like that won't have a big benefit
          case  1: s = context.getString(R.string.chat_no_messages); break;
          case  2: s = context.getString(R.string.self); break;
          case  3: s = context.getString(R.string.draft); break;
          case  4: s = context.getResources().getQuantityString(R.plurals.n_members, (int)data2, (int)data2); break;
          case  6: s = context.getResources().getQuantityString(R.plurals.n_contacts, (int)data2, (int)data2); break;
          case  7: s = context.getString(R.string.voice_message); break;
          case  8: s = context.getString(R.string.menu_deaddrop); break;
          case  9: s = context.getString(R.string.image); break;
          case 10: s = context.getString(R.string.video); break;
          case 11: s = context.getString(R.string.audio); break;
          case 12: s = context.getString(R.string.file); break;
          case 13: s = context.getString(R.string.pref_default_status_text); break;
          case 14: s = context.getString(R.string.group_hello_draft); break;
          case 15: s = context.getString(R.string.systemmsg_group_name_changed); break;
          case 16: s = context.getString(R.string.systemmsg_group_image_changed); break;
          case 17: s = context.getString(R.string.systemmsg_member_added); break;
          case 18: s = context.getString(R.string.systemmsg_member_removed); break;
          case 19: s = context.getString(R.string.systemmsg_group_left); break;
          case 20: s = context.getString(R.string.error_x); break;
          case 21: s = context.getString(R.string.group_self_not_in_group); break;
          case 22: s = context.getString(R.string.error_no_network); break;
          case 23: s = context.getString(R.string.gif); break;
          case 29: s = context.getString(R.string.systemmsg_cannot_decrypt); break;
          case 31: s = context.getString(R.string.systemmsg_read_receipt_subject); break;
          case 32: s = context.getString(R.string.systemmsg_read_receipt_body); break;
          case 33: s = context.getString(R.string.systemmsg_group_image_deleted); break;
          case 40: s = context.getString(R.string.chat_archived_chats_title); break;
          case 42: s = context.getString(R.string.autocrypt_asm_subject); break;
          case 43: s = context.getString(R.string.autocrypt_asm_general_body); break;
          case 50: s = context.getString(R.string.chat_self_talk_subtitle); break;
          case 60: s = context.getString(R.string.login_error_cannot_login); break;
          case 61: s = context.getString(R.string.login_error_server_response); break;
          default: s = null; break;
        }
        return stringToData(s);

      default: {
        final Object data1obj = data1IsString(event) ? dataToString(data1) : data1;
        final Object data2obj = data2IsString(event) ? dataToString(data2) : data2;
        if (eventCenter != null) {
          eventCenter.sendToObservers(event, data1obj, data2obj);
        }
      }
      break;
    }
    return 0;
  }
}
