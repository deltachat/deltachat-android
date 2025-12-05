package org.thoughtcrime.securesms.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.loader.content.CursorLoader;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.mms.PartAuthority;
import org.thoughtcrime.securesms.util.task.ProgressDialogAsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class SaveAttachmentTask extends ProgressDialogAsyncTask<SaveAttachmentTask.Attachment, Void, Pair<Integer, Uri>> {
  private static final String TAG = SaveAttachmentTask.class.getSimpleName();

          static final int SUCCESS              = 0;
  private static final int FAILURE              = 1;
  private static final int WRITE_ACCESS_FAILURE = 2;

  private final WeakReference<Context>      contextReference;

  public SaveAttachmentTask(Context context) {
    super(context,
          context.getResources().getString(R.string.one_moment),
          context.getResources().getString(R.string.one_moment));
    this.contextReference      = new WeakReference<>(context);
  }

  @Override
  protected Pair<Integer, Uri> doInBackground(SaveAttachmentTask.Attachment... attachments) {
    if (attachments == null || attachments.length == 0) {
      throw new AssertionError("must pass in at least one attachment");
    }

    try {
      Context      context      = contextReference.get();
      Uri          uri          = null;

      if (!StorageUtil.canWriteToMediaStore(context)) {
        return new Pair<>(WRITE_ACCESS_FAILURE, null);
      }

      if (context == null) {
        return new Pair<>(FAILURE, null);
      }

      for (Attachment attachment : attachments) {
        if (attachment != null) {
          uri = saveAttachment(context, attachment);
          if (uri == null) return new Pair<>(FAILURE, null);
        }
      }
      if (attachments.length > 1) return new Pair<>(SUCCESS, null);
      else                        return new Pair<>(SUCCESS, uri);
    } catch (IOException ioe) {
      Log.w(TAG, ioe);
      return new Pair<>(FAILURE, null);
    }
  }

  private @Nullable Uri saveAttachment(Context context, Attachment attachment) throws IOException
  {
    String      contentType = Objects.requireNonNull(MediaUtil.getCorrectedMimeType(attachment.contentType));
    String         fileName = attachment.fileName;

    if (fileName == null) fileName = generateOutputFileName(contentType, attachment.date);
    fileName = sanitizeOutputFileName(fileName);

    Uri           outputUri    = getMediaStoreContentUriForType(contentType);
    Uri           mediaUri     = createOutputUri(outputUri, contentType, fileName);
    ContentValues updateValues = new ContentValues();

    if (mediaUri == null) {
      Log.w(TAG, "Failed to create mediaUri for " + contentType);
      return null;
    }

    try (InputStream inputStream = PartAuthority.getAttachmentStream(context, attachment.uri)) {

      if (inputStream == null) {
        return null;
      }

      if (Util.equals(outputUri.getScheme(), ContentResolver.SCHEME_FILE)) {
        try (OutputStream outputStream = new FileOutputStream(mediaUri.getPath())) {
          StreamUtil.copy(inputStream, outputStream);
          MediaScannerConnection.scanFile(context, new String[]{mediaUri.getPath()}, new String[]{contentType}, null);
        }
      } else {
        try (OutputStream outputStream = context.getContentResolver().openOutputStream(mediaUri, "w")) {
          long total = StreamUtil.copy(inputStream, outputStream);
          if (total > 0) {
            updateValues.put(MediaStore.MediaColumns.SIZE, total);
          }
        }
      }
    }

    if (Build.VERSION.SDK_INT > 28) {
      updateValues.put(MediaStore.MediaColumns.IS_PENDING, 0);
    }

    if (updateValues.size() > 0) {
      getContext().getContentResolver().update(mediaUri, updateValues, null, null);
    }

    return mediaUri;
  }

  private @Nullable String getRealPathFromURI(Uri contentUri) {
    String[] proj = {MediaStore.MediaColumns.DATA};
    CursorLoader loader = new CursorLoader(getContext(), contentUri, proj, null, null, null);
    Cursor cursor = loader.loadInBackground();
    int column_index = 0;
    String result = null;
    if (cursor != null) {
      column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
      cursor.moveToFirst();
      result = cursor.getString(column_index);
      cursor.close();
    }
    return result;
  }

  private @NonNull Uri getMediaStoreContentUriForType(@NonNull String contentType) {
    if (contentType.startsWith("video/")) {
      return StorageUtil.getVideoUri();
    } else if (contentType.startsWith("audio/")) {
      return StorageUtil.getAudioUri();
    } else if (isMediaStoreImageType(contentType)) {
      return StorageUtil.getImageUri();
    } else {
      return StorageUtil.getDownloadUri();
    }
  }

  /**
   * Checks if the content type is a standard image format supported by Android's MediaStore.
   * Non-standard image formats (like XCF, PSD, etc.) should be saved to Downloads instead.
   */
  private boolean isMediaStoreImageType(@NonNull String contentType) {
    if (!contentType.startsWith("image/")) {
      return false;
    }
    return contentType.equals("image/jpeg") ||
      contentType.equals("image/jpg") ||
      contentType.equals("image/png") ||
      contentType.equals("image/gif") ||
      contentType.equals("image/webp") ||
      contentType.equals("image/bmp") ||
      contentType.equals("image/heic") ||
      contentType.equals("image/heif") ||
      contentType.equals("image/avif");
  }

  private @Nullable File ensureExternalPath(@Nullable File path) {
    if (path != null && path.exists()) {
      return path;
    }

    if (path == null) {
      File documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
      if (documents.exists() || documents.mkdirs()) {
        return documents;
      } else {
        return null;
      }
    }

    if (path.mkdirs()) {
      return path;
    } else {
      return null;
    }
  }

  /**
   * Returns a path to a shared media (or documents) directory for the type of the file.
   *
   * Note that this method attempts to create a directory if the path returned from
   * Environment object does not exist yet. The attempt may fail in which case it attempts
   * to return the default "Document" path. It finally returns null if it also fails.
   * Otherwise it returns the absolute path to the directory.
   *
   * @param contentType a MIME type of a file
   * @return an absolute path to a directory or null
   */
  private @Nullable String getExternalPathForType(String contentType) {
    File storage = null;
    if (contentType.startsWith("video/")) {
      storage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
    } else if (contentType.startsWith("audio/")) {
      storage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
    } else if (isMediaStoreImageType(contentType)) {
      storage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
    }

    storage = ensureExternalPath(storage);
    if (storage == null) {
      return null;
    }

    return storage.getAbsolutePath();
  }

  private String generateOutputFileName(@NonNull String contentType, long timestamp) {
    String           extension     = MediaUtil.getExtensionFromMimeType(contentType);
    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
    String           base          = "deltachat-" + dateFormatter.format(timestamp);

    if (extension == null) extension = "attach";

    return base + "." + extension;
  }

  private String sanitizeOutputFileName(@NonNull String fileName) {
    return new File(fileName).getName();
  }

  private @Nullable Uri createOutputUri(@NonNull Uri outputUri, @NonNull String contentType, @NonNull String fileName)
          throws IOException
  {
    String[] fileParts = getFileNameParts(fileName);
    String   base      = fileParts[0];
    String   extension = fileParts[1];
    String   mimeType  = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

    if (MediaUtil.isOctetStream(mimeType) && MediaUtil.isImageVideoOrAudioType(contentType)) {
      Log.d(TAG, "MimeTypeMap returned octet stream for media, changing to provided content type [" + contentType + "] instead.");
      mimeType = contentType;
    }

    ContentValues contentValues = new ContentValues();
    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
    contentValues.put(MediaStore.MediaColumns.DATE_ADDED, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
    contentValues.put(MediaStore.MediaColumns.DATE_MODIFIED, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));

    if (Build.VERSION.SDK_INT > 28) {
      contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1);
    } else if (Util.equals(outputUri.getScheme(), ContentResolver.SCHEME_FILE)) {
      File outputDirectory = new File(outputUri.getPath());
      File outputFile      = new File(outputDirectory, base + "." + extension);

      int i = 0;
      while (outputFile.exists()) {
        outputFile = new File(outputDirectory, base + "-" + (++i) + "." + extension);
      }

      if (outputFile.isHidden()) {
        throw new IOException("Specified name would not be visible");
      }

      return Uri.fromFile(outputFile);
    } else {
      String dir = getExternalPathForType(contentType);
      if (dir == null) {
        throw new IOException(String.format(Locale.ENGLISH, "Path for type: %s was not available", contentType));
      }

      String outputFileName = fileName;
      String dataPath       = String.format("%s/%s", dir, outputFileName);
      int    i              = 0;
      while (pathTaken(outputUri, dataPath)) {
        Log.d(TAG, "The content exists. Rename and check again.");
        outputFileName = base + "-" + (++i) + "." + extension;
        dataPath       = String.format("%s/%s", dir, outputFileName);
      }
      contentValues.put(MediaStore.MediaColumns.DATA, dataPath);
    }

    return getContext().getContentResolver().insert(outputUri, contentValues);
  }

  private boolean pathTaken(@NonNull Uri outputUri, @NonNull String dataPath) throws IOException {
    try (Cursor cursor = getContext().getContentResolver().query(outputUri,
            new String[] { MediaStore.MediaColumns.DATA },
            MediaStore.MediaColumns.DATA + " = ?",
            new String[] { dataPath },
            null))
    {
      if (cursor == null) {
        throw new IOException("Something is wrong with the filename to save");
      }
      return cursor.moveToFirst();
    }
  }

  private String[] getFileNameParts(String fileName) {
    String[] result = new String[2];
    String[] tokens = fileName.split("\\.(?=[^\\.]+$)");

    result[0] = tokens[0];

    if (tokens.length > 1) result[1] = tokens[1];
    else                   result[1] = "";

    return result;
  }

  @Override
  protected void onPostExecute(final Pair<Integer, Uri> result) {
    super.onPostExecute(result);
    final Context context = contextReference.get();
    if (context == null) return;

    switch (result.first()) {
      case FAILURE:
        Toast.makeText(context,
                       context.getResources().getString(R.string.error),
                       Toast.LENGTH_LONG).show();
        break;
      case SUCCESS:
        Uri uri = result.second();
        String dir;

        if (uri == null) {
          dir = null;
        } else {
          String path = getRealPathFromURI(uri);
          if (path != null) uri = Uri.parse(path);

          List<String> segments = uri.getPathSegments();
          if (segments.size() >= 2) {
            dir = segments.get(segments.size() - 2);
          } else {
            dir = uri.getPath();
          }
        }

        Toast.makeText(context,
                       dir==null? context.getString(R.string.done) : context.getString(R.string.file_saved_to, dir),
                       Toast.LENGTH_LONG).show();
        break;
      case WRITE_ACCESS_FAILURE:
        Toast.makeText(context, R.string.error,
            Toast.LENGTH_LONG).show();
        break;
    }
  }

  public static class Attachment {
    public Uri    uri;
    public String fileName;
    public String contentType;
    public long   date;

    public Attachment(@NonNull Uri uri, @NonNull String contentType,
                      long date, @Nullable String fileName)
    {
      if (uri == null || contentType == null || date < 0) {
        throw new AssertionError("uri, content type, and date must all be specified");
      }
      this.uri         = uri;
      this.fileName    = fileName;
      this.contentType = contentType;
      this.date        = date;
    }
  }

  public static void showWarningDialog(Context context, OnClickListener onAcceptListener) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setCancelable(true);
    builder.setMessage(R.string.ask_export_attachment);
    builder.setPositiveButton(R.string.yes, onAcceptListener);
    builder.setNegativeButton(R.string.no, null);
    builder.show();
  }
}

