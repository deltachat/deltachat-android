package org.thoughtcrime.securesms;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Process;
import android.provider.OpenableColumns;
import android.util.Log;

import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashSet;

import static org.thoughtcrime.securesms.util.MediaUtil.getMimeType;

public class ResolveMediaTask extends AsyncTask<Uri, Void, Uri> {

    private static final String TAG = ResolveMediaTask.class.getSimpleName();

    interface OnMediaResolvedListener {
            void onMediaResolved(Uri uri);
        }

        private WeakReference<Activity> contextRef;
        private WeakReference<OnMediaResolvedListener> listenerWeakReference;

        private static HashSet<ResolveMediaTask> instances = new HashSet<>();

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

                if (hasFileScheme(uri)) {
                    inputStream = this.openFileUri(uri);
                    if (uri.getPath() != null) {
                        File file = new File(uri.getPath());
                        fileName = file.getName();
                        fileSize = file.length();
                    }
                } else {
                    inputStream = contextRef.get().getContentResolver().openInputStream(uri);
                }

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
            } catch (NullPointerException | IOException ioe) {
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

        private InputStream openFileUri(Uri uri) throws IOException {
            FileInputStream fin = new FileInputStream(uri.getPath());
            int owner = FileUtils.getFileDescriptorOwner(fin.getFD());


            if (owner == -1 || owner == Process.myUid()) {
                fin.close();
                throw new IOException("File owned by application");
            }

            return fin;
        }


    private boolean hasFileScheme(Uri uri) {
        if (uri == null) {
            return false;
        }
        return "file".equals(uri.getScheme());
    }

}
