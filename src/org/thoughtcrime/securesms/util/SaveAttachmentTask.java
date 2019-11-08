package org.thoughtcrime.securesms.util;

import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.media.MediaScannerConnection;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.database.NoExternalStorageException;
import org.thoughtcrime.securesms.mms.PartAuthority;
import org.thoughtcrime.securesms.util.task.ProgressDialogAsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;

public class SaveAttachmentTask extends ProgressDialogAsyncTask<SaveAttachmentTask.Attachment, Void, Pair<Integer, String>> {
  private static final String TAG = SaveAttachmentTask.class.getSimpleName();

          static final int SUCCESS              = 0;
  private static final int FAILURE              = 1;
  private static final int WRITE_ACCESS_FAILURE = 2;

  private final WeakReference<Context>      contextReference;

  private final int attachmentCount;

  public SaveAttachmentTask(Context context) {
    this(context, 1);
  }

  public SaveAttachmentTask(Context context, int count) {
    super(context,
          context.getResources().getString(R.string.one_moment),
          context.getResources().getString(R.string.one_moment));
    this.contextReference      = new WeakReference<>(context);
    this.attachmentCount       = count;
  }

  @Override
  protected Pair<Integer, String> doInBackground(SaveAttachmentTask.Attachment... attachments) {
    if (attachments == null || attachments.length == 0) {
      throw new AssertionError("must pass in at least one attachment");
    }

    try {
      Context      context      = contextReference.get();
      String       directory    = null;

      if (!StorageUtil.canWriteInSignalStorageDir()) {
        return new Pair<>(WRITE_ACCESS_FAILURE, null);
      }

      if (context == null) {
        return new Pair<>(FAILURE, null);
      }

      for (Attachment attachment : attachments) {
        if (attachment != null) {
          directory = saveAttachment(context, attachment);
          if (directory == null) return new Pair<>(FAILURE, null);
        }
      }

      if (attachments.length > 1) return new Pair<>(SUCCESS, null);
      else                        return new Pair<>(SUCCESS, directory);
    } catch (NoExternalStorageException|IOException ioe) {
      Log.w(TAG, ioe);
      return new Pair<>(FAILURE, null);
    }
  }

  private @Nullable String saveAttachment(Context context, Attachment attachment)
      throws NoExternalStorageException, IOException
  {
    String      contentType = MediaUtil.getCorrectedMimeType(attachment.contentType);
    String         fileName = attachment.fileName;

    if (fileName == null) fileName = generateOutputFileName(contentType, attachment.date);
    fileName = sanitizeOutputFileName(fileName);

    File    outputDirectory = createOutputDirectoryFromContentType(contentType);
    File          mediaFile = createOutputFile(outputDirectory, fileName);
    InputStream inputStream = PartAuthority.getAttachmentStream(context, attachment.uri);

    if (inputStream == null) {
      return null;
    }

    OutputStream outputStream = new FileOutputStream(mediaFile);
    Util.copy(inputStream, outputStream);

    MediaScannerConnection.scanFile(context, new String[]{mediaFile.getAbsolutePath()},
                                    new String[]{contentType}, null);

    return outputDirectory.getName();
  }

  private File createOutputDirectoryFromContentType(@NonNull String contentType)
      throws NoExternalStorageException
  {
    File outputDirectory;

    if (contentType.startsWith("video/")) {
      outputDirectory = StorageUtil.getVideoDir();
    } else if (contentType.startsWith("audio/")) {
      outputDirectory = StorageUtil.getAudioDir();
    } else if (contentType.startsWith("image/")) {
      outputDirectory = StorageUtil.getImageDir();
    } else {
      outputDirectory = StorageUtil.getDownloadDir();
    }

    if (!outputDirectory.mkdirs()) Log.w(TAG, "mkdirs() returned false, attempting to continue");
    return outputDirectory;
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

  private File createOutputFile(@NonNull File outputDirectory, @NonNull String fileName)
      throws IOException
  {
    String[] fileParts = getFileNameParts(fileName);
    String base = fileParts[0];
    String extension = fileParts[1];

    File outputFile = new File(outputDirectory, base + "." + extension);

    int i = 0;
    while (outputFile.exists()) {
      outputFile = new File(outputDirectory, base + "-" + (++i) + "." + extension);
    }

    if (outputFile.isHidden()) {
      throw new IOException("Specified name would not be visible");
    }

    return outputFile;
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
  protected void onPostExecute(final Pair<Integer, String> result) {
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
        Toast.makeText(context,
                       context.getResources().getString(R.string.done),
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
    showWarningDialog(context, onAcceptListener, 1);
  }

  public static void showWarningDialog(Context context, OnClickListener onAcceptListener, int count) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setCancelable(true);
    builder.setMessage(R.string.ask_export_attachment);
    builder.setPositiveButton(R.string.yes, onAcceptListener);
    builder.setNegativeButton(R.string.no, null);
    builder.show();
  }
}

