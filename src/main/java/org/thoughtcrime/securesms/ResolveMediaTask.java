package org.thoughtcrime.securesms;

import static org.thoughtcrime.securesms.util.MediaUtil.getMimeType;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.OpenableColumns;
import android.util.Log;

import org.thoughtcrime.securesms.providers.PersistentBlobProvider;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashSet;

import de.cketti.safecontentresolver.SafeContentResolver;

public class ResolveMediaTask extends AsyncTask<Uri, Void, Uri> {

    private static final String TAG = ResolveMediaTask.class.getSimpleName();

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

                InputStream inputStream;
                String fileName = null;
                Long fileSize = null;

                SafeContentResolver safeContentResolver = SafeContentResolver.newInstance(contextRef.get());
                inputStream = safeContentResolver.openInputStream(uri);

                if (inputStream == null) {
                    return null;
                }

                Cursor cursor = contextRef.get().getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null);

                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        try {
                            fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                            fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
                        } catch (IllegalArgumentException e) {
                            Log.w(TAG, e);
                        }
                    }
                } finally {
                    if (cursor != null) cursor.close();
                }

                if (fileName == null) {
                    fileName = uri.getLastPathSegment();
                }

                String mimeType = getMimeType(contextRef.get(), uri);
                return PersistentBlobProvider.getInstance().create(contextRef.get(), inputStream, mimeType, fileName, fileSize);
            } catch (NullPointerException | FileNotFoundException ioe) {
                Log.w(TAG, ioe);
                return null;
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
