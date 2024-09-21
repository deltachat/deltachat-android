package org.thoughtcrime.securesms.service;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.content.ContextCompat;

import org.thoughtcrime.securesms.DummyActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.notifications.NotificationCenter;
import org.thoughtcrime.securesms.util.IntentUtils;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class GenericForegroundService extends Service {

  private static final String TAG = GenericForegroundService.class.getSimpleName();

  private final IBinder binder = new LocalBinder();

  private static final String EXTRA_TITLE                  = "extra_title";
  private static final String EXTRA_CONTENT_TEXT           = "extra_content_text";
  private static final String EXTRA_CHANNEL_ID             = "extra_channel_id";
  private static final String EXTRA_ICON_RES               = "extra_icon_res";
  private static final String EXTRA_ID                     = "extra_id";
  private static final String EXTRA_PROGRESS               = "extra_progress";
  private static final String EXTRA_PROGRESS_MAX           = "extra_progress_max";
  private static final String EXTRA_PROGRESS_INDETERMINATE = "extra_progress_indeterminate";

  private static final String ACTION_START = "start";
  private static final String ACTION_STOP  = "stop";

  private static final AtomicInteger NEXT_ID = new AtomicInteger();
  private static final AtomicBoolean CHANNEL_CREATED = new AtomicBoolean(false);

  private static int startedCounter = 0;

  private final LinkedHashMap<Integer, Entry> allActiveMessages = new LinkedHashMap<>();

  private static final Entry DEFAULTS = new Entry("", "", NotificationCenter.CH_GENERIC, R.drawable.icon_notification, -1, 0, 0, false);

  private @Nullable Entry lastPosted;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent == null) {
      throw new IllegalStateException("Intent needs to be non-null.");
    }

    synchronized (GenericForegroundService.class) {
      String action = intent.getAction();
      if      (ACTION_START.equals(action)) handleStart(intent);
      else if (ACTION_STOP .equals(action)) handleStop(intent);
      else                                  throw new IllegalStateException(String.format("Action needs to be %s or %s.", ACTION_START, ACTION_STOP));

      updateNotification();

      return START_NOT_STICKY;
    }
  }

  private synchronized void updateNotification() {
    Iterator<Entry> iterator = allActiveMessages.values().iterator();

    if (iterator.hasNext()) {
      postObligatoryForegroundNotification(iterator.next());
    } else {
      Log.i(TAG, "Last request. Ending foreground service.");
      postObligatoryForegroundNotification(lastPosted != null ? lastPosted : DEFAULTS);
      stopForeground(true);
      stopSelf();
    }
  }


  private synchronized void handleStart(@NonNull Intent intent) {
    Entry entry = Entry.fromIntent(intent);

    Log.i(TAG, String.format(Locale.ENGLISH, "handleStart() %s", entry));

    allActiveMessages.put(entry.id, entry);
  }

  private synchronized void handleStop(@NonNull Intent intent) {
    Log.i(TAG, "handleStop()");

    int id = intent.getIntExtra(EXTRA_ID, -1);

    Entry removed = allActiveMessages.remove(id);

    if (removed == null) {
      Log.w(TAG, "Could not find entry to remove");
    }
  }

  private void postObligatoryForegroundNotification(@NonNull Entry active) {
    lastPosted = active;
    startForeground(NotificationCenter.ID_GENERIC, new Builder(this, active.channelId)
                                                           .setSmallIcon(active.iconRes)
                                                           .setContentTitle(active.title)
                                                           .setTicker(active.contentText)
                                                           .setContentText(active.contentText)
                                                           .setProgress(active.progressMax, active.progress, active.indeterminate)
                                                           .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, DummyActivity.class), IntentUtils.FLAG_MUTABLE()))
                                                           .build());
  }

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }


  public static NotificationController startForegroundTask(@NonNull Context context, @NonNull String task) {
    startedCounter++;
    final int id = NEXT_ID.getAndIncrement();

    createFgNotificationChannel(context);
    Intent intent = new Intent(context, GenericForegroundService.class);
    intent.setAction(ACTION_START);
    intent.putExtra(EXTRA_TITLE, task);
    intent.putExtra(EXTRA_CHANNEL_ID, NotificationCenter.CH_GENERIC);
    intent.putExtra(EXTRA_ICON_RES, R.drawable.notification_permanent);
    intent.putExtra(EXTRA_ID, id);

    ContextCompat.startForegroundService(context, intent);

    return new NotificationController(context, id);
  }

  public static void stopForegroundTask(@NonNull Context context, int id) {
    Intent intent = new Intent(context, GenericForegroundService.class);
    intent.setAction(ACTION_STOP);
    intent.putExtra(EXTRA_ID, id);

    ContextCompat.startForegroundService(context, intent);
    startedCounter = Math.max(startedCounter-1, 0);
  }

  public static boolean isForegroundTaskStarted() {
    return startedCounter > 0;
  }

  synchronized void replaceProgress(int id, int progressMax, int progress, boolean indeterminate, String message) {
    Entry oldEntry = allActiveMessages.get(id);

    if (oldEntry == null) {
      Log.w(TAG, "Failed to replace notification, it was not found");
      return;
    }

    if (message == null) {
      message = oldEntry.contentText;
    }

    Entry newEntry = new Entry(oldEntry.title, message, oldEntry.channelId, oldEntry.iconRes, oldEntry.id, progressMax, progress, indeterminate);

    if (oldEntry.equals(newEntry)) {
      Log.d(TAG, String.format("handleReplace() skip, no change %s", newEntry));
      return;
    }

    Log.i(TAG, String.format("handleReplace() %s", newEntry));

    allActiveMessages.put(newEntry.id, newEntry);

    updateNotification();
  }

  @TargetApi(Build.VERSION_CODES.O)
  static public void createFgNotificationChannel(Context context) {
    if(!CHANNEL_CREATED.get() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      CHANNEL_CREATED.set(true);
      NotificationChannel channel = new NotificationChannel(NotificationCenter.CH_GENERIC,
              "Generic Background Service", NotificationManager.IMPORTANCE_MIN);
      channel.setDescription("Ensure app will not be killed while long ongoing background tasks are running.");
      NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
      notificationManager.createNotificationChannel(channel);
    }
  }


  private static class Entry {
    final @NonNull     String  title;
    final @NonNull    String  contentText;
    final @NonNull     String  channelId;
    final              int     id;
    final @DrawableRes int     iconRes;
    final              int     progress;
    final              int     progressMax;
    final              boolean indeterminate;

    private Entry(@NonNull String title, @NonNull String contentText, @NonNull String channelId, @DrawableRes int iconRes, int id, int progressMax, int progress, boolean indeterminate) {
      this.title         = title;
      this.contentText   = contentText;
      this.channelId     = channelId;
      this.iconRes       = iconRes;
      this.id            = id;
      this.progress      = progress;
      this.progressMax   = progressMax;
      this.indeterminate = indeterminate;
    }

    private static Entry fromIntent(@NonNull Intent intent) {
      int id = intent.getIntExtra(EXTRA_ID, DEFAULTS.id);

      String title = intent.getStringExtra(EXTRA_TITLE);
      if (title == null) title = DEFAULTS.title;

      String contentText = intent.getStringExtra(EXTRA_CONTENT_TEXT);
      if (contentText == null) contentText = DEFAULTS.contentText;

      String channelId = intent.getStringExtra(EXTRA_CHANNEL_ID);
      if (channelId == null) channelId = DEFAULTS.channelId;

      int     iconRes       = intent.getIntExtra(EXTRA_ICON_RES, DEFAULTS.iconRes);
      int     progress      = intent.getIntExtra(EXTRA_PROGRESS, DEFAULTS.progress);
      int     progressMax   = intent.getIntExtra(EXTRA_PROGRESS_MAX, DEFAULTS.progressMax);
      boolean indeterminate = intent.getBooleanExtra(EXTRA_PROGRESS_INDETERMINATE, DEFAULTS.indeterminate);

      return new Entry(title, contentText, channelId, iconRes, id, progressMax, progress, indeterminate);
    }

    @Override
    public @NonNull String toString() {
      return String.format(Locale.ENGLISH, "ChannelId: %s  Id: %d Progress: %d/%d %s", channelId, id, progress, progressMax, indeterminate ? "indeterminate" : "determinate");
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Entry entry = (Entry) o;
      return id == entry.id &&
             iconRes == entry.iconRes &&
             progress == entry.progress &&
             progressMax == entry.progressMax &&
             indeterminate == entry.indeterminate &&
             title.equals(entry.title) &&
             contentText.equals(entry.contentText) &&
             channelId.equals(entry.channelId);
    }

    @Override
    public int hashCode() {
      int hashCode = title.hashCode();
      hashCode *= 31;
      hashCode += channelId.hashCode();
      hashCode *= 31;
      hashCode += id;
      hashCode *= 31;
      hashCode += iconRes;
      hashCode *= 31;
      hashCode += progress;
      hashCode *= 31;
      hashCode += progressMax;
      hashCode *= 31;
      hashCode += indeterminate ? 1 : 0;
      return hashCode;
    }
  }

  class LocalBinder extends Binder {
    GenericForegroundService getService() {
      // Return this instance of LocalService so clients can call public methods
      return GenericForegroundService.this;
    }
  }
}
