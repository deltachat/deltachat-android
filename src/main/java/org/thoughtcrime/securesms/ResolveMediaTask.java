package org.thoughtcrime.securesms;

import static org.thoughtcrime.securesms.util.MediaUtil.getMimeType;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;
import de.cketti.safecontentresolver.SafeContentResolver;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;

public class ResolveMediaTask extends AsyncTask<Uri, Void, Uri> {

  private static final String TAG = "ResolveMediaTask";
  private static final String TAG_DEBUG = "DEBUG_4490";

  interface OnMediaResolvedListener {
    void onMediaResolved(Uri uri);
  }

  private final WeakReference<Activity> contextRef;
  private final WeakReference<OnMediaResolvedListener> listenerWeakReference;

  private static final HashSet<ResolveMediaTask> instances = new HashSet<>();

  ResolveMediaTask(Activity activityContext, ResolveMediaTask.OnMediaResolvedListener listener) {
    this.contextRef = new WeakReference<>(activityContext);
    this.listenerWeakReference = new WeakReference<>(listener);
    instances.add(this);
  }

  @Override
  protected Uri doInBackground(Uri... uris) {
    try {
      Uri uri = uris[0];
      if (uris.length != 1 || uri == null) {
        return null;
      }

      logUri(uri);

      InputStream inputStream;
      String fileName = null;
      Long fileSize = null;

      SafeContentResolver safeContentResolver = SafeContentResolver.newInstance(contextRef.get());
      inputStream = safeContentResolver.openInputStream(uri);

      if (inputStream == null) {
        Log.w(TAG_DEBUG, "openInputStream returned null");
        return null;
      }

      logGetType(uri);
      logDocumentId(uri);
      dumpFullRow(uri);

      Cursor cursor = null;
      try {
        cursor =
            contextRef
                .get()
                .getContentResolver()
                .query(
                    uri,
                    new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE},
                    null,
                    null,
                    null);
        if (cursor != null && cursor.moveToFirst()) {
          Log.w(
              TAG_DEBUG,
              "projected query: columns="
                  + Arrays.toString(cursor.getColumnNames())
                  + " count="
                  + cursor.getCount());
          try {
            fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
            fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
          } catch (IllegalArgumentException e) {
            Log.w(TAG_DEBUG, "projected query: column missing", e);
          }
        } else {
          Log.w(TAG_DEBUG, "projected query: " + (cursor == null ? "cursor=null" : "cursor empty"));
        }
      } catch (Exception e) {
        Log.w(TAG_DEBUG, "projected query threw", e);
      } finally {
        if (cursor != null) cursor.close();
      }

      if (fileName == null) {
        fileName = uri.getLastPathSegment();
        Log.w(TAG_DEBUG, "no DISPLAY_NAME, fell back to lastPathSegment=" + fileName);
      } else {
        Log.w(TAG_DEBUG, "resolved DISPLAY_NAME=" + fileName + " SIZE=" + fileSize);
      }

      String mimeType = getMimeType(contextRef.get(), uri);
      Log.w(TAG_DEBUG, "mimeType=" + mimeType);
      Uri blobUri =
          PersistentBlobProvider.getInstance()
              .create(contextRef.get(), inputStream, mimeType, fileName, fileSize);
      Log.w(TAG_DEBUG, "blobUri=" + blobUri);
      return blobUri;
    } catch (NullPointerException | FileNotFoundException ioe) {
      Log.w(TAG, ioe);
      return null;
    }
  }

  private void logUri(Uri uri) {
    try {
      Log.w(
          TAG_DEBUG,
          "uri="
              + uri
              + " scheme="
              + uri.getScheme()
              + " authority="
              + uri.getAuthority()
              + " path="
              + uri.getPath()
              + " segments="
              + uri.getPathSegments()
              + " lastSegment="
              + uri.getLastPathSegment());
    } catch (Exception e) {
      Log.w(TAG_DEBUG, "logUri failed", e);
    }
  }

  private void logGetType(Uri uri) {
    try {
      Log.w(
          TAG_DEBUG,
          "contentResolver.getType=" + contextRef.get().getContentResolver().getType(uri));
    } catch (Exception e) {
      Log.w(TAG_DEBUG, "getType threw", e);
    }
  }

  private void logDocumentId(Uri uri) {
    try {
      boolean isDoc = DocumentsContract.isDocumentUri(contextRef.get(), uri);
      Log.w(TAG_DEBUG, "isDocumentUri=" + isDoc);
      if (isDoc) {
        Log.w(TAG_DEBUG, "documentId=" + DocumentsContract.getDocumentId(uri));
      }
    } catch (Exception e) {
      Log.w(TAG_DEBUG, "documentId probe failed", e);
    }
  }

  private void dumpFullRow(Uri uri) {
    Cursor cursor = null;
    try {
      cursor = contextRef.get().getContentResolver().query(uri, null, null, null, null);
      if (cursor == null) {
        Log.w(TAG_DEBUG, "full query: cursor=null");
        return;
      }
      Log.w(
          TAG_DEBUG,
          "full query: columns="
              + Arrays.toString(cursor.getColumnNames())
              + " count="
              + cursor.getCount());
      if (cursor.moveToFirst()) {
        for (int i = 0; i < cursor.getColumnCount(); i++) {
          String value;
          try {
            value = cursor.getType(i) == Cursor.FIELD_TYPE_BLOB ? "<blob>" : cursor.getString(i);
          } catch (Exception e) {
            value = "<error: " + e + ">";
          }
          Log.w(
              TAG_DEBUG,
              "  col["
                  + i
                  + "] "
                  + cursor.getColumnName(i)
                  + " (type="
                  + cursor.getType(i)
                  + ") = "
                  + value);
        }
      }
    } catch (Exception e) {
      Log.w(TAG_DEBUG, "full query threw", e);
    } finally {
      if (cursor != null) cursor.close();
    }
  }

  @Override
  protected void onPostExecute(Uri uri) {
    instances.remove(this);
    if (!this.isCancelled()) {
      listenerWeakReference.get().onMediaResolved(uri);
    }
  }

  @Override
  protected void onCancelled() {
    instances.remove(this);
    super.onCancelled();
    listenerWeakReference.get().onMediaResolved(null);
  }

  public static boolean isExecuting() {
    return !instances.isEmpty();
  }

  public static void cancelTasks() {
    for (ResolveMediaTask task : instances) {
      task.cancel(true);
    }
  }

  private boolean hasFileScheme(Uri uri) {
    if (uri == null) {
      return false;
    }
    return "file".equals(uri.getScheme());
  }
}
